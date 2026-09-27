package net.icxd.dungeons.item.ability.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.utils.Replacement;

/**
 * The Spirit Boots' Spirit Glide, worn: "Press and hold Sneak to fly around as a spirit", 250 mana and every
 * 60 seconds (its data's). As the wiki has it since 0.19.2, sneaking launches them into the air and holding
 * sneak "will propel the player forward", "for a duration of approximately 5s, granting invincibility during
 * this duration". UNKNOWN, and plain here: it starts only on a sneak in mid-air (so that sneaking on the ground
 * stays sneaking), how high the launch and how fast the flight are, that letting go of sneak glides them down
 * gently, and what it looks and sounds like. It doesn't land them with fall damage. A ghost's (dead in a run)
 * boots don't glide.
 */
final class SpiritGlide {
    static final String NAME = "Spirit Glide";
    private static final long MILLIS = 5_000;
    private static final double LAUNCH = 1.0;
    private static final double FORWARD = 0.7;
    /** How fast they sink while not holding sneak. */
    private static final double SINK = -0.08;

    /** Until when, by player. */
    private static final Map<UUID, Long> GLIDING = new HashMap<>();

    private SpiritGlide() {
    }

    static boolean gliding(Player player) {
        return GLIDING.containsKey(player.getUniqueId());
    }

    /** They began to sneak: in mid-air with the boots on and the ability ready, off they go. */
    static void sneaked(Player player) {
        if (gliding(player) || player.isOnGround() || player.isFlying()) return;
        DungeonRun run = RunManager.of(player);
        if (run != null && run.isGhost(player.getUniqueId())) return;
        ItemBlock block = Worn.ability(player.getInventory().getBoots(), NAME);
        if (block == null) return;
        PlayerSession session = PlayerSession.of(player);
        String cooldown = "ability:" + NAME;
        if (session.cooldownLeft(cooldown) > 0) return;
        int mana = session.getMana() < 0 ? session.maxMana() : session.getMana();
        int cost = Abilities.manaCost(block, session.maxMana());
        if (mana < cost) {
            session.setManaReplacement(Replacement.forMillis("§c§lNOT ENOUGH MANA", 2000));
            return;
        }
        session.setMana(mana - cost);
        if (cost > 0) session.setDefenseReplacement(Replacement.forMillis("§b-" + cost + " Mana (§6" + NAME + "§b)", 400));
        if (block.cooldown() > 0) session.startCooldown(cooldown, (long) (block.cooldown() * 1000));
        GLIDING.put(player.getUniqueId(), System.currentTimeMillis() + MILLIS);
        Protection.immunity(player, NAME, MILLIS);
        player.setVelocity(player.getVelocity().setY(LAUNCH));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1, 1.2f);
    }

    /** Every tick: holding sneak flies them on, else they sink slowly; after 5 seconds they're themselves again. */
    static void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : List.copyOf(GLIDING.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || player.isDead() || now >= entry.getValue()) {
                GLIDING.remove(entry.getKey());
                if (player != null) player.setFallDistance(0);
                continue;
            }
            player.setFallDistance(0);
            Vector velocity = player.getVelocity();
            if (player.isSneaking()) {
                velocity = player.getLocation().getDirection().multiply(FORWARD);
            } else if (velocity.getY() < SINK) {
                velocity.setY(SINK);
            }
            player.setVelocity(velocity);
            player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    static void forget(Player player) {
        GLIDING.remove(player.getUniqueId());
    }
}
