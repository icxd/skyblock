package net.icxd.dungeons.item.ability.weapons;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * Giant's Sword: "Slam your sword into the ground dealing 100,000 damage to nearby enemies": "an 8-block
 * radius 5 blocks in front of them" (the wiki), base 100,000 with Intelligence scaling 0.05. As recorded
 * (two uses in a run): the middle is 5 blocks ahead of their feet along the ground; a giant copy of the
 * sword stands there (an item display, 5 times the size, 3 blocks up and tipped 135°, facing their way,
 * for 5.8 s), with an anvil landing and thunder at pitch 0.492; each hit is a whole number (the chat line's
 * totals were), and the line is "Your Giant's Sword hit 3 enemies for 10,646,732 damage.".
 */
final class GiantsSlam implements AbilityHandler {
    static final Magic.Spell SLAM = new Magic.Spell(100_000, 0.05, true, true);
    static final double AHEAD = 5;
    static final double RADIUS = 8;
    /** Recorded: the display went 5.8 s after it came. */
    static final int SHOWN_TICKS = 116;
    private static final float PITCH = 0.492f;

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Location feet = player.getLocation();
        Vector middle = Shapes.ahead(feet.toVector(), feet.getYaw(), AHEAD);
        Location at = new Location(feet.getWorld(), middle.getX(), middle.getY(), middle.getZ(), feet.getYaw(), 0);
        show(at, player);
        at.getWorld().playSound(at, Sound.BLOCK_ANVIL_LAND, 1, PITCH);
        at.getWorld().playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, PITCH);
        Hits.report(player, item.name(), Hits.spell(player, item, tag, Hits.spellOf(item, SLAM), Hits.near(at, RADIUS)));
    }

    /** The recorded display: the sword they hold, 5 times over, 3 blocks up and tipped 135° about its length. */
    private static void show(Location at, Player player) {
        ItemDisplay sword = at.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setItemStack(player.getInventory().getItemInMainHand().clone());
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTransformation(new Transformation(new Vector3f(0, 3, 0), new Quaternionf(), new Vector3f(5, 5, 5),
                    new Quaternionf().rotateZ((float) Math.toRadians(135))));
        });
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), sword::remove, SHOWN_TICKS);
    }
}
