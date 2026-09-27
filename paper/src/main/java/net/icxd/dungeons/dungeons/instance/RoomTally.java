package net.icxd.dungeons.dungeons.instance;

/**
 * A room's starred mobs, counted: how many it has, how many of them haven't spawned yet and how many are
 * dead, and so when it's cleared (research mobs.md 1.4: when the last starred mob dies; a room with none is
 * cleared when it opens). Apart from the world, so it can be tested alone; {@link RoomMobs} keeps one a room.
 */
final class RoomTally {
    private int planned;
    private int waiting;
    private int dead;
    private boolean opened;
    private boolean cleared;

    /** One more starred mob, still to be spawned. */
    void plan() {
        planned++;
        waiting++;
    }

    /** One of the planned starred mobs is in the world. */
    void spawned() {
        waiting--;
    }

    /** One of them couldn't be spawned, so it isn't one to kill any more: true if that clears the room. */
    boolean failed() {
        planned--;
        waiting--;
        return check();
    }

    /** The room opened: true if that clears it (nothing starred left to kill). */
    boolean open() {
        if (opened) return false;
        opened = true;
        return check();
    }

    /** A starred mob died: true if it was the last. */
    boolean died() {
        dead++;
        return check();
    }

    /** Starred mobs it has (spawned or still to be). */
    int planned() {
        return planned;
    }

    boolean cleared() {
        return cleared;
    }

    /** Cleared once all are in the world and dead, and it's been opened (or one of them died anyway). */
    private boolean check() {
        if (cleared || waiting > 0 || dead < planned || !(opened || dead > 0)) return false;
        cleared = true;
        return true;
    }
}
