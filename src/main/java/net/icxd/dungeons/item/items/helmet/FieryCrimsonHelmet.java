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

public class FieryCrimsonHelmet implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTY0NTUwNDM2NTM3OSwKICAicHJvZmlsZUlkIiA6ICI2OTBkMDM2OGM2NTE0OGM5ODZjMzEwN2FjMmRjNjFlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ5emZyXzciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTFmMGM3YWZmZjE3ODI0NjVkOGNkYjVlYmEyNjFiNjU0MjNhN2EwNzEyZWUzYTRjNTcyYzMzZjk0YzY4YzU1IgogICAgfQogIH0KfQ=="; }
    @Override public String name() { return "Fiery Crimson Helmet"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.HELMET; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Soulbound soulbound() { return Soulbound.COOP; }
    @Override public Stats stats() { return new Stats(0, 0, 320, 100, 60, 30, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 24)); }
    @Override public boolean canHaveAttributes() { return true; }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 5000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 5600)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 6300)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 7000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 8000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 9000)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 10200)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 11500), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 3)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 13000), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 4)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 14500), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 5))); }
    @Override public Prestige prestige() { return new Prestige(ItemRegistry.get("INFERNAL_CRIMSON_HELMET"), new EssenceCost(EssenceType.CRIMSON, 25500), new ItemCost(ItemRegistry.get("KUUDRA_TEETH"), 80)); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public String id() { return "FIERY_CRIMSON_HELMET"; }
}
