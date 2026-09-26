package net.icxd.dungeons.item.items.helmet;

import net.icxd.dungeons.item.*;
import net.icxd.dungeons.item.cost.*;
import net.icxd.dungeons.item.cost.coins.*;
import net.icxd.dungeons.item.cost.essence.*;
import net.icxd.dungeons.item.cost.item.*;
import net.icxd.dungeons.item.enums.*;
import net.icxd.dungeons.item.gemstone.*;
import net.icxd.dungeons.item.requirement.*;
import net.icxd.dungeons.item.requirement.skill.*;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;

import static net.icxd.dungeons.stats.Stat.*;

public class BurningCrimsonHelmet implements SkyBlockItem {
    @Override public Material material() { return Material.PLAYER_HEAD; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTY0NTUwNDM4MTkxMiwKICAicHJvZmlsZUlkIiA6ICJmZDQ3Y2I4YjgzNjQ0YmY3YWIyYmUxODZkYjI1ZmMwZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJDVUNGTDEyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2ViMDM0YTVkOTdjMjRmZTBlYzkwMmJkMDJmZWM1MmEwOWQ5MTczNjZiZTA2MGM1MWY5YTFhMjc2YjI4NGE5ZDciCiAgICB9CiAgfQp9"; }
    @Override public String name() { return "Burning Crimson Helmet"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.HELMET; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Soulbound soulbound() { return Soulbound.COOP; }
    @Override public Stats stats() { return new Stats().set(HEALTH, 254).set(DEFENSE, 79).set(STRENGTH, 48).set(INTELLIGENCE, 24).set(CRIT_DAMAGE, 32); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 24)); }
    @Override public boolean canHaveAttributes() { return true; }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 900)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 1000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 1125)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 1270)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 1450)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 1650)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 1850)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 2100), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 3)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 2350), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 4)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 2650), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 5))); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public String id() { return "BURNING_CRIMSON_HELMET"; }
}
