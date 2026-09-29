package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.DungeonItems;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;

/**
 * What a player's accessories do (see {@link Accessories} for which count): the counted ones' stats, from
 * their Accessory Bag and their inventory, and the selected power's at the bag's Accessory Power (see
 * {@link Powers}), added to their stats (PlayerStats#addModifier). Worked out when the bag changes, and
 * at most once a second for what's in their inventory (accessories there count too), not every tick.
 * Main thread.
 */
public final class AccessoryBag {
    /** The profile's selected power, by name (see StorageTables#power). */
    public static final String SELECTED_POWER = "selectedPower";
    /** How often what's in their inventory is looked at again. */
    private static final long INVENTORY_MILLIS = 1_000;

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** A counted accessory's item and what's known of it. */
    private record Entry(ItemStack item, Accessories.Held held) {
    }

    /** One player's: the bag's accessories (read when it changes), and what came of them and their inventory. */
    private static final class State {
        Document profile;
        List<Entry> bag;
        long at;
        boolean inDungeon;
        int power;
        Stats stats = new Stats();
        /** The counted ones' ids, and each one's item (see {@link #counted}). */
        Set<String> ids = Set.of();
        Map<String, ItemStack> items = Map.of();
    }

    private AccessoryBag() {
    }

    /** Once, at startup. */
    static void register() {
        PlayerStats.addModifier((player, stats) -> stats.add(state(player).stats));
    }

    /** The bag's items changed (or the tables were read): read again next time. */
    static void changed(Player player) {
        STATES.remove(player.getUniqueId());
    }

    static void forget(UUID player) {
        STATES.remove(player);
    }

    /**
     * The Accessory Power of their bag's accessories (0 until their data is loaded), which every menu shows
     * and works the power out at (UNKNOWN: the recorded loadout pages showed Silky at 500 while Select Power
     * Stone and Stats Tuning said 571).
     */
    public static int accessoryPower(Player player) {
        return state(player).power;
    }

    /** Their selected power; null for none (or one the tables don't have). */
    public static StorageTables.Power selectedPower(Document profile) {
        return StorageTables.get().power(profile.get(SELECTED_POWER) instanceof String s ? s : null);
    }

    static void select(Document profile, String power) {
        if (power == null) profile.remove(SELECTED_POWER);
        else profile.put(SELECTED_POWER, power);
    }

    /** The powers they may pick (see Powers#unlocked). */
    static List<StorageTables.Power> unlockedPowers(Document profile) {
        return Powers.unlocked(Skills.level(profile, Skill.COMBAT), StorageTables.get());
    }

    /** How many slots their Accessory Bag has. */
    public static int capacity(Document profile) {
        return Bag.ACCESSORY_BAG.capacity(profile, StorageTables.get());
    }

    private static State state(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        State state = STATES.get(player.getUniqueId());
        if (user == null) return state == null ? new State() : state;
        Document profile = user.profile();
        long now = System.currentTimeMillis();
        boolean inDungeon = RunManager.inRun(player);
        if (state != null && state.profile == profile && state.inDungeon == inDungeon && now - state.at < INVENTORY_MILLIS) return state;
        if (state == null || state.profile != profile) {
            state = new State();
            state.profile = profile;
            state.bag = entries(read(player, profile));
            STATES.put(player.getUniqueId(), state);
        }
        state.at = now;
        state.inDungeon = inDungeon;
        work(player, profile, state);
        return state;
    }

    /** The bag's items as stored (its menu writes them back after every click, and says so, see {@link #changed}). */
    private static ItemStack[] read(Player player, Document profile) {
        int capacity = capacity(profile);
        Document storage = StoredInventory.storage(profile);
        List<Object> slots = StorageDocument.slots(storage, Bag.ACCESSORY_BAG.key(), capacity);
        ItemStack[] items = new ItemStack[capacity];
        for (int i = 0; i < capacity; i++) items[i] = StorageItems.peek(slots.get(i));
        return items;
    }

    /** The accessories among items, with what's known of them. */
    private static List<Entry> entries(ItemStack[] items) {
        List<Entry> out = new ArrayList<>();
        for (ItemStack item : items) {
            Accessories.Held held = held(item);
            if (held != null) out.add(new Entry(item, held));
        }
        return out;
    }

