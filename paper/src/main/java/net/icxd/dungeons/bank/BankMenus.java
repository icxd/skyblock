package net.icxd.dungeons.bank;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.bson.Document;
import org.bukkit.Material;

import net.icxd.dungeons.bank.BankAccount.Refusal;
import net.icxd.dungeons.bank.BankAccount.Transaction;
import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.menu.Icon;

/**
 * What the bank's menus show and its chat says, as recorded (2026-10-24, a new profile's first deposits
 * and withdrawals): the Personal Bank Account, Bank Deposit and Bank Withdrawal, four rows of black glass
 * round their buttons. The menus give amounts to a tenth, a half going up; chat and the transactions
 * give whole coins. As data, so it's checked without a server (see {@link BankMenu} for the menus).
 */
public final class BankMenus {
    public static final String ACCOUNT = "Personal Bank Account";
    public static final String DEPOSIT = "Bank Deposit";
    public static final String WITHDRAWAL = "Bank Withdrawal";
    static final int SIZE = 36;

    static final int DEPOSIT_COINS = 11;
    static final int WITHDRAW_COINS = 13;
    static final int TRANSACTIONS = 15;
    static final int CLOSE = 31;
    static final int INFORMATION = 32;

    static final int WHOLE_PURSE = 11;
    static final int HALF_PURSE = 13;
    static final int DEPOSIT_AMOUNT = 15;

    static final int EVERYTHING = 10;
    static final int HALF = 12;
    static final int FIFTH = 14;
    static final int WITHDRAW_AMOUNT = 16;

    static final int BACK = 31;

    /** The sign that asks how much (its first line is for the number). */
    static final List<String> DEPOSIT_SIGN = List.of("", "^^^^^^^^^^^^^^^", "Enter the amount", "to deposit");
    static final List<String> WITHDRAW_SIGN = List.of("", "^^^^^^^^^^^^^^^", "Enter the amount", "to withdraw");

    /** A menu: its items by slot, how many each is (one if it isn't here), and where its glass is. */
    public record Layout(Map<Integer, Icon> icons, Map<Integer, Integer> counts, Set<Integer> glass) {
    }

    private BankMenus() {
    }

    /** The Personal Bank Account, at {@code now} (its countdown ticks). */
    public static Layout account(Document profile, long now) {
        double balance = BankAccount.balance(profile);
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(DEPOSIT_COINS, new Icon(Material.CHEST, "&aDeposit Coins", balance(balance), "", "&7Store coins in the bank to keep them",
                "&7safe while you go on adventures!", "", "&7You will earn &b2% &7interest every season", "&7for your first &610 million &7banked coins.",
                "", "&7Until interest: &b" + untilInterest(now), "", "&eClick to make a deposit!"));
        icons.put(WITHDRAW_COINS, new Icon(Material.DROPPER, "&aWithdraw Coins", balance(balance), "", "&7Take your coins out of the bank in",
                "&7order to spend them.", "", "&eClick to withdraw coins!"));
        icons.put(TRANSACTIONS, new Icon(Material.FILLED_MAP, "&aRecent transactions", transactions(BankAccount.recent(profile), now)));
        icons.put(CLOSE, new Icon(Material.BARRIER, "&cClose"));
        icons.put(INFORMATION, new Icon(Material.REDSTONE_TORCH, "&aInformation", "&7Keep your coins safe in the bank!",
                "&7You lose half the coins in your purse", "&7when dying in combat.", "", "&7Balance limit: &650 Million", "",
                "&7The banker rewards you every 31", "&7hours with &binterest &7for the coins in", "&7your bank balance.", "",
                "&7Interest in: &b" + interestIn(now), "", "&a>&a&m------- &r  &6Interest Tranches&a  &a&m-------&a<",
                " &eFirst &610M &ecoins yields &b2% &einterest.", " &eFrom &610M &eto &615M &ecoins yields &b1% &einterest.", "",
                " &7Max interest: &6250,000", " &8 (With 15,000,000 balance)", "&a>&a&m---------------------------------&a<"));
        return new Layout(icons, Map.of(), glassBut(icons.keySet()));
    }

    /** Bank Deposit: the whole purse, half of it, or an amount typed on a sign. */
    public static Layout deposit(Document profile) {
        double balance = BankAccount.balance(profile);
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(WHOLE_PURSE, preset("&aYour whole purse", "&8Bank deposit", balance, "&7Amount to deposit: &6", BankAccount.wholePurse(profile),
                "&eClick to deposit coins!"));
        icons.put(HALF_PURSE, preset("&aHalf your purse", "&8Bank deposit", balance, "&7Amount to deposit: &6", BankAccount.halfPurse(profile),
                "&eClick to deposit coins!"));
        icons.put(DEPOSIT_AMOUNT, new Icon(Material.OAK_SIGN, "&aSpecific amount", balance(balance), "", "&eClick to deposit coins!"));
        icons.put(BACK, back());
        return new Layout(icons, Map.of(WHOLE_PURSE, 64, HALF_PURSE, 32), glassBut(icons.keySet()));
    }

    /** Bank Withdrawal: everything, half, a fifth, or an amount typed on a sign. */
    public static Layout withdrawal(Document profile) {
        double balance = BankAccount.balance(profile);
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(EVERYTHING, withdraw("&aEverything in the account", balance, BankAccount.everything(profile)));
        icons.put(HALF, withdraw("&aHalf the account", balance, BankAccount.half(profile)));
        icons.put(FIFTH, withdraw("&aWithdraw 20%", balance, BankAccount.fifth(profile)));
        icons.put(WITHDRAW_AMOUNT, new Icon(Material.OAK_SIGN, "&aSpecific amount", balance(balance), "", "&eClick to withdraw coins!"));
        icons.put(BACK, back());
        return new Layout(icons, Map.of(EVERYTHING, 64, HALF, 32), glassBut(icons.keySet()));
    }

