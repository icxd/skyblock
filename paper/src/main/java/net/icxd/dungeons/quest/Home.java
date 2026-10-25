package net.icxd.dungeons.quest;

/**
 * Where a quest is followed: the boss bar and sidebar show the objective of the quest most at home
 * where the player is. An area's own quest first (the Bank's "Deposit Coins in the Bank" in the
 * Bank), then the island's (the Hub's "Talk to the Bartender" anywhere in the Hub, Getting Started on
 * your island), then one that's at home anywhere ("Talk to Marco"). That order is the recording's
 * (2026-10-24): every change of the boss bar there fits it.
 *
 * @param island    where it's at home; null for nowhere in particular
 * @param area      the area of that island; null for all of it
 * @param elsewhere how much it's followed everywhere else: 1 if anywhere (Marco's quest is, but most
 *                  of all in the Flower House, as recorded), 0 if not
 */
public record Home(Place.Island island, String area, int elsewhere) {
    public static Home anywhere() {
        return new Home(null, null, 1);
    }

    /** Never followed: Saving Up's "Talk to the Banker" wasn't, even in the Bank (both recordings). */
    public static Home nowhere() {
        return new Home(null, null, 0);
    }

    public static Home on(Place.Island island) {
        return new Home(island, null, 0);
    }

    public static Home in(Place.Island island, String area) {
        return new Home(island, area, 0);
    }

    /** This, and followed anywhere else too, as a quest that belongs anywhere is. */
    public Home orAnywhere() {
        return new Home(island, area, 1);
    }

    /** How much at home it is there: 3 in its area, 2 on its island, 1 for anywhere, 0 if it isn't followed there. */
    int score(Place place) {
        if (island != null && island == place.island() && (area == null || area.equals(place.area()))) return area == null ? 2 : 3;
        return elsewhere;
    }
}
