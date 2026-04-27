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

public class CrimsonHelmet implements SkyBlockItem {
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public int durability() { return 3; }
    @Override public String skin() { return "ewogICJ0aW1lc3RhbXAiIDogMTY0NTUwNDQxNDE3MywKICAicHJvZmlsZUlkIiA6ICI5MThhMDI5NTU5ZGQ0Y2U2YjE2ZjdhNWQ1M2VmYjQxMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCZWV2ZWxvcGVyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzUwNTFjODNkOWViZjY5MDEzZjFlYzhjOWVmYzk3OWVjMmQ5MjVhOTIxY2M4NzdmZjY0YWJlMDlhYWRkMmY2Y2MiCiAgICB9CiAgfQp9"; }
    @Override public String name() { return "Crimson Helmet"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.HELMET; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Stats stats() { return new Stats(0, 0, 160, 50, 30, 15, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 24)); }
    @Override public boolean canHaveAttributes() { return true; }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 30)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 35)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 40)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 45)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 50)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 55)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 60)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 70), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 2)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 80), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 3)), new UpgradeCost(new EssenceCost(EssenceType.CRIMSON, 90), new ItemCost(ItemRegistry.get("HEAVY_PEARL"), 4))); }
    @Override public Prestige prestige() { return new Prestige(ItemRegistry.get("HOT_CRIMSON_HELMET"), new EssenceCost(EssenceType.CRIMSON, 150), new ItemCost(ItemRegistry.get("KUUDRA_TEETH"), 10)); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public String id() { return "CRIMSON_HELMET"; }
}
