package net.icxd.dungeons.item.items.boots;

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

public class HotCrimsonBoots implements SkyBlockItem {
    @Override public Material material() { return Material.LEATHER_BOOTS; }
    @Override public Color color() { return new Color(230,83,0); }
    @Override public String name() { return "Hot Crimson Boots"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.BOOTS; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Soulbound soulbound() { return Soulbound.COOP; }
    @Override public Stats stats() { return new Stats(0, 0, 164, 50, 38, 6, 0, 0, 0, 0, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 24)); }
    @Override public boolean canHaveAttributes() { return true; }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 170)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 190)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 215)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 240)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 270)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 300)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 340)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 390), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 3)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 440), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 4)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 500), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 5))); }
    @Override public Prestige prestige() { return new Prestige(ItemRegistry.get("BURNING_CRIMSON_BOOTS"), new EssenceCost(EssenceType.CRIMSON, 800), new ItemCost(ItemRegistry.get("KUUDRA_TEETH"), 20)); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public String id() { return "HOT_CRIMSON_BOOTS"; }
}
