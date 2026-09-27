package net.icxd.dungeons.user;

import lombok.Getter;
import net.icxd.dungeons.common.Rank;
import lombok.Setter;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.bank.BankTransaction;
import net.icxd.dungeons.crimsonisle.factions.FactionTitle;
import net.icxd.dungeons.crimsonisle.factions.FactionType;
import net.icxd.dungeons.dwarven.Perk;
import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.profile.Profiles;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** A player's data, held by this server while they're on it (see {@link UserStore}). */
@Getter
public class User {
    /** Written by logins (off the main thread), read everywhere. */
    private static final Map<UUID, User> usersCache = new ConcurrentHashMap<>();

    private final UUID uuid;
    private Document document;
    /** When this server claimed the data, and how long loading it took. */
    private long claimedAt;
    private long loadMillis;
    /** Handed off: still readable while the player is here, but no longer saved from this server. */
    private volatile boolean released;
    /** This server has put the stored inventory on the player (or adopted theirs), so saving it is safe. */
    private boolean inventoryRestored;
    /** Main thread. */
    private long lastSavedAt;
    private int skyBlockXp;

    @Setter
    private boolean inDungeon = false;

    private User(UUID uuid) {
        this.uuid = uuid;
    }

    /** The user if this server holds their data, else null. */
    public static User cached(UUID uuid) {
        return usersCache.get(uuid);
    }

    /** The user if this server holds their data and it's loaded, else null. */
    public static User ifLoaded(UUID uuid) {
        User user = usersCache.get(uuid);
        return user == null || !user.isLoaded() ? null : user;
    }

    /** A player's rank; DEFAULT until their data is loaded. */
    public static Rank rankOf(UUID uuid) {
        User user = ifLoaded(uuid);
        return user == null ? Rank.DEFAULT : user.getRank();
    }

    static List<User> all() {
        return List.copyOf(usersCache.values());
    }

    static User loaded(UUID uuid, Document document, long startedAt) {
        User user = new User(uuid);
        user.document = document;
        user.claimedAt = System.currentTimeMillis();
        user.loadMillis = user.claimedAt - startedAt;
        usersCache.put(uuid, user);
        return user;
    }

    static void forget(User user) {
        usersCache.remove(user.uuid, user);
    }

    void markReleased() {
        released = true;
    }

    void markInventoryRestored() {
        inventoryRestored = true;
    }

    void markSaved() {
        lastSavedAt = System.currentTimeMillis();
    }

    public boolean isLoaded() {
        return document != null;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    /** Saves in the background. Main thread. */
    public void save() {
        Dungeons.getUserStore().save(this);
    }

    /** An account value by its dotted path, e.g. "settings.autoReadyUp". Their profile's are {@link #profileValue}. */
    public <T> T get(String path, Class<T> clazz) {
        return at(document, path, clazz);
    }

    /** A value of the profile they play on, by its dotted path, e.g. "bank.balance". */
    public <T> T profileValue(String path, Class<T> clazz) {
        return at(profile(), path, clazz);
    }

    private static <T> T at(Document doc, String path, Class<T> clazz) {
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1 && doc != null; i++) {
            doc = doc.get(parts[i], Document.class);
        }
        return doc == null ? null : doc.get(parts[parts.length - 1], clazz);
    }

    /**
     * The profile they play on: its coins, skills, items and the rest (see {@link Profiles}). Every
     * loaded user has one (UserStore sees to it); changing it is a profile switch, which swaps their
     * items too (see UserStore#switchProfile).
     */
    public Document profile() {
        return Profiles.selected(document);
    }

    public String profileId() {
        return document.getString(Profiles.SELECTED);
    }

    public String profileName() {
        Document profile = profile();
        String name = profile == null ? null : profile.getString(Profiles.NAME);
        return name == null ? "?" : name;
    }

    public ProfileMode mode() {
        return Profiles.mode(profile());
    }

    public Rank getRank() { return Rank.valueOf(get("rank", String.class)); }
    public int getCoins() { return number(profileValue("coins", Number.class)); }
    /** Older saves stored it as a double. */
    public int getBankBalance() { return number(profileValue("bank.balance", Number.class)); }
    public int getBits() { return number(profileValue("bits", Number.class)); }
    public int getGems() { return number(get("gems", Number.class)); }
    /** Null until they pick one. */
    public FactionType getFaction() {
        String faction = profileValue("crimsonIsle.selectedFaction", String.class);
        return faction == null ? null : FactionType.valueOf(faction);
    }
    public int getFactionReputation() { return number(profileValue("crimsonIsle.factions."+getFaction().name().toLowerCase()+".reputation", Number.class)); }
    public FactionTitle getFactionTitle() { return FactionTitle.get(getFactionReputation()); }

    public int getHOTMTokens() { return number(profileValue("dwarvenMines.hotm.tokens", Number.class)); }
    public int getHOTMPowder(PowderType type) { return number(profileValue("dwarvenMines.powder."+type.name(), Number.class)); }
    public int getHOTMPerkLevel(Perk perk) { return number(profileValue("dwarvenMines.hotm.tree."+perk.name(), Number.class)); }

    private static int number(Number n) {
        return n == null ? 0 : n.intValue();
    }

    /** In their purse, on the profile they play on. */
    public void setCoins(int coins) {
        profile().put("coins", coins);
    }

    public void withdrawBank(int amount) {
        bank(getBankBalance() - amount, amount, BankTransaction.TransactionType.WITHDRAW);
    }

    public void depositBank(int amount) {
        bank(getBankBalance() + amount, amount, BankTransaction.TransactionType.DEPOSIT);
    }

    private void bank(int balance, int amount, BankTransaction.TransactionType type) {
        Document bank = profile().get("bank", Document.class);
        bank.append("balance", balance);
        bank.getList("transactions", Document.class).add(new BankTransaction(getPlayer(), amount, type).toDocument());
        save();
    }

    public void calculateSkyBlockXp() {
        int xp = 0;

        this.skyBlockXp = xp;
    }
}
