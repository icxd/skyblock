package net.icxd.dungeons.crimsonisle.factions;

import lombok.Getter;

@Getter
public enum FactionType {
    BARBARIAN("Barbarian", '⚒', "§c"),
    MAGE("Mage", 'ቾ', "§5"); // TODO: Fix icon.

    private final String name;
    private final char icon;
    private final String color;
    FactionType(String name, char icon, String color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
    }
}
