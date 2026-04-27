package net.icxd.dungeons.item.items.bow;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.abilities.InstantlyShoots;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.UpgradeCosts;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.item.requirement.slayer.SlayerBossType;
import net.icxd.dungeons.item.requirement.slayer.SlayerRequirement;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

public class Terminator implements SkyBlockItem {
    @Override public Material material() { return Material.BOW; }
    @Override public String name() { return "Terminator"; }
    @Override public boolean glowing() { return true; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.BOW; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public Stats stats() { return new Stats(310, 0, 0, 0, 50, 0, 0, 0, 0, 0, 0, 0, 250, 0, 0, 0, 40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
    @Override public UpgradeCosts upgradeCosts() { return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.DRAGON, 100)), new UpgradeCost(new EssenceCost(EssenceType.DRAGON, 200)), new UpgradeCost(new EssenceCost(EssenceType.DRAGON, 300)), new UpgradeCost(new EssenceCost(EssenceType.DRAGON, 500)), new UpgradeCost(new EssenceCost(EssenceType.DRAGON, 750))); }
    @Override public Requirements requirements() { return new Requirements(new SlayerRequirement(SlayerBossType.ENDERMAN, 7)); }
    @Override public String id() { return "TERMINATOR"; }
    @Override public Ability ability() { return new InstantlyShoots(3); }
}
