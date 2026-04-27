package net.icxd.dungeons.item.gemstone;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class GemstoneSlots {
    private final List<GemstoneSlot> slots;
    public GemstoneSlots(GemstoneSlot... slots) {
        this.slots = Arrays.asList(slots);
    }
}
