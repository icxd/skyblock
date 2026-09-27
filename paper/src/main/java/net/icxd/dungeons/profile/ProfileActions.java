package net.icxd.dungeons.profile;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.SkyBlockMenuItem;
import net.icxd.dungeons.scoreboard.ScoreboardRunnable;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Making, switching and deleting profiles. Hypixel sends you to the profile's island on another
 * server; there are no islands here, so it happens in place, on this server: your items are swapped,
 * your session (health, mana, cooldowns) starts over and you're put at spawn. It's saved like any
 * other change, and the profile you're on travels with the rest of your data. Main thread.
 */
public final class ProfileActions {
    private static final Random RANDOM = new Random();

    private ProfileActions() {
    }

    /** "You are playing on profile: Kiwi (Sandbox)" and its id, as Hypixel says when you join a server. */
    public static void announce(Player player, User user) {
        tell(player, user, "&aYou are playing on profile: &e");
    }

    private static void tell(Player player, User user, String text) {
        player.sendMessage(Text.line(text + user.profileName() + user.mode().suffix()));
        player.sendMessage(Text.line("&8Profile ID: " + user.profileId()));
    }

    /** Why they can't make or switch profiles here and now; null if they can. */
    static String cantChange(Player player, User user) {
        if (Dungeons.getSkyBlockServer().getServerType() == ServerType.DUNGEONS) return "&cYou can't change profiles in a dungeon!";
        // Handed off (their items are frozen), dead, or not loaded yet.
        if (user == null || !user.isLoaded() || InventorySyncListener.frozen(player) || player.isDead()) return "&cYou can't change profiles right now.";
        return null;
    }

    /** A random name none of their profiles has; null if they've used them all. */
    static String newName(User user) {
        return Profiles.pickName(Profiles.names(user.getDocument()), RANDOM);
    }

    public static void switchTo(Player player, String id) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || !(Profiles.profiles(user.getDocument()).get(id) instanceof Document target) || id.equals(user.profileId())) return;
        if (!refuse(player, user, "switching")) return;
        say(player, "&7Switching to profile " + target.getString(Profiles.NAME) + "...");
        change(player, user, id);
    }

    public static void create(Player player, ProfileMode mode, String name) {
        User user = User.ifLoaded(player.getUniqueId());
        if (!refuse(player, user, "creation")) return;
        Document doc = user.getDocument();
        if (Profiles.ordered(doc).size() >= Profiles.slots(user.getRank())) {
            player.closeInventory();
            say(player, "&cYou don't have a free profile slot!");
            return;
        }
        // Taken since the menu showed it (another profile made in between): another one.
        List<String> used = Profiles.names(doc);
        if (name == null || used.stream().anyMatch(name::equalsIgnoreCase)) name = Profiles.pickName(used, RANDOM);
        if (name == null) {
            player.closeInventory();
            say(player, "&cThere are no profile names left!");
            return;
        }
        say(player, "&7Making a little bit of room for your new profile...");
        String id = UUID.randomUUID().toString();
        Profiles.create(doc, id, name, mode, Dungeons.getUserCollection().profileDefaults(), System.currentTimeMillis());
        if (!change(player, user, id)) Profiles.profiles(doc).remove(id);
    }

    public static void delete(Player player, String id) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) {
            player.closeInventory();
            say(player, "&cYou can't change profiles right now.");
            return;
        }
        Document doc = user.getDocument();
        if (id.equals(user.profileId()) || !(Profiles.profiles(doc).get(id) instanceof Document profile)) return;
        player.closeInventory();
        if (Profiles.cooldownLeft(doc, System.currentTimeMillis()) > 0) {
            say(player, "&cProfile deletion is currently on cooldown!");
            return;
        }
        Profiles.delete(doc, id, System.currentTimeMillis());
        doc.put(Profiles.LAST_ACTION, new Date());
        user.save();
        say(player, "&aDone! Your &e" + profile.getString(Profiles.NAME) + " &aprofile was deleted!");
        Dungeons.getInstance().getLogger().info(player.getName() + " deleted their profile " + profile.getString(Profiles.NAME) + " (" + id + ")");
    }

    /** False, having told them why, if they can't make or switch profiles now. */
    private static boolean refuse(Player player, User user, String action) {
        String reason = cantChange(player, user);
        if (reason == null && Profiles.cooldownLeft(user.getDocument(), System.currentTimeMillis()) > 0) {
            reason = "&cProfile " + action + " is currently on cooldown!";
        }
        if (reason == null) return true;
        player.closeInventory();
        say(player, reason);
        return false;
    }

    /** Onto profile {@code id}, here: their items, a fresh session, spawn. */
    private static boolean change(Player player, User user, String id) {
        try {
            Dungeons.getUserStore().switchProfile(player, user, id);
        } catch (StoredInventory.NewerDataException | RuntimeException e) {
            Dungeons.getInstance().getLogger().log(Level.SEVERE, "Couldn't move " + player.getName() + " onto profile " + id, e);
            say(player, e instanceof StoredInventory.NewerDataException
                    ? "&cThat profile's items were saved by a newer version of Minecraft than this server runs."
                    : "&cCouldn't change your profile, try again later.");
            return false;
        }
        user.getDocument().put(Profiles.LAST_ACTION, new Date());
        // Items made before an update to how items look are brought up to date, as when they join.
        ItemBuilder.refreshInventory(player);
        // The SkyBlock Menu isn't one of the profile's items.
        SkyBlockMenuItem.give(player);
        PlayerSession.end(player.getUniqueId());
        ScoreboardRunnable.forget(player.getUniqueId());
        PlayerHealth.sync(player);
        // Which commands they may use depends on the profile's mode (the Sandbox tools).
        player.updateCommands();
        player.setFallDistance(0);
        player.setFireTicks(0);
        player.teleport(Dungeons.getSkyBlockServer().getMainWorld().getSpawnLocation());
        user.save();
        tell(player, user, "&aYour profile was changed to: &e");
        return true;
    }

    private static void say(Player player, String text) {
        player.sendMessage(Text.line(text));
    }
}
