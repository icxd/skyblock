package net.icxd.dungeons.item.items.sword;

import net.icxd.dungeons.item.*;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.abilities.InstantTransmission;
import net.icxd.dungeons.item.cost.*;
import net.icxd.dungeons.item.cost.coins.*;
import net.icxd.dungeons.item.cost.essence.*;
import net.icxd.dungeons.item.cost.item.*;
import net.icxd.dungeons.item.enums.*;
import net.icxd.dungeons.item.gemstone.*;
import net.icxd.dungeons.item.prestige.*;
import net.icxd.dungeons.item.requirement.*;
import net.icxd.dungeons.item.requirement.skill.*;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;

public class AspectoftheVoid implements SkyBlockItem {
    @Override public Material material() { return Material.DIAMOND_SPADE; }
    @Override public String name() { return "Aspect of the Void"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.SWORD; }
    @Override public Rarity rarity() { return Rarity.EPIC; }
    @Override public Stats stats() { return new Stats(120, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Ability ability() { return new InstantTransmission(); }
    @Override public double npcSellPrice() { return 56000; }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.SAPPHIRE)); }
    @Override public String id() { return "ASPECT_OF_THE_VOID"; }
}
