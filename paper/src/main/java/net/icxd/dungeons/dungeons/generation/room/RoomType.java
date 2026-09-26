package net.icxd.dungeons.dungeons.generation.room;

public enum RoomType {
    EMPTY,
    REGULAR,
    /** 1x1 brown dead end, only used when a 1x1 regular room ends up with a single door. */
    RARE,
    START,
    FAIRY,
    PUZZLE,
    /** Yellow room. */
    MINIBOSS,
    TRAP,
    BLOOD,
}
