package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.abilities.InstantTransmission;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.bonus.SetBonuses;

/** Abilities that hit with what they hold, going somewhere: a spear thrown and back, a zap forward and back. */
final class Strikes {
    private Strikes() {
    }

    /**
     * The Tribal Spear's Thwack: "Throw the spear like an arrow which will return to you"; it "pierces through
     * infinite mobs, traveling 20 blocks" (the wiki). Each mob it passes on the way out takes an arrow's hit with
     * the spear (UNKNOWN: "like an arrow"); on the way back it hits nothing (UNKNOWN). How fast it flies is
     * UNKNOWN (1.5 blocks a tick).
     */
    static final class Thwack implements AbilityHandler {
        static final double RANGE = 20;

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location eye = player.getEyeLocation();
            boolean[] back = {false};
            // Its hits are the throw's, whatever they hold when it lands.
            Damage.Attacker thrown = Hits.striker(player, tag, Hits.Strike.arrow(0, 1));
            new Missile(player, eye, eye.getDirection().multiply(1.5))
                    .range(RANGE * 3)
                    .width(0.4)
                    .look(Missile.display(eye, player.getInventory().getItemInMainHand().clone(), 1, 45))
                    .steer(missile -> {
                        if (!back[0] && missile.travelled() >= RANGE) back[0] = true;
                        if (!back[0]) return null;
                        Vector home = missile.caster().getEyeLocation().toVector().subtract(missile.at().toVector());
                        if (home.lengthSquared() < 2.25) missile.cancel();
                        return home;
                    })
                    .onHit((missile, mob) -> {
                        if (!back[0]) {
                            double travelled = mob.getBoundingBox().getCenter().distance(missile.caster().getEyeLocation().toVector());
                            Hits.weaponHit(missile.caster(), thrown, mob, Hits.Strike.arrow(travelled, 1));
                        }
                        return true;
                    })
                    .launch();
            player.getWorld().playSound(eye, Sound.ITEM_TRIDENT_THROW, 1, 1);
        }
    }

    /**
     * The Sinseeker Scythe's Sinrecall Transmission: "Zap a line 4 blocks forward. Recast within 1s (1.5x Mana)
     * or warp back to starting point. Mobs crossing the line(s) receive a melee hit from what you're holding";
     * "For every time the ability is recast within 1 second, its mana cost increases by 1.5x" (the wiki). Each
     * zap goes as Instant Transmission's teleports do; the line between where they were and where they got to
     * hits every mob on it (a melee hit, crits as their Crit Chance rolls). A second after the last zap they're
     * back where the first one started.
     */
    static final class SinrecallTransmission implements AbilityHandler {
        static final double ZAP = 4;
        static final int RECAST_TICKS = 20;

        /** A run of zaps: where it started, how many recasts, and the warp back waiting. */
        private record Run(Location start, int recasts, BukkitTask back) {
        }

        private static final Map<UUID, Run> RUNS = new HashMap<>();

        /** The mana cost's factor on the {@code recasts}th recast: 1.5 times the last each time. */
        static double costFactor(int recasts) {
            return Math.pow(1.5, Math.max(0, recasts));
        }

        private static int extraMana(ItemBlock block, int recasts) {
            return (int) Math.round(block.mana() * (costFactor(recasts) - 1));
        }

        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Run run = RUNS.get(player.getUniqueId());
            return run == null || Hits.enoughMana(player, block, extraMana(block, run.recasts() + 1));
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Run run = RUNS.remove(player.getUniqueId());
            Location start = player.getLocation();
            int recasts = 0;
            if (run != null) {
                run.back().cancel();
                recasts = run.recasts() + 1;
                start = run.start();
                Hits.takeMana(player, extraMana(block, recasts), block.name());
            }
            zap(player, tag);
            Location home = start;
            BukkitTask back = new BukkitRunnable() {
                @Override
                public void run() {
                    RUNS.remove(player.getUniqueId());
                    if (!player.isOnline() || !player.getWorld().equals(home.getWorld())) return;
                    Location to = home.clone();
                    to.setDirection(player.getLocation().getDirection());
                    player.teleport(to);
                    SetBonuses.teleported(player);
                    player.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.7f);
                }
            }.runTaskLater(Dungeons.getInstance(), RECAST_TICKS);
            RUNS.put(player.getUniqueId(), new Run(start, recasts, back));
        }

        /** 4 blocks forward, and a red line where they went, hitting every mob on it. */
        private static void zap(Player player, NBTTagCompound tag) {
            Location from = player.getLocation().add(0, 1, 0);
            InstantTransmission.Landing landing = InstantTransmission.landing(player, ZAP);
            if (landing != null) {
                Location to = new Location(from.getWorld(), landing.x() + 0.5, landing.y(), landing.z() + 0.5, from.getYaw(), from.getPitch());
                player.teleport(to);
                SetBonuses.teleported(player);
                player.setFallDistance(0);
            }
            Location now = player.getLocation().add(0, 1, 0);
            Vector line = now.toVector().subtract(from.toVector());
            double length = line.length();
            if (length > 1e-6) {
                for (LivingEntity mob : Hits.along(from, line, length, 0.5)) Hits.weaponHit(player, tag, mob, Hits.Strike.melee(1));
                Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(200, 20, 20), 1);
                Vector step = line.clone().normalize().multiply(0.3);
                Location at = from.clone();
                for (double gone = 0; gone < length; gone += 0.3, at.add(step)) player.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, red);
            }
            player.playSound(now, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1.4f);
        }

        static void forget(UUID player) {
            Run run = RUNS.remove(player);
            if (run != null) run.back().cancel();
        }
    }
}
