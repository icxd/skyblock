package net.icxd.dungeons.dungeons;

public enum DungeonState {
    NONE, PREPARING, STARTED, BOSS_FIGHT, ENDED;

    public DungeonState next() {
        return values()[ordinal() + 1];
    }
}
