package net.icxd.dungeons.item.items.drill;

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
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DivansDrill implements SkyBlockItem {
    @Override public Material material() { return Material.PRISMARINE_SHARD; }
    @Override public String name() { return "Divan's Drill"; }
    @Override public boolean glowing() { return true; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.DRILL; }
    @Override public Rarity rarity() { return Rarity.MYTHIC; }
    @Override public Stats stats() { return new Stats(70, 0, 0, 0, 0, 0, 150, 0, 0, 0, 0, 0, 0, 0, 1800, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public Requirements requirements() { return new Requirements(); }
    @Override public boolean museum() { return true; }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.AMBER), new GemstoneSlot(GemstoneType.AMBER), new GemstoneSlot(GemstoneType.JADE, new CoinCost(50000), new ItemCost(ItemRegistry.get("FINE_JADE_GEM"), 20)), new GemstoneSlot(GemstoneType.JADE, new CoinCost(100000), new ItemCost(ItemRegistry.get("FINE_JADE_GEM"), 40)), new GemstoneSlot(GemstoneType.MINING, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JADE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMBER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_TOPAZ_GEM"), 1))); }
    @Override public String id() { return "DIVAN_DRILL"; }

    @Override
    public NBTTagCompound nbt() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setString("fuel_tank", "null");
        compound.setString("drill_engine", "null");
        compound.setString("upgrade_module", "null");
        compound.setInt("fuel_max", 3000);
        compound.setInt("fuel", compound.getInt("fuel_max"));
        return compound;
    }

    @Override
    public List<String> nbtLore(NBTTagCompound compound) {
        final List<String> lore = new ArrayList<>();
        if (compound.getString("fuel_tank").equals("null")) {
            lore.add("§7Fuel Tank: §cNot Installed");
            lore.add("§7Increases fuel capacity with");
            lore.add("§7part installed.");
            lore.add("");
        }
        if (compound.getString("drill_engine").equals("null")) {
            lore.add("§7Drill Engine: §cNot Installed");
            lore.add("§7Increases §6Mining Speed");
            lore.add("§7with part installed.");
            lore.add("");
        }
        if (compound.getString("upgrade_module").equals("null")) {
            lore.add("§7Upgrade Module: §cNot Installed");
            lore.add("§7Applies a passive upgrade with");
            lore.add("§7part installed.");
            lore.add("");
        }
        lore.add("§7Apply Drill Parts to this Drill");
        lore.add("§7by talking to §2Jotraeline");
        lore.add("§2Greatforge§7 in the §2Dwarven");
        lore.add("§2Mines§7!");
        lore.add("");
        lore.add("§7Fuel: §2" + compound.getInt("fuel") + "§8/" + compound.getInt("fuel_max"));

        return lore;
    }
}
