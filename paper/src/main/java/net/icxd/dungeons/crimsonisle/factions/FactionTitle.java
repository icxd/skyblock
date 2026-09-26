package net.icxd.dungeons.crimsonisle.factions;

import lombok.Getter;

@Getter
public enum FactionTitle {
    NEUTRAL("Neutral", 0),
    FRIENDLY("Friendly", 1000),
    TRUSTED("Trusted", 3000),
    HONORED("Honored", 6000),
    HERO("Hero", 12000);

    private final String name;
    private final int reputation;
    FactionTitle(String name, int reputation) {
        this.name = name;
        this.reputation = reputation;
    }

    public static FactionTitle get(int reputation) {
        for (int i = values().length - 1; i >= 0; i--) {
            FactionTitle title = values()[i];
            if (reputation >= title.getReputation()) return title;
        }
        return null;
    }

    public FactionTitle next() {
        if (ordinal() == values().length - 1) return null;
        return values()[ordinal() + 1];
    }
}
