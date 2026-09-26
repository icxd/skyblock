package net.icxd.dungeons.item.items.sword;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.abilities.scrolls.Implosion;
import net.icxd.dungeons.item.ability.abilities.scrolls.ShadowWarp;
import net.icxd.dungeons.item.ability.abilities.scrolls.WitherImpact;
import net.icxd.dungeons.item.ability.abilities.scrolls.WitherShield;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.UpgradeCosts;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonTierRequirement;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonType;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

import static net.icxd.dungeons.stats.Stat.DAMAGE;
import static net.icxd.dungeons.stats.Stat.FEROCITY;
import static net.icxd.dungeons.stats.Stat.INTELLIGENCE;
import static net.icxd.dungeons.stats.Stat.STRENGTH;

public class Hyperion implements SkyBlockItem {
    @Override public String id() { return "HYPERION"; }
    @Override public String name() { return "Hyperion"; }
    @Override public Material material() { return Material.IRON_SWORD; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.SWORD; }
    @Override public int gearScore() { return 615; }
    @Override public Stats stats() { return new Stats().set(DAMAGE, 260).set(STRENGTH, 150).set(FEROCITY, 30).set(INTELLIGENCE, 350); }
    @Override public GemstoneSlots gemstoneSlots() {
        return new GemstoneSlots(new GemstoneSlot(GemstoneType.SAPPHIRE, new CoinCost(250000), new ItemCost("FLAWLESS_SAPPHIRE_GEM", 4)), GemstoneSlot.combat());
    }
    @Override public List<String> lore() {
        return List.of("&7Deals &c+50% &7damage to &8☠ Wither &7mobs.",
                "&7Grants &c+1 ❁ Damage &7and &a+2 &b✎",
                "&bIntelligence &7per &cCatacombs &7level.");
    }
    @Override public double npcSellPrice() { return 200; }
    @Override public UpgradeCosts upgradeCosts() {
        return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.WITHER, 150)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 300)),
                new UpgradeCost(new EssenceCost(EssenceType.WITHER, 500)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 900)),
                new UpgradeCost(new EssenceCost(EssenceType.WITHER, 1500)));
    }
    @Override public Requirements requirements() { return new Requirements(new DungeonTierRequirement(DungeonType.CATACOMBS, 7)); }
    @Override public boolean dungeonItem() { return true; }

    @Override
    public NBTTagCompound nbt() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setBoolean("implosion", false);
        compound.setBoolean("wither_shield", false);
        compound.setBoolean("shadow_warp", false);
        return compound;
    }

    /** The scroll abilities it has (all three make Wither Impact); with none, right-click is the class ability. */
    @Override
    public List<String> nbtLore(NBTTagCompound compound) {
        boolean implosion = compound.getBoolean("implosion"), shield = compound.getBoolean("wither_shield"), warp = compound.getBoolean("shadow_warp");
        List<String> lore = new ArrayList<>();
        if (implosion && shield && warp) {
            lore.addAll(ItemBuilder.abilityLore(new WitherImpact(), rarity()));
        } else if (implosion || shield || warp) {
            if (implosion) addAbility(lore, ItemBuilder.abilityLore(new Implosion(), rarity()));
            if (shield) addAbility(lore, ItemBuilder.abilityLore(new WitherShield(), rarity()));
            if (warp) addAbility(lore, ItemBuilder.abilityLore(new ShadowWarp(), rarity()));
        } else {
            lore.add("&eRight-click to use your class ability!");
        }
        return lore;
    }

    private static void addAbility(List<String> lore, List<String> ability) {
        if (!lore.isEmpty()) lore.add("");
        lore.addAll(ability);
    }
}
