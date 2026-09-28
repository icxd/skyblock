package net.icxd.dungeons.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import net.icxd.dungeons.collection.CollectionData.Collection;
import net.icxd.dungeons.collection.CollectionData.Counted;
import net.icxd.dungeons.collection.CollectionData.Reward;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Collections coming in. An item counts when a player first gets it from the world, as the wiki's
 * Collections has it ("Obtaining Collection Items manually ... progresses the corresponding Collection,
 * while obtaining them from Merchants, the Bazaar, or through Trading does not"): a mob's drop or a block's,
 * when it goes into their inventory (dungeon mobs' and mined blocks' go straight there) or when they pick it
 * up off the ground. What a player dropped, traded, bought, took from storage or was given (/item, the item
 * browser) never counts: only the plugin's own world drops are marked as collectable, and a marked stack
 * doesn't merge with an unmarked one. An enchanted item counts as what it's made of (160 Rotten Flesh).
 * Only Normal profiles collect (see {@link Collections#counts}). Main thread.
 */
public final class CollectionGains implements Listener {
    private static final NamespacedKey WORLD = new NamespacedKey("skyblock", "collection_drop");

    /** A marked item being picked up: what it is, and how many there were before the pickup handlers ran. */
    private record Pending(String item, int amount) {
    }

    /** By the item's entity; an entry lives from a pickup's first handler to its last. */
    private final Map<UUID, Pending> pickingUp = new HashMap<>();

    /** The stats collection tiers give (Mining Fortune) count from now on, on the profile a player plays on. */
    public CollectionGains() {
        PlayerStats.addModifier((player, stats) -> {
            User user = User.ifLoaded(player.getUniqueId());
            if (user != null) stats.add(Collections.stats(user.profile()));
        });
    }

    /** A mob's or a block's drop on the ground: whoever picks it up collects it. Returns the item. */
    public static Item fromWorld(Item item) {
        if (item != null) item.getPersistentDataContainer().set(WORLD, PersistentDataType.BOOLEAN, true);
        return item;
    }

    static boolean isFromWorld(Item item) {
        return item.getPersistentDataContainer().has(WORLD);
    }

    /** Items that went straight to the player from the world (a dungeon mob's drops, a mined block's). */
    public static void collect(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        collect(player, id(stack), stack.getAmount());
    }

    /** {@code amount} of an item (by its SkyBlock id) collected by a player: into the collection it counts toward, if any. */
    public static void collect(Player player, String itemId, int amount) {
        Counted worth = worth(itemId, amount);
        if (worth != null) give(player, worth.collection(), worth.amount());
    }

    /** What collecting {@code amount} of an item adds, to which collection (160 a piece for an enchanted one); null for nothing. */
    static Counted worth(String itemId, int amount) {
        Counted counted = Collections.data().counted(itemId);
        return counted == null || amount <= 0 ? null : new Counted(counted.collection(), counted.amount() * amount);
    }

    /**
     * How many of an item a pickup put in the player's inventory: all of it if the item is gone; what's
     * gone from it if a handler took them itself (PlayerListener puts SkyBlock items in and cancels the
     * pickup); else what vanilla takes, all but {@code remaining}.
     */
    static int pickedUp(int before, boolean gone, boolean cancelled, int left, int remaining) {
        if (gone) return before;
        return Math.max(0, before - (cancelled ? left : remaining));
    }

    /**
     * Adds to a collection on the profile the player plays on, with each tier's level-up message and
     * rewards (its skill XP; the rest are what the tier shows). Null (and nothing) while their data isn't
     * here, or on a profile that doesn't collect.
     */
    public static Collections.Gain give(Player player, String collectionId, long amount) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || !Collections.counts(user.mode())) return null;
        Collections.Gain gain = Collections.add(user.profile(), collectionId, amount);
        if (gain == null || !gain.leveledUp()) return gain;
        for (int tier = gain.oldTier() + 1; tier <= gain.newTier(); tier++) levelUp(player, gain.collection(), tier);
        // UNKNOWN: the collections' sound, taken as the skills'.
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        user.save();
        return gain;
    }

    /** A tier's message, and what it gives now: skill XP (SkyBlock XP and recipes come from the tier itself). */
    static void levelUp(Player player, Collection collection, int tier) {
        for (String line : CollectionText.levelUp(collection, tier)) player.sendMessage(Text.line(line));
        for (Reward reward : collection.tier(tier).rewards()) {
            if (reward.type() != Reward.Type.SKILL_XP) continue;
            try {
                // A reward's fixed XP: no Wisdom on it (UNKNOWN whether Hypixel's has it).
                SkillGains.giveFlat(player, Skill.valueOf(reward.skill()), reward.amount());
            } catch (IllegalArgumentException | NullPointerException e) {
                // A skill the plugin doesn't have.
            }
        }
    }

    /**
     * A dungeon floor finished, on this profile: its boss's kills, +1 (+2 in Master Mode: the recorded
     * Bonzo Collection's "Master Mode completions reward more Kill Count"). Null if the floor has no boss
     * collection (the Entrance) or the profile doesn't collect. What a boss tier says in chat is UNKNOWN, so
     * it says nothing; its items are claimed from its Rewards menu.
     */
    public static Collections.Gain bossDefeated(Document profile, DungeonFloor floor) {
        if (profile == null || floor.getNumber() <= 0 || !Collections.counts(Profiles.mode(profile))) return null;
        for (Collection boss : Collections.data().bosses()) {
            if (boss.floor() == floor.getNumber()) return Collections.add(profile, boss.id(), floor.isMasterMode() ? 2 : 1);
        }
        return null;
    }

    /** The SkyBlock id of an item; null for one that isn't a SkyBlock item. */
    static String id(ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag == null ? null : tag.getString("id");
    }

    // Picking up

    /** Before anything puts the item in their inventory (PlayerListener rebuilds SkyBlock items as they're picked up). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickingUp(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player) || !isFromWorld(event.getItem())) return;
        ItemStack stack = event.getItem().getItemStack();
        pickingUp.put(event.getItem().getUniqueId(), new Pending(id(stack), stack.getAmount()));
    }

    /** After: what went into their inventory is what's gone from the ground (all of it if the item is gone). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickedUp(EntityPickupItemEvent event) {
        Pending before = pickingUp.remove(event.getItem().getUniqueId());
        if (before == null || !(event.getEntity() instanceof Player player)) return;
        Item item = event.getItem();
        boolean gone = !item.isValid();
        collect(player, before.item(), pickedUp(before.amount(), gone, event.isCancelled(), gone ? 0 : item.getItemStack().getAmount(),
                event.getRemaining()));
    }

    /** A collectable drop and an item that isn't stay two, so one's count doesn't take the other's. */
    @EventHandler(ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        if (!mayMerge(isFromWorld(event.getEntity()), isFromWorld(event.getTarget()))) event.setCancelled(true);
    }

    /** Two drops on the ground become one only if both are collectable or neither is (a player's 64 String on a mob's 1 would count). */
    static boolean mayMerge(boolean fromWorld, boolean otherFromWorld) {
        return fromWorld == otherFromWorld;
    }
}
