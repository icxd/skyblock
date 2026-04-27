package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class InstantTransmission extends Ability {
    public InstantTransmission() {
        super("Instant Transmission", AbilityType.ABILITY, AbilityActivation.RIGHT_CLICK, "&7Teleport &a8 blocks&7 ahead of you and gain &a+50 &f✦ Speed &7for &a3 seconds&7.", 0, 45, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {
        ArrayList<Block> Blocks = (ArrayList<Block>) player.getLineOfSight((HashSet<Material>) null, 12);
        Vector vec = player.getLocation().getDirection();
        Location playerLoc = player.getLocation().add(vec);
        Location loc;
        boolean solidBlockFound = false;
        for (int i = 0; i < 11; i++) {
            if (Blocks.get(i).getType().isSolid() || Blocks.get(i).getRelative(BlockFace.DOWN).getType().isSolid()) {
                if (i - 1 >= 0) {
                    loc = Blocks.get(i - 1).getLocation();


                    loc.setYaw(playerLoc.getYaw());
                    loc.setPitch(playerLoc.getPitch());
                    loc.setX(loc.getX() + 0.5);
                    loc.setY(loc.getY() - 1);
                    loc.setZ(loc.getZ() + 0.5);

                    player.teleport(loc);
                    player.playSound(loc, Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                    player.setFallDistance(0);

                }
                solidBlockFound = true;
                break;
            }
        }
        if (!solidBlockFound) {
            loc = Blocks.get(10).getLocation();
            loc.setYaw(playerLoc.getYaw());
            loc.setPitch(playerLoc.getPitch());
            loc.setX(loc.getX() + 0.5);
            loc.setY(loc.getY() - 1);
            loc.setZ(loc.getZ() + 0.5);
            player.teleport(loc);
            player.playSound(loc, Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);

            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 3f, 1f);
        }

        if (true)
            return;

        try {
            Set<Material> mats = new HashSet<>();
            mats.add(Material.AIR);
            Location location = player.getTargetBlock(mats, 8).getLocation();
            Location ploc = player.getLocation();

            if (ploc.distance(location) < 8) {
                location = player.getTargetBlock(mats, (int) ploc.distance(location) - 1).getLocation();
            }

            location.setYaw(player.getLocation().getYaw());
            location.setPitch(player.getLocation().getPitch());
            player.teleport(location);
        } catch (IllegalStateException ignored) { }
    }
}
