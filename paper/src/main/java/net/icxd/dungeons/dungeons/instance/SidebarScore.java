package net.icxd.dungeons.dungeons.instance;

import java.util.function.IntSupplier;

/**
 * The score in brackets after "Cleared:" on the sidebar. Hypixel works it out every 10 seconds from
 * the start rather than when something changes (recorded: 01:14.1, 01:24.5, 01:34.5, ...), and it
 * shows 0 until the first time; after the run the next update shows the score card's total
 * (research score_rewards.md 1.6).
 */
final class SidebarScore {
    static final long EVERY_MILLIS = 10_000;

    private long next;
    private int shown;

    /** The run starts: 0 until the first update, 10 seconds from now. */
    void start(long now) {
        next = now + EVERY_MILLIS;
        shown = 0;
    }

    /** Every second or so: the score, if an update is due. */
    void tick(long now, IntSupplier score) {
        if (next == 0 || now < next) return;
        shown = score.getAsInt();
        while (next <= now) next += EVERY_MILLIS;
    }

    int shown() {
        return shown;
    }
}
