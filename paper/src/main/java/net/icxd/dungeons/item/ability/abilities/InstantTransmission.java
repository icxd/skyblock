package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.StrongBlood;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Aspect of the End/Void: "Teleport 8 blocks ahead of you and gain +50 Speed for 3 seconds". How Hypixel
 * works out where to isn't published; this follows what the recorded runs show and the Skyblocker mod's
 * copy of it ("very similar to Hypixel's method"): the way is traced along a line from the eyes, and the
 * feet go where that line got to, in the middle of that block (all 33 teleports of a recorded run landed
 * in a block's middle, 29 of them at a whole height), a block lower if there's room below. So looking a
 * little down doesn't put them in the floor, and they go over a block in front of their feet. Where the
 * line runs into blocks it stops short, and Hypixel says "There are blocks in the way!"; the recording
 * shows the teleport still happening then, and its mana spent, and neither when there's no room to move
 * at all. Running into the floor looking down isn't that: they land on it.
 */
public class InstantTransmission implements AbilityHandler {
    private static final double DISTANCE = 8;
    private static final double SPEED = 50;
    private static final long SPEED_MILLIS = 3_000;
    /** Finer than a block, so the line can't slip past a corner. */
    private static final double STEP = 0.25;
    public static final String BLOCKED = "§cThere are blocks in the way!";
    /** The most snow layers a teleport goes through (Skyblocker's "3 or less snow layers"). */
    static final int MAX_SNOW_LAYERS = 3;

    /** Whether a block can be teleported through, by its coordinates. */
    @FunctionalInterface
    interface Blocks {
        boolean passable(int x, int y, int z);
    }

    /** Where the feet land (a block), and whether blocks cut the way short. */
    public record Landing(int x, int y, int z, boolean blocked) {
    }

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        if (landing(player) != null) return true;
        player.sendMessage(BLOCKED);
        return false;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Landing landing = landing(player);
        if (landing == null) {
            player.sendMessage(BLOCKED);
            return;
        }
        Location from = player.getLocation();
        Location to = new Location(from.getWorld(), landing.x() + 0.5, landing.y(), landing.z() + 0.5, from.getYaw(), from.getPitch());
        player.teleport(to);
        player.setFallDistance(0);
        player.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
        if (landing.blocked()) player.sendMessage(BLOCKED);
        speedUp(player);
        SetBonuses.teleported(player);
    }

    /**
     * "...and gain +50 ✦ Speed for 3 seconds" (both swords' lore): their walk speed follows at once, and
     * again when it's over (a use in between starts the 3 seconds again, see {@link PlayerSession#buff}).
     * Strong Dragon Armor makes it last longer and adds Strength (see {@link StrongBlood}).
     */
    private static void speedUp(Player player) {
        PlayerSession session = PlayerSession.of(player);
        long millis = SPEED_MILLIS + StrongBlood.extraMillis(player);
        session.buff("Instant Transmission", new Stats().set(Stat.SPEED, SPEED), millis);
        StrongBlood.onCast(player, millis);
        PlayerAttributes.apply(player, session.stats());
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (player.isOnline()) PlayerAttributes.apply(player, PlayerSession.of(player).stats());
        }, millis / 50 + 1);
    }

    private static Landing landing(Player player) {
        // Strong Blood reaches further with this one only, not with the teleports that reuse the path.
        return landing(player, DISTANCE + StrongBlood.range(player));
    }

    /** Where a teleport up to {@code distance} blocks the way they look takes them, as this one's does (Wither Impact's 10). */
    public static Landing landing(Player player, double distance) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        return landing(eye.toVector(), eye.getDirection(), distance, (x, y, z) -> passable(world.getBlockAt(x, y, z)));
    }

    /**
     * What a teleport goes through, as the Skyblocker mod has Hypixel's ({@code canTeleportThrough}: "Air,
     * non-collidable blocks, carpets, pots, 3 or less snow layers"): air and what has nothing to bump into
     * (cobwebs among them), carpets (wool and moss, its CarpetBlocks), flower pots, and snow up to 3
     * layers, whose little shapes don't stop it.
     */
    private static boolean passable(Block block) {
        Material type = block.getType();
        return block.isPassable() || throughAnyway(type, type == Material.SNOW ? ((Snow) block.getBlockData()).getLayers() : 0);
    }

    /** The blocks with a shape that a teleport still goes through; {@code snowLayers} counts for snow. */
    static boolean throughAnyway(Material type, int snowLayers) {
        if (type == Material.SNOW) return snowLayers <= MAX_SNOW_LAYERS;
        return type == Material.MOSS_CARPET || Tag.WOOL_CARPETS.isTagged(type) || Tag.FLOWER_POTS.isTagged(type);
    }

    /**
     * Where the line from the eyes ({@code eye}, going {@code direction}) up to {@code distance} blocks
     * takes the feet: the last block along it that they and the head above fit in, then a block lower if
     * that fits too. Null if it can't leave the block it starts in.
     */
    static Landing landing(Vector eye, Vector direction, double distance, Blocks blocks) {
        Vector step = direction.clone().normalize().multiply(STEP);
        Vector at = eye.clone();
        int startX = at.getBlockX(), startY = at.getBlockY(), startZ = at.getBlockZ();
        int[] last = null;
        boolean blocked = false;
        for (double travelled = 0; travelled <= distance + 1e-9; travelled += STEP, at.add(step)) {
            int x = at.getBlockX(), y = at.getBlockY(), z = at.getBlockZ();
            if (last != null && x == last[0] && y == last[1] && z == last[2]) continue;
            if (!blocks.passable(x, y, z) || !blocks.passable(x, y + 1, z)) {
                // Coming down onto a block with room on it is the floor: a landing, not blocks in the way.
                boolean floor = last != null && y < last[1] && !blocks.passable(x, y, z) && blocks.passable(x, y + 1, z);
                blocked = !floor;
                break;
            }
            last = new int[] {x, y, z};
        }
        if (last == null || last[0] == startX && last[1] == startY && last[2] == startZ) return null;
        int y = blocks.passable(last[0], last[1] - 1, last[2]) ? last[1] - 1 : last[1];
        return new Landing(last[0], y, last[2], blocked);
    }
}
