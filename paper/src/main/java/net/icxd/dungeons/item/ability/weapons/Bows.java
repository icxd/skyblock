package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.Shots;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.item.bonus.SetBonuses;

/** Bows' own abilities, and what their arrows do when they land. Main thread. */
final class Bows {
    /** On a Mosquito Shortbow's Nasty Bite arrow. */
    static final String NASTY_BITE = "skyblock_nasty_bite";
    /** "Hits 3 mobs on impact." on the Juju Shortbow's lore. */
    private static final Pattern HITS_ON_IMPACT = Pattern.compile("Hits (\\d+) mobs on impact");
    /** The Juju's "3-block radius" (the wiki). */
    static final double IMPACT_RADIUS = 3;

    /** A Juju arrow in flight: what its impact's hits are worked out with (the shot's, as the arrow's own), and how many mobs it hits. */
    private record ImpactArrow(Damage.Attacker shot, int mobs) {
    }

    private static final Map<UUID, ImpactArrow> IMPACT_ARROWS = new HashMap<>();
    /** What each Nasty Bite heals, by arrow. */
    private static final Map<UUID, Double> BITES = new HashMap<>();

    private Bows() {
    }

    /** How many mobs a shortbow's arrows hit on impact, from its lore ("Hits 3 mobs on impact."); 0 for none. */
    static int mobsOnImpact(List<String> lore) {
        return (int) Math.max(0, Bolts.number(lore, HITS_ON_IMPACT));
    }

    /** A shortbow shot this arrow (see InstantlyShoots): its impact, if the bow's lore gives it one. */
    static void shot(AbstractArrow arrow, SkyBlockItem bow, NBTTagCompound tag) {
        int mobs = mobsOnImpact(bow.lore());
        if (mobs <= 0 || !(arrow.getShooter() instanceof Player shooter)) return;
        IMPACT_ARROWS.put(arrow.getUniqueId(), new ImpactArrow(Hits.striker(shooter, tag, Hits.Strike.arrow(0, 1)), mobs));
    }

    /**
     * One of their arrows landed, on {@code hit} (null for a block): the Juju Shortbow's impact "creates a
     * 3-block radius that damages up to 3 mobs, with each mob taking the full initial damage" (the wiki) -
     * the one it hit takes the arrow's own hit, the rest one each as the bow's arrow would (their own crit
     * rolls: UNKNOWN whether they share the arrow's) - and a Nasty Bite heals them.
     */
    static void landed(AbstractArrow arrow, Player shooter, Entity hit) {
        ImpactArrow impact = IMPACT_ARROWS.remove(arrow.getUniqueId());
        Double heal = BITES.remove(arrow.getUniqueId());
        boolean onMob = hit != null && Hits.hittable(hit);
        if (heal != null && onMob) PlayerHealth.heal(shooter, heal);
        if (impact == null || !Hits.canStillHit(shooter)) return;
        int left = impact.mobs() - (onMob ? 1 : 0);
        Location at = arrow.getLocation();
        for (LivingEntity mob : Hits.near(at, IMPACT_RADIUS)) {
            if (left <= 0) break;
            if (mob.equals(hit)) continue;
            double travelled = mob.getBoundingBox().getCenter().distance(shooter.getEyeLocation().toVector());
            if (Hits.weaponHit(shooter, impact.shot(), mob, Hits.Strike.arrow(travelled, 1)) > 0) left--;
        }
    }

    /** An arrow that went without landing. */
    static void gone(UUID arrow) {
        IMPACT_ARROWS.remove(arrow);
        BITES.remove(arrow);
    }

    /**
     * The Mosquito Shortbow's Nasty Bite (LEFT CLICK, 10 Vitality): "Shoot an enhanced shot, healing you for
     * 189 Health on hit"; "This has a fixed cooldown of 0.5 seconds regardless of the player's Attack Speed"
     * (the wiki). What makes it enhanced is UNKNOWN: it's the bow's own arrow. Without the Vitality, or within
     * its 0.5 s, a left click shoots as ever. The heal is its text's number.
     */
    static final class NastyBite implements AbilityHandler {
        static final long COOLDOWN_MILLIS = 500;
        private static final Pattern HEAL = Pattern.compile("healing you for (\\d+)");

        @Override
        public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
            double vitality = item.blocks().stream().filter(b -> b.isAbility() && "Nasty Bite".equals(b.name()))
                    .mapToDouble(ItemBlock::vitality).findFirst().orElse(0);
            return Vitality.has(player, vitality) && PlayerSession.of(player).cooldownLeft("nasty_bite") <= 0;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            PlayerSession.of(player).startCooldown("nasty_bite", COOLDOWN_MILLIS);
            Location eye = player.getEyeLocation();
            Arrow arrow = player.getWorld().spawnArrow(eye.clone().add(eye.getDirection().multiply(0.7)), eye.getDirection(), 5, 1);
            // Before Shots.record, which makes it an arrow nobody can pick up.
            arrow.setShooter(player);
            Shots.record(arrow, player, tag, true);
            double heal = Bolts.number(block.text(), HEAL);
            if (heal > 0) BITES.put(arrow.getUniqueId(), heal);
            player.getWorld().playSound(eye, Sound.ENTITY_ARROW_SHOOT, 1, 1.2f);
        }
    }

    /**
     * The Ender Bow's Ender Warp (LEFT CLICK): "Shoots an Ender Pearl. Upon landing you deal damage to all
     * Monsters in a 8 block radius for 10% of their Health. (Max 500 damage)". The pearl takes them where it
     * lands, as a pearl does (UNKNOWN: the name says so). The damage is 10% of the health they have then, as it
     * is (no Defense: UNKNOWN). The pearl flies as a thrown one does.
     */
    static final class EnderWarp implements AbilityHandler {
        static final double RADIUS = 8;
        static final double SHARE = 0.1;
        static final double MOST = 500;

        /** What a mob with this much health takes: 10%, at most 500. */
        static double damage(double health) {
            return Math.min(MOST, Math.max(0, health) * SHARE);
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            Location eye = player.getEyeLocation();
            new Missile(player, eye, eye.getDirection().multiply(1.5))
                    .gravity(0.03)
                    .range(80)
                    .look(Missile.display(eye, new ItemStack(Material.ENDER_PEARL), 0.5f, 0))
                    .trail(at -> at.getWorld().spawnParticle(Particle.PORTAL, at, 3, 0.1, 0.1, 0.1, 0))
                    .onEnd((missile, at, impact) -> {
                        if (!impact) return;
                        Player caster = missile.caster();
                        // Where it landed, a little back along its way so they aren't put in the block.
                        Location to = at.clone().subtract(missile.velocity().normalize().multiply(0.5));
                        to.setDirection(caster.getLocation().getDirection());
                        caster.teleport(to);
                        SetBonuses.teleported(caster);
                        caster.setFallDistance(0);
                        caster.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        for (LivingEntity mob : Hits.near(to, RADIUS)) {
                            if (Hits.hittable(mob)) Hits.hurt(caster, mob, damage(Hits.target(mob).health()), DamageIndicators.Look.NORMAL);
                        }
                    })
                    .launch();
            player.getWorld().playSound(eye, Sound.ENTITY_ENDER_PEARL_THROW, 1, 0.6f);
        }
    }
}
