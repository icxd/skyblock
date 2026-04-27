package net.icxd.dungeons.item.items.none;

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
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;

public class PerfectHopper implements SkyBlockItem {
    @Override public Material material() { return Material.HOPPER; }
    @Override public String name() { return "Perfect Hopper"; }
    @Override public boolean glowing() { return true; }
    @Override public Rarity rarity() { return Rarity.EPIC; }
    @Override public double npcSellPrice() { return 500000; }
    @Override public String id() { return "PERFECT_HOPPER"; }
}
