package net.icxd.dungeons.item.items.helmet;

import net.icxd.dungeons.item.SkyBlockItem;
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

import static net.icxd.dungeons.stats.Stat.CRIT_CHANCE;
import static net.icxd.dungeons.stats.Stat.CRIT_DAMAGE;
import static net.icxd.dungeons.stats.Stat.DEFENSE;
import static net.icxd.dungeons.stats.Stat.HEALTH;
import static net.icxd.dungeons.stats.Stat.STRENGTH;

/** This plugin's own helmet (Hypixel has none by this name). */
public class SubzeroHelmet implements SkyBlockItem {
    @Override public String id() { return "SUBZERO_HELMET"; }
    @Override public String name() { return "Subzero Helmet"; }
    @Override public Material material() { return Material.PLAYER_HEAD; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public String skin() { return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTBiM2M5MWI3MjdkODdkOGM4YWE5NjAyOGYyMjc1Yjg0MDVkZWJjNzUxNmEwMjNkMGY3NzQ4YmFiMjFmOWM0MyJ9fX0="; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.HELMET; }
    @Override public Stats stats() { return new Stats().set(HEALTH, 325).set(DEFENSE, 250).set(STRENGTH, 150).set(CRIT_CHANCE, 25).set(CRIT_DAMAGE, 50); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.COMBAT), GemstoneSlot.combat(), GemstoneSlot.combat()); }
    @Override public Requirements requirements() { return new Requirements(new SkillRequirement(Skill.COMBAT, 50), new SlayerRequirement(SlayerBossType.BLAZE, 9)); }
    @Override public double npcSellPrice() { return 10000; }
}
