package net.icxd.dungeons.crimsonisle.factions;

import lombok.Getter;
import org.bukkit.ChatColor;

@Getter
public enum FactionType {
    BARBARIAN("Barbarian", '⚒', ChatColor.RED),
    MAGE("Mage", 'ቾ', ChatColor.DARK_PURPLE); // TODO: Fix icon.

    private final String name;
    private final char icon;
    private final ChatColor color;
    FactionType(String name, char icon, ChatColor color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
    }
}
