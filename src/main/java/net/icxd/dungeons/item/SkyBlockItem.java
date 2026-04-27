package net.icxd.dungeons.item;

import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.cost.UpgradeCosts;
import net.icxd.dungeons.item.enchanting.Enchantment;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.Soulbound;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.prestige.Prestige;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.stats.Stats;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Material;

import java.awt.*;
import java.util.List;

public interface SkyBlockItem {
    String name();
    Material material();

    default String skin() { return null; }
    default String description() { return null; }

    default int itemDurability() { return 0; }
    default int durability() { return 0; }
    default double npcSellPrice() { return 0; }

    default Color color() { return null; }

    default SpecificItemType specificItemType() { return SpecificItemType.NONE; }
    default GenericItemType genericItemType() { return GenericItemType.get(specificItemType()); }

    default Rarity rarity() { return Rarity.COMMON; }
    default int gearScore() { return -1; }
    default Stats stats() { return new Stats(); }
    default Ability ability() { return null; }

    default Soulbound soulbound() { return Soulbound.NONE; }
    default Requirements requirements() { return null; }
    default UpgradeCosts upgradeCosts() { return null; }
    default Prestige prestige() { return null; }
    default GemstoneSlots gemstoneSlots() { return null; }

    default boolean canHaveAttributes() { return false; }
    default boolean dungeonItem() { return false; }
    default boolean glowing() { return false; }
    default boolean museum() { return false; }
    default boolean unstackable() { return false; }

    default NBTTagCompound nbt() { return null; }
    default List<String> nbtLore(NBTTagCompound compound) { return null; }

    default List<Enchantment> enchantments() { return null; }

    default boolean isOwnable() { return requirements() != null; }
    default String id() { return name().toLowerCase().replace(" ", "_").replace("'", ""); }
}
