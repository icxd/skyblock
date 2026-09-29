package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What armor pieces' own text does: the rules that are only numbers and text. */
class PieceTextTest {
    private static final double EPSILON = 1e-9;

    @Test
    void romanNumerals() {
        assertEquals(1, PieceText.roman("I"));
        assertEquals(4, PieceText.roman("IV"));
        assertEquals(7, PieceText.roman("VII"));
        assertEquals(9, PieceText.roman("IX"));
    }

    /** A boss head's floor, from "Grants 2x stats on The Catacombs Floor V"; none without both. */
    @Test
    void bossHeadFloors() {
        assertEquals(5, PieceText.BossHeads.floor(List.of("&7Grants 2x stats on The Test", "&7Floor V.")));
        assertEquals(-1, PieceText.BossHeads.floor(List.of("&7A test from Floor V.")));
    }

    /** The Clover Helmet: Magic Find more, every other stat but Breaking Power less. */
    @Test
    void cloverHelmet() {
        Stats stats = new Stats().set(Stat.MAGIC_FIND, 100).set(Stat.STRENGTH, 200).set(Stat.BREAKING_POWER, 5);
        PieceText.CloverHelmet.apply(stats, 5, 5);
        assertEquals(105, stats.get(Stat.MAGIC_FIND), EPSILON);
        assertEquals(190, stats.get(Stat.STRENGTH), EPSILON);
        assertEquals(5, stats.get(Stat.BREAKING_POWER), EPSILON);
    }

    /** Stats a Farming level: the text's before its Garden part, never Farming Fortune. */
    @Test
    void farmingLevels() {
        Stats boots = PieceText.FarmingLevels.perLevel(List.of("&7These test boots gain &a+2❈ Defense &7and", "&f+4✦ Speed&7 a level. While on The",
                "&7Garden, they gain &6+1☘ Farming Fortune&7."));
        assertEquals(2, boots.get(Stat.DEFENSE), EPSILON);
        assertEquals(4, boots.get(Stat.SPEED), EPSILON);
        assertEquals(0, boots.get(Stat.FARMING_FORTUNE), EPSILON);
        Stats helmet = PieceText.FarmingLevels.perLevel(List.of("&7A level, this gains:", "&a+2 ❈ Defense", "&c+4 ❤ Health",
                "&6+1 ☘ Farming Fortune &7(Holding a Test)"));
        assertEquals(4, helmet.get(Stat.HEALTH), EPSILON);
        assertEquals(0, helmet.get(Stat.FARMING_FORTUNE), EPSILON);
    }

    /** The Rabbit Hat's Jump Boost IV over the Balloon Snake's II. */
    @Test
    void jumps() {
        Worn.Piece hat = TestPieces.worn(itemWithLore("RABBIT_HAT", "HELMET", "&7Grants Jump Boost IV while equipped."));
        Worn.Piece snake = TestPieces.worn(itemWithLore("BALLOON_SNAKE", "NECKLACE", "&7Grants Jump Boost II while equipped."));
        assertEquals(4, PieceText.Jumps.level(List.of(snake, hat)));
        assertEquals(2, PieceText.Jumps.level(List.of(snake)));
    }

    /** The Demonlord Gauntlet's "1.15x" is +15% additive; a factor of 1 or less adds nothing. */
    @Test
    void demonlordGauntlet() {
        assertEquals(15, PieceText.DemonlordGauntlet.additive(1.15), EPSILON);
        assertEquals(0, PieceText.DemonlordGauntlet.additive(1), EPSILON);
    }

    /** The Sea Lantern Hat's "5x as long": 16 of every 20 ticks' air back each second; nothing for 1x. */
    @Test
    void seaLanternHat() {
        assertEquals(16, PieceText.SeaLanternHat.airBack(5));
        assertEquals(0, PieceText.SeaLanternHat.airBack(1));
    }

    private static net.icxd.dungeons.item.data.DataItem itemWithLore(String id, String type, String lore) {
        String json = "{\"format\":1,\"items\":{\"" + id + "\":{\"material\":\"PLAYER_HEAD\",\"name\":\"Test\",\"type\":\"" + type
                + "\",\"lore\":[\"" + lore + "\"]}}}";
        try {
            return net.icxd.dungeons.item.data.ItemData.load(new java.io.StringReader(json)).items().values().iterator().next();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
