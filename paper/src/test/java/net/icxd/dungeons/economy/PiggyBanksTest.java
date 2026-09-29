package net.icxd.dungeons.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class PiggyBanksTest {
    @Test
    void aWholeBankBeforeACrackedOne() {
        assertEquals(PiggyBanks.PIGGY, PiggyBanks.bank(List.of("SCAVENGER_TALISMAN", PiggyBanks.CRACKED, PiggyBanks.PIGGY)));
        assertEquals(PiggyBanks.CRACKED, PiggyBanks.bank(List.of(PiggyBanks.CRACKED)));
        assertNull(PiggyBanks.bank(List.of(PiggyBanks.BROKEN)));
    }

    @Test
    void onlyForALossOf20kOrMore() {
        assertEquals(0, PiggyBanks.share(PiggyBanks.PIGGY, 19_999.9));
        assertEquals(1, PiggyBanks.share(PiggyBanks.PIGGY, 20_000));
        assertEquals(0.75, PiggyBanks.share(PiggyBanks.CRACKED, 50_000));
        assertEquals(0, PiggyBanks.share(null, 50_000));
    }
}
