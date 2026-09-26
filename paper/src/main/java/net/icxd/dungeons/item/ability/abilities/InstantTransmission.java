package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Aspect of the End/Void: up to 8 blocks the way you're looking, stopping short of anything solid. */
public class InstantTransmission implements AbilityHandler {
    private static final double DISTANCE = 8;
    private static final double STEP = 0.25;

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
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
