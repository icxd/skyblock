package net.icxd.dungeons.command.commands.admin;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.mongodb.client.model.Updates;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.user.UserStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * {@code /playerdata} ({@code /pd}): look at and change any player's data, and see the servers
 * sharing it.
 * <ul>
 *   <li>{@code /pd <player>}: who holds their data, their main numbers and their fields</li>
 *   <li>{@code /pd <player> get <path>}: one field, e.g. {@code dungeons.floors.highest}</li>
 *   <li>{@code /pd <player> set <path> <value>}: keeps the field's type. For a player on no server
 *       it changes the database directly; if another server holds them, run it there.</li>
 *   <li>{@code /pd <player> inv}: their items, read-only (live if they're here, else as stored)</li>
 *   <li>{@code /pd <player> save} and {@code /pd <player> handoff}</li>
 *   <li>{@code /pd servers}: every server's type, players and heartbeat</li>
 * </ul>
 * Fields are clickable: documents open, values fill in a set command.
 */
@CommandParameters(description = "View and edit player data", usage = "/playerdata <player> [get|set|inv|save|handoff] | servers",
    aliases = "pd", permission = Rank.STAFF)
public class PlayerDataCommand extends SCommand {
  private static final List<String> ACTIONS = List.of("get", "set", "inv", "save", "handoff");
  private static final Component RULE = Component.text("━".repeat(34), NamedTextColor.DARK_GRAY);
  private static final TextColor LABEL = NamedTextColor.GRAY;

  @Override
  public void run(CommandSource source, String[] args) {
    CommandSender sender = source.getSender();
    if (args.length == 0) {
      usage(sender);
      return;
    }
    if (args[0].equalsIgnoreCase("servers")) {
      async(() -> servers(sender));
      return;
    }
    String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "view";
    if (!action.equals("view") && !ACTIONS.contains(action)) {
      usage(sender);
      return;
    }
    Player online = Bukkit.getPlayerExact(args[0]);
    User user = online == null ? null : User.cached(online.getUniqueId());
    if (user != null && user.isLoaded()) {
      here(sender, online, user, action, args);
    } else {
      String name = args[0];
      async(() -> stored(sender, name, action, args));
    }
  }

  /** A player on this server: their data is in memory. */
  private void here(CommandSender sender, Player player, User user, String action, String[] args) {
    String name = player.getName();
    Document doc = user.getDocument();
    UserStore store = Dungeons.getUserStore();
    if (user.isReleased() && (action.equals("set") || action.equals("save") || action.equals("handoff"))) {
      error(sender, name + "'s data has been handed off; this server doesn't save it any more.");
      return;
    }
    switch (action) {
      case "view" -> overview(sender, name, doc,
          user.isReleased() ? "nobody (handed off from " + store.server() + ")" : store.server() + " (this server)",
          user.isReleased() ? -1 : user.getLoadMillis());
      case "inv" -> {
        PlayerInventory inventory = player.getInventory();
        showItems(sender, name, "live", inventory.getStorageContents(), inventory.getArmorContents(), inventory.getItemInOffHand(),
            StoredInventory.stored(doc, "overflow", 4));
      }
      case "get" -> show(sender, name, doc, path(args));
      case "set" -> {
        if (args.length < 4) {
          error(sender, "Usage: /pd " + name + " set <path> <value>");
          return;
        }
        String path = args[2];
        Document parent = parent(doc, path);
        String key = leaf(path);
        if (parent == null) {
          error(sender, "No field " + path.substring(0, path.lastIndexOf('.')));
          return;
        }
        Object old = parent.get(key);
        Object value;
        try {
          value = parse(old, String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
        } catch (IllegalArgumentException e) {
          error(sender, e.getMessage());
          return;
        }
        parent.put(key, value);
        user.save();
        sender.sendMessage(Component.text("Set ", NamedTextColor.GREEN).append(Component.text(path, NamedTextColor.YELLOW))
            .append(Component.text(" to ", NamedTextColor.GREEN)).append(value(value))
            .append(Component.text(" (was ", LABEL)).append(value(old)).append(Component.text("), saving.", LABEL)));
      }
      case "save" -> {
        user.save();
        sender.sendMessage(Component.text("Saving " + name + "'s data.", NamedTextColor.GREEN));
      }
      case "handoff" -> store.handOff(player).thenRun(() -> sender.sendMessage(
          Component.text("Released " + name + "'s data; another server can load it now. ", NamedTextColor.GREEN)
              .append(Component.text("What changes here from now on isn't saved.", NamedTextColor.RED))));
      default -> usage(sender);
    }
  }

  /** A player on no server or another one: read from the database. Off the main thread. */
  private void stored(CommandSender sender, String name, String action, String[] args) {
    UserStore store = Dungeons.getUserStore();
    Document doc = store.users().find(regex("username", "^" + Pattern.quote(name) + "$", "i")).first();
    if (doc == null) {
      error(sender, "Nobody called " + name + " has played here.");
      return;
    }
    name = doc.getString("username");
    Document session = doc.get("session", Document.class);
    String holder = session == null ? null : session.getString("server");
    switch (action) {
      case "view" -> overview(sender, name, doc, holder, -1);
      case "get" -> show(sender, name, doc, path(args));
      case "inv" -> {
        String who = name;
        // Items are read on the main thread.
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
          if (StoredInventory.isEmpty(doc)) {
            error(sender, "No items stored for " + who + " yet.");
            return;
          }
          showItems(sender, who, holder == null ? "stored" : "stored, " + holder + " has newer", StoredInventory.stored(doc, "inventory", 36),
              StoredInventory.stored(doc, "armor", 4), StoredInventory.stored(doc, "offhand", 1)[0], StoredInventory.stored(doc, "overflow", 4));
        });
      }
      case "set" -> {
        if (holder != null) {
          error(sender, holder + " holds " + name + "'s data; change it there.");
          return;
        }
        if (args.length < 4) {
          error(sender, "Usage: /pd " + name + " set <path> <value>");
          return;
        }
        String path = args[2];
        Document parent = parent(doc, path);
        if (parent == null) {
          error(sender, "No field " + path.substring(0, path.lastIndexOf('.')));
          return;
        }
        Object old = parent.get(leaf(path));
        Object value;
        try {
          value = parse(old, String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
        } catch (IllegalArgumentException e) {
          error(sender, e.getMessage());
          return;
        }
        // Only while no server holds them, so a login can't load the old value and save it back.
        long changed = store.users().updateOne(and(eq("uuid", doc.getString("uuid")), eq("session", null)), Updates.set(path, value))
            .getMatchedCount();
        if (changed == 0) {
          error(sender, name + " just logged in somewhere; try again there.");
          return;
        }
        sender.sendMessage(Component.text("Set ", NamedTextColor.GREEN).append(Component.text(path, NamedTextColor.YELLOW))
            .append(Component.text(" to ", NamedTextColor.GREEN)).append(value(value))
            .append(Component.text(" (was ", LABEL)).append(value(old)).append(Component.text(") in the database.", LABEL)));
      }
      default -> error(sender, name + " isn't on this server" + (holder == null ? "." : "; " + holder + " holds their data."));
    }
  }

  private void overview(CommandSender sender, String name, Document doc, String holder, long loadMillis) {
    Document session = doc.get("session", Document.class);
    Date since = session == null ? null : session.getDate("since");
    Component held = holder == null
        ? Component.text("Not on any server", NamedTextColor.GRAY)
        : Component.text("Held by ", LABEL).append(Component.text(holder, NamedTextColor.GREEN))
            .append(since == null ? Component.empty() : Component.text(" since " + new SimpleDateFormat("HH:mm:ss").format(since), LABEL))
            .append(loadMillis < 0 ? Component.empty() : Component.text(", loaded in " + loadMillis + " ms", LABEL));

    TextComponent.Builder fields = Component.text().append(Component.text(" Fields ", LABEL));
    for (Map.Entry<String, Object> e : doc.entrySet()) {
      if (e.getKey().equals("_id")) continue;
      fields.append(chip(name, e.getKey(), e.getValue())).append(Component.space());
    }

    sender.sendMessage(RULE);
    sender.sendMessage(Component.text(" " + name, NamedTextColor.GOLD, TextDecoration.BOLD)
        .append(Component.text("  " + doc.getString("uuid"), NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false)));
    sender.sendMessage(Component.text(" ").append(held));
    sender.sendMessage(Component.text(" ")
        .append(stat("Rank", doc.get("rank"))).append(stat("Purse", doc.get("coins"))).append(stat("Bank", get(doc, "bank.balance")))
        .append(stat("Bits", doc.get("bits"))).append(stat("Gems", doc.get("gems"))));
    sender.sendMessage(fields.build());
    if (loadMillis >= 0) {
      sender.sendMessage(Component.text(" ").append(button("Items", "/pd " + name + " inv", "Look at their inventory", NamedTextColor.GOLD))
          .append(Component.space()).append(button("Save", "/pd " + name + " save", "Save their data now", NamedTextColor.GREEN))
          .append(Component.space()).append(button("Hand off", "/pd " + name + " handoff",
              "Save and release their data, as before sending them to another server", NamedTextColor.RED))
          .append(Component.space()).append(button("Refresh", "/pd " + name, "Show this again", NamedTextColor.AQUA)));
    }
    sender.sendMessage(RULE);
  }

  /** One field; a document lists its fields. */
  private void show(CommandSender sender, String name, Document doc, String path) {
    Object value = path.isEmpty() ? doc : get(doc, path);
    if (value == null && !path.isEmpty() && (parent(doc, path) == null || !parent(doc, path).containsKey(leaf(path)))) {
      error(sender, "No field " + path);
      return;
    }
    if (!(value instanceof Document sub)) {
      sender.sendMessage(line(name, path, value, 1));
      return;
    }
    TextComponent.Builder title = Component.text().append(Component.text(" " + name, NamedTextColor.GOLD));
    // Breadcrumbs back up the path.
    String at = "";
    title.append(Component.text(" / ", NamedTextColor.DARK_GRAY)).append(link("root", "/pd " + name + " get", "Everything"));
    if (!path.isEmpty()) {
      for (String part : path.split("\\.")) {
        at = at.isEmpty() ? part : at + "." + part;
        title.append(Component.text(" / ", NamedTextColor.DARK_GRAY)).append(link(part, "/pd " + name + " get " + at, at));
      }
    }
    sender.sendMessage(RULE);
    sender.sendMessage(title.build());
    for (Map.Entry<String, Object> e : sub.entrySet()) {
      if (e.getKey().equals("_id")) continue;
      sender.sendMessage(line(name, path.isEmpty() ? e.getKey() : path + "." + e.getKey(), e.getValue(), 2));
    }
    sender.sendMessage(RULE);
  }

  private void servers(CommandSender sender) {
    UserStore store = Dungeons.getUserStore();
    List<Document> servers = store.servers().find().into(new ArrayList<>());
    sender.sendMessage(RULE);
    sender.sendMessage(Component.text(" Servers ", NamedTextColor.GOLD, TextDecoration.BOLD)
        .append(Component.text(servers.size() + " running", LABEL).decoration(TextDecoration.BOLD, false)));
    for (Document s : servers) {
      String id = s.getString("_id");
      Date seen = s.getDate("seen");
      long ago = seen == null ? Long.MAX_VALUE : (System.currentTimeMillis() - seen.getTime()) / 1000;
      boolean alive = ago < 15;
      long holds = store.users().countDocuments(eq("session.server", id));
      sender.sendMessage(Component.text(" ● ", alive ? NamedTextColor.GREEN : NamedTextColor.RED)
          .append(Component.text(id, NamedTextColor.WHITE))
          .append(Component.text(id.equals(store.server()) ? " (this one)" : "", NamedTextColor.DARK_GRAY))
          .append(Component.text("  " + s.get("type"), NamedTextColor.AQUA))
          .append(Component.text("  " + s.get("players") + " online, holds " + holds, LABEL))
          .append(Component.text("  seen " + (seen == null ? "never" : ago + "s ago"), alive ? LABEL : NamedTextColor.RED)
              .hoverEvent(HoverEvent.showText(Component.text("Heartbeat #" + s.get("beat"))))));
    }
    sender.sendMessage(RULE);
  }

  private static void usage(CommandSender sender) {
    sender.sendMessage(RULE);
    sender.sendMessage(Component.text(" /playerdata", NamedTextColor.GOLD, TextDecoration.BOLD)
        .append(Component.text(" (/pd)", LABEL).decoration(TextDecoration.BOLD, false)));
    for (String[] u : new String[][]{
        {"<player>", "their data at a glance"},
        {"<player> get <path>", "one field, e.g. dungeons.floors.highest"},
        {"<player> set <path> <value>", "change a field, keeping its type"},
        {"<player> inv", "their items, read-only"},
        {"<player> save", "save now"},
        {"<player> handoff", "save and release, as for a server switch"},
        {"servers", "the servers sharing player data"}}) {
      sender.sendMessage(Component.text(" /pd " + u[0], NamedTextColor.YELLOW).append(Component.text("  " + u[1], LABEL))
          .clickEvent(ClickEvent.suggestCommand("/pd " + u[0].split(" ")[0] + " ")));
    }
    sender.sendMessage(RULE);
  }

  // Rendering

  private static Component line(String name, String path, Object value, int indent) {
    Component key = Component.text(" ".repeat(indent) + leaf(path), NamedTextColor.YELLOW);
    Component hover = Component.text(path, NamedTextColor.YELLOW).append(Component.newline())
        .append(Component.text(type(value), LABEL)).append(Component.newline())
        .append(Component.text(value instanceof Document ? "Click to open" : "Click to change", NamedTextColor.GREEN));
    ClickEvent click = value instanceof Document
        ? ClickEvent.runCommand("/pd " + name + " get " + path)
        : ClickEvent.suggestCommand("/pd " + name + " set " + path + " " + raw(value));
    return key.append(Component.text(": ", NamedTextColor.DARK_GRAY)).append(value(value))
        .hoverEvent(HoverEvent.showText(hover)).clickEvent(click);
  }

  private static Component chip(String name, String key, Object value) {
    return Component.text("[" + key + "]", value instanceof Document ? NamedTextColor.AQUA : NamedTextColor.YELLOW)
        .hoverEvent(HoverEvent.showText(Component.text(key, NamedTextColor.YELLOW).append(Component.newline()).append(value(value))))
        .clickEvent(ClickEvent.runCommand("/pd " + name + " get " + key));
  }

  private static Component stat(String label, Object value) {
    return Component.text(label + " ", LABEL).append(value(value)).append(Component.text("   "));
  }

  private static Component button(String label, String command, String hover, TextColor color) {
    return Component.text("[" + label + "]", color, TextDecoration.BOLD)
        .hoverEvent(HoverEvent.showText(Component.text(hover, LABEL))).clickEvent(ClickEvent.runCommand(command));
  }

  private static Component link(String label, String command, String hover) {
    return Component.text(label, NamedTextColor.AQUA).hoverEvent(HoverEvent.showText(Component.text(hover, LABEL)))
        .clickEvent(ClickEvent.runCommand(command));
  }

  private static Component value(Object value) {
    if (value == null) return Component.text("null", NamedTextColor.DARK_GRAY);
    if (value instanceof Binary b) return Component.text("<" + b.length() + " bytes>", NamedTextColor.DARK_AQUA);
    if (value instanceof byte[] b) return Component.text("<" + b.length + " bytes>", NamedTextColor.DARK_AQUA);
    if (value instanceof String s) return Component.text("\"" + s + "\"", NamedTextColor.GREEN);
    if (value instanceof Boolean b) return Component.text(b.toString(), b ? NamedTextColor.GREEN : NamedTextColor.RED);
    if (value instanceof Number n) return Component.text(NumberFormat.getInstance(Locale.US).format(n), NamedTextColor.GOLD);
    if (value instanceof Date d) return Component.text(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d), NamedTextColor.LIGHT_PURPLE);
    if (value instanceof Document d) return Component.text("{" + d.size() + " fields}", NamedTextColor.AQUA);
    if (value instanceof List<?> l) {
      TextComponent.Builder items = Component.text();
      for (int i = 0; i < Math.min(l.size(), 10); i++) items.append(Component.newline()).append(value(l.get(i)));
      if (l.size() > 10) items.append(Component.newline()).append(Component.text("... " + (l.size() - 10) + " more", LABEL));
      return Component.text("[" + l.size() + " items]", NamedTextColor.AQUA).hoverEvent(HoverEvent.showText(items.build()));
    }
    return Component.text(value.toString(), NamedTextColor.WHITE);
  }

  private static String type(Object value) {
    return switch (value) {
      case null -> "nothing";
      case Integer i -> "a whole number";
      case Long l -> "a whole number (long)";
      case Double d -> "a number";
      case Boolean b -> "true or false";
      case String s -> "text";
      case Date d -> "a date (milliseconds since 1970)";
      case Document d -> "a document";
      case List<?> l -> "a list";
      default -> "a " + value.getClass().getSimpleName();
    };
  }

  /** How a value is typed back into a set command. */
  private static String raw(Object value) {
    return value == null ? "" : value instanceof Date d ? String.valueOf(d.getTime()) : value.toString();
  }

  // Paths and values

  private static String path(String[] args) {
    return args.length > 2 ? args[2] : "";
  }

  private static String leaf(String path) {
    return path.substring(path.lastIndexOf('.') + 1);
  }

  /** The document holding the last part of {@code path}, or null if a part on the way isn't a document. */
  private static Document parent(Document doc, String path) {
    int dot = path.lastIndexOf('.');
    if (dot < 0) return doc;
    Object at = get(doc, path.substring(0, dot));
    return at instanceof Document d ? d : null;
  }

  private static Object get(Document doc, String path) {
    Object at = doc;
    for (String part : path.split("\\.")) {
      if (!(at instanceof Document d)) return null;
      at = d.get(part);
    }
    return at;
  }

  /** {@code raw} as the same type as {@code old}; for a new field, whatever it looks like. */
  static Object parse(Object old, String raw) {
    try {
      if (old instanceof Integer) return Integer.parseInt(raw);
      if (old instanceof Long) return Long.parseLong(raw);
      if (old instanceof Double) return Double.parseDouble(raw);
      if (old instanceof Boolean) {
        if (!raw.equals("true") && !raw.equals("false")) throw new IllegalArgumentException("That's a true/false field.");
        return Boolean.parseBoolean(raw);
      }
      if (old instanceof Date) return new Date(Long.parseLong(raw));
      if (old instanceof Document) return Document.parse(raw);
      if (old instanceof List) throw new IllegalArgumentException("Lists can't be set from here.");
      if (old instanceof String) return raw;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("That field holds " + type(old) + "; \"" + raw + "\" isn't one.");
    } catch (org.bson.json.JsonParseException e) {
      throw new IllegalArgumentException("That field holds a document; give it as JSON.");
    }
    if (raw.equals("true") || raw.equals("false")) return Boolean.parseBoolean(raw);
    try {
      long n = Long.parseLong(raw);
      return n == (int) n ? (Object) (int) n : (Object) n;
    } catch (NumberFormatException ignored) {
    }
    try {
      return Double.parseDouble(raw);
    } catch (NumberFormatException ignored) {
    }
    return raw;
  }

  /** Holder of the read-only item views; InventorySyncListener cancels clicks in them. */
  public static final class ItemsView implements InventoryHolder {
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }

  /**
   * A chest laid out like the player's inventory: the main rows, then the hotbar, then labels over
   * helmet, chestplate, leggings, boots, off-hand and anything waiting in overflow. Main thread.
   *
   * @param armor boots first, as Bukkit orders it
   */
  private static void showItems(CommandSender sender, String name, String what, ItemStack[] main, ItemStack[] armor, ItemStack offhand,
                                ItemStack[] overflow) {
    if (!(sender instanceof Player viewer)) {
      error(sender, "Only players can look at inventories.");
      return;
    }
    ItemsView holder = new ItemsView();
    Inventory view = Bukkit.createInventory(holder, 54, Component.text(name + "'s items (" + what + ")"));
    holder.inventory = view;
    for (int i = 9; i < 36; i++) view.setItem(i - 9, main[i]);
    for (int i = 0; i < 9; i++) view.setItem(27 + i, main[i]);
    String[] labels = {"Helmet", "Chestplate", "Leggings", "Boots", "Off-hand", "Overflow", "Overflow", "Overflow", "Overflow"};
    for (int i = 0; i < labels.length; i++) view.setItem(36 + i, pane(labels[i]));
    for (int i = 0; i < 4; i++) view.setItem(45 + i, armor[3 - i]);
    view.setItem(49, offhand);
    for (int i = 0; i < 4; i++) view.setItem(50 + i, overflow[i]);
    viewer.openInventory(view);
  }

  private static ItemStack pane(String label) {
    ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    pane.editMeta(meta -> meta.displayName(Component.text(label + " \u2193", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
    return pane;
  }

  private static void error(CommandSender sender, String message) {
    sender.sendMessage(Component.text(message, NamedTextColor.RED));
  }

  private static void async(Runnable task) {
    Bukkit.getScheduler().runTaskAsynchronously(Dungeons.getInstance(), task);
  }

  @Override
  public List<String> tabCompleters(CommandSender sender, String alias, String[] args) {
    List<String> options = new ArrayList<>();
    if (args.length == 1) {
      options.add("servers");
      for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
    } else if (args.length == 2 && !args[0].equalsIgnoreCase("servers")) {
      options.addAll(ACTIONS);
    } else if (args.length >= 3 && (args[1].equalsIgnoreCase("get") || args[1].equalsIgnoreCase("set"))) {
      Player p = Bukkit.getPlayerExact(args[0]);
      User user = p == null ? null : User.cached(p.getUniqueId());
      if (user == null || !user.isLoaded()) return List.of();
      if (args.length == 3) {
        paths(user.getDocument(), "", options);
      } else if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
        Object value = get(user.getDocument(), args[2]);
        if (value != null && !(value instanceof Document)) options.add(raw(value));
      }
    }
    String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
    return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
  }

  private static void paths(Document doc, String prefix, List<String> out) {
    for (Map.Entry<String, Object> e : doc.entrySet()) {
      if (e.getKey().equals("_id")) continue;
      String path = prefix + e.getKey();
      out.add(path);
      if (e.getValue() instanceof Document d) paths(d, path + ".", out);
    }
  }
}
