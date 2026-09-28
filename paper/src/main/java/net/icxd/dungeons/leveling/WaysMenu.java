package net.icxd.dungeons.leveling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.LevelingData.Category;
import net.icxd.dungeons.leveling.LevelingData.Task;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Ways to Level Up, as recorded (01:46.1): the nine categories of XP tasks with what they've earned of each, opening
 * its tasks ({@link TasksMenu}), and the tasks they viewed last. The progress bars switch is shown as recorded, but
 * what it shows is UNKNOWN (never recorded on), so it does nothing yet. Main thread.
 */
public final class WaysMenu extends GUI {
    public static final String TITLE = "Ways to Level Up";
    /** Where the categories go, in the data's (the recorded) order. */
    static final int[] CATEGORIES = {11, 12, 13, 14, 15, 20, 21, 23, 24};
    static final int RECENT = 37;
    static final int[] RECENTS = {38, 39, 40, 41, 42};
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int BARS = 50;

    /**
     * The tasks (or categories) each player viewed last, newest first, while they're on this server: UNKNOWN whether
     * Hypixel keeps them longer.
     */
    private static final Map<UUID, Deque<String>> recent = new ConcurrentHashMap<>();

    private final Player viewer;

    public WaysMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        LevelingView view = LevelingView.of(viewer, user);
        List<String> viewed = viewed(viewer.getUniqueId());
        Map<Integer, Icon> icons = icons(view, viewed);
        List<Category> categories = view.data().categories();
        for (Map.Entry<Integer, Icon> entry : icons.entrySet()) {
            int slot = entry.getKey();
            Runnable action = null;
            for (int i = 0; i < CATEGORIES.length && i < categories.size(); i++) {
                Category category = categories.get(i);
                if (CATEGORIES[i] == slot) action = () -> new TasksMenu(viewer, category.id(), null).open(viewer);
            }
            for (int i = 0; i < RECENTS.length && i < viewed.size(); i++) {
                String id = viewed.get(i);
                if (RECENTS[i] == slot) action = () -> TasksMenu.openViewed(viewer, id);
            }
            if (slot == BACK) action = () -> new LevelingMenu(viewer).open(viewer);
            if (action == null) set(slot, entry.getValue().stack());
            else set(GUIClickableItem.button(slot, entry.getValue().stack(), viewer, action));
        }
        set(GUIClickableItem.close(CLOSE));
    }

    // Recently Viewed

    /** Someone viewed a category's or a task's list: it goes first in their Recently Viewed. */
    static void viewed(UUID player, String id) {
        Deque<String> list = recent.computeIfAbsent(player, k -> new ArrayDeque<>());
        synchronized (list) {
            list.remove(id);
            list.addFirst(id);
            while (list.size() > RECENTS.length) list.removeLast();
        }
    }

    static List<String> viewed(UUID player) {
        Deque<String> list = recent.get(player);
        if (list == null) return List.of();
        synchronized (list) {
            return List.copyOf(list);
        }
    }

    static void forget(UUID player) {
        recent.remove(player);
    }

    // What each slot shows

    /** Every slot but the glass and Close, by slot. */
    static Map<Integer, Icon> icons(LevelingView view, List<String> viewed) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        List<Category> categories = view.data().categories();
        for (int i = 0; i < CATEGORIES.length && i < categories.size(); i++) icons.put(CATEGORIES[i], category(categories.get(i), view, true));
        icons.put(RECENT, new Icon(Material.OAK_SIGN, "&bRecently Viewed ➜", "&7When you view XP Tasks, they will",
                "&7show up here for your convenience!"));
        for (int i = 0; i < RECENTS.length; i++) {
            Icon shown = i < viewed.size() ? TasksMenu.viewedIcon(view, viewed.get(i)) : null;
            icons.put(RECENTS[i], shown != null ? shown : new Icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&bRecently Viewed #" + (i + 1)));
        }
        icons.put(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Leveling"));
        icons.put(BARS, new Icon(Material.GRAY_DYE, "&aShow Progress Bars", "&7Toggle whether to display more", "&7detailed task progress in each",
                "&7category.", "", "&eClick to show!"));
        return icons;
    }

    /**
     * A category's item: how many tasks, what it is, its tasks, and what they've earned of what it's worth; with
     * {@code click}, "Click to view tasks!" (its task list's top has none, as the wiki's).
     */
    static Icon category(Category category, LevelingView view, boolean click) {
        List<String> lore = new ArrayList<>();
        int tasks = category.tasks().size();
        lore.add("&8" + tasks + (tasks == 1 ? " Task" : " Tasks"));
        lore.add("");
        lore.addAll(category.description());
        lore.add("");
        for (Task task : category.tasks()) lore.add("&e▶ " + task.name());
        lore.add("");
        lore.addAll(progress("Progress to Complete Category", view.xp().of(category), category.max()));
        if (click) lore.addAll(List.of("", "&eClick to view tasks!"));
        return category.look().icon(category.name(), lore);
    }

    /** "Progress to Complete Category: &620.3%" and its gold bar, "4,031/19,810 XP". */
    static List<String> progress(String label, int xp, int max) {
        double fraction = max == 0 ? 0 : Math.min(1, (double) xp / max);
        return List.of("&7" + label + ": &6" + LevelingText.percent(fraction) + "%",
                LevelingText.strip("&6", fraction) + " &e" + Text.number(xp) + "&6/&e" + Text.number(max) + " XP");
    }
}
