package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.behaviour.HeldStats;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** The Blazetekk™ Ham Radio's rules. */
class HamRadioTest {
    private static final double EPS = 1e-9;

    /** "If exactly two players ... are on the same channel", "Earn half stats while alone!", none with more. */
    @Test
    void exactlyTwoOnAChannel() {
        assertEquals(HamRadio.Signal.WEAK, HamRadio.Signal.of(1));
        assertEquals(HamRadio.Signal.STRONG, HamRadio.Signal.of(2));
        assertEquals(HamRadio.Signal.LOST, HamRadio.Signal.of(3));
        Stats text = AbilityText.stats("+60❈ Defense +40❂ True Defense +5☯ Combat Wisdom +10♨ Vitality");
        Stats alone = HamRadio.times(text, HamRadio.Signal.WEAK.share);
        assertEquals(30, alone.get(Stat.DEFENSE), EPS);
        assertEquals(20, alone.get(Stat.TRUE_DEFENSE), EPS);
        assertEquals(2.5, alone.get(Stat.COMBAT_WISDOM), EPS);
        assertEquals(5, alone.get(Stat.VITALITY), EPS);
        assertFalse(HamRadio.times(text, HamRadio.Signal.LOST.share).get(Stat.DEFENSE) > 0);
    }

    /** Its data's channel is 103.5 FM, the last one, until it's switched. */
    @Test
    void channelsGoRound() {
        assertEquals("&b103.5 FM", HeldStats.RADIO_CHANNELS.get(HeldStats.hamRadioChannel(null)));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInt(HeldStats.HAM_RADIO_CHANNEL, 0);
        assertEquals("&c610 AM", HeldStats.RADIO_CHANNELS.get(HeldStats.hamRadioChannel(tag)));
        tag.setInt(HeldStats.HAM_RADIO_CHANNEL, 99);
        assertEquals(HeldStats.RADIO_CHANNELS.size() - 1, HeldStats.hamRadioChannel(tag));
    }
}
