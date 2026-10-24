package net.icxd.dungeons.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.bank.BankAccount.Refusal;
import net.icxd.dungeons.bank.BankAccount.Transaction;
import net.icxd.dungeons.bank.BankAccount.Type;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.utils.SkyBlockTime;

/** Against the recording (2026-10-24): a new profile's deposits and withdrawals, as they went. */
class BankAccountTest {
    private static final UUID ME = UUID.fromString("8f1c3c1e-7d0a-4f5e-9a51-3b2a4c6d7e8f");

    private static double deposit(Document profile, double amount) {
        BankAccount.Result result = BankAccount.deposit(profile, amount, ME, 0);
        assertTrue(result.done(), () -> "refused: " + result.refusal());
        return result.amount();
    }

    private static double withdraw(Document profile, double amount) {
        BankAccount.Result result = BankAccount.withdraw(profile, amount, ME, 0);
        assertTrue(result.done(), () -> "refused: " + result.refusal());
        return result.amount();
    }

    /**
     * 20.5 coins from the Farm Merchant went in, and came out whole: each step's amount and the balance
     * after it as chat and the menus said them.
     */
    @Test
    void theRecordedDepositsAndWithdrawals() {
        Document profile = new Document(Purse.FIELD, 20.5);
        assertEquals(20.5, deposit(profile, BankAccount.wholePurse(profile)));
        assertEquals(20.5, BankAccount.balance(profile));
        assertEquals(0, Purse.coins(profile));
        // Saving Up's +10 Coins, into the purse.
        Purse.add(profile, 10);
        assertEquals(20, withdraw(profile, BankAccount.everything(profile)));
        assertEquals(0.5, BankAccount.balance(profile));
        assertEquals(15, deposit(profile, BankAccount.halfPurse(profile)));
        assertEquals(15.5, BankAccount.balance(profile));
        assertEquals(7, withdraw(profile, BankAccount.half(profile)));
        assertEquals(8.5, BankAccount.balance(profile));
        assertEquals(1, withdraw(profile, BankAccount.fifth(profile)));
        assertEquals(7.5, BankAccount.balance(profile));
        // Typed on the sign.
        assertEquals(23, deposit(profile, 23));
        assertEquals(30.5, BankAccount.balance(profile));
        assertEquals(24, withdraw(profile, 24));
        assertEquals(6.5, BankAccount.balance(profile));
        assertEquals(6, withdraw(profile, BankAccount.everything(profile)));
        assertEquals(0.5, BankAccount.balance(profile));
        assertEquals(30, Purse.coins(profile));

        // The menu's list at 06:56.0, newest first (withdrawals negative here).
        assertEquals(List.of(-6.0, -24.0, 23.0, -1.0, -7.0, 15.0, -20.0, 20.5),
                BankAccount.recent(profile).stream().map(t -> t.type() == Type.DEPOSIT ? t.amount() : -t.amount()).toList());
        assertEquals(ME, BankAccount.recent(profile).getFirst().by());
    }

    /** What each menu offered as the balance went (the recorded "Amount to withdraw" and "Amount to deposit"). */
    @Test
    void theMenusAmounts() {
        assertEquals(List.of(20.0, 10.0, 4.0), offered(20.5));
        assertEquals(List.of(15.0, 7.0, 3.0), offered(15.5));
        assertEquals(List.of(8.0, 4.0, 1.0), offered(8.5));
        assertEquals(List.of(30.0, 15.0, 6.0), offered(30.5));
        assertEquals(List.of(6.0, 3.0, 1.0), offered(6.5));
        Document purse = new Document(Purse.FIELD, 20.5);
        assertEquals(20.5, BankAccount.wholePurse(purse));
        assertEquals(10.25, BankAccount.halfPurse(purse));
    }

    private static List<Double> offered(double balance) {
        Document profile = new Document(BankAccount.KEY, new Document("balance", balance));
        return List.of(BankAccount.everything(profile), BankAccount.half(profile), BankAccount.fifth(profile));
    }

