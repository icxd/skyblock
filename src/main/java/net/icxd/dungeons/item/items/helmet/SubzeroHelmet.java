package net.icxd.dungeons.item.items.helmet;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.abilities.PowerHouse;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.item.requirement.skill.SkillRequirement;
import net.icxd.dungeons.item.requirement.slayer.SlayerBossType;
import net.icxd.dungeons.item.requirement.slayer.SlayerRequirement;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

public class SubzeroHelmet implements SkyBlockItem {
    @Override public String name() { return "Subzero Helmet"; }
    @Override public Material material() { return Material.SKULL_ITEM; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public String skin() { return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTBiM2M5MWI3MjdkODdkOGM4YWE5NjAyOGYyMjc1Yjg0MDVkZWJjNzUxNmEwMjNkMGY3NzQ4YmFiMjFmOWM0MyJ9fX0="; }
    @Override public int durability() { return 3; }
    @Override public double npcSellPrice() { return 10000; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.HELMET; }
    @Override public Stats stats() { return new Stats(0, 0, 325, 250, 150, 0, 0, 0, 0, 0, 0, 0, 50, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 50), new SlayerRequirement(SlayerBossType.BLAZE, 9)); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public Ability ability() { return new PowerHouse(); }
    @Override public String id() { return "SUBZERO_HELMET"; }
}
