package net.icxd.dungeons.npc;

import java.util.List;
import java.util.Random;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * How an NPC sounds as it says a line or opens its menu, as Hypixel's did (recorded 2026-10-24),
 * played where the listener stands. Most speak with a villager's "yes" or hum, picked as they go
 * (Jerry, the Banker, the Auction Master and four others: no pattern in twenty lines), at their own
 * pitch (1, or 2 for Elizabeth, Clerk Seraphine and the Farm Merchant). Ophelia haggles, a
 * villager's trade at 1.7 to 1.9. Some say nothing (Malik, Saul, Mort).
 *
 * @param sounds the ones it picks from, by name (as the recording has them; names need no server, so this
 *               loads in tests)
 * @param pitch the middle of its pitch
 * @param spread how far either side of it a line's pitch may be
 */
public record Voice(List<String> sounds, float pitch, float spread) {
    private static final Random RANDOM = new Random();

    public static final Voice VILLAGER = villager(1);
    public static final Voice OPHELIA = new Voice(List.of("entity.villager.trade"), 1.8f, 0.1f);

    /** A villager's yes or hum at this pitch. */
    public static Voice villager(float pitch) {
        return new Voice(List.of("entity.villager.yes", "entity.villager.ambient"), pitch, 0);
    }

    /** A line's sound and its pitch. */
    record Said(String sound, float pitch) {
    }

    Said next(Random random) {
        String sound = sounds.get(random.nextInt(sounds.size()));
        return new Said(sound, spread == 0 ? pitch : pitch + (random.nextFloat() * 2 - 1) * spread);
    }

    public void speak(Player player) {
        Said said = next(RANDOM);
        player.playSound(player.getLocation(), said.sound(), SoundCategory.NEUTRAL, 1, said.pitch());
    }
}
