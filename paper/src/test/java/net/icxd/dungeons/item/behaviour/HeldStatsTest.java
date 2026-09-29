package net.icxd.dungeons.item.behaviour;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** Weapons' stats that grow with their holder or their counts, and their count lines. */
class HeldStatsTest {
    private static final double EPS = 1e-9;
    /** Its lines as items.json has them. */
    private static final List<String> STONE_BLADE = List.of("&7As a weapon created by &cScarf&7, it automatically",
            "&7adapts to its user inside Dungeons.", "", "&bBerserk&7: +&c10❁ Damage&7, +&c75❁ Strength&7, +&f10✦", "&fSpeed",
            "&bHealer&7: +&a10☄ Mending&7 & tethered players and", "&7players within &e10 &7blocks are healed for &a33%",
            "&7of your &9Life Steal&7, &9Drain&7, and &9Vampirism &7healing.", "&bMage&7: +&b100✎ Intelligence&7, +&c10❁ Damage&7 &",
            "&7melee attacks restore &b25% &7additional mana.", "&bTank&7: +&a100❈ Defense&7, +&45♨ Vitality&7, +&f5❂ True", "&fDefense",
            "&bArcher&7: Your melee attacks cause enemies to", "&7take &c10% &7more damage from your arrows for", "&b5 &7seconds.", "",
            "&eRight-click to use your class ability!");

    @Test
    void stoneBladeGivesEachClassItsLine() {
        Stats berserk = HeldStats.ClassAdaptive.classStats(STONE_BLADE, DungeonClass.BERSERK);
        assertEquals(10, berserk.get(Stat.DAMAGE), EPS);
        assertEquals(75, berserk.get(Stat.STRENGTH), EPS);
        assertEquals(10, berserk.get(Stat.SPEED), EPS);
        assertEquals(0, berserk.get(Stat.INTELLIGENCE), EPS);
        Stats tank = HeldStats.ClassAdaptive.classStats(STONE_BLADE, DungeonClass.TANK);
        assertEquals(100, tank.get(Stat.DEFENSE), EPS);
        assertEquals(5, tank.get(Stat.VITALITY), EPS);
        assertEquals(5, tank.get(Stat.TRUE_DEFENSE), EPS);
        Stats mage = HeldStats.ClassAdaptive.classStats(STONE_BLADE, DungeonClass.MAGE);
        assertEquals(100, mage.get(Stat.INTELLIGENCE), EPS);
        assertEquals(10, mage.get(Stat.DAMAGE), EPS);
        assertEquals(10, HeldStats.ClassAdaptive.classStats(STONE_BLADE, DungeonClass.HEALER).get(Stat.MENDING), EPS);
        assertEquals(new Stats(), HeldStats.ClassAdaptive.classStats(STONE_BLADE, DungeonClass.ARCHER));
    }

    @Test
    void necronsBladesPerCatacombsLevel() {
        Stats hyperion = NecronsBlade.perCatacombsLevel(List.of("&7Deals &c+50% &7damage to &8☠ Wither &7mobs.",
                "&7Grants &c+1 ❁ Damage &7and &a+2 &b✎", "&bIntelligence &7per &cCatacombs &7level."));
        assertEquals(1, hyperion.get(Stat.DAMAGE), EPS);
        assertEquals(2, hyperion.get(Stat.INTELLIGENCE), EPS);
        Stats scylla = NecronsBlade.perCatacombsLevel(List.of("&7Grants &c+1 ❁ Damage &7and &c+1 &9☠ Crit", "&9Damage &7per &cCatacombs &7level."));
        assertEquals(1, scylla.get(Stat.CRIT_DAMAGE), EPS);
        assertEquals(new Stats(), NecronsBlade.perCatacombsLevel(List.of("&7Deals &c+50% &7damage to &8☠ Wither &7mobs.")));
    }

