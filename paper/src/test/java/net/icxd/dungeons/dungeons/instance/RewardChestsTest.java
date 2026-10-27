package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.bank.BankAccount;
import net.icxd.dungeons.dungeons.chests.ChestLoot.Kind;
import net.icxd.dungeons.dungeons.chests.ChestLoot.Reward;
import net.icxd.dungeons.economy.Purse;

class RewardChestsTest {
    private static final UUID ME = UUID.randomUUID();

    /** As recorded: three on 11, 13 and 15, four on 11, 12, 14 and 15. */
    @Test
    void slots() {
        assertArrayEquals(new int[]{11, 13, 15}, RewardChestMenu.contentSlots(3));
        assertArrayEquals(new int[]{11, 12, 14, 15}, RewardChestMenu.contentSlots(4));
        assertEquals(7, RewardChestMenu.contentSlots(9).length);
    }

    /** The Diamond and Wood chests' buttons, line for line as recorded. */
    @Test
    void openButton() {
        assertEquals(List.of("&7Contents", "&fEnchanted Book (&d&lCombo I&f)", "&fEnchanted Book (Rejuvenate I)", "&dUndead Essence &8x12",
                "&dWither Essence &8x6", "", "&7Cost", "&650,000 Coins", "", "&cNOTE: Coins are withdrawn from your",
                "&cbank if you don't have enough in", "&cyour purse.", "", "&eClick to open!"),
                RewardChestMenu.openLore(List.of("&fEnchanted Book (&d&lCombo I&f)", "&fEnchanted Book (Rejuvenate I)", "&dUndead Essence &8x12",
                        "&dWither Essence &8x6"), 50_000));
        assertEquals(List.of("&7Contents", "&fEnchanted Book (Feather Falling VI)", "&dUndead Essence &8x5", "&dWither Essence &8x7", "",
                "&7Cost", "&aFREE", "", "&eClick to open!"),
                RewardChestMenu.openLore(List.of("&fEnchanted Book (Feather Falling VI)", "&dUndead Essence &8x5", "&dWither Essence &8x7"), 0));
    }

    @Test
    void rerollButton() {
        assertEquals(List.of("&7Consume a &9Kismet Feather &7to reroll", "&7the loot in this chest.", "", "&7You may only use a feather once",
                "&7per run/chest where applicable.", "", "&eBring a Kismet Feather"), RewardChestMenu.rerollLore(false, false));
    }

    @Test
    void essenceLine() {
        assertEquals("&dWither Essence &8x7", RewardChests.line(new Reward(Kind.ESSENCE, "WITHER", 7, 0)));
    }

    /** From the purse; what it's short from the bank; nothing if both together are short. */
    @Test
    void paying() {
        Document profile = new Document();
        Purse.add(profile, 30_000);
        assertTrue(RewardChests.pay(profile, 25_000, ME));
        assertEquals(5_000, Purse.coins(profile));

        BankAccount.setBalance(profile, 100_000);
        assertTrue(RewardChests.pay(profile, 50_000, ME));
        assertEquals(0, Purse.coins(profile));
        assertEquals(55_000, BankAccount.balance(profile));

        assertFalse(RewardChests.pay(profile, 100_000, ME));
        assertEquals(0, Purse.coins(profile));
        assertEquals(55_000, BankAccount.balance(profile));
    }
}
