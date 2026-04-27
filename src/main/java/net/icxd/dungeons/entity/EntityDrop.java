package net.icxd.dungeons.entity;

import lombok.Getter;
import net.icxd.dungeons.entity.enums.EntityDropType;
import net.icxd.dungeons.item.SkyBlockItem;

@Getter
public class EntityDrop {
    private final SkyBlockItem item;
    private final EntityDropType type;
    private final double chance;
    private final int minAmount;
    private final int maxAmount;

    public EntityDrop(SkyBlockItem item, EntityDropType type, double chance, int minAmount, int maxAmount) {
        this.item = item;
        this.type = type;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public EntityDrop(SkyBlockItem item, EntityDropType type, double chance, int amount) {
        this(item, type, chance, amount, amount);
    }

    public EntityDrop(SkyBlockItem item, EntityDropType type, double chance) {
        this(item, type, chance, 1, 1);
    }
}
