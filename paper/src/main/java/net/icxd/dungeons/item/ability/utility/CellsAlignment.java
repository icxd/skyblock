package net.icxd.dungeons.item.ability.utility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerHealth;

/**
 * The Gyrokinetic Wand's Cells Alignment: "Apply Aligned to yourself for 6s, plus 4 nearby players on grouped islands.
 * (Catacombs, etc.) ||| Aligned: Splits incoming damage and applies it over 3s." The wiki's Gyrokinetic Wand: "When any
 * player with Aligned takes damage, it is reduced by an amount, with the remaining damage applied over 3s instead.
 * Whenever the player takes further damage, the delayed damage will refresh until either the player stops taking
 * further damage or the buff expires." So a hit takes a share now and leaves the rest waiting, and what waits is
 * taken a second at a time over the next 3 seconds, starting again with each hit while Aligned lasts. The share now is
 * UNKNOWN: a third (the hit spread evenly over the 3 seconds). What waits goes through their absorption, then their
 * health, and can kill. The others are their run's 4 nearest living teammates within 20 blocks (UNKNOWN how far
 * "nearby" is; the others in a run only: grouped islands are the Catacombs here). Its 2 Soulflow isn't charged: there's
 * no Soulflow (LATER).
 */
final class CellsAlignment implements AbilityHandler {
    static final String NAME = "Cells Alignment";
    private static final Pattern FOR = Pattern.compile("Apply Aligned to yourself for ([\\d.]+)s");
    private static final Pattern OVER = Pattern.compile("applies it over ([\\d.]+)s");
    private static final Pattern PLAYERS = Pattern.compile("plus (\\d+) nearby players");
    private static final double NEARBY = 20;

    /** Aligned on a player: until when, what waits, over how many more seconds, and the ticks to the next. */
    private static final class Aligned {
        long until;
        int over;
        double waiting;
        int secondsLeft;
        int ticksToNext = 20;
    }

    private static final Map<UUID, Aligned> ALIGNED = new HashMap<>();

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        long millis = (long) (number(FOR, plain, 6) * 1000);
        int over = (int) Math.max(1, number(OVER, plain, 3));
        int others = RunManager.inRun(player) ? (int) number(PLAYERS, plain, 4) : 0;
        for (Player aligned : Buffs.youAndNearby(player, NEARBY, others)) {
            if (aligned != player && RunManager.of(aligned) != RunManager.of(player)) continue;
            Aligned state = ALIGNED.computeIfAbsent(aligned.getUniqueId(), id -> new Aligned());
            state.until = System.currentTimeMillis() + millis;
            state.over = over;
            aligned.getWorld().spawnParticle(Particle.PORTAL, aligned.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.2);
        }
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
    }

    private static double number(Pattern pattern, String plain, double otherwise) {
        Matcher m = pattern.matcher(plain);
        return m.find() ? Double.parseDouble(m.group(1)) : otherwise;
    }

    /** A shield: what of a hit they take now while Aligned (a share of it, see {@link #now}); the rest waits. */
    static double aligned(Player player, double taken) {
        Aligned state = ALIGNED.get(player.getUniqueId());
        if (state == null || System.currentTimeMillis() >= state.until || taken <= 0) return taken;
        double now = now(taken, state.over);
        state.waiting += taken - now;
        state.secondsLeft = state.over;
        state.ticksToNext = 20;
        return now;
    }

    /** What of a hit is taken at once when it's spread over {@code seconds}: an even share (UNKNOWN). */
    static double now(double taken, int seconds) {
        return taken / Math.max(1, seconds);
    }

    /** What waits is taken this second: an even share of it for each second left. */
    static double due(double waiting, int secondsLeft) {
        return secondsLeft <= 1 ? waiting : waiting / secondsLeft;
    }

    /** Every tick: what waits is taken once a second; Aligned goes once it's over and nothing waits. */
    static void tick() {
        if (ALIGNED.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Aligned>> it = ALIGNED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Aligned> entry = it.next();
            Aligned state = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || player.isDead() || player.isInvulnerable()) {
                it.remove();
                continue;
            }
            if (state.waiting > 0 && --state.ticksToNext <= 0) {
                state.ticksToNext = 20;
                double due = due(state.waiting, state.secondsLeft);
                state.waiting -= due;
                state.secondsLeft = Math.max(0, state.secondsLeft - 1);
                double toHealth = Absorption.absorb(player, due);
                if (toHealth > 0) PlayerHealth.damage(player, toHealth);
            }
            if (now >= state.until && state.waiting <= 1e-9) it.remove();
        }
    }

    static void forget(UUID player) {
        ALIGNED.remove(player);
    }
}
