package net.icxd.dungeons.dwarven;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PowderType {
    MITHRIL("§2"), GEMSTONE("§d"), GLACITE("§b");

    private final String color;
}
