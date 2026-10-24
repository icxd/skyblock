package net.icxd.dungeons.quest;

/**
 * Where a quest is followed: the boss bar and sidebar show the objective of the quest most at home
 * where the player is. An area's own quest first (the Bank's "Deposit Coins in the Bank" in the
 * Bank), then the island's (the Hub's "Talk to the Bartender" anywhere in the Hub, Getting Started on
 * your island), then one that's at home anywhere ("Talk to Marco"). That order is the recording's
 * (2026-10-24): every change of the boss bar there fits it.
 *
 * @param island where it's at home; null for anywhere
 * @param area the area of that island; null for all of it
 */
public record Home(Place.Island island, String area) {
    public static Home anywhere() {
        return new Home(null, null);
    }

    public static Home on(Place.Island island) {
        return new Home(island, null);
    }

    public static Home in(Place.Island island, String area) {
        return new Home(island, area);
    }

    /** How much at home it is there: 3 in its area, 2 on its island, 1 for anywhere, 0 if it isn't followed there. */
    int score(Place place) {
        if (island == null) return 1;
        if (island != place.island()) return 0;
        if (area == null) return 2;
        return area.equals(place.area()) ? 3 : 0;
    }
}
