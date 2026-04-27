package net.icxd.dungeons.item.enums;

import lombok.Getter;

public enum GenericItemType {
    ARMOR(SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS),
    EQUIPMENT(SpecificItemType.CLOAK, SpecificItemType.BELT, SpecificItemType.NECKLACE, SpecificItemType.GLOVES, SpecificItemType.BRACELET, SpecificItemType.GAUNTLET),
    WEAPON(SpecificItemType.SWORD, SpecificItemType.WAND, SpecificItemType.LONGSWORD, SpecificItemType.FISHING_WEAPON, SpecificItemType.BOW),
    TOOL(SpecificItemType.PICKAXE, SpecificItemType.SHEARS, SpecificItemType.AXE, SpecificItemType.SPADE, SpecificItemType.HOE, SpecificItemType.DRILL),
    CONSUMABLE(SpecificItemType.BAIT, SpecificItemType.PET_ITEM, SpecificItemType.DUNGEON_PASS, SpecificItemType.REFORGE_STONE, SpecificItemType.COSMETIC, SpecificItemType.PORTAL, SpecificItemType.TRAVEL_SCROLL),
    OTHER(SpecificItemType.NONE, SpecificItemType.DEPLOYABLE, SpecificItemType.ARROW, SpecificItemType.ARROW_POISON, SpecificItemType.ACCESSORY);

    @Getter
    private final SpecificItemType[] specificItemTypes;

    GenericItemType(SpecificItemType... specificItemTypes) {
        this.specificItemTypes = specificItemTypes;
    }

    public static GenericItemType get(SpecificItemType specificItemType) {
        for (GenericItemType genericItemType : values()) {
            for (SpecificItemType type : genericItemType.specificItemTypes) {
                if (type == specificItemType) {
                    return genericItemType;
                }
            }
        }
        return null;
    }
}
