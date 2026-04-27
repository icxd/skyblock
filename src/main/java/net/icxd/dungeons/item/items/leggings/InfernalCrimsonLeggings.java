package net.icxd.dungeons.item.items.leggings;

import net.icxd.dungeons.item.*;
import net.icxd.dungeons.item.cost.*;
import net.icxd.dungeons.item.cost.coins.*;
import net.icxd.dungeons.item.cost.essence.*;
import net.icxd.dungeons.item.cost.item.*;
import net.icxd.dungeons.item.enums.*;
import net.icxd.dungeons.item.gemstone.*;
import net.icxd.dungeons.item.prestige.*;
import net.icxd.dungeons.item.requirement.*;
import net.icxd.dungeons.item.requirement.skill.*;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;

public class InfernalCrimsonLeggings implements SkyBlockItem {
    @Override public Material material() { return Material.LEATHER_LEGGINGS; }
    @Override public Color color() { return new Color(230,97,5); }
    @Override public String name() { return "Infernal Crimson Leggings"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.LEGGINGS; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Soulbound soulbound() { return Soulbound.COOP; }
    @Override public Stats stats() { return new Stats(0, 0, 517, 139, 76, 13, 0, 0, 0, 0, 0, 0, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 24)); }
    @Override public boolean canHaveAttributes() { return true; }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 30000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 35000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 41000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 48000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 56000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 65500)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 76000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 89000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 3)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 105000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 4)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 120000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 5)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 140000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 6)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 165000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 7)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 192000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 8)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 225000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 9)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 265000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 10))); }
    @Override public String id() { return "INFERNAL_CRIMSON_LEGGINGS"; }
}
