package net.icxd.dungeons.item.ability.weapons;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerHealth;

/**
 * Homing roses that bounce between enemies. Flower of Truth's Heat-Seeking Rose: "Shoots a rose that
 * ricochets between enemies, damaging up to 3 of your foes! Damage multiplies as more enemies are hit";
 * Bouquet of Lies' Petal Barrage: "Fire a barrage of 3 roses that ricochets between enemies, hitting up to
 * 5 foes. For every 10% of your health missing, your roses deal 2% more damage." A rose "can travel up to
 * 20 blocks before disappearing. After traveling 5 blocks, it homes in on enemies within 10 blocks ... The
 * homing rose's damage is doubled on the first bounce and tripled on the final bounce. The rose is
 * destroyed upon hitting a block"; "the damage does not scale with Intelligence or Ability Damage: instead,
 * it uses the same calculation as melee damage, including factoring in Strength and Crit Damage", without
 * the enchantments that don't count for abilities; its health cost can't be paid from the last of their
 * health (the wiki). How fast a rose flies and how far apart the barrage's go are UNKNOWN (a block a tick,
 * 10° apart); the barrage's bounces aren't said to multiply, so they don't.
 */
final class Roses implements AbilityHandler {
    static final double RANGE = 20;
    static final double HOMES_AFTER = 5;
    static final double HOMES_WITHIN = 10;
    private static final double SPEED = 1;
    private static final float SPREAD = 10;

    private final int roses;
    private final int mostHit;
    private final boolean multiplies;

    Roses(int roses, int mostHit, boolean multiplies) {
        this.roses = roses;
        this.mostHit = mostHit;
        this.multiplies = multiplies;
    }

    /** The Heat-Seeking Rose's damage on its {@code hit}th enemy (from 0): x1, then doubled, then tripled. */
    static double bounceFactor(int hit) {
        return hit + 1;
    }

    /** The barrage's +2% for every whole 10% of their health missing. */
    static double missingHealthFactor(double health, double maxHealth) {
        if (maxHealth <= 0) return 1;
        double missing = Math.max(0, Math.min(1, 1 - health / maxHealth));
        return 1 + 0.02 * Math.floor(missing * 10 + 1e-9);
    }

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return Hits.canPayHealth(player, block.healthCost());
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        // The barrage's bonus is for the health missing when it's fired, after its cost.
        Hits.payHealth(player, block.healthCost());
        double bonus = multiplies ? 1 : missingHealthFactor(PlayerHealth.get(player), PlayerHealth.max(player));
        Location eye = player.getEyeLocation();
        for (int i = 0; i < roses; i++) {
            Location from = eye.clone();
            from.setYaw(eye.getYaw() + (i - (roses - 1) / 2f) * SPREAD);
            int[] hits = {0};
            new Missile(player, eye, from.getDirection().multiply(SPEED))
                    .range(RANGE)
                    .look(Missile.display(eye, new ItemStack(Material.POPPY), 0.6f, 0))
                    .steer(missile -> missile.travelled() < HOMES_AFTER ? null : homing(missile))
                    .onHit((missile, mob) -> {
                        double factor = (multiplies ? bounceFactor(hits[0]) : 1) * bonus;
                        Hits.weaponHit(missile.caster(), tag, mob, new Hits.Strike(false, 0, factor, false, true));
                        return ++hits[0] < mostHit;
                    })
                    .launch();
        }
        player.getWorld().playSound(eye, Sound.BLOCK_GRASS_BREAK, 1, 1.4f);
    }

    /** Towards the nearest enemy within 10 blocks it hasn't hit; straight on if there's none. */
    private static Vector homing(Missile missile) {
        Location at = missile.at();
        List<LivingEntity> near = Hits.near(at, HOMES_WITHIN);
        for (LivingEntity mob : near) {
            if (missile.touched(mob)) continue;
            return mob.getBoundingBox().getCenter().subtract(at.toVector());
        }
        return null;
    }
}
