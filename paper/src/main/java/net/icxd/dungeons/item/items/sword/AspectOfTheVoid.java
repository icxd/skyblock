package net.icxd.dungeons.item.items.sword;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.abilities.InstantTransmission;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import static net.icxd.dungeons.stats.Stat.DAMAGE;
import static net.icxd.dungeons.stats.Stat.STRENGTH;

public class AspectOfTheVoid implements SkyBlockItem {
    @Override public String id() { return "ASPECT_OF_THE_VOID"; }
    @Override public String name() { return "Aspect of the Void"; }
    @Override public Material material() { return Material.DIAMOND_SHOVEL; }
    @Override public Rarity rarity() { return Rarity.EPIC; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.SWORD; }
    @Override public Stats stats() { return new Stats().set(DAMAGE, 120).set(STRENGTH, 100); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.SAPPHIRE)); }
    @Override public Ability ability() { return new InstantTransmission(); }
    @Override public double npcSellPrice() { return 56000; }
}
