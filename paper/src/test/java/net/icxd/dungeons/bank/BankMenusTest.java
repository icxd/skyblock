package net.icxd.dungeons.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.bank.BankAccount.Refusal;
import net.icxd.dungeons.bank.BankMenus.Layout;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.utils.SkyBlockTime;

/** Against the recording (2026-10-24): the bank's menus and chat, as Hypixel wrote them. */
class BankMenusTest {
    private static final UUID ME = UUID.fromString("8f1c3c1e-7d0a-4f5e-9a51-3b2a4c6d7e8f");
    /** A season's start, and when the recording's menu said "04h 52m 09s" before one. */
    private static final long SEASON_START = SkyBlockTime.EPOCH + 100 * BankAccount.SEASON_MS;
    private static final long AT_0521 = SEASON_START - (4 * 3600 + 52 * 60 + 9) * 1000L;

    @Test
    void theAccountAsRecorded() {
        Layout account = BankMenus.account(new Document(), AT_0521);
        assertEquals(Set.of(11, 13, 15, 31, 32), account.icons().keySet());
        assertEquals(31, account.glass().size());
        assertEquals(Map.of(), account.counts());
        assertIcon(account, 11, Material.CHEST, "&aDeposit Coins", "&7Current balance: &60", "", "&7Store coins in the bank to keep them",
                "&7safe while you go on adventures!", "", "&7You will earn &b2% &7interest every season", "&7for your first &610 million &7banked coins.",
                "", "&7Until interest: &b4 Hours", "", "&eClick to make a deposit!");
        assertIcon(account, 13, Material.DROPPER, "&aWithdraw Coins", "&7Current balance: &60", "", "&7Take your coins out of the bank in",
                "&7order to spend them.", "", "&eClick to withdraw coins!");
        assertIcon(account, 15, Material.FILLED_MAP, "&aRecent transactions", "&7There are no recent transactions!");
        assertIcon(account, 31, Material.BARRIER, "&cClose");
        assertIcon(account, 32, Material.REDSTONE_TORCH, "&aInformation", "&7Keep your coins safe in the bank!",
                "&7You lose half the coins in your purse", "&7when dying in combat.", "", "&7Balance limit: &650 Million", "",
                "&7The banker rewards you every 31", "&7hours with &binterest &7for the coins in", "&7your bank balance.", "",
                "&7Interest in: &b04h 52m 09s", "", "&a>&a&m------- &r  &6Interest Tranches&a  &a&m-------&a<",
                " &eFirst &610M &ecoins yields &b2% &einterest.", " &eFrom &610M &eto &615M &ecoins yields &b1% &einterest.", "",
                " &7Max interest: &6250,000", " &8 (With 15,000,000 balance)", "&a>&a&m---------------------------------&a<");
        // A second on (05:22.0's update).
        assertEquals("&7Interest in: &b04h 52m 08s", BankMenus.account(new Document(), AT_0521 + 1000).icons().get(32).lore().get(10));
    }

    /** The recording's deposits and withdrawals at the times chat said them, and the list at 06:56.0. */
    @Test
    void theRecordedTransactions() {
        Document profile = new Document(Purse.FIELD, 20.5);
        BankAccount.deposit(profile, 20.5, ME, 396_300);
        Purse.add(profile, 10);
        BankAccount.withdraw(profile, 20, ME, 399_600);
        BankAccount.deposit(profile, 15, ME, 401_200);
        BankAccount.withdraw(profile, 7, ME, 402_800);
        BankAccount.withdraw(profile, 1, ME, 404_100);
        BankAccount.deposit(profile, 23, ME, 408_500);
        BankAccount.withdraw(profile, 24, ME, 412_600);
        BankAccount.withdraw(profile, 6, ME, 416_000);
        assertEquals(List.of("", "&c- &66&7, &eJust now", "&c- &624&7, &e3 secs ago", "&a+ &623&7, &e7 secs ago", "&c- &61&7, &e11 secs ago",
                        "&c- &67&7, &e13 secs ago", "&a+ &615&7, &e14 secs ago", "&c- &620&7, &e16 secs ago", "&a+ &620&7, &e19 secs ago"),
                BankMenus.account(profile, 416_000).icons().get(15).lore());
        assertEquals("&7Current balance: &60.5", BankMenus.account(profile, 416_000).icons().get(11).lore().getFirst());
    }

    @Test
    void theDepositMenu() {
        Layout deposit = BankMenus.deposit(new Document(Purse.FIELD, 20.5));
        assertEquals(Set.of(11, 13, 15, 31), deposit.icons().keySet());
        assertEquals(32, deposit.glass().size());
        assertEquals(Map.of(11, 64, 13, 32), deposit.counts());
        assertIcon(deposit, 11, Material.CHEST, "&aYour whole purse", "&8Bank deposit", "", "&7Current balance: &60", "&7Amount to deposit: &620.5",
                "", "&eClick to deposit coins!");
        // Half of 20.5 is 10.25, shown "10.3".
        assertIcon(deposit, 13, Material.CHEST, "&aHalf your purse", "&8Bank deposit", "", "&7Current balance: &60", "&7Amount to deposit: &610.3",
                "", "&eClick to deposit coins!");
        assertIcon(deposit, 15, Material.OAK_SIGN, "&aSpecific amount", "&7Current balance: &60", "", "&eClick to deposit coins!");
        assertIcon(deposit, 31, Material.ARROW, "&aGo Back", "&7To Personal Bank Account");
        assertEquals("&7Amount to deposit: &611.5", BankMenus.deposit(new Document(Purse.FIELD, 23.0)).icons().get(13).lore().get(3));
    }