    private static Icon preset(String name, String kind, double balance, String amountLine, double amount, String click) {
        return new Icon(Material.CHEST, name, kind, "", balance(balance), amountLine + Coins.bank(amount), "", click);
    }

    private static Icon withdraw(String name, double balance, double amount) {
        return new Icon(Material.DROPPER, name, "&8Bank withdrawal", "", balance(balance), "&7Amount to withdraw: &6" + Coins.bank(amount), "",
                "&eClick to withdraw coins!");
    }

    private static Icon back() {
        return new Icon(Material.ARROW, "&aGo Back", "&7To Personal Bank Account");
    }

    private static String balance(double balance) {
        return "&7Current balance: &6" + Coins.bank(balance);
    }

    private static Set<Integer> glassBut(Set<Integer> used) {
        Set<Integer> glass = new TreeSet<>();
        for (int slot = 0; slot < SIZE; slot++) if (!used.contains(slot)) glass.add(slot);
        return glass;
    }

    // The words

    /** "&7There are no recent transactions!", or a blank and then each, newest first. */
    static List<String> transactions(List<Transaction> recent, long now) {
        if (recent.isEmpty()) return List.of("&7There are no recent transactions!");
        List<String> lines = new ArrayList<>();
        lines.add("");
        for (Transaction t : recent) lines.add(transaction(t, now));
        return lines;
    }

    /** "&a+ &620&7, &eJust now", "&c- &67&7, &e3 secs ago". */
    static String transaction(Transaction t, long now) {
        return (t.type() == BankAccount.Type.DEPOSIT ? "&a+ " : "&c- ") + "&6" + Coins.whole(t.amount()) + "&7, &e" + ago(now - t.at());
    }

    /**
     * "Just now", "1 sec ago", "12 secs ago" (all recorded); then minutes, hours and days the same way
     * ("1 min ago", "3 hours ago"), which no recording has (UNKNOWN).
     */
    static String ago(long millis) {
        long seconds = Math.max(0, millis) / 1000;
        if (seconds == 0) return "Just now";
        if (seconds < 60) return count(seconds, "sec") + " ago";
        if (seconds < 3600) return count(seconds / 60, "min") + " ago";
        if (seconds < 86400) return count(seconds / 3600, "hour") + " ago";
        return count(seconds / 86400, "day") + " ago";
    }

    /**
     * "Until interest: 4 Hours", as the tab list's "Interest: 4 Hours" too (whole hours, recorded; under an
     * hour, minutes, then seconds: UNKNOWN).
     */
    public static String untilInterest(long now) {
        long seconds = (BankAccount.nextInterest(now) - now) / 1000;
        if (seconds >= 3600) return count(seconds / 3600, "Hour");
        if (seconds >= 60) return count(seconds / 60, "Minute");
        return count(seconds, "Second");
    }

    /** "Interest in: 04h 52m 09s". */
    static String interestIn(long now) {
        long seconds = (BankAccount.nextInterest(now) - now) / 1000;
        return String.format("%02dh %02dm %02ds", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    private static String count(long n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }

    /** "&8Depositing coins..." before a deposit is made. */
    static final String DEPOSITING = "&8Depositing coins...";
    static final String WITHDRAWING = "&8Withdrawing coins...";

    /** "&aYou have deposited &620 coins&a! You now have &620 coins &ain your account!" */
    static String deposited(double amount, double balance) {
        return "&aYou have deposited &6" + coins(amount) + "&a! You now have &6" + coins(balance) + " &ain your account!";
    }

    /** "&aYou have withdrawn &61 coin&a! You now have &67 coins &ain your account!" */
    static String withdrawn(double amount, double balance) {
        return "&aYou have withdrawn &6" + coins(amount) + "&a! You now have &6" + coins(balance) + " &ain your account!";
    }

    /** "20 coins", "1 coin", "0 coins": whole coins. */
    static String coins(double amount) {
        String whole = Coins.whole(amount);
        return whole + (whole.equals("1") ? " coin" : " coins");
    }

    /**
     * Why it didn't happen. "You can't deposit this little!" is recorded (an empty purse's whole purse,
     * which made no sound); the others are this plugin's words (UNKNOWN).
     */
    static String refusal(Refusal why, boolean deposit) {
        return switch (why) {
            case TOO_LITTLE -> deposit ? "&cYou can't deposit this little!" : "&cYou can't withdraw this little!";
            case NOT_ENOUGH -> deposit ? "&cYou don't have that many coins in your purse!" : "&cYou don't have that many coins in the bank!";
            case FULL -> "&cYour bank account is full!";
        };
    }

    /** What's typed on the sign isn't an amount (UNKNOWN words). */
    static final String NOT_A_NUMBER = "&cThat's not a valid amount!";

    /**
     * An amount as typed: "24", "1,500", "2.5k", "1m" (k, m and b for thousands, millions and billions:
     * UNKNOWN whether Hypixel's bank takes those). Null if it isn't one.
     */
    static Double typed(String text) {
        if (text == null) return null;
        String s = text.strip().replace(",", "").toLowerCase(Locale.ROOT);
        double scale = 1;
        if (s.endsWith("k")) scale = 1e3;
        else if (s.endsWith("m")) scale = 1e6;
        else if (s.endsWith("b")) scale = 1e9;
        if (scale != 1) s = s.substring(0, s.length() - 1);
        if (!s.matches("\\d+(\\.\\d+)?|\\.\\d+")) return null;
        double amount = new BigDecimal(s).multiply(BigDecimal.valueOf(scale)).doubleValue();
        return Double.isFinite(amount) ? amount : null;
    }
}