    /** An accessory's id, rarity (recombobulated or not) and dungeon-ness; null if it isn't an accessory. */
    static Accessories.Held held(ItemStack stack) {
        SkyBlockItem item = StorageItems.skyBlockItem(stack);
        if (item == null || item.specificItemType() != SpecificItemType.ACCESSORY) return null;
        NBTTagCompound tag = StorageItems.tag(stack);
        return new Accessories.Held(item.id(), ItemBuilder.rarity(item, tag), DungeonItems.is(item, tag));
    }

    /** Accessory Power from the bag alone; stats from the bag and the inventory together, and the power's. */
    private static void work(Player player, Document profile, State state) {
        StorageTables tables = StorageTables.get();
        List<Accessories.Held> bag = state.bag.stream().map(Entry::held).toList();
        state.power = Accessories.power(bag, Accessories.counted(bag, tables), state.inDungeon, tables);

        List<Entry> all = new ArrayList<>(state.bag);
        all.addAll(entries(player.getInventory().getStorageContents()));
        List<Accessories.Held> held = all.stream().map(Entry::held).toList();
        counted(state, all, Accessories.counted(held, tables));
        Stats stats = new Stats();
        for (int i : Accessories.counted(held, tables)) stats.add(ItemStats.of(all.get(i).item(), player));
        StorageTables.Power power = selectedPower(profile);
        if (power != null) stats.add(Powers.stats(power, state.power, tables)).add(Powers.bonus(power));
        // What their Tuning Points are put in (Stats Tuning), with the points this Accessory Power gives.
        stats.add(StatsTuning.stats(profile, Accessories.tuningPoints(state.power)));
        state.stats = stats;
    }

    /** Keeps which of them count, and their items, for {@link #counted} and {@link #countedItem}. */
    private static void counted(State state, List<Entry> all, List<Integer> counted) {
        Set<String> ids = new HashSet<>();
        Map<String, ItemStack> items = new HashMap<>();
        for (int i : counted) {
            ids.add(all.get(i).held().id());
            items.put(all.get(i).held().id(), all.get(i).item());
        }
        state.ids = Set.copyOf(ids);
        state.items = items;
    }

    /**
     * The ids of the accessories that count for them now (see {@link #countedIds}), as they were at the last look
     * (at most a second ago): cheap to ask on every hit, for what accessories do beyond their stats.
     */
    public static Set<String> counted(Player player) {
        return state(player).ids;
    }

    /** The item of the counted accessory with this id, as it was at the last look; null if it doesn't count. */
    public static ItemStack countedItem(Player player, String id) {
        return state(player).items.get(id);
    }

    /**
     * Changes the first accessory with this id in their Accessory Bag ({@code change}: a count on it, the Blood God
     * Crest's kills) and keeps it; false if it isn't in their bag or their data isn't loaded. What's known of the bag
     * follows at once, without reading it all again.
     */
    public static boolean rewrite(Player player, String id, UnaryOperator<ItemStack> change) {
        User user = User.ifLoaded(player.getUniqueId());
        State state = STATES.get(player.getUniqueId());
        if (user == null || user.isReleased() || state == null || state.bag == null || state.profile != user.profile()) return false;
        Document storage = StoredInventory.storage(user.profile());
        int capacity = capacity(user.profile());
        List<Object> slots = StorageDocument.slots(storage, Bag.ACCESSORY_BAG.key(), capacity);
        for (int i = 0; i < slots.size(); i++) {
            ItemStack item = StorageItems.peek(slots.get(i));
            Accessories.Held held = held(item);
            if (held == null || !held.id().equals(id)) continue;
            ItemStack changed = change.apply(item);
            if (changed == null) return false;
            slots.set(i, StorageItems.encode(new ItemStack[] {changed}).get(0));
            StorageDocument.setSlots(storage, Bag.ACCESSORY_BAG.key(), slots);
            for (int e = 0; e < state.bag.size(); e++) {
                if (state.bag.get(e).held().id().equals(id)) {
                    state.bag.set(e, new Entry(changed, held(changed)));
                    break;
                }
            }
            state.at = 0;
            return true;
        }
        return false;
    }

    /** The SkyBlock ids of the accessories that count for them: in the bag or their inventory (KillCoins's Scavenger). */
    public static List<String> countedIds(Player player) {
        State state = state(player);
        List<Accessories.Held> held = new ArrayList<>();
        if (state.bag != null) state.bag.forEach(e -> held.add(e.held()));
        for (ItemStack item : player.getInventory().getStorageContents()) {
            Accessories.Held h = held(item);
            if (h != null) held.add(h);
        }
        List<String> ids = new ArrayList<>();
        for (int i : Accessories.counted(held, StorageTables.get())) ids.add(held.get(i).id());
        return ids;
    }
}
