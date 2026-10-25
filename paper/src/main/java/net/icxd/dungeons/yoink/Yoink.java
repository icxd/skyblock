package net.icxd.dungeons.yoink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.bank.BankAccount;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dwarven.Perk;
import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.fairysouls.FairySouls;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.SkyBlockMenuItem;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.storage.AccessoryBag;
import net.icxd.dungeons.storage.Bag;
import net.icxd.dungeons.storage.StatsTuning;
import net.icxd.dungeons.storage.Storage;
import net.icxd.dungeons.storage.StorageDocument;
import net.icxd.dungeons.storage.StorageMenu;
import net.icxd.dungeons.storage.StorageTables;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Copies the picked parts of a real profile onto the one a player plays on, each replacing that part of theirs
 * (see {@link YoinkPart}), then saves and says what came over and what couldn't. The profile's numbers are written
 * as they are, its items through {@link HypixelItems}: the inventory onto the player, the rest into their storage
 * as its menus keep it (see StorageDocument). A big profile's storage is a thousand-odd items, which take a second
 * or two to make and store, so that's done off the main thread ({@link #prepare}, as plain data: those items have
 * no owner yet, so making them reads nothing of the player's) and everything is written on it at once
 * ({@link #apply}), if they're still on the profile it was for.
 */
public final class Yoink {
    /** Hypixel's wardrobe pages: 9 sets across, their helmets in the top row and boots in the bottom. */
    static final int WARDROBE_PAGE = 36;
    static final int WARDROBE_SETS_PER_PAGE = 9;
    /** Hypixel's Stats Tuning by its keys, as this plugin's stats (see StatsTuning). */
    private static final Map<String, String> TUNING = Map.of("health", "HEALTH", "defense", "DEFENSE", "walk_speed", "SPEED",
            "strength", "STRENGTH", "critical_damage", "CRIT_DAMAGE", "critical_chance", "CRIT_CHANCE", "attack_speed", "ATTACK_SPEED",
            "intelligence", "INTELLIGENCE");
    /** The Heart of the Mountain's perks here, by Hypixel's node ids (SkyCrypt's constants/hotm.js). */
    private static final Map<Perk, String> NODES = Map.of(Perk.MINING_SPEED, "mining_speed", Perk.MINING_FORTUNE, "mining_fortune",
            Perk.TITANIUM_INSANIUM, "titanium_insanium", Perk.QUICK_FORGE, "forge_time");
    /** Powder.CAP. */
    private static final long MOST_POWDER = 2_000_000_000L;
    /** Kuudra's tiers by the API's keys in {@code kuudra_completed_tiers} (NEU's CrimsonIslePage), easiest first. */
    private static final List<String> KUUDRA_TIERS = List.of("none", "hot", "burning", "fiery", "infernal");
    private static final List<String> KUUDRA_NAMES = List.of("Basic", "Hot", "Burning", "Fiery", "Infernal");
    private static final Set<YoinkPart> STORED = EnumSet.of(YoinkPart.EQUIPMENT, YoinkPart.WARDROBE, YoinkPart.ENDER_CHEST,
            YoinkPart.BACKPACKS, YoinkPart.ACCESSORIES, YoinkPart.BAGS);
    private static final int SHOWN_LEFT = 12;
    /** Who's yoinking now: one at a time each. */
    private static final Set<UUID> YOINKING = ConcurrentHashMap.newKeySet();

    private final UUID player;
    private final User user;
    /** The profile it goes onto: if they're on another by then, nothing is written. */
    private final Document profile;
    private final HypixelProfile from;
    private final Set<YoinkPart> parts;
    /** What each part brought, for the summary ("Skills: 12 of 12"). */
    private final Map<YoinkPart, String> done = new EnumMap<>(YoinkPart.class);
    private final Set<YoinkPart> failed = EnumSet.noneOf(YoinkPart.class);
    /** What each storage part writes, made ready off the main thread: into the profile and its storage. */
    private final Map<YoinkPart, BiConsumer<Document, Document>> writes = new EnumMap<>(YoinkPart.class);
    private final HypixelItems.Left left = new HypixelItems.Left();
    private List<HypixelItems.Item> inventory = List.of();
    private List<HypixelItems.Item> armor = List.of();

    private Yoink(UUID player, User user, HypixelProfile from, Set<YoinkPart> parts) {
        this.player = player;
        this.user = user;
        this.profile = user.profile();
        this.from = from;
        this.parts = parts;
    }

    /** Copies these parts of {@code from} (a profile of {@code owner}'s) onto the player's, and tells them how it went. */
    public static void run(Player player, String owner, HypixelProfile from, Collection<YoinkPart> picked) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        if (InventorySyncListener.frozen(player)) {
            player.sendMessage(Text.line("&cYou can't yoink a profile right now!"));
            return;
        }
        Set<YoinkPart> parts = EnumSet.noneOf(YoinkPart.class);
        for (YoinkPart part : picked) if (part.available(from)) parts.add(part);
        if (parts.isEmpty()) return;
        UUID id = player.getUniqueId();
        if (!YOINKING.add(id)) {
            player.sendMessage(Text.line("&cYou're already yoinking a profile!"));
            return;
        }
        player.sendMessage(Text.line("&7Yoinking &b" + from.name() + "&7..."));
        Yoink yoink = new Yoink(id, user, from, parts);
        Plugin plugin = Dungeons.getInstance();
        long start = System.nanoTime();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean prepared = false;
            try {
                yoink.prepare();
                prepared = true;
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Yoinking " + from.name() + " for " + player.getName(), e);
            } finally {
                // Half made, nothing is written (an inventory not read yet would empty theirs).
                boolean ready = prepared;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (ready) yoink.apply(owner, start);
                        else if (Bukkit.getPlayer(id) instanceof Player online) {
                            online.sendMessage(Text.line("&cSomething went wrong yoinking, so nothing was yoinked."));
                        }
                    } finally {
                        YOINKING.remove(id);
                    }
                });
            }
        });
    }

    /** "NOT_AN_ITEM x3, GOLDEN_DRAGON Pet and 4 more", the most first. */
    static String summary(Map<String, Integer> counts) {
        List<String> shown = counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(SHOWN_LEFT).map(e -> e.getKey() + (e.getValue() > 1 ? " x" + e.getValue() : "")).toList();
        return String.join("&8, &7", shown) + (counts.size() > SHOWN_LEFT ? " &8and " + (counts.size() - SHOWN_LEFT) + " more" : "");
    }

    /** Off the main thread: the storage parts' items, made and stored, and the inventory's as data. */
    private void prepare() {
        for (YoinkPart part : parts) {
            try {
                switch (part) {
                    case INVENTORY -> {
                        // Theirs at once: what they're for decides the requirement lines (made on the main thread).
                        inventory = items("inventory.inv_contents", player);
                        armor = items("inventory.inv_armor", player);
                    }
                    case EQUIPMENT -> equipment();
                    case WARDROBE -> wardrobe();
                    case ENDER_CHEST -> enderChest();
                    case BACKPACKS -> backpacks();
                    case ACCESSORIES -> accessories();
                    case BAGS -> bags();
                    default -> {
                    }
                }
            } catch (RuntimeException e) {
                fail(part, e);
            }
        }
    }

    /** On the main thread: everything written, saved and said, if they're still on the profile. */
    private void apply(String owner, long start) {
        long applying = System.nanoTime();
        Player online = Bukkit.getPlayer(player);
        if (online == null || User.ifLoaded(player) != user || user.isReleased()) return;
        if (user.profile() != profile || InventorySyncListener.frozen(online)) {
            online.sendMessage(Text.line("&cYour profile changed while yoinking, so nothing was yoinked."));
            return;
        }
        // An open storage menu writes its items back as it closes: before these, not over them.
        online.closeInventory();
        Document storage = StoredInventory.storage(profile);
        for (YoinkPart part : parts) {
            if (failed.contains(part)) continue;
            try {
                switch (part) {
                    case INVENTORY -> inventory(online);
                    case COINS -> coins(profile);
                    case SKILLS -> skills(profile);
                    case COLLECTIONS -> collections(profile);
                    case DUNGEONS -> dungeons(profile);
                    case MINING -> mining(profile);
                    case CRIMSON_ISLE -> crimsonIsle(profile);
                    case MINIONS -> minions(profile);
                    case FAIRY_SOULS -> fairySouls(profile);
                    default -> writes.get(part).accept(profile, storage);
                }
            } catch (RuntimeException e) {
                fail(part, e);
            }
        }
        if (parts.stream().anyMatch(STORED::contains)) Storage.changed(online);
        // Made again now that everything's in: their requirement lines go by the skills and floors just yoinked.
        ItemBuilder.refreshInventory(online);
        SkyBlockMenuItem.give(online);
        user.save();
        Dungeons.getInstance().getLogger().info(online.getName() + " yoinked " + from.name() + " from " + owner + " onto " + user.profileName()
                + " " + parts + " in " + (System.nanoTime() - start) / 1_000_000 + " ms (" + (System.nanoTime() - applying) / 1_000_000
                + " on the main thread)");
        online.sendMessage(Text.line("&aYoinked &b" + from.name() + " &afrom " + owner + " onto &e" + user.profileName() + "&a:"));
        for (String line : done.values()) online.sendMessage(Text.line("&8 - &7" + line));
        if (!left.items.isEmpty()) online.sendMessage(Text.line("&eNot brought over: &7" + summary(left.items)));
        if (!left.enchantments.isEmpty()) online.sendMessage(Text.line("&eEnchantments this plugin doesn't have: &7" + summary(left.enchantments)));
        online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
    }

    private void fail(YoinkPart part, RuntimeException e) {
        failed.add(part);
        done.put(part, "&c" + part.title() + ": failed (" + e.getMessage() + ")");
    }

    // Items

    /** The inventory and armor onto the player (a save stores what they have on them); the SkyBlock Menu is ours. */
    private void inventory(Player online) {
        PlayerInventory slots = online.getInventory();
        slots.setStorageContents(HypixelItems.stacks(inventory, slots.getStorageContents().length));
        slots.setArmorContents(HypixelItems.stacks(armor, 4));
        done.put(YoinkPart.INVENTORY, "Inventory: " + count(inventory, "item") + " and " + count(armor, "armor piece"));
    }

    /** What they wear (necklace, cloak, belt, gloves, as here), in no Equipment Set. */
    private void equipment() {
        List<HypixelItems.Item> pieces = items("inventory.equipment_contents", null);
        List<Binary> stored = blobs(pieces, 4);
        writes.put(YoinkPart.EQUIPMENT, (profile, storage) -> {
            storage.put(StorageDocument.EQUIPMENT, stored);
            StorageDocument.setWorn(storage, StorageDocument.WORN_EQUIPMENT_SET, -1);
        });
        done.put(YoinkPart.EQUIPMENT, "Equipment: " + count(pieces, "piece"));
    }

    /**
     * The wardrobe's sets, from set 1 as here. The set they wear on Hypixel is empty in the wardrobe (its pieces are
     * their armor): it's the one worn here too when their inventory comes over, else its pieces go back in it.
     */
    private void wardrobe() {
        List<List<HypixelItems.Item>> sets = wardrobeSets(items("inventory.wardrobe_contents", null));
        JsonElement equipped = from.at("inventory.wardrobe_equipped_slot");
        int worn = equipped != null && equipped.isJsonPrimitive() ? equipped.getAsInt() - 1 : -1;
        if (worn >= sets.size() || worn >= 0 && sets.get(worn).stream().anyMatch(Objects::nonNull)) worn = -1;
        if (worn >= 0 && !parts.contains(YoinkPart.INVENTORY)) {
            List<HypixelItems.Item> wearing = items("inventory.inv_armor", null);
            List<HypixelItems.Item> pieces = new ArrayList<>(4);
            // Their armor is boots first; a set is helmet first.
            for (int piece = 3; piece >= 0; piece--) pieces.add(piece < wearing.size() ? wearing.get(piece) : null);
            sets.set(worn, pieces);
            worn = -1;
        }
        List<Object> stored = new ArrayList<>(sets.size());
        int full = 0;
        for (List<HypixelItems.Item> set : sets) {
            stored.add(blobs(set, 4));
            if (set.stream().anyMatch(Objects::nonNull)) full++;
        }
        int wornSet = worn;
        writes.put(YoinkPart.WARDROBE, (profile, storage) -> {
            storage.put(StorageDocument.ARMOR_SETS, stored);
            StorageDocument.setWorn(storage, StorageDocument.WORN_ARMOR_SET, wornSet);
        });
        done.put(YoinkPart.WARDROBE, "Wardrobe: " + plural(full, "set") + (worn >= 0 ? ", wearing set " + (worn + 1) : ""));
    }

    /** Hypixel's wardrobe slots as sets of four, helmet first. */
    static <T> List<List<T>> wardrobeSets(List<T> slots) {
        int pages = (slots.size() + WARDROBE_PAGE - 1) / WARDROBE_PAGE;
        List<List<T>> sets = new ArrayList<>(pages * WARDROBE_SETS_PER_PAGE);
        for (int set = 0; set < pages * WARDROBE_SETS_PER_PAGE; set++) {
            int page = set / WARDROBE_SETS_PER_PAGE;
            int column = set % WARDROBE_SETS_PER_PAGE;
            List<T> pieces = new ArrayList<>(4);
            for (int row = 0; row < 4; row++) {
                int slot = page * WARDROBE_PAGE + row * WARDROBE_SETS_PER_PAGE + column;
                pieces.add(slot < slots.size() ? slots.get(slot) : null);
            }
            sets.add(pieces);
        }
        return sets;
    }

    /** Its pages, 45 slots each as here. */
    private void enderChest() {
        List<HypixelItems.Item> slots = items("inventory.ender_chest_contents", null);
        List<Object> pages = new ArrayList<>();
        for (int first = 0; first < slots.size(); first += StorageMenu.PAGE_SIZE) {
            pages.add(blobs(slots.subList(first, Math.min(slots.size(), first + StorageMenu.PAGE_SIZE)), StorageMenu.PAGE_SIZE));
        }
        writes.put(YoinkPart.ENDER_CHEST, (profile, storage) -> storage.put(StorageDocument.ENDER_CHEST, pages));
        done.put(YoinkPart.ENDER_CHEST, "Ender Chest: " + count(slots, "item") + " on " + plural(pages.size(), "page"));
    }

    /** Each backpack (its icon is the backpack itself) in the same slot, from slot 1 as here, with what's in it. */
    private void backpacks() {
        // Laid out as in a storage of their own, then put in theirs whole.
        Document laidOut = new Document();
        JsonObject icons = HypixelProfile.object(HypixelProfile.object(from.member(), "inventory"), "backpack_icons");
        int backpacks = 0;
        int items = 0;
        for (String key : icons == null ? Set.<String>of() : icons.keySet()) {
            int slot;
            try {
                slot = Integer.parseInt(key) + 1;
            } catch (NumberFormatException e) {
                continue;
            }
            List<HypixelItems.Item> icon = items("inventory.backpack_icons." + key, null);
            List<HypixelItems.Item> inside = items("inventory.backpack_contents." + key, null);
            HypixelItems.Item backpack = icon.isEmpty() ? null : icon.getFirst();
            if (backpack == null || slot > StorageMenu.BACKPACK_SLOTS) {
                // Nowhere for what's in it to go.
                for (HypixelItems.Item item : inside) if (item != null) left.item(item.kind().id(), item.amount());
                continue;
            }
            StorageDocument.placeBackpack(laidOut, slot, backpack.kind().id(), blobs(List.of(backpack), 1).getFirst());
            StorageDocument.setBackpackContents(laidOut, slot, blobs(inside, inside.size()));
            backpacks++;
            items += count(inside);
        }
        Object placed = laidOut.get(StorageDocument.BACKPACKS);
        writes.put(YoinkPart.BACKPACKS, (profile, storage) -> storage.put(StorageDocument.BACKPACKS, placed == null ? new Document() : placed));
        done.put(YoinkPart.BACKPACKS, "Backpacks: " + backpacks + ", with " + plural(items, "item") + " in them");
    }

    /** The Accessory Bag, with the power they picked and their Stats Tuning. */
    private void accessories() {
        List<HypixelItems.Item> accessories = items("inventory.bag_contents.talisman_bag", null);
        List<Binary> stored = blobs(accessories, accessories.size());
        JsonObject bag = HypixelProfile.object(from.member(), "accessory_bag_storage");
        StorageTables.Power power = power(bag != null && bag.get("selected_power") instanceof JsonElement e && e.isJsonPrimitive() ? e.getAsString() : null);
        JsonObject tuning = HypixelProfile.object(HypixelProfile.object(bag, "tuning"), "slot_0");
        int highest = (int) Math.max(0, number(bag, "highest_magical_power"));
        Document points = new Document();
        int total = 0;
        for (Map.Entry<String, String> stat : TUNING.entrySet()) {
            int n = (int) Math.max(0, number(tuning, stat.getKey()));
            if (n > 0) points.put(stat.getValue(), n);
            total += n;
        }
        writes.put(YoinkPart.ACCESSORIES, (profile, storage) -> {
            storage.put(Bag.ACCESSORY_BAG.key(), stored);
            // The most Accessory Power their bag has had, as the API keeps it; the bag here can only raise it.
            if (highest > 0) profile.put(AccessoryBag.HIGHEST_POWER, highest);
            else profile.remove(AccessoryBag.HIGHEST_POWER);
            if (power == null) profile.remove(AccessoryBag.SELECTED_POWER);
            else profile.put(AccessoryBag.SELECTED_POWER, power.name());
            profile.put(StatsTuning.FIELD, points);
        });
        done.put(YoinkPart.ACCESSORIES, "Accessory Bag: " + count(accessories, "accessory") + ", " + (power == null ? "no power" : power.name())
                + ", " + plural(total, "tuning point"));
    }

    /** A power by Hypixel's id ("silky"), as the tables name it ("Silky"); null for none or one the tables haven't. */
    private static StorageTables.Power power(String id) {
        if (id == null || id.isBlank()) return null;
        String wanted = id.replace("_", "").toLowerCase(Locale.ROOT);
        for (StorageTables.Power power : StorageTables.get().powers().values()) {
            if (power.name().replace(" ", "").toLowerCase(Locale.ROOT).equals(wanted)) return power;
        }
        return null;
    }

    /** The Potion Bag, the Fishing Bag and the Sack of Sacks (there's no Quiver here: what's in it is left). */
    private void bags() {
        Map<String, List<Binary>> bags = new LinkedHashMap<>();
        List<String> counts = new ArrayList<>();
        for (Map.Entry<String, Bag> bag : Map.of("potion_bag", Bag.POTION_BAG, "fishing_bag", Bag.FISHING_BAG, "sacks_bag", Bag.SACK_OF_SACKS)
                .entrySet()) {
            List<HypixelItems.Item> items = items("inventory.bag_contents." + bag.getKey(), null);
            bags.put(bag.getValue().key(), blobs(items, items.size()));
            counts.add(bag.getValue().displayName() + " " + count(items));
        }
        for (HypixelItems.Item arrow : items("inventory.bag_contents.quiver", null)) if (arrow != null) left.item(arrow.kind().id(), arrow.amount());
        counts.sort(null);
        writes.put(YoinkPart.BAGS, (profile, storage) -> bags.forEach(storage::put));
        done.put(YoinkPart.BAGS, "Bags: " + String.join(", ", counts));
    }

    /**
     * The items in one of the API's blobs at this path ({@code inventory.inv_contents}); none if it isn't there. Those
     * that are owned are {@code owner}'s; with none, nobody's yet (they get one as items here do, see SkyBlockItem#isOwnable).
     */
    private List<HypixelItems.Item> items(String path, UUID owner) {
        JsonElement data = from.at(path + ".data");
        if (data == null || !data.isJsonPrimitive()) return List.of();
        try {
            return HypixelItems.read(data.getAsString(), owner, left);
        } catch (IOException e) {
            throw new IllegalStateException("its data can't be read: " + e.getMessage(), e);
        }
    }

    /** These as slots' blobs, {@code size} of them. */
    private static List<Binary> blobs(List<HypixelItems.Item> items, int size) {
        return StoredInventory.encode(HypixelItems.stacks(items, size));
    }

    private static int count(List<HypixelItems.Item> items) {
        int count = 0;
        for (HypixelItems.Item item : items) if (item != null) count++;
        return count;
    }

    /** "3 items", "1 item". */
    private static String count(List<HypixelItems.Item> items, String what) {
        return plural(count(items), what);
    }

    static String plural(int n, String what) {
        if (n == 1) return "1 " + what;
        return n + " " + (what.endsWith("y") ? what.substring(0, what.length() - 1) + "ies" : what + "s");
    }

    // Numbers

    private void coins(Document profile) {
        double purse = Math.max(0, from.purse());
        profile.put(Purse.FIELD, purse);
        String line = "Coins: &6" + Coins.format(purse) + " &7in the purse";
        if (from.bank() != null) {
            BankAccount.setBalance(profile, Math.max(0, from.bank()));
            line += ", &6" + Coins.format(from.bank()) + " &7in the bank";
        }
        done.put(YoinkPart.COINS, line);
    }

    private void skills(Document profile) {
        JsonObject experience = HypixelProfile.object(HypixelProfile.object(from.member(), "player_data"), "experience");
        Document skills = profile.get("skills") instanceof Document d ? d : new Document();
        int count = 0;
        for (Skill skill : Skill.values()) {
            JsonElement xp = experience == null ? null : experience.get("SKILL_" + skill.key().toUpperCase(Locale.ROOT));
            skills.put(skill.key(), xp != null && xp.isJsonPrimitive() ? Math.max(0, xp.getAsDouble()) : 0.0);
            if (xp != null) count++;
        }
        profile.put("skills", skills);
        done.put(YoinkPart.SKILLS, "Skills: " + count + " of " + Skill.values().length);
    }

    private void collections(Document profile) {
        JsonObject counts = HypixelProfile.object(from.member(), "collection");
        profile.remove(Collections.COUNTS);
        int known = 0;
        int unknown = 0;
        for (Map.Entry<String, JsonElement> entry : counts == null ? List.<Map.Entry<String, JsonElement>>of() : counts.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) continue;
            if (Collections.set(profile, entry.getKey(), entry.getValue().getAsLong()) != null) known++;
            else unknown++;
        }
        done.put(YoinkPart.COLLECTIONS, "Collections: " + known + (unknown > 0 ? " &8(" + unknown + " this plugin doesn't have)" : ""));
    }

    private void dungeons(Document profile) {
        JsonObject dungeons = HypixelProfile.object(from.member(), "dungeons");
        JsonObject types = HypixelProfile.object(dungeons, "dungeon_types");
        JsonObject catacombs = HypixelProfile.object(types, "catacombs");
        JsonObject master = HypixelProfile.object(types, "master_catacombs");
        JsonObject classes = HypixelProfile.object(dungeons, "player_classes");
        Document ours = profile.get("dungeons") instanceof Document d ? d : new Document();
        ours.put("catacombsExp", number(catacombs, "experience"));
        Document floors = ours.get("floors") instanceof Document d ? d : new Document();
        floors.put("highest", (int) Math.max(0, number(catacombs, "highest_tier_completed")));
        floors.put("masterHighest", (int) Math.max(0, number(master, "highest_tier_completed")));
        ours.put("floors", floors);
        Document classExp = new Document();
        for (DungeonClass dungeonClass : DungeonClass.values()) {
            classExp.put(dungeonClass.name(), number(HypixelProfile.object(classes, dungeonClass.name().toLowerCase(Locale.ROOT)), "experience"));
        }
        ours.put("classExp", classExp);
        if (dungeons != null && dungeons.has("selected_dungeon_class") && dungeons.get("selected_dungeon_class").isJsonPrimitive()) {
            String selected = dungeons.get("selected_dungeon_class").getAsString().toUpperCase(Locale.ROOT);
            for (DungeonClass dungeonClass : DungeonClass.values()) if (dungeonClass.name().equals(selected)) ours.put("selectedClass", selected);
        }
        JsonObject essences = HypixelProfile.object(HypixelProfile.object(from.member(), "currencies"), "essence");
        Document essence = new Document();
        for (EssenceType type : EssenceType.values()) {
            essence.put(type.name().toLowerCase(Locale.ROOT), (int) number(HypixelProfile.object(essences, type.name()), "current"));
        }
        ours.put("essence", essence);
        profile.put("dungeons", ours);
        done.put(YoinkPart.DUNGEONS, "Dungeons: Catacombs XP &b" + Text.number(number(catacombs, "experience")) + "&7, floor " + floors.get("highest")
                + ", master floor " + floors.get("masterHighest"));
    }

    /** Tokens left to spend, the perks this plugin has (to their most here) and powder; its XP as it is. */
    private void mining(Document profile) {
        JsonObject core = HypixelProfile.object(from.member(), "mining_core");
        Document mines = profile.get("dwarvenMines") instanceof Document d ? d : new Document();
        Document hotm = mines.get("hotm") instanceof Document d ? d : new Document();
        int tokens = (int) Math.max(0, number(core, "tokens") - number(core, "tokens_spent"));
        hotm.put("tokens", tokens);
        hotm.put("xp", Math.max(0, number(core, "experience")));
        JsonObject nodes = HypixelProfile.object(core, "nodes");
        Document tree = new Document();
        int perks = 0;
        for (Perk perk : Perk.values()) {
            int level = (int) Math.min(perk.getMaxLevel(), Math.max(0, number(nodes, NODES.get(perk))));
            tree.put(perk.name(), level);
            if (level > 0) perks++;
        }
        hotm.put("tree", tree);
        mines.put("hotm", hotm);
        Document powder = mines.get("powder") instanceof Document d ? d : new Document();
        for (PowderType type : PowderType.values()) {
            powder.put(type.name(), (int) Math.min(MOST_POWDER, Math.max(0, number(core, "powder_" + type.name().toLowerCase(Locale.ROOT)))));
        }
        mines.put("powder", powder);
        profile.put("dwarvenMines", mines);
        done.put(YoinkPart.MINING, "Heart of the Mountain: " + tokens + " tokens to spend, " + perks + " of " + Perk.values().length + " perks, &2"
                + Text.number(powder.getInteger(PowderType.MITHRIL.name())) + " Mithril &7and &d"
                + Text.number(powder.getInteger(PowderType.GEMSTONE.name())) + " Gemstone Powder");
    }

    /**
     * Both factions' reputation and the highest Kuudra tier beaten (1 for Basic to 5 for Infernal: one opens once the
     * one before it is beaten). Not the faction they picked: the tab list's lines for one aren't finished.
     */
    private void crimsonIsle(Document profile) {
        JsonObject nether = HypixelProfile.object(from.member(), "nether_island_player_data");
        Document isle = profile.get("crimsonIsle") instanceof Document d ? d : new Document();
        Document factions = isle.get("factions") instanceof Document d ? d : new Document();
        int mage = (int) number(nether, "mages_reputation");
        int barbarian = (int) number(nether, "barbarians_reputation");
        factions.put("mage", new Document("reputation", mage));
        factions.put("barbarian", new Document("reputation", barbarian));
        isle.put("factions", factions);
        JsonObject completed = HypixelProfile.object(nether, "kuudra_completed_tiers");
        int highest = 0;
        for (int i = 0; i < KUUDRA_TIERS.size(); i++) if (number(completed, KUUDRA_TIERS.get(i)) > 0) highest = i + 1;
        isle.put("kuudra", new Document("highest", highest));
        profile.put("crimsonIsle", isle);
        done.put(YoinkPart.CRIMSON_ISLE, "Crimson Isle: &5" + Text.number(mage) + " Mage &7and &c" + Text.number(barbarian)
                + " Barbarian &7reputation, " + (highest == 0 ? "no Kuudra tier beaten" : "Kuudra beaten up to " + KUUDRA_NAMES.get(highest - 1)));
    }

    private void minions(Document profile) {
        JsonElement crafted = from.at("player_data.crafted_generators");
        List<String> tiers = new ArrayList<>();
        if (crafted != null && crafted.isJsonArray()) for (JsonElement tier : crafted.getAsJsonArray()) tiers.add(tier.getAsString());
        Document minions = profile.get("minions") instanceof Document d ? d : new Document();
        minions.put("craftedMinions", tiers);
        profile.put("minions", minions);
        done.put(YoinkPart.MINIONS, "Crafted Minions: " + tiers.size());
    }

    /** How many were found and given to Tia. The API doesn't say which, so none of this map's count as found (FairySouls). */
    private void fairySouls(Document profile) {
        JsonObject souls = HypixelProfile.object(from.member(), "fairy_soul");
        int collected = (int) number(souls, "total_collected");
        int exchanges = (int) number(souls, "fairy_exchanges");
        FairySouls.yoinked(profile, collected, exchanges);
        done.put(YoinkPart.FAIRY_SOULS, "Fairy Souls: &d" + Text.number(collected) + " &7found, " + plural(exchanges, "exchange") + " with Tia");
    }

    private static double number(JsonObject in, String key) {
        return in != null && key != null && in.get(key) instanceof JsonElement e && e.isJsonPrimitive() ? e.getAsDouble() : 0;
    }
}
