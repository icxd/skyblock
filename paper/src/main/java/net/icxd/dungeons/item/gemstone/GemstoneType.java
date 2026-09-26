package net.icxd.dungeons.item.gemstone;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum GemstoneType {
    RUBY("Ruby", '❤', 'c'),
    AMETHYST("Amethyst", '❈', '5'),
    JADE("Jade", '☘', 'a'),
    SAPPHIRE("Sapphire", '✎', 'b'),
    AMBER("Amber", '⸕', '6'),
    TOPAZ("Topaz", '✧', 'e'),
    JASPER("Jasper", '❁', 'd'),
    OPAL("Opal", '❂', 'f'),
    ONYX("Onyx", '☠', '8'),
    AQUAMARINE("Aquamarine", '☂', '3'),
    CITRINE("Citrine", '☘', '4'),
    PERIDOT("Peridot", '☘', '2'),

    COMBAT("Combat", '⚔', '4', Arrays.asList(GemstoneType.RUBY, GemstoneType.AMETHYST, GemstoneType.SAPPHIRE, GemstoneType.JASPER, GemstoneType.ONYX)),
    OFFENSIVE("Offensive", '☠', '9', Arrays.asList(GemstoneType.SAPPHIRE, GemstoneType.JASPER)),
    DEFENSIVE("Defensive", '☤', 'a', Arrays.asList(GemstoneType.RUBY, GemstoneType.AMETHYST)),
    MINING("Mining", '✦', '7', Arrays.asList(GemstoneType.JADE, GemstoneType.AMBER, GemstoneType.TOPAZ)),
    /** The Fossil Excavator's chisels: the gems that give a chisel bonus. */
    CHISEL("Chisel", '❥', '6', Arrays.asList(GemstoneType.ONYX, GemstoneType.AQUAMARINE, GemstoneType.CITRINE, GemstoneType.PERIDOT)),
    UNIVERSAL("Universal", '\u2742', 'f', Arrays.asList(GemstoneType.RUBY, GemstoneType.AMETHYST, GemstoneType.JADE, GemstoneType.SAPPHIRE, GemstoneType.AMBER, GemstoneType.TOPAZ, GemstoneType.JASPER, GemstoneType.OPAL,
            GemstoneType.ONYX, GemstoneType.AQUAMARINE, GemstoneType.CITRINE, GemstoneType.PERIDOT));
    ;

    private final List<GemstoneType> types;
    private final String name;
    private final char icon;
    /** Its colour code (the character after '§'). */
    private final char color;

    GemstoneType(String name, char icon, char color, List<GemstoneType> types) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.types = types;
    }

    GemstoneType(String name, char icon, char color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.types = null;
    }
}
