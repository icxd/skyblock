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

public class CrimsonLeggings implements SkyBlockItem {
    @Override public Material material() { return Material.LEATHER_LEGGINGS; }
    @Override public Color color() { return new Color(230,97,5); }
    @Override public String name() { return "Crimson Leggings"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.LEGGINGS; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Stats stats() { return new Stats(0, 0, 205, 55, 30, 5, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 24)); }
    @Override public boolean canHaveAttributes() { return true; }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 30)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 35)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 40)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 45)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 50)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 55)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 60)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 70), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 2)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 80), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 3)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 90), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 4))); }
    @Override public Prestige prestige() { return new Prestige(ItemRegistry.get("HOT_CRIMSON_LEGGINGS"), new EssenceCost(EssenceType.CRIMSON, 150), new ItemCost(ItemRegistry.get("KUUDRA_TEETH"), 10)); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public String id() { return "CRIMSON_LEGGINGS"; }
}
