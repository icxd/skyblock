package net.icxd.dungeons.item.items.longsword;

import net.icxd.dungeons.item.*;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.abilities.CoolThing;
import net.icxd.dungeons.item.ability.abilities.Testing;
import net.icxd.dungeons.item.cost.*;
import net.icxd.dungeons.item.cost.coins.*;
import net.icxd.dungeons.item.cost.essence.*;
import net.icxd.dungeons.item.cost.item.*;
import net.icxd.dungeons.item.enums.*;
import net.icxd.dungeons.item.gemstone.*;
import net.icxd.dungeons.item.prestige.*;
import net.icxd.dungeons.item.requirement.*;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonTierRequirement;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonType;
import net.icxd.dungeons.item.requirement.skill.*;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;

public class DarkClaymore implements SkyBlockItem {
    @Override public Material material() { return Material.STONE_SWORD; }
    @Override public String name() { return "Dark Claymore"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.LONGSWORD; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public String description() { return "%%gray%%%%italic%%That thing was too big to be called a sword, it was more like a large hunk of stone."; }
    @Override public Stats stats() { return new Stats(500, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public double npcSellPrice() { return 0; }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.WITHER, 150)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 300)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 500)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 900)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 1500))); }
    @Override public Requirements requirements() { return new Requirements(new DungeonTierRequirement(DungeonType.MASTER_CATACOMBS, 7)); }
    @Override public int itemDurability() { return -1; }
    @Override public boolean dungeonItem() { return true; }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.JASPER, new CoinCost(50000), new ItemCost(ItemRegistry.get("FINE_JASPER_GEM"), 20)), new GemstoneSlot(GemstoneType.JASPER, new CoinCost(100000), new ItemCost(ItemRegistry.get("FINE_JASPER_GEM"), 40))); }
    @Override public String id() { return "DARK_CLAYMORE"; }
    @Override public Ability ability() { return new CoolThing(); }
}
