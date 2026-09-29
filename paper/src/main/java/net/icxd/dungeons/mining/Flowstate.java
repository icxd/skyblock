package net.icxd.dungeons.mining;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Flowstate (ultimate, mining tools): "Consecutive blocks broken grant +1⸕ Mining Speed. Stops after 10s of not mining
 * and caps at 200 blocks." at I (+2 at II, +3 at III); the numbers are its text's. A streak of blocks broken with a
 * Flowstate tool, which ends 10 s after the last and starts again in another world ("after mining 200 blocks in the
 * same world", the wiki's Mining Speed), gives its Mining Speed while they hold a tool with it: holding another doesn't
 * end it ("this does not reset the timer", the wiki's Flowstate). Blocks broken with another tool don't count (as
 * SkyHanni's Flowstate helper counts; UNKNOWN on Hypixel). Its Mining Speed goes through their stats (a PlayerStats
 * modifier, BlockListener's), so the Stats menu shows it and mining uses it. Main thread.
 */
final class Flowstate {
    static final String ID = "flowstate";
    /** "grant &6+1⸕ Mining Speed&7", "Stops after &a10s", "caps at &a200 &7blocks". */
    private static final Pattern PER_BLOCK = Pattern.compile("&.\\+([\\d.]+)\\S? Mining Speed");
    private static final Pattern STOPS = Pattern.compile("after &a([\\d.]+)s");
    private static final Pattern CAP = Pattern.compile("caps at &a([\\d,]+)");

    /** A streak: its blocks, the world it's in and when it ends. */
    private record Streak(int blocks, UUID world, long endsAt) {
    }

    /** Its numbers at a level, from its text: Mining Speed a block, seconds it lasts, blocks it caps at. */
    record Numbers(double perBlock, double seconds, int cap) {
        static Numbers of(String text) {
            if (text == null) return null;
            Matcher perBlock = PER_BLOCK.matcher(text);
            Matcher stops = STOPS.matcher(text);
            Matcher cap = CAP.matcher(text);
            if (!perBlock.find() || !stops.find() || !cap.find()) return null;
            return new Numbers(Double.parseDouble(perBlock.group(1)), Double.parseDouble(stops.group(1)),
                    Integer.parseInt(cap.group(1).replace(",", "")));
        }
    }

    private static final Map<UUID, Streak> STREAKS = new HashMap<>();

    private Flowstate() {
    }

    /** A block broken: one more on their streak if they hold a Flowstate tool (a new streak if the last has ended). */
    static void broke(Player player) {
        Numbers numbers = numbers(Combat.heldEnchantments(player).getOrDefault(ID, 0));
        if (numbers == null) return;
        long now = System.currentTimeMillis();
        UUID world = player.getWorld().getUID();
        Streak streak = STREAKS.get(player.getUniqueId());
        int blocks = streak == null || !streak.world().equals(world) || now >= streak.endsAt() ? 1 : streak.blocks() + 1;
        STREAKS.put(player.getUniqueId(), new Streak(blocks, world, now + (long) (numbers.seconds() * 1000)));
    }

    /** Their streak's Mining Speed on their stats, while it lasts and they hold a Flowstate tool. */
    static void addSpeed(Player player, Stats stats) {
        Streak streak = STREAKS.get(player.getUniqueId());
        if (streak == null) return;
        if (System.currentTimeMillis() >= streak.endsAt() || !streak.world().equals(player.getWorld().getUID())) {
            STREAKS.remove(player.getUniqueId());
            return;
        }
        Numbers numbers = numbers(Combat.heldEnchantments(player).getOrDefault(ID, 0));
        if (numbers != null) stats.add(Stat.MINING_SPEED, speed(numbers, streak.blocks()));
    }

    /** A streak's Mining Speed: so much a block, up to the cap's worth. */
    static double speed(Numbers numbers, int blocks) {
        return numbers.perBlock() * Math.min(Math.max(0, blocks), numbers.cap());
    }

    static void forget(UUID player) {
        STREAKS.remove(player);
    }

    /** Its numbers at a level, read from the table once (the stats ask every tick while a streak lasts). */
    private static Numbers numbers(int level) {
        if (level <= 0) return null;
        EnchantmentData data = EnchantmentData.current();
        if (data != readFrom) {
            NUMBERS.clear();
            readFrom = data;
        }
        return NUMBERS.computeIfAbsent(level, l -> {
            EnchantmentType type = EnchantmentType.getByNamespace(ID);
            return type == null ? null : Numbers.of(type.getDescription(l));
        });
    }

    private static final Map<Integer, Numbers> NUMBERS = new HashMap<>();
    /** The table {@link #NUMBERS} were read from. */
    private static EnchantmentData readFrom;
}
