package net.icxd.dungeons.item.items.helmet;

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

public class InfernalCrimsonHelmet implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTY0NTUwNDM1MTEzMSwKICAicHJvZmlsZUlkIiA6ICI4ODBiZWMwYTE0MmM0YzRlYTJlZjliMTFiMTBkNWNiNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJmZ2FiIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzUwN2Q2YmY3NjExMTkwZWQ5YzU4MGQ4Yzg3YzI5NjA1OTIxNjIzMGM5NTAxZWVjNjM1OWUwZDYwZWM4NDc1OGUiCiAgICB9CiAgfQp9"; }
    @Override public String name() { return "Infernal Crimson Helmet"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.HELMET; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Soulbound soulbound() { return Soulbound.COOP; }
    @Override public Stats stats() { return new Stats(0, 0, 403, 126, 76, 38, 0, 0, 0, 0, 0, 0, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 24)); }
    @Override public boolean canHaveAttributes() { return true; }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 30000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 35000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 41000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 48000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 56000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 65500)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 76000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 89000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 3)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 105000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 4)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 120000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 5)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 140000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 6)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 165000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 7)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 192000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 8)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 225000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 9)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 265000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 10))); }
    @Override public String id() { return "INFERNAL_CRIMSON_HELMET"; }
}
