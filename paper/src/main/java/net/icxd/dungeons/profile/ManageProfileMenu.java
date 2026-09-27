package net.icxd.dungeons.profile;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.event.GUIOpenEvent;
import net.icxd.dungeons.gui.RefreshingGUI;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * "Profile: Apple": switch to it or delete it, as Hypixel's. The cooldown counts down in the menu.
 * The profile you're on can be looked at here, but not switched to or deleted.
 */
final class ManageProfileMenu extends Menu implements RefreshingGUI {
    private static final int SWITCH = 11;
    private static final int DELETE = 15;
    private static final int BACK = 31;

    private final String id;

    ManageProfileMenu(Player viewer, String id) {
        super("Profile: " + nameOf(viewer, id), Size.FOUR, viewer);
        this.id = id;
    }

    private static String nameOf(Player viewer, String id) {
        User user = User.ifLoaded(viewer.getUniqueId());
        Document profile = user == null ? null : Profiles.profiles(user.getDocument()).get(id, Document.class);
        return profile == null ? "?" : profile.getString(Profiles.NAME);
    }

    @Override
    public long refreshRate() {
        return 20;
    }

    @Override
    void items(User user) {
        fill(filler());
        set(goBack(BACK, "Profile Management", () -> new ProfileManagementMenu(viewer)));
        if (!(Profiles.profiles(user.getDocument()).get(id) instanceof Document profile)) return;
        String name = profile.getString(Profiles.NAME);
        boolean selected = id.equals(user.profileId());
        long cooldown = Profiles.cooldownLeft(user.getDocument(), System.currentTimeMillis());

        // Hypixel's says it teleports you to your island on the other profile.
        List<String> lore = new ArrayList<>(List.of("&7Teleports you to spawn and loads", "&7your inventory, skills, collections",
                "&7and more from another profile.", ""));
        String reason = ProfileActions.cantChange(viewer, user);
        if (selected) {
            lore.add("&aYou are playing on this profile!");
        } else {
            lore.add("&7Current: &e" + user.profileName());
            lore.add("&7Switching to: &a" + name);
            lore.add("");
            if (reason != null) {
                lore.add(reason);
            } else if (cooldown > 0) {
                lore.add("&7On cooldown!");
                lore.add("&7Try again in &e" + Profiles.cooldown(cooldown) + "&7!");
            } else {
                lore.add("&eClick to select profile!");
            }
        }
        set(button(SWITCH, item(Material.GRASS_BLOCK, "&aSwitch to Profile", lore), () -> ProfileActions.switchTo(viewer, id)));

        List<String> delete = new ArrayList<>(List.of("&7Clear this profile slot by", "&7deleting this profile forever.", "", "&cWarning!",
                "&fYou cannot revert this action!", ""));
        if (selected) {
            delete.add("&cYou can't delete the profile");
            delete.add("&cyou are playing on!");
        } else {
            delete.add(cooldown > 0 ? "&cAction on cooldown!" : "&eClick to continue!");
        }
        set(button(DELETE, item(Material.RED_STAINED_GLASS, "&cDelete Profile", delete), () -> {
            User now = User.ifLoaded(viewer.getUniqueId());
            if (now == null || id.equals(now.profileId())) return;
            if (Profiles.cooldownLeft(now.getDocument(), System.currentTimeMillis()) > 0) {
                viewer.closeInventory();
                viewer.sendMessage(Text.line("&cProfile deletion is currently on cooldown!"));
                return;
            }
            new DeleteProfileMenu(viewer, id, DeleteProfileMenu.COUNTDOWN).open(viewer);
        }));
    }

    /** "Delete profile? (5)" down to "(1)", a second each, then "Delete profile?" to confirm, as Hypixel's. */
    static final class DeleteProfileMenu extends Menu {
        static final int COUNTDOWN = 5;
        private static final int CONFIRM = 11;
        private static final int CANCEL = 15;

        private final String id;
        private final int seconds;

        DeleteProfileMenu(Player viewer, String id, int seconds) {
            super(seconds > 0 ? "Delete profile? (" + seconds + ")" : "Delete profile?", Size.THREE, viewer);
            this.id = id;
            this.seconds = seconds;
        }

        @Override
        void items(User user) {
            // No glass in this one; the empty slots still take nothing.
            fill(ItemStack.empty());
            set(button(CANCEL, item(Material.RED_TERRACOTTA, "&aCancel", "&7Return to previous menu."),
                    () -> new ManageProfileMenu(viewer, id).open(viewer)));
            if (!(Profiles.profiles(user.getDocument()).get(id) instanceof Document profile)) return;
            Profiles.Entry entry = new Profiles.Entry(id, profile);
            List<String> lore = new ArrayList<>();
            lore.add("");
            // As Hypixel's; what it says for a special mode is unknown.
            lore.add("&7Profile: &e" + entry.name());
            lore.add("");
            lore.addAll(summary(user, entry));
            lore.add("");
            lore.add("&c&lTHIS ACTION IS PERMANENT");
            lore.add("&fThe profile will be &cdeleted&f!");
            lore.add("");
            lore.add(seconds > 0 ? "&6Make sure to read!!!" : "&eClick to confirm!");
            set(button(CONFIRM, item(seconds > 0 ? Material.RED_STAINED_GLASS : Material.BARRIER, "&cDelete Profile", lore), () -> {
                if (seconds == 0) ProfileActions.delete(viewer, id);
            }));
        }

        @Override
        public void afterOpen(GUIOpenEvent event) {
            if (seconds == 0) return;
            Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
                if (viewer.isOnline() && GUI_MAP.get(viewer.getUniqueId()) == this) new DeleteProfileMenu(viewer, id, seconds - 1).open(viewer);
            }, 20);
        }
    }
}
