package net.icxd.dungeons.user;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Keeps each player's data held by one server at a time, so it can move between servers behind a
 * proxy without one server overwriting what another saved.
 *
 * <p>A user document's {@code session.server} names the server holding it. A server claims it
 * while the player logs in (before they join, so the data is there from their first tick), saves
 * only while it still holds it, and releases it when they leave. If another server holds it, the
 * login waits for that server to release it; if that server has stopped (its heartbeat in the
 * {@code servers} collection doesn't move), it takes it over. To send a player to another server,
 * {@link #handOff} them first, so the data is saved and released before they arrive.
 *
 * <p>Writes run on one thread, in order. Documents are copied on the main thread first, since
 * that's where they change.
 */
public final class UserStore {
  private static final long POLL_MILLIS = 100;
  /** How long a login waits for a running server to release the player's data. */
  private static final long WAIT_FOR_RELEASE_MILLIS = 5_000;
  /** A server whose heartbeat hasn't moved for this long has stopped. */
  private static final long STOPPED_AFTER_MILLIS = 12_000;
  private static final long HEARTBEAT_TICKS = 3 * 20;
  /** Each player is saved this long after their last save, so saves spread out instead of all landing in one tick. */
  private static final long AUTOSAVE_MILLIS = 60_000;
  /** Data claimed for a login that never joined (refused later, or the connection dropped) is released after this. */
  private static final long ABANDONED_AFTER_MILLIS = 30_000;
  /** Taking back a handed-off player's data is tried this often, this far apart, before they're sent to rejoin. */
  private static final int RECLAIM_ATTEMPTS = 8;
  private static final long RECLAIM_RETRY_TICKS = 5 * 20;
  private static final FindOneAndUpdateOptions AFTER = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

  /** Why a login can't have its data yet. */
  public static final class HeldElsewhereException extends Exception {
    public HeldElsewhereException(String server) {
      super("held by " + server);
    }
  }

  private final Plugin plugin;
  private final Logger log;
  private final String server;
  private final String type;
  /** A fresh default document each time: nested documents must not be shared between players. */
  private final Supplier<Document> defaults;
  private final MongoCollection<Document> users;
  private final MongoCollection<Document> servers;
  private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "user-saves"));
  /** Players whose data is being taken back right now. Main thread. */
  private final Set<UUID> reclaiming = new HashSet<>();

  /** @param server this server's name; every server on the network needs its own */
  public UserStore(Plugin plugin, String server, String type, MongoCollection<Document> users, MongoCollection<Document> servers,
                   Supplier<Document> defaults) {
    this.plugin = plugin;
    this.log = plugin.getLogger();
    this.server = server;
    this.type = type;
    this.users = users;
    this.servers = servers;
    this.defaults = defaults;
  }

  public String server() {
    return server;
  }

  public MongoCollection<Document> users() {
    return users;
  }

  /** One document per running server: {@code _id} (its name), {@code type}, {@code players}, {@code seen}, {@code beat}. */
  public MongoCollection<Document> servers() {
    return servers;
  }

  /** Call before players can join. */
  public void start() {
    try {
      users.createIndex(Indexes.ascending("uuid"), new IndexOptions().unique(true));
    } catch (RuntimeException e) {
      log.log(Level.WARNING, "Couldn't make user uuids unique (duplicates in the users collection?)", e);
    }
    // Anything still held by this server's name is left over from before it stopped; what it
    // hadn't saved is gone, so the players get their last saved data.
    long stale = users.updateMany(eq("session.server", server), set("session", null)).getModifiedCount();
    if (stale > 0) log.warning("Released " + stale + " players' data this server still held from before it stopped");

    Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, HEARTBEAT_TICKS);
  }

  /**
   * Saves and releases everyone. Call when the plugin disables: players are still connected then,
   * but this plugin won't see them quit.
   */
  public void stop() {
    for (User user : User.all()) {
      try {
        Player player = user.getPlayer();
        if (player != null && user.isInventoryRestored() && !user.isReleased()) {
          StoredInventory.rescueLooseItems(player, user.getDocument());
          player.closeInventory();
        }
        leave(user);
      } catch (RuntimeException e) {
        log.log(Level.SEVERE, "Couldn't save " + user.getUuid() + " while stopping", e);
      }
    }
    writer.shutdown();
    try {
      if (!writer.awaitTermination(10, TimeUnit.SECONDS)) log.severe("Gave up waiting for player data to save");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    try {
      // Gone from the list, so other servers take over anything still held here right away.
      servers.deleteOne(eq("_id", server));
    } catch (RuntimeException e) {
      log.log(Level.WARNING, "Couldn't remove this server from the server list", e);
    }
  }

  /** Heartbeat, autosaves, and releasing data claimed for logins that never joined. Main thread. */
  private void tick() {
    int players = Bukkit.getOnlinePlayers().size();
    long now = System.currentTimeMillis();
    for (User user : User.all()) {
      try {
        if (user.getPlayer() == null) {
          if (now - user.getClaimedAt() > ABANDONED_AFTER_MILLIS) {
            if (user.isLoaded() && !user.isReleased()) log.info("Releasing " + user.getUuid() + ": claimed for a login that never joined");
            leave(user);
          }
        } else if (user.isLoaded() && now - Math.max(user.getLastSavedAt(), user.getClaimedAt()) >= AUTOSAVE_MILLIS) {
          save(user);
        }
      } catch (RuntimeException e) {
        log.log(Level.SEVERE, "Couldn't save " + user.getUuid(), e);
      }
    }
    writer.execute(() -> {
      try {
        servers.updateOne(eq("_id", server), combine(set("type", type), set("players", players), set("seen", new Date()), inc("beat", 1L)),
            new UpdateOptions().upsert(true));
      } catch (RuntimeException e) {
        log.log(Level.WARNING, "Heartbeat failed", e);
      }
    });
  }

  /**
   * Loads a logging-in player's data, waiting for (or taking it over from) the server that holds
   * it. Blocks, so call it off the main thread.
   */
  public User claim(UUID uuid, String name, String ip) throws HeldElsewhereException, InterruptedException {
    long start = System.currentTimeMillis();
    // This server's own last save of the player (they may have just left) goes first.
    try {
      writer.submit(() -> { }).get();
    } catch (java.util.concurrent.ExecutionException e) {
      throw new IllegalStateException(e);
    }
    String id = uuid.toString();
    String holder = null;
    long beat = 0;
    long beatSince = start;
    boolean holderRunning = false;
    while (true) {
      Document doc = users.findOneAndUpdate(and(eq("uuid", id), or(eq("session", null), eq("session.server", server))),
          set("session", session()), AFTER);
      if (doc == null && users.countDocuments(eq("uuid", id)) == 0) doc = insert(id, name, ip);
      if (doc != null) return User.loaded(uuid, withDefaults(doc), start);

      Document held = users.find(eq("uuid", id)).first();
      Document session = held == null ? null : held.get("session", Document.class);
      if (session == null) continue; // released in the meantime
      String current = session.getString("server");
      long heartbeat = heartbeat(current);
      long time = System.currentTimeMillis();
      if (!current.equals(holder)) {
        holder = current;
        holderRunning = false;
        beat = heartbeat;
        beatSince = time;
      } else if (heartbeat != beat) {
        holderRunning = true;
        beat = heartbeat;
        beatSince = time;
      }

      if (heartbeat < 0 || time - beatSince >= STOPPED_AFTER_MILLIS) {
        Document taken = users.findOneAndUpdate(and(eq("uuid", id), eq("session.server", holder)), set("session", session()), AFTER);
        if (taken != null) {
          log.warning("Took over " + name + "'s data from " + holder + ", which has stopped; what it hadn't saved is lost");
          return User.loaded(uuid, withDefaults(taken), start);
        }
      } else if (holderRunning && time - start >= WAIT_FOR_RELEASE_MILLIS) {
        throw new HeldElsewhereException(holder);
      }
      Thread.sleep(POLL_MILLIS);
    }
  }

  private Document insert(String id, String name, String ip) {
    Document doc = defaults.get().append("uuid", id).append("username", name).append("ip", ip).append("session", session());
    try {
      users.insertOne(doc);
      return doc;
    } catch (MongoWriteException e) {
      if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) return null; // created elsewhere just now
      throw e;
    }
  }

  /** Top-level fields added to the defaults since the document was made. */
  private Document withDefaults(Document doc) {
    Document fresh = defaults.get();
    for (String key : fresh.keySet()) {
      if (!doc.containsKey(key)) doc.append(key, fresh.get(key));
    }
    return doc;
  }

  private Document session() {
    return new Document("server", server).append("since", new Date());
  }

  /** The server's heartbeat count, or -1 if it isn't running (never started, or stopped cleanly). */
  private long heartbeat(String name) {
    Document doc = servers.find(eq("_id", name)).first();
    return doc == null || doc.get("beat") == null ? -1 : ((Number) doc.get("beat")).longValue();
  }

  /** Saves in the background, unless the data has been handed off. Main thread. */
  public void save(User user) {
    if (!user.isReleased()) write(user, false);
  }

  /** The player is gone (or never joined): save, let other servers have the data, and drop it here. Main thread. */
  public CompletableFuture<Void> leave(User user) {
    User.forget(user);
    return user.isReleased() ? CompletableFuture.completedFuture(null) : release(user);
  }

  /**
   * Saves and releases a player's data before they're sent to another server; send them once this
   * completes. It stays readable here until they leave, but nothing more is saved from this
   * server, and their items are frozen (see InventorySyncListener) so nothing is lost or
   * duplicated in between. If they don't go after all, {@link #reclaim} it. Main thread.
   */
  public CompletableFuture<Void> handOff(Player player) {
    User user = User.cached(player.getUniqueId());
    if (user == null || user.isReleased()) return CompletableFuture.completedFuture(null);
    if (user.isInventoryRestored()) {
      StoredInventory.rescueLooseItems(player, user.getDocument());
      player.closeInventory();
    }
    return release(user);
  }

  /**
   * Takes back the data of a handed-off player who is still here: the move didn't happen. If the
   * server they were going to holds it for now (a login there that never finished), this keeps
   * trying for a while, then asks them to rejoin. Their items stay frozen until it's back. Main
   * thread.
   */
  public void reclaim(Player player) {
    reclaim(player, 0);
  }

  private void reclaim(Player player, int attempt) {
    UUID uuid = player.getUniqueId();
    User user = User.cached(uuid);
    if (!player.isOnline() || user == null || !user.isReleased() || !reclaiming.add(uuid)) return;
    String name = player.getName();
    String ip = player.getAddress().getAddress().getHostAddress();
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      User claimed = null;
      try {
        claimed = claim(uuid, name, ip);
      } catch (HeldElsewhereException e) {
        // Still held over there; try again below.
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (RuntimeException e) {
        log.log(Level.SEVERE, "Couldn't take back " + name + "'s data", e);
      }
      User result = claimed;
      Bukkit.getScheduler().runTask(plugin, () -> {
        reclaiming.remove(uuid);
        if (result == null) {
          if (!player.isOnline()) return;
          if (attempt + 1 < RECLAIM_ATTEMPTS) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> reclaim(player, attempt + 1), RECLAIM_RETRY_TICKS);
          } else {
            player.kick(Component.text("Couldn't load your profile, please rejoin.", NamedTextColor.RED));
          }
          return;
        }
        if (!player.isOnline()) {
          if (User.cached(uuid) == result) leave(result);
          return;
        }
        try {
          restoreInventory(player, result);
          log.info("Took back " + name + "'s data: they stayed on this server");
        } catch (StoredInventory.NewerDataException | RuntimeException e) {
          log.log(Level.SEVERE, "Couldn't restore " + name + "'s inventory after taking their data back", e);
          player.kick(Component.text("Couldn't load your profile, please rejoin.", NamedTextColor.RED));
        }
      });
    });
  }

  /**
   * Puts the stored inventory on a player who just joined. Until this has run, their inventory
   * isn't saved. Main thread.
   */
  public void restoreInventory(Player player, User user) throws StoredInventory.NewerDataException {
    StoredInventory.restore(player, user.getDocument(), log);
    user.markInventoryRestored();
  }

  /** The player is disconnecting: see {@link StoredInventory#rescueLooseItems}. Main thread. */
  public void rescueLooseItems(Player player, User user) {
    if (user.isLoaded() && user.isInventoryRestored() && !user.isReleased()) StoredInventory.rescueLooseItems(player, user.getDocument());
  }

  /**
   * Claims a player who is already online, when the plugin (re)loads. Their inventory is the live
   * one, so it counts as restored. Main thread.
   */
  public void claimOnline(Player player) throws HeldElsewhereException, InterruptedException {
    claim(player.getUniqueId(), player.getName(), player.getAddress().getAddress().getHostAddress()).markInventoryRestored();
  }

  private CompletableFuture<Void> release(User user) {
    user.markReleased();
    return write(user, true);
  }

  private CompletableFuture<Void> write(User user, boolean release) {
    Document doc = user.getDocument();
    if (doc == null) return CompletableFuture.completedFuture(null);
    Player player = user.getPlayer();
    if (player != null && user.isInventoryRestored()) {
      try {
        StoredInventory.capture(player, doc);
      } catch (RuntimeException e) {
        // The rest still saves; the stored inventory stays as it was.
        log.log(Level.SEVERE, "Couldn't save " + player.getName() + "'s inventory", e);
      }
    }
    user.markSaved();
    BsonDocument copy;
    try {
      copy = doc.toBsonDocument(Document.class, users.getCodecRegistry());
    } catch (RuntimeException e) {
      log.log(Level.SEVERE, "Couldn't save " + user.getUuid(), e);
      if (!release) return CompletableFuture.failedFuture(e);
      // Still let go of it, or they couldn't join any other server until this one restarts.
      return CompletableFuture.runAsync(() -> users.updateOne(and(eq("uuid", user.getUuid().toString()), eq("session.server", server)),
          set("session", null)), writer);
    }
    if (release) copy.put("session", BsonNull.VALUE);
    return CompletableFuture.runAsync(() -> {
      // Only while this server still holds it: after a takeover the other server's data is newer.
      UpdateResult result = users.withDocumentClass(BsonDocument.class)
          .replaceOne(and(eq("uuid", user.getUuid().toString()), eq("session.server", server)), copy);
      if (result.getMatchedCount() == 0) log.warning("Didn't save " + user.getUuid() + ": another server has taken over their data");
    }, writer).exceptionally(e -> {
      log.log(Level.SEVERE, "Couldn't save " + user.getUuid(), e);
      return null;
    });
  }
}
