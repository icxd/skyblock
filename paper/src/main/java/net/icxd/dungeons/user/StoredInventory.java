package net.icxd.dungeons.user;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

/**
 * A player's inventory, armor and off-hand, kept in their user document under {@code storage} so
 * they follow the player between servers. The ender chest is separate (it isn't vanilla's).
 *
 * <p>Each slot is its own {@link ItemStack#serializeAsBytes()} blob (gzipped NBT with the
 * Minecraft data version, so Paper upgrades it after a Minecraft update), or null when empty. A
 * slot that can't be read back is moved to {@code storage.unreadable} with its bytes instead of
 * being lost, and the rest of the inventory still loads.
 *
 * <p>The inventory is restored when the player joins, overwriting what this server's own player
 * file had, and saved with the rest of their data. It's only saved once this server has restored
 * it, so a failed restore can't overwrite the stored copy with a stale local one.
 */
public final class StoredInventory {
  static final String STORAGE = "storage";
  static final String INVENTORY = "inventory";
  static final String ARMOR = "armor";
  static final String OFFHAND = "offhand";
  static final String HELD_SLOT = "heldSlot";
  /** Items that didn't fit when they were rescued (see {@link #rescueLooseItems}); given back on the next join. */
  static final String OVERFLOW = "overflow";
  static final String UNREADABLE = "unreadable";
  static final String DATA_VERSION = "dataVersion";

  /** Containers whose slots hold the player's own items only until it closes, all but the last slot (the result). */
  private static final Set<InventoryType> INPUTS_AND_RESULT = EnumSet.of(InventoryType.ANVIL, InventoryType.SMITHING,
      InventoryType.SMITHING_NEW, InventoryType.GRINDSTONE, InventoryType.STONECUTTER, InventoryType.LOOM,
      InventoryType.CARTOGRAPHY, InventoryType.MERCHANT);
  /** Same, without a result slot. */
  private static final Set<InventoryType> INPUTS_ONLY = EnumSet.of(InventoryType.ENCHANTING, InventoryType.BEACON);

  private StoredInventory() {
  }

  /** The data is newer than this server's Minecraft version understands. */
  public static final class NewerDataException extends Exception {
    NewerDataException(int stored, int current) {
      super("stored with data version " + stored + ", this server is on " + current);
    }
  }

  /** Nothing has been stored for this player yet. */
  public static boolean isEmpty(Document doc) {
    return storage(doc).get(INVENTORY) == null;
  }

  /**
   * Puts the stored inventory on the player. Main thread.
   *
   * @return false if nothing was stored yet: they keep what this server had, and that's saved from now on
   */
  static boolean restore(Player player, Document doc, Logger log) throws NewerDataException {
    Document storage = storage(doc);
    if (storage.get(INVENTORY) == null) return false;
    int stored = storage.getInteger(DATA_VERSION, 0);
    int current = dataVersion();
    if (stored > current) throw new NewerDataException(stored, current);

    PlayerInventory inventory = player.getInventory();
    inventory.setStorageContents(decode(player, storage, INVENTORY, 36, log));
    inventory.setArmorContents(decode(player, storage, ARMOR, 4, log));
    inventory.setItemInOffHand(decode(player, storage, OFFHAND, 1, log)[0]);
    Integer held = storage.getInteger(HELD_SLOT);
    if (held != null && held >= 0 && held < 9) inventory.setHeldItemSlot(held);
    player.setItemOnCursor(null);

    // Anything rescued last time that didn't fit then.
    List<?> overflow = list(storage, OVERFLOW);
    if (!overflow.isEmpty()) {
      List<Binary> left = new ArrayList<>();
      for (int i = 0; i < overflow.size(); i++) {
        ItemStack item = read(player, overflow.get(i), OVERFLOW, i, storage, log);
        if (item == null) continue;
        for (ItemStack rest : inventory.addItem(item).values()) left.add(write(rest));
      }
      storage.put(OVERFLOW, left);
    }
    return true;
  }

  /**
   * Writes the player's inventory, armor and off-hand into their document. Main thread. Throws if
   * an item can't be saved, leaving the stored copy as it was.
   */
  static void capture(Player player, Document doc) {
    PlayerInventory inventory = player.getInventory();
    List<Binary> main = encode(inventory.getStorageContents());
    List<Binary> armor = encode(inventory.getArmorContents());
    List<Binary> offhand = encode(new ItemStack[]{inventory.getItemInOffHand()});
    Document storage = storage(doc);
    storage.put(INVENTORY, main);
    storage.put(ARMOR, armor);
    storage.put(OFFHAND, offhand);
    storage.put(HELD_SLOT, inventory.getHeldItemSlot());
    storage.put(DATA_VERSION, dataVersion());
  }

