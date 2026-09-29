package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.CombatState;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.MobDebuffs;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerHealth;

/**
 * The Tactical Insertion's Gorilla Tactics: "Marks your location and teleport back there after 3s. On coming back,
 * burn enemies within 3 blocks and set your ❤ Health to HALF of what it was. The burn deals 10% of ALL damage you
 * dealt within the 3s, spread over 6s." (its text's numbers). The wiki's Tactical Insertion: "sets the player's
 * health to half of what it was prior to teleporting", and it "can deal full damage to all mobs, regardless of any
 * damage reduction". So everything they deal in those 3 seconds is counted ({@link CombatState#addDealtListener}),
 * and back at the mark, each of SkyBlock's mobs within 3 blocks of it burns for a tenth of it, a sixth a second for 6
 * seconds, as it is (no Defense). Nothing if they're gone, dead or a ghost by then. Its look is UNKNOWN. Main thread.
 */
final class GorillaTactics implements AbilityHandler {
    private static final Pattern BACK = Pattern.compile("teleport back there after (\\d+)s");
    private static final Pattern WITHIN = Pattern.compile("burn enemies within (\\d+) blocks");
    private static final Pattern BURN = Pattern.compile("The burn deals (\\d+)% of ALL damage you dealt within the \\d+s, spread over (\\d+)s");

    /** What each marked player has dealt since they cast it. */
    private static final Map<UUID, double[]> DEALT = new HashMap<>();

    static void register() {
        CombatState.addDealtListener((player, damage) -> {
            double[] dealt = DEALT.get(player.getUniqueId());
            if (dealt != null) dealt[0] += damage;
        });
    }

    static void forget(UUID player) {
        DEALT.remove(player);
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        int back = (int) number(BACK, plain, 1, 3);
        double radius = number(WITHIN, plain, 1, 3);
        double share = number(BURN, plain, 1, 10) / 100;
        int seconds = (int) number(BURN, plain, 2, 6);
        Location mark = player.getLocation();
        UUID id = player.getUniqueId();
        double[] dealt = {0};
        DEALT.put(id, dealt);
        mark.getWorld().spawnParticle(Particle.FLAME, mark.clone().add(0, 0.1, 0), 20, 0.4, 0, 0.4, 0.01);
        player.playSound(mark, Sound.BLOCK_BEACON_ACTIVATE, 1, 1.6f);
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (DEALT.get(id) != dealt) return;
            DEALT.remove(id);
            if (!Hits.canStillHit(player) || !player.getWorld().equals(mark.getWorld())) return;
            PlayerHealth.set(player, PlayerHealth.get(player) / 2);
            player.teleport(mark);
            SetBonuses.teleported(player);
            player.setFallDistance(0);
            mark.getWorld().spawnParticle(Particle.LAVA, mark, 20, radius / 2, 0.3, radius / 2, 0);
            mark.getWorld().playSound(mark, Sound.ENTITY_BLAZE_SHOOT, 1, 0.8f);
            double each = burnEachSecond(dealt[0], share, seconds);
            if (each <= 0) return;
            for (LivingEntity mob : Hits.near(mark, radius)) {
                mob.setFireTicks(seconds * 20);
                MobDebuffs.dot(mob, "Gorilla Tactics", player, each, 20, seconds, DamageIndicators.Look.FIRE);
            }
        }, back * 20L);
    }

    /** A tenth of what they dealt, spread evenly over the seconds. */
    static double burnEachSecond(double dealt, double share, int seconds) {
        return seconds <= 0 ? 0 : Math.max(0, dealt) * share / seconds;
    }

    private static double number(Pattern pattern, String plain, int group, double otherwise) {
        Matcher m = pattern.matcher(plain);
        return m.find() ? Double.parseDouble(m.group(group)) : otherwise;
    }
}
