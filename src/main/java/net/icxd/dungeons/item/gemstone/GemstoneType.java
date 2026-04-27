package net.icxd.dungeons.item.gemstone;

import lombok.Getter;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.List;

@Getter
public enum GemstoneType {
    RUBY("Ruby", '❤', ChatColor.RED),
    AMETHYST("Amethyst", '❈', ChatColor.DARK_PURPLE),
    JADE("Jade", '☘', ChatColor.GREEN),
    SAPPHIRE("Sapphire", '✎', ChatColor.AQUA),
    AMBER("Amber", '⸕', ChatColor.GOLD),
    TOPAZ("Topaz", '✧', ChatColor.YELLOW),
    JASPER("Jasper", '❁', ChatColor.LIGHT_PURPLE),
    OPAL("Opal", '❂', ChatColor.WHITE),

    COMBAT("Combat", '⚔', ChatColor.DARK_RED, Arrays.asList(GemstoneType.RUBY, GemstoneType.AMETHYST, GemstoneType.SAPPHIRE, GemstoneType.JASPER)),
    OFFENSIVE("Offensive", '☠', ChatColor.BLUE, Arrays.asList(GemstoneType.SAPPHIRE, GemstoneType.JASPER)),
    DEFENSIVE("Defensive", '☤', ChatColor.GREEN, Arrays.asList(GemstoneType.RUBY, GemstoneType.AMETHYST)),
    MINING("Mining", '✦', ChatColor.GRAY, Arrays.asList(GemstoneType.JADE, GemstoneType.AMBER, GemstoneType.TOPAZ)),
    UNIVERSAL("Universal", '✪', ChatColor.WHITE, Arrays.asList(GemstoneType.RUBY, GemstoneType.AMETHYST, GemstoneType.JADE, GemstoneType.SAPPHIRE, GemstoneType.AMBER, GemstoneType.TOPAZ, GemstoneType.JASPER, GemstoneType.OPAL));
    ;

    private final List<GemstoneType> types;
    private final String name;
    private final char icon;
    private final ChatColor color;

    GemstoneType(String name, char icon, ChatColor color, List<GemstoneType> types) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.types = types;
    }

    GemstoneType(String name, char icon, ChatColor color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.types = null;
    }
}
