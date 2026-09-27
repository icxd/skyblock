package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.combat.Shots;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.weapons.WeaponAbilities;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/**
 * A shortbow's shot ("Shortbow: Instantly shoots!"): its arrows at once. With more than one (the Terminator),
 * the middle arrow has a bow's inaccuracy and the others go out exactly 5° to either side of the crosshair
 * (the wiki's Terminator page, in a note its editors hid as not needed).
 */
public class InstantlyShoots implements AbilityHandler {
    /** How many arrows a shot is, by item id (upper case, as the registry has them); 1 for the rest. */
    private static final Map<String, Integer> ARROWS = Map.of("TERMINATOR", 3);
    private static final float SIDE_DEGREES = 5;

    public static int arrows(SkyBlockItem item) {
        return ARROWS.getOrDefault(item.id().toUpperCase(Locale.ROOT), 1);
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        int arrows = arrows(item);
        Location location = player.getEyeLocation();
        for (int i = 0; i < arrows; i++) {
            Location l = location.clone();
            float offset = sideAngle(i, arrows);
            l.setYaw(location.getYaw() + offset);
            // A vanilla bow's inaccuracy is 1; the side arrows have none.
            Arrow a = player.getWorld().spawnArrow(l.clone().add(l.getDirection().multiply(0.7)), l.getDirection(), 5, offset == 0 ? 1 : 0);
            // Before Shots.record, which makes it an arrow nobody can pick up: a player shooter makes it one they can.
            a.setShooter(player);
            // A shortbow's shot is always a full draw.
            Shots.record(a, player, tag, true);
            WeaponAbilities.shortbowArrow(a, item);
        }
    }

    /** How far arrow {@code i} of {@code arrows} goes from the crosshair, in degrees of yaw: 0 for the middle, 5 apart. */
    static float sideAngle(int i, int arrows) {
        return (i - (arrows - 1) / 2f) * SIDE_DEGREES;
    }
}
