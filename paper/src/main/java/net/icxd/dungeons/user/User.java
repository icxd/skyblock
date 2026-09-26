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

  /** A value by its dotted path, e.g. "bank.balance". */
  public <T> T get(String path, Class<T> clazz) {
    String[] parts = path.split("\\.");
    Document doc = document;
    for (int i = 0; i < parts.length - 1; i++) {
      doc = doc.get(parts[i], Document.class);
      if (doc == null) return null;
    }
    return doc.get(parts[parts.length - 1], clazz);
  }
  public Rank getRank() { return Rank.valueOf(get("rank", String.class)); }
  public int getCoins() { return get("coins", Integer.class); }
  /** Older saves stored it as a double. */
  public int getBankBalance() {
    Number balance = get("bank.balance", Number.class);
    return balance == null ? 0 : balance.intValue();
  }
  public int getBits() { return get("bits", Integer.class); }
  public int getGems() { return get("gems", Integer.class); }
  /** Null until they pick one. */
  public FactionType getFaction() {
    String faction = get("crimsonIsle.selectedFaction", String.class);
    return faction == null ? null : FactionType.valueOf(faction);
  }
  public int getFactionReputation() { return get("crimsonIsle.factions."+getFaction().name().toLowerCase()+".reputation", Integer.class); }
  public FactionTitle getFactionTitle() { return FactionTitle.get(getFactionReputation()); }

  public int getHOTMTokens() { return get("dwarvenMines.hotm.tokens", Integer.class); }
  public int getHOTMPowder(PowderType type) { return get("dwarvenMines.powder."+type.name(), Integer.class); }
  public int getHOTMPerkLevel(Perk perk) { return get("dwarvenMines.hotm.tree."+perk.name(), Integer.class); }

  public void withdrawBank(int amount) {
    int newBalance = getBankBalance() - amount;
    document.get("bank", Document.class).append("balance", newBalance);
    document.get("bank", Document.class).getList("transactions", Document.class)
        .add(new BankTransaction(getPlayer(), amount, BankTransaction.TransactionType.WITHDRAW).toDocument());
    save();
  }

  public void depositBank(int amount) {
    int newBalance = getBankBalance() + amount;
    document.get("bank", Document.class).append("balance", newBalance);
    document.get("bank", Document.class).getList("transactions", Document.class)
        .add(new BankTransaction(getPlayer(), amount, BankTransaction.TransactionType.DEPOSIT).toDocument());
    save();
  }

  public void calculateSkyBlockXp() {
    int xp = 0;

    this.skyBlockXp = xp;
  }
}
