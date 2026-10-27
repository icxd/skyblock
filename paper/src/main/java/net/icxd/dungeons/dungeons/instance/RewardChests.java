package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.EulerAngle;
import org.bson.Document;

import net.icxd.dungeons.bank.BankAccount;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.chests.ChestLoot;
import net.icxd.dungeons.dungeons.chests.ChestLoot.Reward;
import net.icxd.dungeons.dungeons.chests.ChestModel;
import net.icxd.dungeons.dungeons.chests.ChestType;
import net.icxd.dungeons.dungeons.chests.RecordedChest;
import net.icxd.dungeons.dungeons.paste.BossArena;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.hex.enchant.EnchantRules;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Essences;
import net.icxd.dungeons.storage.AccessoryBag;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * A beaten boss's reward chests (MCW Dungeon Reward Chest, and the three recorded Floor I ends): they appear with the
 * run's summary where the boss room has them, Wood, Gold and Diamond always and Emerald and Obsidian for a score of
 * 230 and 270. Each member gets loot of their own in each, rolled the first time they look ({@link ChestLoot}, the
 * quality with S+ and their Treasure accessory), and may open one, or two with a Dungeon Chest Key (used up by the
 * second), paying from their purse and then their bank. A Kismet Feather rerolls one chest a run.
 *
 * <p>Opening it, as recorded for a Wood chest (2026_10_27_06_28_42_1, 02:27.7 to 02:31.7): the menu closes, the lid
 * shakes for 4 seconds (its head turned a little every way each tick, more and more, from about 2 to 5 degrees) with a
 * wooden step every quarter second, then the lid goes, a white firework bursts on the chest, "WOOD CHEST REWARDS" and
 * the rewards go to chat, and each reward stands named round the chest 2 blocks out until it's walked over: an item
 * into the inventory, essence onto the profile with its action bar line.
 *
 * <p>UNKNOWN, not recorded: what's said for a second chest without a key, for a chest already opened, for too few
 * coins, and around a reroll (the plugin's words, below); the Class Milestone 2 a chest needs (the plugin has no
 * milestones); whether teammates see a member's rewards (here only they do); and what becomes of rewards nobody
 * picked up when the instance closes (here they're given then). Chests nobody opened aren't kept for Croesus yet.
 * Main thread.
 */
final class RewardChests {
    /** From the click to the rewards (recorded 4.0 s), and the step sound's beat. */
    static final int SHAKE = 80;
    static final int STEP_EVERY = 5;
    /** The lid's shake, in degrees each way: from this to this over the 4 seconds. */
    static final double SHAKE_FROM = 2;
    static final double SHAKE_TO = 5;
    /** How close a reward has to be walked (recorded: picked up 1.1 to 1.4 blocks off). */
    static final double PICKUP_REACH = 1.5;

    static final String ALREADY_OPENED = "&cYou have already opened this chest!";
    static final String ONE_A_RUN = "&cYou can only open one chest per run! Bring a &9Dungeon Chest Key &cto open another.";
    static final String NO_COINS = "&cYou don't have enough coins!";
    static final String NO_SPACE = "&cYou don't have enough space in your inventory to pick up this item!";

    /** On the chests' fireworks, whose bursts are for looking at only. */
    static final String BURST = "reward_chest_burst";

    static final String CHEST_KEY = "DUNGEON_CHEST_KEY";
    static final String KISMET_FEATHER = "KISMET_FEATHER";

    /** A member's loot in a chest, and what it costs them. */
    record Loot(int quality, List<Reward> rewards, long cost) {
    }

    /** What a member has done with the chests. */
    private static final class Claims {
        final Map<ChestType, Loot> loot = new EnumMap<>(ChestType.class);
        final Set<ChestType> opened = EnumSet.noneOf(ChestType.class);
        boolean rerolled;
        boolean keyUsed;
    }

    /** A chest a member opened, shaking. */
    private static final class Opening {
        final UUID player;
        final ChestType type;
        final Loot loot;
        int age;

        Opening(UUID player, ChestType type, Loot loot) {
            this.player = player;
            this.type = type;
            this.loot = loot;
        }
    }

    /** A reward waiting round its chest for the member it's for. */
    private static final class Drop {
        final UUID player;
        final Reward reward;
        final ArmorStand stand;
        final Location at;
        boolean told;

        Drop(UUID player, Reward reward, ArmorStand stand, Location at) {
            this.player = player;
            this.reward = reward;
            this.stand = stand;
            this.at = at;
        }
    }

    private final DungeonRun run;
    private final World world;
    private final DungeonFloor floor;
    private final boolean sPlus;
    private final Map<ChestType, RecordedChest> chests = new EnumMap<>(ChestType.class);
    private final Map<UUID, Claims> claims = new HashMap<>();
    private final List<Opening> openings = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();

    private RewardChests(DungeonRun run, DungeonFloor floor, int score) {
        this.run = run;
        this.world = run.world;
        this.floor = floor;
        this.sPlus = score >= ChestType.BEDROCK.minScore();
    }

    /** The chests a boss room has for this score, built (those with a model and a loot table on this floor). */
    static RewardChests spawn(DungeonRun run, BossArena arena, DungeonFloor floor, int score) {
        RewardChests out = new RewardChests(run, floor, score);
        for (BossArena.ChestSpot spot : arena.chests()) {
            if (score < spot.type().minScore() || !ChestModel.recorded(spot.type()) || ChestLoot.table(floor, spot.type()) == null) continue;
            RecordedChest chest = new RecordedChest(spot.type());
            chest.spawn(new Location(out.world, spot.x(), spot.y(), spot.z(), spot.yaw(), 0));
            out.chests.put(spot.type(), chest);
        }
        return out;
    }

    /** The chest this is part of; null if none. */
    ChestType chestOf(Entity entity) {
        for (Map.Entry<ChestType, RecordedChest> e : chests.entrySet()) if (e.getValue().isPart(entity)) return e.getKey();
        return null;
    }

    // Looking

    /** A right click on a chest: its menu, if they may still open it. */
    void click(Player player, ChestType type) {
        if (run.member(player.getUniqueId()) == null || !chests.containsKey(type)) return;
        Claims mine = claims.computeIfAbsent(player.getUniqueId(), id -> new Claims());
        if (mine.opened.contains(type)) {
            player.sendMessage(Text.line(ALREADY_OPENED));
            return;
        }
        if (!mayOpenAnother(player, mine)) {
            player.sendMessage(Text.line(ONE_A_RUN));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5f, 1);
        new RewardChestMenu(this, player, type).open(player, null);
    }

    /** None opened yet, or one and a Dungeon Chest Key in the inventory (one key a run). */
    private boolean mayOpenAnother(Player player, Claims mine) {
        if (mine.opened.isEmpty()) return true;
        return mine.opened.size() == 1 && !mine.keyUsed && slotOf(player, CHEST_KEY) >= 0;
    }

    /** Their loot in this chest, rolled the first time. */
    Loot loot(Player player, ChestType type) {
        Claims mine = claims.computeIfAbsent(player.getUniqueId(), id -> new Claims());
        return mine.loot.computeIfAbsent(type, t -> roll(player, t));
    }

    private Loot roll(Player player, ChestType type) {
        ChestLoot.Table table = ChestLoot.table(floor, type);
        int quality = ChestLoot.quality(table.quality(), sPlus, ChestLoot.treasure(AccessoryBag.counted(player)), 0);
        List<Reward> rewards = ChestLoot.roll(table, quality, ThreadLocalRandom.current());
        return new Loot(quality, rewards, ChestLoot.cost(table, rewards));
    }

    boolean opened(Player player, ChestType type) {
        Claims mine = claims.get(player.getUniqueId());
        return mine != null && mine.opened.contains(type);
    }

    /** Whether they've rerolled a chest this run. */
    boolean rerolled(Player player) {
        Claims mine = claims.get(player.getUniqueId());
        return mine != null && mine.rerolled;
    }

    /** Whether a Kismet Feather is in their inventory. */
    boolean hasFeather(Player player) {
        return slotOf(player, KISMET_FEATHER) >= 0;
    }

    /** A chest's loot rolled again for them, using up a Kismet Feather (once a run, and not on a chest they've opened). */
    boolean reroll(Player player, ChestType type) {
        Claims mine = claims.computeIfAbsent(player.getUniqueId(), id -> new Claims());
        if (mine.rerolled || mine.opened.contains(type) || !takeOne(player, KISMET_FEATHER)) return false;
        mine.rerolled = true;
        mine.loot.put(type, roll(player, type));
        return true;
    }

    // Opening

    /** "Open Reward Chest": paid for, the menu shut, the chest shaking. False (and why, in chat) if it can't be. */
    boolean open(Player player, ChestType type) {
        Claims mine = claims.computeIfAbsent(player.getUniqueId(), id -> new Claims());
        if (mine.opened.contains(type)) {
            player.sendMessage(Text.line(ALREADY_OPENED));
            return false;
        }
        if (!mayOpenAnother(player, mine)) {
            player.sendMessage(Text.line(ONE_A_RUN));
            return false;
        }
        Loot loot = loot(player, type);
        User user = User.ifLoaded(player.getUniqueId());
        if (loot.cost() > 0 && (user == null || user.isReleased() || !pay(user.profile(), loot.cost(), player.getUniqueId()))) {
            player.sendMessage(Text.line(NO_COINS));
            return false;
        }
        if (!mine.opened.isEmpty()) {
            takeOne(player, CHEST_KEY);
            mine.keyUsed = true;
        }
        mine.opened.add(type);
        if (user != null && loot.cost() > 0) user.save();
        player.closeInventory();
        openings.add(new Opening(player.getUniqueId(), type, loot));
        return true;
    }

    /** Takes the coins from the purse, and what it's short from the bank (MCW). False, taking nothing, if both together are short. */
    static boolean pay(Document profile, long cost, UUID by) {
        double purse = Purse.coins(profile);
        if (purse >= cost) return Purse.take(profile, cost);
        double shortfall = Math.ceil(cost - purse);
        if (purse + BankAccount.balance(profile) < cost) return false;
        if (!BankAccount.withdraw(profile, shortfall, by, System.currentTimeMillis()).done()) return false;
        return Purse.take(profile, cost);
    }

    void tick() {
        for (Opening opening : List.copyOf(openings)) tick(opening);
        for (Drop drop : List.copyOf(drops)) {
            Player player = org.bukkit.Bukkit.getPlayer(drop.player);
            if (player == null || !player.getWorld().equals(world)) continue;
            Location feet = player.getLocation();
            double dx = feet.getX() - drop.at.getX();
            double dz = feet.getZ() - drop.at.getZ();
            if (Math.abs(feet.getY() - drop.at.getY()) < 3 && dx * dx + dz * dz <= PICKUP_REACH * PICKUP_REACH) pickUp(player, drop);
        }
    }

    private void tick(Opening opening) {
        RecordedChest chest = chests.get(opening.type);
        Location anchor = chest.anchor();
        if (opening.age < SHAKE) {
            double amount = SHAKE_FROM + (SHAKE_TO - SHAKE_FROM) * opening.age / SHAKE;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (ArmorStand lid : chest.lid()) {
                lid.setHeadPose(new EulerAngle(Math.toRadians(random.nextDouble(-amount, amount)), Math.toRadians(random.nextDouble(-amount, amount)),
                        Math.toRadians(random.nextDouble(-amount, amount))));
            }
            if (opening.age % STEP_EVERY == 0) world.playSound(anchor.clone().add(0, 0.5, 0), Sound.BLOCK_WOOD_STEP, SoundCategory.MASTER, 1.5f, 0.7f);
            opening.age++;
            return;
        }
        openings.remove(opening);
        chest.removeLid();
        burst(anchor);
        Player player = org.bukkit.Bukkit.getPlayer(opening.player);
        List<Reward> rewards = opening.loot.rewards();
        if (player != null) {
            player.sendMessage("");
            player.sendMessage(Text.line("  " + opening.type.rewardsTitle()));
            for (Reward reward : rewards) player.sendMessage(Text.line("    " + line(reward)));
            player.sendMessage("");
        }
        for (int i = 0; i < rewards.size(); i++) {
            double[] xz = ChestModel.rewardSpot(i, rewards.size(), anchor.getYaw());
            Location at = new Location(world, anchor.getX() + xz[0], anchor.getY() - 1, anchor.getZ() + xz[1]);
            drops.add(new Drop(opening.player, rewards.get(i), rewardStand(at, line(rewards.get(i)), opening.player), at));
        }
    }

    /** A white firework's small burst on the chest, as recorded. */
    private void burst(Location at) {
        world.spawn(at, Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(Color.WHITE, Color.WHITE).build());
            meta.setPower(0);
            firework.setFireworkMeta(meta);
            firework.setPersistent(false);
            firework.addScoreboardTag(BURST);
        }).detonate();
    }

    /** Whether this is a chest's firework (it hurts nobody). */
    static boolean isBurst(Entity entity) {
        return entity instanceof Firework && entity.getScoreboardTags().contains(BURST);
    }

    /** Its name over where it lies (as recorded: an invisible stand with the name), seen only by the member it's for. */
    private ArmorStand rewardStand(Location at, String name, UUID owner) {
        ArmorStand stand = world.spawn(at, ArmorStand.class, s -> {
            s.setInvisible(true);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setPersistent(false);
            s.setCanPickupItems(false);
            s.customName(Text.line(name));
            s.setCustomNameVisible(true);
        });
        for (Player other : world.getPlayers()) if (!other.getUniqueId().equals(owner)) other.hideEntity(run.plugin(), stand);
        return stand;
    }

    private void pickUp(Player player, Drop drop) {
        if (!give(player, drop.reward)) {
            if (!drop.told) player.sendMessage(Text.line(NO_SPACE));
            drop.told = true;
            return;
        }
        drops.remove(drop);
        drop.stand.remove();
        if (drop.reward.essence() != null) player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1, 1.49f);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1, 1);
    }

    /** Into their inventory, or onto their profile for essence; false if there's no room. */
    private static boolean give(Player player, Reward reward) {
        if (reward.essence() != null) {
            Essences.give(player, reward.essence(), reward.count());
            return true;
        }
        ItemStack item = item(reward);
        if (item == null) return true;
        if (player.getInventory().firstEmpty() < 0) return false;
        player.getInventory().addItem(item);
        return true;
    }

    /** Before the instance closes: whatever they didn't pick up, given. */
    void handOver() {
        for (Drop drop : List.copyOf(drops)) {
            Player player = org.bukkit.Bukkit.getPlayer(drop.player);
            if (player == null) continue;
            ItemStack item = item(drop.reward);
            if (item == null || player.getInventory().firstEmpty() >= 0) give(player, drop.reward);
            else player.getWorld().dropItemNaturally(player.getLocation(), item);
            drops.remove(drop);
            drop.stand.remove();
        }
    }

    void dispose() {
        for (RecordedChest chest : chests.values()) chest.remove();
        chests.clear();
        for (Drop drop : drops) drop.stand.remove();
        drops.clear();
        openings.clear();
    }

    // What the rewards are

    /** The reward as an item (a book with its enchantment); null for essence or an item that isn't in the item data. */
    static ItemStack item(Reward reward) {
        return switch (reward.kind()) {
            case ESSENCE -> null;
            case ITEM -> {
                SkyBlockItem item = ItemRegistry.get(reward.id());
                yield item == null ? null : ItemBuilder.build(item);
            }
            case BOOK -> {
                SkyBlockItem book = ItemRegistry.get("ENCHANTED_BOOK");
                if (book == null) yield null;
                NBTTagCompound tag = EnchantRules.with(ItemBuilder.newData(book), Map.of(reward.id(), reward.count()));
                yield ItemBuilder.build(book, tag);
            }
        };
    }

    /** How the menu, the chat and its name over it give it: "&fEnchanted Book (Feather Falling VI)", "&dUndead Essence &8x5". */
    static String line(Reward reward) {
        return switch (reward.kind()) {
            case ESSENCE -> ChestLoot.essenceLine(reward.essence(), reward.count());
            case BOOK -> {
                EnchantmentData.Entry entry = EnchantmentData.current().get(reward.id());
                String name = entry != null ? entry.name() : Utils.title(reward.id().toUpperCase(java.util.Locale.ROOT)).replace('_', ' ');
                yield ChestLoot.bookLine(name, reward.count(), entry != null && entry.ultimate());
            }
            case ITEM -> {
                SkyBlockItem item = ItemRegistry.get(reward.id());
                // UNKNOWN: an item's line (none recorded); its name as it shows.
                yield item == null ? reward.id() : ItemBuilder.name(item, ItemBuilder.newData(item));
            }
        };
    }

    /** The slot of the first of these items (by SkyBlock id) in their inventory; -1 for none. */
    private static int slotOf(Player player, String id) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.isEmpty()) continue;
            NBTTagCompound tag = ItemNBT.read(stack);
            if (tag != null && id.equals(tag.getString("id"))) return i;
        }
        return -1;
    }

    private static boolean takeOne(Player player, String id) {
        int slot = slotOf(player, id);
        if (slot < 0) return false;
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.getAmount() > 1) stack.setAmount(stack.getAmount() - 1);
        else player.getInventory().setItem(slot, null);
        return true;
    }
}
