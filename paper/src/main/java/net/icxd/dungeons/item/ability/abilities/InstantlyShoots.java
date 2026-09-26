package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/** A shortbow's shot ("Shortbow: Instantly shoots!"): its arrows at once, fanned out 10° apart. */
public class InstantlyShoots implements AbilityHandler {
    /** How many arrows a shot is, by item id (upper case, as the registry has them); 1 for the rest. */
    private static final Map<String, Integer> ARROWS = Map.of("TERMINATOR", 3);

    public static int arrows(SkyBlockItem item) {
        return ARROWS.getOrDefault(item.id().toUpperCase(Locale.ROOT), 1);
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        int arrows = arrows(item);
        Location location = player.getEyeLocation();
        for (int i = 0; i < arrows; i++) {
            Location l = location.clone();
            l.setYaw(location.getYaw() + (i * 10) - (arrows * 5) + 5);
            Arrow a = player.getWorld().spawnArrow(l.clone().add(l.getDirection().multiply(0.7)), l.getDirection(), 5, 1);
            a.setShooter(player);
        }
    }
}
