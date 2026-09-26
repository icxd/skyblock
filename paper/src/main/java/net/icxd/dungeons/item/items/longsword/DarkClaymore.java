package net.icxd.dungeons.item.items.longsword;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.UpgradeCosts;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonTierRequirement;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonType;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Material;

import java.util.List;

import static net.icxd.dungeons.stats.Stat.CRIT_DAMAGE;
import static net.icxd.dungeons.stats.Stat.DAMAGE;
import static net.icxd.dungeons.stats.Stat.STRENGTH;
import static net.icxd.dungeons.stats.Stat.SWING_RANGE;

public class DarkClaymore implements SkyBlockItem {
    @Override public String id() { return "DARK_CLAYMORE"; }
    @Override public String name() { return "Dark Claymore"; }
    @Override public Material material() { return Material.STONE_SWORD; }
    @Override public Rarity rarity() { return Rarity.LEGENDARY; }
    @Override public SpecificItemType specificItemType() { return SpecificItemType.LONGSWORD; }
    @Override public int gearScore() { return 680; }
    @Override public Stats stats() { return new Stats().set(DAMAGE, 500).set(STRENGTH, 100).set(CRIT_DAMAGE, 100).set(SWING_RANGE, 2); }
    @Override public GemstoneSlots gemstoneSlots() { return new GemstoneSlots(GemstoneSlot.combat(), GemstoneSlot.combat()); }
    @Override public List<String> lore() {
        return List.of("&8&oThat thing was too big to be called a", "&8&osword, it was more like a large hunk", "&8&oof stone.");
    }
    @Override public UpgradeCosts upgradeCosts() {
        return new UpgradeCosts(new UpgradeCost(new EssenceCost(EssenceType.WITHER, 150)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 300)),
                new UpgradeCost(new EssenceCost(EssenceType.WITHER, 500)), new UpgradeCost(new EssenceCost(EssenceType.WITHER, 900)),
                new UpgradeCost(new EssenceCost(EssenceType.WITHER, 1500)));
    }
    @Override public Requirements requirements() { return new Requirements(new DungeonTierRequirement(DungeonType.MASTER_CATACOMBS, 7)); }
    @Override public boolean dungeonItem() { return true; }
}