    @Test
    void theWithdrawalMenu() {
        Layout withdrawal = BankMenus.withdrawal(new Document(BankAccount.KEY, new Document("balance", 20.5)));
        assertEquals(Set.of(10, 12, 14, 16, 31), withdrawal.icons().keySet());
        assertEquals(31, withdrawal.glass().size());
        assertEquals(Map.of(10, 64, 12, 32), withdrawal.counts());
        assertIcon(withdrawal, 10, Material.DROPPER, "&aEverything in the account", "&8Bank withdrawal", "", "&7Current balance: &620.5",
                "&7Amount to withdraw: &620", "", "&eClick to withdraw coins!");
        assertIcon(withdrawal, 12, Material.DROPPER, "&aHalf the account", "&8Bank withdrawal", "", "&7Current balance: &620.5",
                "&7Amount to withdraw: &610", "", "&eClick to withdraw coins!");
        assertIcon(withdrawal, 14, Material.DROPPER, "&aWithdraw 20%", "&8Bank withdrawal", "", "&7Current balance: &620.5",
                "&7Amount to withdraw: &64", "", "&eClick to withdraw coins!");
        assertIcon(withdrawal, 16, Material.OAK_SIGN, "&aSpecific amount", "&7Current balance: &620.5", "", "&eClick to withdraw coins!");
        assertIcon(withdrawal, 31, Material.ARROW, "&aGo Back", "&7To Personal Bank Account");
    }

    @Test
    void chat() {
        assertEquals("&8Depositing coins...", BankMenus.DEPOSITING);
        assertEquals("&8Withdrawing coins...", BankMenus.WITHDRAWING);
        assertEquals("&aYou have deposited &620 coins&a! You now have &620 coins &ain your account!", BankMenus.deposited(20.5, 20.5));
        assertEquals("&aYou have withdrawn &620 coins&a! You now have &60 coins &ain your account!", BankMenus.withdrawn(20, 0.5));
        assertEquals("&aYou have withdrawn &61 coin&a! You now have &67 coins &ain your account!", BankMenus.withdrawn(1, 7.5));
        assertEquals("&aYou have deposited &61,500 coins&a! You now have &61,500 coins &ain your account!", BankMenus.deposited(1500, 1500));
        assertEquals("&cYou can't deposit this little!", BankMenus.refusal(Refusal.TOO_LITTLE, true));
        assertEquals(List.of("", "^^^^^^^^^^^^^^^", "Enter the amount", "to deposit"), BankMenus.DEPOSIT_SIGN);
        assertEquals(List.of("", "^^^^^^^^^^^^^^^", "Enter the amount", "to withdraw"), BankMenus.WITHDRAW_SIGN);
    }

    @Test
    void howLongAgo() {
        assertEquals("Just now", BankMenus.ago(0));
        assertEquals("Just now", BankMenus.ago(999));
        assertEquals("1 sec ago", BankMenus.ago(1600));
        assertEquals("3 secs ago", BankMenus.ago(3400));
        assertEquals("1 min ago", BankMenus.ago(60_000));
        assertEquals("2 hours ago", BankMenus.ago(2 * 3600_000 + 5));
        assertEquals("1 day ago", BankMenus.ago(86_400_000));
        assertEquals("Just now", BankMenus.ago(-50));
    }

    @Test
    void untilInterest() {
        assertEquals("4 Hours", BankMenus.untilInterest(AT_0521));
        assertEquals("28 Hours", BankMenus.untilInterest(SEASON_START - 28 * 3600_000L - 1));
        assertEquals("1 Hour", BankMenus.untilInterest(SEASON_START - 3600_000L));
        assertEquals("59 Minutes", BankMenus.untilInterest(SEASON_START - 3599_000L));
        assertEquals("30 Seconds", BankMenus.untilInterest(SEASON_START - 30_000L));
        assertEquals("30h 59m 59s", BankMenus.interestIn(SEASON_START + 1000));
    }

    @Test
    void typedAmounts() {
        assertEquals(24.0, BankMenus.typed("24"));
        assertEquals(24.0, BankMenus.typed(" 24 "));
        assertEquals(1500.0, BankMenus.typed("1,500"));
        assertEquals(2.5, BankMenus.typed("2.5"));
        assertEquals(2500.0, BankMenus.typed("2.5k"));
        assertEquals(1_000_000.0, BankMenus.typed("1M"));
        assertEquals(3_000_000_000.0, BankMenus.typed("3b"));
        assertNull(BankMenus.typed("abc"));
        assertNull(BankMenus.typed("-5"));
        assertNull(BankMenus.typed(""));
        assertNull(BankMenus.typed("k"));
        assertNull(BankMenus.typed("1e5"));
    }

    private static void assertIcon(Layout layout, int slot, Material material, String name, String... lore) {
        Icon icon = layout.icons().get(slot);
        assertEquals(material, icon.material(), "slot " + slot);
        assertEquals(name, icon.name(), "slot " + slot);
        assertEquals(List.of(lore), icon.lore(), "slot " + slot);
    }
}
