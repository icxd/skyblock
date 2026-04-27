package net.icxd.dungeons.user;

import lombok.Getter;
import lombok.Setter;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.bank.BankTransaction;
import net.icxd.dungeons.crimsonisle.factions.FactionTitle;
import net.icxd.dungeons.crimsonisle.factions.FactionType;
import net.icxd.dungeons.database.ICollection;
import net.icxd.dungeons.dwarven.Perk;
import net.icxd.dungeons.dwarven.PowderType;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class User {
  public static HashMap<UUID, User> usersCache = new HashMap<>();

  private UUID uuid;
  private Document document;

  private Player player;
  private int skyBlockXp;

  @Setter
  private boolean inDungeon = false;

  public User(UUID uuid) {
    this.uuid = uuid;
    this.player = Bukkit.getPlayer(uuid);
  }

  public static User getUser(UUID uuid) {
    if (usersCache.containsKey(uuid))
      return usersCache.get(uuid);
    User user = new User(uuid);
    usersCache.put(uuid, user);
    return user;
  }

  public void load() {
    ICollection users = Dungeons.getUserCollection();
    Document found = users.get().find(new Document("uuid", uuid.toString())).first();

    if (found == null) {
      document = users.defaultDocument()
          .append("uuid", uuid.toString())
          .append("username", player.getName())
          .append("ip", player.getAddress().getAddress().getHostAddress());
      users.get().insertOne(document);
    }

    document = found;

    for (String key : users.defaultDocument().keySet()) {
      if (document.containsKey(key)) continue;
      document.append(key, users.defaultDocument().get(key));
    }

    users.get().replaceOne(new Document("uuid", uuid.toString()), document);
    usersCache.put(uuid, this);
  }

  public void save() {
    ICollection users = Dungeons.getUserCollection();
    users.get().replaceOne(new Document("uuid", uuid.toString()), document);
  }

  public <T> T get(String path, Class<T> clazz) {
    final String[] strings = path.split("\\.");
    if (strings.length == 1)
      return document.get(strings[0], clazz);
    final String last = strings[strings.length - 1];
    Document doc = document;
    for (String string : strings) {
      if (string.equalsIgnoreCase(last)) break;
      doc = doc.get(string, Document.class);
    }
    return doc.get(last, clazz);
  }
  public Rank getRank() { return Rank.valueOf(get("rank", String.class)); }
  public int getCoins() { return get("coins", Integer.class); }
  public int getBankBalance() { return get("bank.balance", Integer.class); }
  public int getBits() { return get("bits", Integer.class); }
  public int getGems() { return get("gems", Integer.class); }
  public FactionType getFaction() { return FactionType.valueOf(get("crimsonIsle.selectedFaction", String.class)); }
  public int getFactionReputation() { return get("crimsonIsle.factions."+getFaction().name().toLowerCase()+".reputation", Integer.class); }
  public FactionTitle getFactionTitle() { return FactionTitle.get(getFactionReputation()); }

  public int getHOTMTokens() { return get("dwarvenMines.hotm.tokens", Integer.class); }
  public int getHOTMPowder(PowderType type) { return get("dwarvenMines.powder."+type.name(), Integer.class); }
  public int getHOTMPerkLevel(Perk perk) { return get("dwarvenMines.hotm.tree."+perk.name(), Integer.class); }

  public void withdrawBank(int amount) {
    double newBalance = getBankBalance() - amount;
    document.get("bank", Document.class).append("balance", newBalance);
    document.get("bank", Document.class).getList("transactions", Document.class)
        .add(new BankTransaction(player, amount, BankTransaction.TransactionType.WITHDRAW).toDocument());
    save();
  }

  public void depositBank(int amount) {
    double newBalance = getBankBalance() + amount;
    document.get("bank", Document.class).append("balance", newBalance);
    document.get("bank", Document.class).getList("transactions", Document.class)
        .add(new BankTransaction(player, amount, BankTransaction.TransactionType.DEPOSIT).toDocument());
    save();
  }

  public void calculateSkyBlockXp() {
    int xp = 0;

    this.skyBlockXp = xp;
  }
}
