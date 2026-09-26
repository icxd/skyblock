package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CoolThing extends Ability {
    public CoolThing() {
        super("Cool Thing", AbilityType.ABILITY, AbilityActivation.RIGHT_CLICK, "&7Example ability.", 0, 0, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {
        final Location startLocation = player.getLocation();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 5));
        new BukkitRunnable() {
            @Override
            public void run() {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5, 5));

                Location currentLocation = player.getLocation();
                Location endLocation = startLocation.clone().add(startLocation.getDirection().multiply(10));
                double distance = currentLocation.distance(endLocation);
                double x = (endLocation.getX() - currentLocation.getX()) / distance;
                double y = (endLocation.getY() - currentLocation.getY()) / distance;
                double z = (endLocation.getZ() - currentLocation.getZ()) / distance;
                for (double i = 0; i < distance; i += 0.5) {
                    currentLocation.add(x, y, z);
                    player.teleport(currentLocation);
                    for (Player player : player.getWorld().getPlayers()) {
                        player.spawnParticle(Particle.FLAME, currentLocation, 1, 0, 0, 0, 0, null, true);
                    }
                }

            }
        }.runTaskLater(Dungeons.getInstance(), 20);
    }
}
