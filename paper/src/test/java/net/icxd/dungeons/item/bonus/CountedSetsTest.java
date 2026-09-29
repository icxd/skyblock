package net.icxd.dungeons.item.bonus;

import com.google.gson.JsonParser;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** The bonuses that count on their piece, and the tables they read: made-up text in the items' shape, made-up steps. */
class CountedSetsTest {
    private static final double EPSILON = 1e-9;
    private static final String BULWARK = "{\"kind\":\"PIECE\",\"name\":\"Zombie Bulwark\",\"header\":\"&6Piece Bonus: Zombie Bulwark\",\"text\":["
            + "\"&7Kill tests to accumulate defense.\",\"&7Piece Bonus: &a+0❈\",\"&7Next Upgrade: &a+20❈ &8(&a0&7/&c50&8)\"]}";

    @AfterEach
    void noTables() {
        BonusTables.set(new BonusTables(Map.of()));
    }

    /** The private file's steps: fewest kills first; what's reached, and what's next. */
    @Test
    void tables() {
        BonusTables tables = BonusTables.parse(JsonParser.parseString("{\"kill_steps\":{\"Test\":[[300,50],[50,20],[1000,90]]}}").getAsJsonObject());
        List<BonusTables.Step> steps = tables.killSteps("Test");
        assertEquals(50, steps.get(0).at(), EPSILON);
        assertEquals(0, BonusTables.reached(steps, 49), EPSILON);
        assertEquals(20, BonusTables.reached(steps, 50), EPSILON);
        assertEquals(50, BonusTables.reached(steps, 999), EPSILON);
        assertEquals(90, BonusTables.reached(steps, 5_000), EPSILON);
        assertEquals(new BonusTables.Step(300, 50), BonusTables.next(steps, 50));
        assertNull(BonusTables.next(steps, 1_000));
        assertEquals(List.of(), tables.killSteps("Other"));
    }

    /** Training's Health: every 50 kills +5, at most 500; its lines show the kills and the Health. */
    @Test
    void training() {
        assertEquals(0, CountedSets.Training.health(49, 50, 5, 500), EPSILON);
        assertEquals(15, CountedSets.Training.health(170, 50, 5, 500), EPSILON);
        assertEquals(500, CountedSets.Training.health(1_000_000, 50, 5, 500), EPSILON);
        List<String> text = List.of("&7Every &950 tests killed&7 gives the", "&7wearer &a+5 &c❤ Health&7. Max 500.", "",
                "&7Zombies Killed: &a0", "&7Bonus HP: &a0");
        List<String> counted = new CountedSets.Training().counted(text, 1_234, null);
        assertEquals("&7Zombies Killed: &a1,234", counted.get(3));
        assertEquals("&7Bonus HP: &a120", counted.get(4));
    }

    /** A Bulwark without the tables has only the step its text shows; with them, all of theirs, and "Maxed!" past the last. */
    @Test
    void bulwark() {
        DataItem item = TestPieces.item("REVENANT_CHESTPLATE", BULWARK);
        CountedSets.Bulwark bulwark = new CountedSets.Bulwark("Zombie Bulwark", CountedSets.ZOMBIE_KILLS, CountedSets.ZOMBIES);
        List<BonusTables.Step> own = bulwark.steps(item);
        assertEquals(List.of(new BonusTables.Step(50, 20)), own);
        List<String> text = item.blocks().get(0).text();
        assertEquals("&7Next Upgrade: &aMaxed!", bulwark.counted(text, 60, item).get(2));

        BonusTables.set(BonusTables.parse(JsonParser.parseString("{\"kill_steps\":{\"Zombie Bulwark\":[[50,20],[300,50]]}}").getAsJsonObject()));
        List<String> counted = bulwark.counted(text, 60, item);
        assertEquals("&7Piece Bonus: &a+20❈", counted.get(1));
        assertEquals("&7Next Upgrade: &a+50❈ &8(&a60&7/&c300&8)", counted.get(2));
        // Nothing counted yet: the data's lines, as they were.
        assertEquals(text, bulwark.counted(text, 0, item));
    }

    /** The counting pieces' lore is the data's until they've counted something. */
    @Test
    void countersLeaveNewPiecesAlone() {
        DataItem item = TestPieces.item("REVENANT_CHESTPLATE", BULWARK);
        List<ItemBlock> blocks = item.blocks();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", item.id());
        BonusCounters behaviour = counters();
        assertSame(blocks, behaviour.blocks(item, tag, blocks));
        tag.setDouble(CountedSets.ZOMBIE_KILLS, 12);
        assertEquals("&7Next Upgrade: &a+20❈ &8(&a12&7/&c50&8)", behaviour.blocks(item, tag, blocks).get(0).text().get(2));
    }

    private static BonusCounters counters() {
        BonusCounters[] out = new BonusCounters[1];
        BonusCounters.register((id, behaviour) -> {
            if (behaviour instanceof BonusCounters counters) out[0] = counters;
        });
        return out[0];
    }

    /** Riches: 1.5x, 2x and 3x Scavenger coins for 2, 3 and 4 pieces, and its text's number with them. */
    @Test
    void riches() {
        assertEquals(1.5, CountedSets.Riches.FACTOR.at(2), EPSILON);
        assertEquals(3, CountedSets.Riches.FACTOR.at(4), EPSILON);
        List<String> text = List.of("&7Gain &a1.5x &9Test &7Coins.", "", "&7Scavenger Coins Gained: &60");
        assertEquals("&7Gain &a3x &9Test &7Coins.", new CountedSets.Riches().text(text, 4).get(0));
        assertEquals("&7Scavenger Coins Gained: &64,321", new CountedSets.Riches().counted(text, 4_321.7, null).get(2));
        assertEquals(1, CountedSets.richesFactor(List.of()), EPSILON);
    }

    /** Armor of Magma's Absorb: every 10 kills +1 Health and Intelligence each, at most 200. */
    @Test
    void magmaAbsorb() {
        assertEquals(0, OtherSets.Absorb.each(9, 10, 1, 200), EPSILON);
        assertEquals(12, OtherSets.Absorb.each(125, 10, 1, 200), EPSILON);
        assertEquals(200, OtherSets.Absorb.each(100_000, 10, 1, 200), EPSILON);
        List<String> text = List.of("&7Every &910 tests killed&7 gives the", "&7wearer &a+1 &c❤ Health&7. Max 200 each.", "",
                "&7Magma Cubes Killed: &a0", "&7Bonus HP: &a0", "&7Bonus Intelligence: &a0");
        List<String> counted = new OtherSets.Absorb().counted(text, 57, null);
        assertEquals(List.of("&7Magma Cubes Killed: &a57", "&7Bonus HP: &a5", "&7Bonus Intelligence: &a5"), counted.subList(3, 6));
    }
}
