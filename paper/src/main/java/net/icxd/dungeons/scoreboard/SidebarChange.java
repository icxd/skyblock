package net.icxd.dungeons.scoreboard;

/**
 * How much a sidebar number (the purse, bits) just changed, for its " (+545)": Hypixel shows the new
 * number with the change for 3 updates of the sidebar (recorded for bits: 2,069, then "2,614
 * (+545)" at 00:18.5, 19.5 and 20.5, gone at 21.5; research coins.md 1.1). A change while one is
 * still shown adds to it and starts the 3 updates again; how Hypixel adds them up isn't known.
 */
final class SidebarChange {
    static final int UPDATES = 3;

    private Double last;
    private double change;
    private int left;

    /**
     * The number is {@code now} at this update: the change to show with it, or null for none. The
     * first update shows none (there's nothing to compare with yet).
     */
    Double update(double now) {
        if (last != null && now != last) {
            change = (left > 0 ? change : 0) + now - last;
            left = UPDATES;
        }
        last = now;
        if (left == 0) return null;
        left--;
        return change;
    }
}
