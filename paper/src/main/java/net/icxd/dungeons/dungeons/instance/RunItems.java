package net.icxd.dungeons.dungeons.instance;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * What items' abilities and passives ask of the run a player is in (ABILITIES_WEAPONS.md, ABILITIES_UTILITY.md):
 * its class ability from a weapon that "adapts to its user inside Dungeons", Superboom TNT's blast from an
 * arrow, where its nearest unfound secret is, which blocks a Dungeonbreaker may break, its floor, and which
 * run it is. Everything answers as if there were no run for a player outside one or a ghost. Main thread.
 */
public final class RunItems {
    /** "Right-click to use your class ability!" (Necron's Blade and its swords, the Stone Blade, the Earth Shard), "Right-click to use Class Ability!" (the Fel Sword). */
    private static final Pattern CLASS_ABILITY = Pattern.compile("(?i)Right-click to use (?:your )?class ability!");
    /** "Reduces the cooldown of Seismic Wave by 2s" (the Earth Shard). */
    private static final Pattern SEISMIC_WAVE_CUT = Pattern.compile("Reduces the cooldown of Seismic Wave by ([\\d.]+)s");
    /** "Reduces the cooldown of your Wish Ultimate by 10 seconds" (the Fel Sword). */
    private static final Pattern WISH_CUT = Pattern.compile("Reduces the cooldown of your Wish Ultimate by ([\\d.]+) seconds");

    private RunItems() {
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

    /** Where the nearest secret of their run that hasn't been found is, in a room that has its secrets out; null for none. */
    public static Location nearestSecret(Player player) {
        DungeonRun run = running(player);
        RunSecrets secrets = run == null ? null : run.secrets();
        return secrets == null ? null : secrets.nearestUnfound(player.getLocation());
    }
}