    @Test
    void countsGiveWhatTheirTextSays() {
        List<String> fel = List.of("&7Every &a100 &7Kills with this sword", "&7grants +&c1 &7Weapon Damage, up to &c100", "&7extra.",
                "&7Current Kills:&a 0 &8(&c+0 Damage&8)");
        assertEquals(0, HeldStats.FelSword.damage(fel, 99), EPS);
        assertEquals(12, HeldStats.FelSword.damage(fel, 1_250), EPS);
        assertEquals(100, HeldStats.FelSword.damage(fel, 50_000), EPS);

        List<String> whip = List.of("&7Every &91 Zombies killed&7 during a", "&7dungeon run by this weapon gives", "&7the wielder &a+1 &c❁ Strength&7.");
        assertEquals(7, HeldStats.CommanderWhip.strength(whip, 7), EPS);

        List<String> pickaxe = List.of("&7Grants &6+10⸕ Mining Speed &7for every", "&7100 blocks mined.", "&8(Max +250⸕ Mining Speed)");
        assertEquals(30, HeldStats.StoredPotential.speed(pickaxe, 399), EPS);
        assertEquals(250, HeldStats.StoredPotential.speed(pickaxe, 1_000_000), EPS);

        assertEquals(1, HeldStats.Tempest.arrows(19));
        assertEquals(2, HeldStats.Tempest.arrows(20));
        assertEquals(5, HeldStats.Tempest.arrows(250));
        assertEquals(20, HeldStats.perWhole(1_049, 1, 50), EPS);
    }

    /** The wiki's table: 0 coins +0, 10,000 +25, 100M +250, and nothing past 2B (+528). */
    @Test
    void emeraldBladeFromThePurse() {
        assertEquals(0, HeldStats.FromPurse.bonus(0), EPS);
        assertEquals(25, HeldStats.FromPurse.bonus(10_000), 1e-6);
        assertEquals(250, HeldStats.FromPurse.bonus(100_000_000), 1e-6);
        assertEquals(HeldStats.FromPurse.bonus(2e9), HeldStats.FromPurse.bonus(9e12), EPS);
        // The table shows it floored: 658 (+528) for 528.7.
        assertEquals(528, Math.floor(HeldStats.FromPurse.bonus(2e9)), EPS);
    }

    @Test
    void countLinesAreRewrittenOnlyOnceThereIsACount() {
        List<ItemBlock> blocks = List.of(new ItemBlock("ABILITY", "Tempest", null, "PASSIVE", List.of("&7Next Upgrade: &eDouble Shot &8(&a0&7/&c20&8)", "",
                "&7Kills: &b0"), 0, 0, 0, 0, 0, 0, 0));
        net.icxd.dungeons.item.nbt.NBTTagCompound tag = new net.icxd.dungeons.item.nbt.NBTTagCompound();
        HeldStats.Tempest tempest = new HeldStats.Tempest();
        assertSame(blocks, tempest.blocks(null, tag, blocks));
        tag.setDouble(HeldStats.HURRICANE_KILLS, 23);
        assertEquals(List.of("&7Next Upgrade: &eTriple Shot &8(&a23&7/&c50&8)", "", "&7Kills: &b23"), tempest.blocks(null, tag, blocks).get(0).text());
        tag.setDouble(HeldStats.HURRICANE_KILLS, 1_000);
        assertEquals(List.of("", "&7Kills: &b1,000"), tempest.blocks(null, tag, blocks).get(0).text());

        List<String> growth = List.of("&7Bonus HP: &a0/100");
        assertEquals(List.of("&7Bonus HP: &a7/100"), HeldStats.replace(growth, Pattern.compile("(&7Bonus HP: &a)[\\d,]+(/[\\d,]+)"),
                m -> m.group(1) + 7 + m.group(2)));
        assertEquals(40, HeldStats.roman("XL"));
        assertEquals(7, HeldStats.roman("VII"));
    }
}
