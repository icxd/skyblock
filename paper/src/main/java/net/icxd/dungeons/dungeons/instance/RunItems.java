package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.user.StoredInventory;

/**
 * What items' abilities and passives ask of the run a player is in (ABILITIES_WEAPONS.md, ABILITIES_UTILITY.md):
 * its class ability from a weapon that "adapts to its user inside Dungeons", Superboom TNT's blast from an
 * arrow, where its nearest unfound secret is, which blocks a Dungeonbreaker may break, its floor, and which
 * run it is; and its ghosts' ghost ability items. Everything answers as if there were no run for a player outside
 * one or a ghost, but what's asked about ghosts. Main thread.
 */
public final class RunItems {
    /** "Right-click to use your class ability!" (Necron's Blade and its swords, the Stone Blade, the Earth Shard), "Right-click to use Class Ability!" (the Fel Sword). */
    private static final Pattern CLASS_ABILITY = Pattern.compile("(?i)Right-click to use (?:your )?class ability!");
    /** "Reduces the cooldown of Seismic Wave by 2s" (the Earth Shard). */
    private static final Pattern SEISMIC_WAVE_CUT = Pattern.compile("Reduces the cooldown of Seismic Wave by ([\\d.]+)s");
    /** "Reduces the cooldown of your Wish Ultimate by 10 seconds" (the Fel Sword). */
    private static final Pattern WISH_CUT = Pattern.compile("Reduces the cooldown of your Wish Ultimate by ([\\d.]+) seconds");

    /** Every ghost's: "Players can teleport to a chosen alive teammate by using their Haunt ability" (the wiki's Ghosts). */
    public static final String HAUNT = "HAUNT_ABILITY";
    /**
     * A class's ghost abilities' items. Only the Tank's have their abilities' text in the item data (Stun Potion,
     * Absorption Potion); the other classes' items have none, so they aren't given (LATER).
     */
    private static final Map<DungeonClass, List<String>> CLASS_GHOST_ITEMS = Map.of(
            DungeonClass.TANK, List.of("TANK_DUNGEON_ABILITY_1", "TANK_DUNGEON_ABILITY_2"));

    private RunItems() {
    }

    /** Ghost ability items are never saved with a player's items, however they come to have one. Once, at startup. */
    static void neverSaveGhostItems() {
        StoredInventory.neverSave(HAUNT);
        for (List<String> ids : CLASS_GHOST_ITEMS.values()) for (String id : ids) StoredInventory.neverSave(id);
    }

    /** Whether it's one of the ghost ability items (Haunt, a class's). */
    public static boolean ghostItem(String id) {
        if (HAUNT.equals(id)) return true;
        for (List<String> ids : CLASS_GHOST_ITEMS.values()) if (ids.contains(id)) return true;
        return false;
    }

    /** Whether they're a ghost in a running run: its ghost abilities are theirs to use. */
    public static boolean ghost(Player player) {
        DungeonRun run = RunManager.of(player);
        return run != null && run.phase() == DungeonRun.Phase.RUNNING && run.isGhost(player.getUniqueId());
    }

    /** Their level in the class they play in their run; 0 outside one. */
    public static int classLevel(Player player) {
        DungeonRun run = RunManager.of(player);
        return run == null ? 0 : run.classLevel(player.getUniqueId());
    }

    /** The class they play in their run; null outside one. */
    public static DungeonClass playing(Player player) {
        DungeonRun run = RunManager.of(player);
        return run == null ? null : run.classOf(player.getUniqueId());
    }

    /** Their run's other members who are alive and here, within {@code radius} of {@code at}. */
    public static List<Player> aliveTeammatesNear(Player player, Location at, double radius) {
        List<Player> near = new ArrayList<>();
        DungeonRun run = RunManager.of(player);
        if (run == null) return near;
        for (Player other : run.players()) {
            if (other.equals(player) || run.isGhost(other.getUniqueId()) || !other.getWorld().equals(at.getWorld())) continue;
            if (other.getLocation().distanceSquared(at) <= radius * radius) near.add(other);
        }
        return near;
    }

    /**
     * A new ghost's ghost ability items (see {@link Ghosts}): Haunt, and their class's, each in the first free slot
     * of their inventory, the hotbar first; with none free, they go without it. UNKNOWN which slots Hypixel's take.
     * Their own items stay, and their Spirit items become ghost abilities where they are.
     */
    static void giveGhostItems(DungeonRun run, Player player) {
        List<String> ids = new ArrayList<>();
        ids.add(HAUNT);
        ids.addAll(CLASS_GHOST_ITEMS.getOrDefault(run.classOf(player.getUniqueId()), List.of()));
        PlayerInventory inventory = player.getInventory();
        for (String id : ids) {
            SkyBlockItem item = ItemRegistry.get(id);
            if (item == null || has(inventory, id)) continue;
            int free = inventory.firstEmpty();
            if (free < 0) return;
            ItemStack stack = ItemBuilder.build(item);
            StoredInventory.markNotSaved(stack);
            inventory.setItem(free, stack);
        }
    }

