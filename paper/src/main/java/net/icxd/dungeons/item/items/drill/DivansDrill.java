package net.icxd.dungeons.item.items.drill;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.item.requirement.hotm.HeartOfTheMountainRequirement;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Text;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

import static net.icxd.dungeons.stats.Stat.BREAKING_POWER;
import static net.icxd.dungeons.stats.Stat.DAMAGE;
import static net.icxd.dungeons.stats.Stat.MINING_FORTUNE;
import static net.icxd.dungeons.stats.Stat.MINING_SPEED;

/**
 * A prismarine shard, like Hypixel's drills before item models: a pickaxe would let the client break
 * blocks itself, and mining here is done by the server (see BlockListener).
 */
public class DivansDrill implements SkyBlockItem {
    @Override public String id() { return "DIVAN_DRILL"; }
    @Override public String name() { return "Divan's Drill"; }
    @Override public Material material() { return Material.PRISMARINE_SHARD; }
    @Override public Rarity rarity() { return Rarity.MYTHIC; }
    @Override public boolean glowing() { return true; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.DRILL; }
    @Override public Stats stats() { return new Stats().set(DAMAGE, 75).set(MINING_SPEED, 1800).set(MINING_FORTUNE, 150).set(BREAKING_POWER, 10); }
    @Override public GemstoneSlots gemstoneSlots() {
        return new GemstoneSlots(new GemstoneSlot(GemstoneType.AMBER), new GemstoneSlot(GemstoneType.AMBER),
                new GemstoneSlot(GemstoneType.JADE, new CoinCost(50000), new ItemCost("FINE_JADE_GEM", 20)),
                new GemstoneSlot(GemstoneType.JADE, new CoinCost(100000), new ItemCost("FINE_JADE_GEM", 40)),
                new GemstoneSlot(GemstoneType.MINING, new CoinCost(250000), new ItemCost("FLAWLESS_JADE_GEM", 1),
                        new ItemCost("FLAWLESS_AMBER_GEM", 1), new ItemCost("FLAWLESS_TOPAZ_GEM", 1)));
    }
    @Override public Requirements requirements() { return new Requirements(new HeartOfTheMountainRequirement(7)); }

    @Override
    public NBTTagCompound nbt() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setString("fuel_tank", "null");
        compound.setString("drill_engine", "null");
        compound.setString("upgrade_module", "null");
        compound.setInt("fuel_max", 3000);
        compound.setInt("fuel", 3000);
        return compound;
    }

    /** Its parts (none fitted yet) and fuel. */
    @Override
    public List<String> nbtLore(NBTTagCompound compound) {
        List<String> lore = new ArrayList<>();
        if (compound.getString("fuel_tank").equals("null")) {
            lore.addAll(List.of("&7Fuel Tank: &cNot Installed", "&7Increases fuel capacity with part", "&7installed.", ""));
        }
        if (compound.getString("drill_engine").equals("null")) {
            lore.addAll(List.of("&7Drill Engine: &cNot Installed", "&7Increases &6⸕ Mining Speed &7with part", "&7installed.", ""));
        }
        if (compound.getString("upgrade_module").equals("null")) {
            lore.addAll(List.of("&7Upgrade Module: &cNot Installed", "&7Applies a passive upgrade with part", "&7installed.", ""));
        }
        lore.add("&7Apply Drill Parts to this Drill by");
        lore.add("&7talking to a &2Drill Mechanic&7!");
        lore.add("");
        lore.add("&7Fuel: &2" + Text.number(compound.getInt("fuel")) + "&8/" + Text.compact(compound.getInt("fuel_max")));
        return lore;
    }
}
