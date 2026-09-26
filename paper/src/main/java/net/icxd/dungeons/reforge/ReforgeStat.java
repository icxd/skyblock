package net.icxd.dungeons.reforge;

import net.icxd.dungeons.item.enums.Rarity;

/** One stat of a reforge, at each rarity. */
public record ReforgeStat(double common, double uncommon, double rare, double epic, double legendary, double mythic) {
    /** 0 for rarities above mythic (divine, special, ...). */
    public double at(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> common;
            case UNCOMMON -> uncommon;
            case RARE -> rare;
            case EPIC -> epic;
            case LEGENDARY -> legendary;
            case MYTHIC -> mythic;
            default -> 0;
        };
    }
}