    @Test
    void refusals() {
        Document profile = new Document(Purse.FIELD, 0.0);
        // The recorded one: an empty purse's whole purse.
        assertEquals(Refusal.TOO_LITTLE, BankAccount.deposit(profile, BankAccount.wholePurse(profile), ME, 0).refusal());
        assertEquals(Refusal.TOO_LITTLE, BankAccount.deposit(profile, -5, ME, 0).refusal());
        assertEquals(Refusal.NOT_ENOUGH, BankAccount.deposit(profile, 1, ME, 0).refusal());
        Purse.add(profile, 5);
        deposit(profile, 0.5);
        // Less than a coin in it: nothing whole to take.
        assertEquals(Refusal.TOO_LITTLE, BankAccount.withdraw(profile, BankAccount.everything(profile), ME, 0).refusal());
        assertEquals(Refusal.NOT_ENOUGH, BankAccount.withdraw(profile, 2, ME, 0).refusal());
        // Nothing moved.
        assertEquals(0.5, BankAccount.balance(profile));
        assertEquals(4.5, Purse.coins(profile));
        assertEquals(1, BankAccount.recent(profile).size());
    }

    @Test
    void upToItsLimit() {
        Document profile = new Document(Purse.FIELD, 100.0).append(BankAccount.KEY, new Document("balance", BankAccount.LIMIT - 30));
        assertEquals(30, deposit(profile, 100));
        assertEquals(BankAccount.LIMIT, BankAccount.balance(profile));
        assertEquals(70, Purse.coins(profile));
        assertEquals(Refusal.FULL, BankAccount.deposit(profile, 1, ME, 0).refusal());
    }

    @Test
    void keepsTheLastTen() {
        Document profile = new Document(Purse.FIELD, 100.0);
        for (int i = 1; i <= 12; i++) BankAccount.deposit(profile, i, ME, i * 1000L);
        List<Transaction> recent = BankAccount.recent(profile);
        assertEquals(BankAccount.KEPT, recent.size());
        assertEquals(12, recent.getFirst().amount());
        assertEquals(12_000, recent.getFirst().at());
        assertEquals(3, recent.getLast().amount());
        assertEquals(BankAccount.KEPT, ((List<?>) ((Document) profile.get(BankAccount.KEY)).get("transactions")).size());
    }

    @Test
    void aNewProfilesAccount() {
        // As UserCollection makes it: a whole 0 and no transactions.
        Document profile = new Document(BankAccount.KEY, new Document("balance", 0).append("transactions", List.of())).append(Purse.FIELD, 3.0);
        assertEquals(0, BankAccount.balance(profile));
        assertEquals(List.of(), BankAccount.recent(profile));
        assertEquals(3, deposit(profile, 3));
        assertEquals(3, BankAccount.balance(profile));
        assertEquals(0, BankAccount.balance(new Document()));
    }

    /**
     * Interest is due at each season's start: at 05:21.1 into the recording the menu said 04h 52m 09s,
     * 27 seconds short of this (the recording's clock was behind Hypixel's by that much).
     */
    @Test
    void interestAtTheNextSeason() {
        long recordingStart = 1_792_832_222_021L;
        long now = recordingStart + 321_100;
        long next = BankAccount.nextInterest(now);
        assertEquals(0, (next - SkyBlockTime.EPOCH) % BankAccount.SEASON_MS);
        long recorded = (4 * 3600 + 52 * 60 + 9) * 1000L;
        assertTrue(Math.abs(next - now - recorded) < 60_000, () -> (next - now) + " ms to go");
        assertEquals(31 * 3600_000L, BankAccount.SEASON_MS);
        // At a season's very start, the next one.
        assertEquals(next + BankAccount.SEASON_MS, BankAccount.nextInterest(next));
    }
}