    /** They're alive again, or going: their ghost ability items go. */
    static void takeGhostItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            NBTTagCompound tag = ItemNBT.read(inventory.getItem(slot));
            if (tag != null && ghostItem(tag.getString("id"))) inventory.setItem(slot, null);
        }
        NBTTagCompound cursor = ItemNBT.read(player.getItemOnCursor());
        if (cursor != null && ghostItem(cursor.getString("id"))) player.setItemOnCursor(null);
    }

    private static boolean has(PlayerInventory inventory, String id) {
        for (ItemStack stack : inventory.getContents()) {
            NBTTagCompound tag = ItemNBT.read(stack);
            if (tag != null && id.equals(tag.getString("id"))) return true;
        }
        return false;
    }

    /** The run they're alive in once it's running; null otherwise (not in one, before it starts, after it ends, a ghost). */
    static DungeonRun running(Player player) {
        DungeonRun run = RunManager.of(player);
        if (run == null || run.phase() != DungeonRun.Phase.RUNNING || run.isGhost(player.getUniqueId())) return null;
        return run;
    }

    /** Whether they're alive in a running run. */
    public static boolean alive(Player player) {
        return running(player) != null;
    }

    /**
     * Which run they're in, as a name no other run has (its world's), for what an item keeps for one run only (the
     * Zombie Commander Whip's "Zombies killed during a dungeon run"); null outside one.
     */
    public static String runKey(Player player) {
        DungeonRun run = RunManager.of(player);
        return run == null ? null : run.world.getName();
    }

    /** The floor of the run they're in; null outside one. */
    public static DungeonFloor floor(Player player) {
        DungeonRun run = RunManager.of(player);
        return run == null ? null : run.floor;
    }

    /** Whether an item's own text (as its behaviour has it) ends in "Right-click to use your class ability!". */
    public static boolean usesClassAbility(SkyBlockItem item, NBTTagCompound tag) {
        List<String> lore = ItemBehaviours.of(item).lore(item, tag, item.lore());
        return CLASS_ABILITY.matcher(AbilityText.plain(lore)).find();
    }

    /**
     * Their class ability, as a right click on the Dungeon Orb uses it: "This shard automatically adapts to its
     * user inside Dungeons, allowing you to perform your class ability" (the Earth Shard); false (nothing) outside a
     * running run or as a ghost.
     */
    public static boolean classAbility(Player player) {
        DungeonRun run = running(player);
        if (run == null) return false;
        run.classes().ability(player);
        return true;
    }

    /** What what they hold takes off Seismic Wave's cooldown, in milliseconds ("Reduces the cooldown of Seismic Wave by 2s"). */
    static long seismicWaveCut(Player player) {
        return (long) (heldNumber(player, SEISMIC_WAVE_CUT) * 1000);
    }

    /** What what they hold takes off Wish's cooldown, in milliseconds ("... of your Wish Ultimate by 10 seconds"). */
    static long wishCut(Player player) {
        return (long) (heldNumber(player, WISH_CUT) * 1000);
    }

    private static double heldNumber(Player player, Pattern pattern) {
        NBTTagCompound tag = ItemNBT.read(player.getInventory().getItemInMainHand());
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) return 0;
        Matcher m = pattern.matcher(AbilityText.plain(item.lore()));
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    /** Superboom TNT goes off at this block in their run (an Explosive Bow's arrow: "Acts as Superboom TNT!"); false outside one. */
    public static boolean superboom(Player player, Block at) {
        DungeonRun run = running(player);
        if (run == null || !at.getWorld().equals(run.world)) return false;
        run.superboom(at);
        return true;
    }

    /**
     * Whether a Dungeonbreaker may break this block for them: "It cannot be used in puzzle rooms, on doors, or to pass
     * through walls into a separate room", and it "is able to mine any block that is not part of a crypt inside the
     * explored area of the dungeon, including trap rooms" (the wiki's Dungeonbreaker). So only alive in a running run,
     * in the room they're in (which isn't a puzzle room), not a door's block or an unblown tomb's; the gap between two
     * cells counts as the room's only where the room is on both sides of it (inside a room of several cells), so the
     * walls between rooms stay.
     */
    public static boolean mayBreak(Player player, Block block) {
        DungeonRun run = running(player);
        if (run == null || !block.getWorld().equals(run.world)) return false;
        RunLayout layout = run.layout();
        PlacedRoom room = layout.roomAt(player.getLocation());
        if (room == null || room.type() == RoomType.PUZZLE) return false;
        if (!inRoom(layout, room, block.getX(), block.getZ())) return false;
        return layout.doorAt(block.getX(), block.getY(), block.getZ()) == null && !run.inCrypt(block);
    }

    /** Whether a block column is the room's: in one of its cells, or in a gap with only its cells around it. */
    private static boolean inRoom(RunLayout layout, PlacedRoom room, int x, int z) {
        Position cell = layout.cellAt(x, z);
        if (cell != null) return room.cells().contains(cell);
        int around = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Position next = layout.cellAt(x + dx, z + dz);
                if (next == null) continue;
                if (!room.cells().contains(next)) return false;
                around++;
            }
        }
        return around >= 2;
    }

    /** Where the nearest secret of their run that hasn't been found is, in a room that has its secrets out; null for none. */
    public static Location nearestSecret(Player player) {
        DungeonRun run = running(player);
        RunSecrets secrets = run == null ? null : run.secrets();
        return secrets == null ? null : secrets.nearestUnfound(player.getLocation());
    }
}