  /**
   * Moves the items vanilla would drop when the player disconnects into their inventory: the one on
   * the cursor, a crafting grid's, and the inputs of an anvil, stonecutter and the like. What
   * doesn't fit goes to {@code storage.overflow}. Main thread.
   */
  static void rescueLooseItems(Player player, Document doc) {
    List<ItemStack> loose = new ArrayList<>();
    ItemStack cursor = player.getItemOnCursor();
    if (!cursor.isEmpty()) {
      if (!isNotSaved(cursor)) loose.add(cursor.clone());
      player.setItemOnCursor(null);
    }
    Inventory top = player.getOpenInventory().getTopInventory();
    if (top instanceof CraftingInventory crafting) {
      // The player's own 2x2 grid or a crafting table; never the result, which is only a preview.
      ItemStack[] matrix = crafting.getMatrix();
      for (ItemStack item : matrix) if (item != null && !item.isEmpty()) loose.add(item.clone());
      crafting.setMatrix(new ItemStack[matrix.length]);
    } else if (INPUTS_AND_RESULT.contains(top.getType()) || INPUTS_ONLY.contains(top.getType())) {
      int inputs = INPUTS_ONLY.contains(top.getType()) ? top.getSize() : top.getSize() - 1;
      for (int slot = 0; slot < inputs; slot++) {
        ItemStack item = top.getItem(slot);
        if (item == null || item.isEmpty()) continue;
        loose.add(item.clone());
        top.setItem(slot, null);
      }
    }
    if (loose.isEmpty()) return;

    List<Binary> overflow = new ArrayList<>();
    for (ItemStack rest : player.getInventory().addItem(loose.toArray(new ItemStack[0])).values()) overflow.add(write(rest));
    if (overflow.isEmpty()) return;
    Document storage = storage(doc);
    List<Object> kept = new ArrayList<>(list(storage, OVERFLOW));
    kept.addAll(overflow);
    storage.put(OVERFLOW, kept);
  }

  /** The stored items of one section, e.g. for looking at a player's inventory while they're offline. Main thread. */
  public static ItemStack[] stored(Document doc, String section, int size) {
    ItemStack[] out = new ItemStack[size];
    List<?> list = list(storage(doc), section);
    for (int i = 0; i < Math.min(size, list.size()); i++) {
      byte[] bytes = bytes(list.get(i));
      if (bytes == null) continue;
      try {
        out[i] = ItemStack.deserializeBytes(bytes);
      } catch (RuntimeException ignored) {
        // Shown as empty; restore() keeps its bytes.
      }
    }
    return out;
  }

  private static ItemStack[] decode(Player player, Document storage, String section, int size, Logger log) {
    ItemStack[] out = new ItemStack[size];
    List<?> list = list(storage, section);
    for (int i = 0; i < Math.min(size, list.size()); i++) out[i] = read(player, list.get(i), section, i, storage, log);
    return out;
  }

  /** One slot's item, or null. A slot that can't be read is kept in {@code storage.unreadable}. */
  private static ItemStack read(Player player, Object entry, String section, int slot, Document storage, Logger log) {
    byte[] bytes = bytes(entry);
    if (bytes == null) return null;
    try {
      return ItemStack.deserializeBytes(bytes);
    } catch (RuntimeException e) {
      log.severe("Couldn't read " + player.getName() + "'s " + section + " slot " + slot + " (" + e + "); kept in storage."
          + UNREADABLE);
      List<Object> unreadable = new ArrayList<>(list(storage, UNREADABLE));
      unreadable.add(new Document("section", section).append("slot", slot).append("bytes", new Binary(bytes))
          .append("error", String.valueOf(e)).append("at", new java.util.Date()));
      storage.put(UNREADABLE, unreadable);
      return null;
    }
  }

  private static List<Binary> encode(ItemStack[] items) {
    List<Binary> out = new ArrayList<>(items.length);
    for (ItemStack item : items) out.add(item == null || item.isEmpty() || isNotSaved(item) ? null : write(item));
    return out;
  }

  /** Hypixel's {@code dontSaveToProfile}: items that belong to where you are (a dungeon's map), not to you. */
  private static final NamespacedKey NOT_SAVED = new NamespacedKey("skyblock", "dont_save_to_profile");

  /** Keeps the item out of the stored inventory: it stays on this server and is gone when they come back. */
  public static void markNotSaved(ItemStack item) {
    item.editPersistentDataContainer(data -> data.set(NOT_SAVED, PersistentDataType.BOOLEAN, true));
  }

  public static boolean isNotSaved(ItemStack item) {
    return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(NOT_SAVED);
  }

  private static Binary write(ItemStack item) {
    return new Binary(item.serializeAsBytes());
  }

  /** A stored slot's bytes: byte[] until the document has been through the database, then Binary. */
  private static byte[] bytes(Object entry) {
    if (entry instanceof Binary binary) return binary.getData();
    if (entry instanceof byte[] raw) return raw;
    return null;
  }

  /** A list field; empty if it isn't one. Not Document.getList, which fails on the nulls of empty slots. */
  private static List<?> list(Document doc, String key) {
    return doc.get(key) instanceof List<?> list ? list : List.of();
  }

  static Document storage(Document doc) {
    Document storage = doc.get(STORAGE, Document.class);
    if (storage == null) {
      storage = new Document();
      doc.put(STORAGE, storage);
    }
    return storage;
  }

  @SuppressWarnings("deprecation")
  private static int dataVersion() {
    return Bukkit.getUnsafe().getDataVersion();
  }
}
