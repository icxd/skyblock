package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Aspect of the End/Void: up to 8 blocks the way you're looking, stopping short of anything solid. */
public class InstantTransmission extends Ability {
    private static final double DISTANCE = 8;
    private static final double STEP = 0.25;

    public InstantTransmission() {
        super("Instant Transmission", AbilityType.ABILITY, AbilityActivation.RIGHT_CLICK, "&7Teleport &a8 blocks&7 ahead of you and gain &a+50 &f✦ Speed &7for &a3 seconds&7.", 0, 45, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {
        Location from = player.getLocation();
        Vector direction = from.getDirection().normalize();
        Location to = null;
        for (double d = STEP; d <= DISTANCE + 1e-9; d += STEP) {
            Location at = from.clone().add(direction.clone().multiply(d));
            if (!fits(at)) break;
            to = at;
        }
        if (to == null) return;
        player.teleport(to);
        player.setFallDistance(0);
        player.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
    }

    /** Room for a player: nothing solid at the feet or head. */
    private static boolean fits(Location feet) {
        return !feet.getBlock().getType().isSolid() && !feet.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }
}
