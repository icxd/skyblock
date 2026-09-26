package net.icxd.dungeons.crimsonisle.kuudra;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum KuudraTier {
    NONE(0),
    BASIC(1),
    HOT(2),
    BURNING(3),
    FIERY(4),
    INFERNAL(5);

    private final int tier;
}
