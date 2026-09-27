package net.icxd.dungeons.item.bonus;

/**
 * A count that belongs to one dungeon run and starts again in the next ("Strength resets after each
 * run"): a count in another run than the one it was kept for is 0. Runs are told apart by identity.
 */
final class RunCounter {
    private Object run;
    private int count;

    /** One more in this run; returns the count now. */
    int add(Object run) {
        if (run != this.run) {
            this.run = run;
            count = 0;
        }
        return ++count;
    }

    /** The count in this run: 0 for another, or none. */
    int get(Object run) {
        return run != null && run == this.run ? count : 0;
    }
}
