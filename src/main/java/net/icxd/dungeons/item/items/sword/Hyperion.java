package net.icxd.dungeons.item.items.sword;

import net.icxd.dungeons.item.*;
import net.icxd.dungeons.item.ability.abilities.scrolls.Implosion;
import net.icxd.dungeons.item.ability.abilities.scrolls.ShadowWarp;
import net.icxd.dungeons.item.ability.abilities.scrolls.WitherImpact;
import net.icxd.dungeons.item.ability.abilities.scrolls.WitherShield;
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
import net.icxd.dungeons.utils.Utils;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Hyperion implements SkyBlockItem {
    @Override public Material material() { return Material.IRON_SWORD; }
    @Override public int itemDurability() { return -1; }
    @Override public String name() { return "Hyperion"; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.SWORD; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Stats stats() { return new Stats(260, 30, 0, 0, 150, 350, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public double npcSellPrice() { return 200; }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.WITHER, 150)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 300)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 500)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 900)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 1500))); }
    @Override public Requirements requirements() { return new Requirements(new DungeonTierRequirement(DungeonType.CATACOMBS, 7)); }
    @Override public boolean dungeonItem() { return true; }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(new GemstoneSlot(GemstoneType.SAPPHIRE, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 4)), new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost(ItemRegistry.get("FLAWLESS_JASPER_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_SAPPHIRE_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_RUBY_GEM"), 1), new ItemCost(ItemRegistry.get("FLAWLESS_AMETHYST_GEM"), 1))); }
    @Override public String id() { return "HYPERION"; }

    @Override
    public NBTTagCompound nbt() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setBoolean("implosion", false);
        compound.setBoolean("wither_shield", false);
        compound.setBoolean("shadow_warp", false);
        return compound;
    }

    @Override
    public List<String> nbtLore(NBTTagCompound compound) {
        final ArrayList<String> lore = new ArrayList<>();
        if (!compound.hasKey("implosion") || !compound.hasKey("wither_shield") || !compound.hasKey("shadow_warp")) return lore;
        if (compound.getBoolean("implosion") || compound.getBoolean("wither_shield") || compound.getBoolean("shadow_warp"))
            lore.add(ChatColor.GREEN + "Scroll Abilities:");
        if (compound.getBoolean("implosion") && compound.getBoolean("wither_shield") && compound.getBoolean("shadow_warp")) {
            ItemBuilder.buildAbility(new WitherImpact(), lore);
            return lore;
        }
        if (compound.getBoolean("implosion")) ItemBuilder.buildAbility(new Implosion(), lore);
        if (compound.getBoolean("wither_shield")) ItemBuilder.buildAbility(new WitherShield(), lore);
        if (compound.getBoolean("shadow_warp")) ItemBuilder.buildAbility(new ShadowWarp(), lore);
        return lore;
    }
}
