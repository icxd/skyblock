package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.LevelingData.Category;
import net.icxd.dungeons.leveling.LevelingData.Step;
import net.icxd.dungeons.leveling.LevelingData.Task;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.SkillsMenu;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * A category's XP tasks ("Tasks ➜ Core"), or a task's parts ("Core ➜ Bank Upgrades"), with what they've earned of
 * each. Never recorded: it's the wiki's copy of Hypixel's (SkyBlock Levels/UI/Leveling, older than the recorded
 * categories): a border of glass in the category's colour, the category (or task) at the top, the tasks in the
 * middle, Go Back, Close and (for a category's own tasks) Sort at the bottom. The tasks the plugin can't count (a
 * Museum, Slayers...) show none earned. Skill Related's 28 are one list here; the wiki's older menu split them by
 * skill (Mining Tasks, Farming Tasks...), UNKNOWN whether Hypixel still does. Main thread.
 */
public final class TasksMenu extends GUI {
    /** How tasks can be sorted, in the Sort item's order. */
    enum Sort {
        UNLOCKED("Unlocked"), EARNED("Earned XP"), AVAILABLE("Available XP"), A_TO_Z("A to Z"), Z_TO_A("Z to A");

        final String label;

        Sort(String label) {
            this.label = label;
        }

        Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    static final int TOP = 4;
    /** Most tasks one list shows (28, in rows of seven from the second row; no category has more). */
    static final int MOST = 28;

    /** Each player's sort, while they're on this server (UNKNOWN whether Hypixel keeps it). */
    private static final Map<UUID, Sort> sorts = new ConcurrentHashMap<>();

    private final Player viewer;
    private final String category;
    /** The task whose parts these are; null for the category's tasks. */
    private final String task;

    public TasksMenu(Player viewer, String category, String task) {
        super(title(SkyBlockLevels.data(), category, task), rows(size(SkyBlockLevels.data(), category, task)) * 9);
        this.viewer = viewer;
        this.category = category;
        this.task = task;
    }

    /** Opens what someone viewed before: a category ("core") or a task ("bank_upgrades"). */
    static void openViewed(Player viewer, String id) {
        LevelingData data = SkyBlockLevels.data();
        if (data.category(id) != null) {
            new TasksMenu(viewer, id, null).open(viewer);
            return;
        }
        Category category = categoryOf(data, id);
        if (category != null) new TasksMenu(viewer, category.id(), id).open(viewer);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        LevelingView view = LevelingView.of(viewer, user);
        Sort sort = sort(viewer.getUniqueId());
        WaysMenu.viewed(viewer.getUniqueId(), task == null ? category : task);
        List<Task> tasks = shown(view, category, task, sort);
        int[] slots = slots(tasks.size());
        Map<Integer, Icon> icons = icons(view, category, task, sort);
        int size = getSize();
        ItemStack glass = filler().withType(glass(category));
        for (int slot : frame(size)) set(slot, glass);
        for (Map.Entry<Integer, Icon> entry : icons.entrySet()) {
            int slot = entry.getKey();
            Runnable action = null;
            for (int i = 0; i < slots.length; i++) {
                Task shown = tasks.get(i);
                if (slots[i] != slot) continue;
                if (!shown.tasks().isEmpty()) action = () -> new TasksMenu(viewer, category, shown.id()).open(viewer);
                else if (shown.id().equals(SkyBlockXp.SKILLS)) action = () -> new SkillsMenu(viewer).open(viewer);
            }
            if (slot == back(size)) action = () -> back(viewer);
            if (task == null && slot == sort(size)) action = () -> {
                sorts.put(viewer.getUniqueId(), sort.next());
                new TasksMenu(viewer, category, task).open(viewer);
            };
            if (action == null) set(slot, entry.getValue().stack());
            else set(GUIClickableItem.button(slot, entry.getValue().stack(), viewer, action));
        }
        set(GUIClickableItem.close(back(size) + 1));
    }

    /**
     * The border's slots: the top row but its middle, the sides, the bottom row but Go Back, Close and Sort's (a
     * task's parts have glass where Sort would be), as the wiki's; the rest is the usual black glass.
     */
    static int[] frame(int size) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9, column = slot % 9;
            boolean edge = row == 0 || row == size / 9 - 1 || column == 0 || column == 8;
            if (edge && slot != TOP && slot != back(size) && slot != back(size) + 1) slots.add(slot);
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * The border's glass: each category's in the wiki's copies (white for Core, yellow for Event and Miscellaneous,
     * red for Dungeon, brown for Essence Shop, orange for Slaying, purple for Story). Skill Related's (the wiki's split
     * it by skill, each its own) and Consumables' (not on the wiki) are UNKNOWN: black, as the rest of the menu.
     */
    static Material glass(String category) {
        return switch (category) {
            case "core" -> Material.WHITE_STAINED_GLASS_PANE;
            case "event", "miscellaneous" -> Material.YELLOW_STAINED_GLASS_PANE;
            case "dungeon" -> Material.RED_STAINED_GLASS_PANE;
            case "essence_shop" -> Material.BROWN_STAINED_GLASS_PANE;
            case "slaying" -> Material.ORANGE_STAINED_GLASS_PANE;
            case "story" -> Material.PURPLE_STAINED_GLASS_PANE;
            default -> Material.BLACK_STAINED_GLASS_PANE;
        };
    }

    /** Go Back's slot: the bottom row's fourth, Close next to it and Sort after that (the wiki's). */
    static int back(int size) {
        return size - 6;
    }

    static int sort(int size) {
        return size - 4;
    }

    /** Where Go Back goes: a task's parts to its parent's list, a category's tasks to Ways to Level Up. */
    private void back(Player viewer) {
        Task parent = task == null ? null : parent(SkyBlockLevels.data(), category, task);
        if (task == null) new WaysMenu(viewer).open(viewer);
        else new TasksMenu(viewer, category, parent == null ? null : parent.id()).open(viewer);
    }

    static Sort sort(UUID player) {
        return sorts.getOrDefault(player, Sort.UNLOCKED);
    }

    static void forget(UUID player) {
        sorts.remove(player);
    }

    // What it lists

    /** "Tasks ➜ Core" (a category's name without "Tasks"); "Core ➜ Bank Upgrades" for a task's parts. */
    static String title(LevelingData data, String category, String task) {
        Category c = data.category(category);
        String shortName = c == null ? "?" : LevelingText.plain(c.name()).replace(" Tasks", "");
        if (task == null) return "Tasks ➜ " + shortName;
        Task t = data.task(task);
        Task parent = parent(data, category, task);
        return (parent == null ? shortName : parent.name()) + " ➜ " + (t == null ? "?" : t.name());
    }

    /** The category's tasks, or a task's parts. */
    static List<Task> tasks(LevelingData data, String category, String task) {
        if (task != null) {
            Task t = data.task(task);
            return t == null ? List.of() : t.tasks();
        }
        Category c = data.category(category);
        return c == null ? List.of() : c.tasks();
    }

    private static int size(LevelingData data, String category, String task) {
        return Math.min(MOST, tasks(data, category, task).size());
    }

    /** The task whose part this one is (null for a category's own). */
    static Task parent(LevelingData data, String category, String task) {
        Category c = data.category(category);
        return c == null ? null : parent(c.tasks(), task, null);
    }

    private static Task parent(List<Task> tasks, String id, Task parent) {
        for (Task t : tasks) {
            if (t.id().equals(id)) return parent;
            Task found = parent(t.tasks(), id, t);
            if (found != null) return found;
        }
        return null;
    }

    private static Category categoryOf(LevelingData data, String task) {
        for (Category c : data.categories()) if (parent(c.tasks(), task, null) != null || contains(c.tasks(), task)) return c;
        return null;
    }

    private static boolean contains(List<Task> tasks, String id) {
        for (Task t : tasks) if (t.id().equals(id) || contains(t.tasks(), id)) return true;
        return false;
    }

    /** What the list shows, in order: a category's tasks as sorted, a task's parts as they come (the wiki's have no Sort). */
    static List<Task> shown(LevelingView view, String category, String task, Sort sort) {
        List<Task> tasks = tasks(view.data(), category, task);
        return task == null ? sorted(view, tasks, sort) : tasks;
    }

    static List<Task> sorted(LevelingView view, List<Task> tasks, Sort sort) {
        List<Task> out = new ArrayList<>(tasks);
        Comparator<Task> byName = Comparator.comparing(t -> LevelingText.plain(t.name()));
        switch (sort) {
            case EARNED -> out.sort(Comparator.comparingInt((Task t) -> -earned(view, t)));
            case AVAILABLE -> out.sort(Comparator.comparingInt((Task t) -> -(t.max() - earned(view, t))));
            case A_TO_Z -> out.sort(byName);
            case Z_TO_A -> out.sort(byName.reversed());
            // UNKNOWN what "Unlocked" orders by: every task is unlocked here, so the data's (the menu's) order.
            case UNLOCKED -> {
            }
        }
        return out;
    }

    // Where they go

    /** Rows: the top one, an empty one, the tasks' (at least two), the bottom one; at most six. */
    static int rows(int tasks) {
        return Math.min(6, 4 + Math.max(1, taskRows(tasks)));
    }

    private static int taskRows(int n) {
        return n <= 7 ? 1 : n <= 15 ? (n + 4) / 5 : (n + 6) / 7;
    }

    /**
     * Where n tasks go, as the wiki's menus have them: up to three every other slot across the third row, up to seven
     * side by side; up to 15 in rows of five from the third row, more in rows of seven (from the second row past 21).
     * A row short of its width is centred, an even one with its middle empty (the Essence Shop's six). Past what
     * the wiki shows (four, or more than eight) it's UNKNOWN, done the same way.
     */
    static int[] slots(int n) {
        n = Math.min(n, MOST);
        int[] slots = new int[n];
        if (n <= 3) {
            for (int i = 0; i < n; i++) slots[i] = 18 + 4 - (n - 1) + 2 * i;
            return slots;
        }
        int width = n <= 7 ? 7 : n <= 15 ? 5 : 7;
        int firstRow = n > 21 ? 1 : 2;
        for (int i = 0; i < n; i++) {
            int row = i / width, inRow = Math.min(width, n - row * width), column = i % width;
            slots[i] = (firstRow + row) * 9 + centred(inRow, column);
        }
        return slots;
    }

    /** The column (0 to 8) of the k-th of n items across a row: centred, an even count with its middle empty. */
    static int centred(int n, int k) {
        if (n % 2 == 1) return 4 - n / 2 + k;
        return k < n / 2 ? 4 - n / 2 + k : 5 + k - n / 2;
    }

    // What each slot shows

    /** Every slot but the glass and Close, by slot: the top, the tasks, Go Back and (for a category's tasks) Sort. */
    static Map<Integer, Icon> icons(LevelingView view, String category, String task, Sort sort) {
        LevelingData data = view.data();
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        Category c = data.category(category);
        if (c == null) return icons;
        Task t = task == null ? null : data.task(task);
        icons.put(TOP, t == null ? WaysMenu.category(c, view, false) : task(t, view));
        List<Task> tasks = shown(view, category, task, sort);
        int[] slots = slots(tasks.size());
        for (int i = 0; i < slots.length; i++) {
            Task shown = tasks.get(i);
            icons.put(slots[i], t != null && once(shown) ? once(shown, view) : task(shown, view));
        }
        int size = rows(tasks.size()) * 9;
        Task parent = t == null ? null : parent(data, category, task);
        String back = t == null ? "&7To Ways to Level Up" : "&7To " + title(data, category, parent == null ? null : parent.id());
        icons.put(back(size), new Icon(Material.ARROW, "&aGo Back", back));
        if (t == null) icons.put(sort(size), sortIcon(sort));
        return icons;
    }

    static Icon sortIcon(Sort sort) {
        List<String> lore = new ArrayList<>(List.of("&7Change how Tasks are sorted.", ""));
        for (Sort s : Sort.values()) lore.add((s == sort ? "&b▶ " : "&7") + s.label);
        lore.addAll(List.of("", "&eClick to switch sort!"));
        return new Icon(Material.HOPPER, "&aSort", lore);
    }

    /** The XP a task (or part) has given them. */
    static int earned(LevelingView view, Task task) {
        return view.xp().of(task.id());
    }

    /** Whether a part is done: all it's worth earned (something earned, for one that's worth nothing known). */
    static boolean done(LevelingView view, Task task) {
        int earned = earned(view, task);
        return task.max() > 0 ? earned >= task.max() : earned > 0;
    }

    /**
     * A task's item, as the wiki's: a task with parts lists them ("&c✖ &eGold Bank Upgrade", with how many of a
     * part's own parts are done) and opens them; one without has its XP lines. Both say what they've earned of it
     * and what share of their SkyBlock XP that is. What a done part's line looks like is UNKNOWN (the wiki's are all
     * undone): a green tick. A part done once ({@link #once}) is as the wiki's are instead.
     */
    static Icon task(Task task, LevelingView view) {
        List<String> lore = new ArrayList<>();
        int earned = earned(view, task);
        if (!task.tasks().isEmpty()) {
            int parts = task.tasks().size();
            lore.add("&8" + parts + (parts == 1 ? " Task" : " Tasks"));
            lore.add("");
            for (Task part : task.tasks()) {
                String count = "";
                if (!part.tasks().isEmpty()) {
                    int done = 0;
                    for (Task p : part.tasks()) if (done(view, p)) done++;
                    count = "&8 (" + done + "/" + part.tasks().size() + ")";
                }
                lore.add("  " + (done(view, part) ? "&a✔ &e" : "&c✖ &e") + part.name() + count);
            }
        } else {
            lore.add("&8XP Task");
            lore.add("");
            if (task.description() != null) lore.addAll(LevelingText.wrap("&7" + task.description()));
            for (Step step : task.steps()) {
                if (step.xp() <= 0) continue;
                lore.add(step.text().isEmpty() ? "&b+" + step.xp() + " XP" : "&7" + step.text() + ": &b+" + step.xp() + " XP");
                if (step.note() != null) lore.add("&8&o" + LevelingText.plain(step.note()));
            }
        }
        lore.add("");
        if (task.max() > 0) {
            double fraction = Math.min(1, (double) earned / task.max());
            lore.add("&7Total Progress: &3" + LevelingText.percent(fraction) + "%");
            lore.add(LevelingText.strip("&3", fraction) + " &b" + Text.number(earned) + "&3/&b" + Text.number(task.max()) + " XP");
        } else {
            lore.add("&7XP Gained from Task: &3" + Text.number(earned) + " XP");
        }
        lore.add("");
        double share = view.total() == 0 ? 0 : (double) earned / view.total();
        lore.addAll(List.of("&8This task is worth &3" + LevelingText.percent(share) + "%&8 of", "&8your Total SkyBlock XP!"));
        if (!task.tasks().isEmpty()) lore.addAll(List.of("", "&eClick to view tasks!"));
        else if (task.id().equals(SkyBlockXp.SKILLS)) lore.addAll(List.of("", "&eClick to see Your Skills!"));
        return task.look().icon("&a" + task.name(), lore);
    }

    /**
     * Whether a part is done once for all it's worth (a bank upgrade, a floor's first completion): one XP line with
     * no text, worth the part's most (a Chocolate Factory prestige, one of five, isn't).
     */
    static boolean once(Task task) {
        if (!task.tasks().isEmpty() || task.steps().size() != 1) return false;
        Step step = task.steps().getFirst();
        return step.text().isEmpty() && step.xp() > 0 && step.xp() == task.max();
    }

    /**
     * A part done once, as the wiki's copies have them (Gold Bank Upgrade, Slay Superior Dragon, White Belt...): what it
     * gives, and that it can only be done once; no progress. The wiki's say what to do in their own words ("Upgrade
     * your Bank Account to this level to gain +20 XP."), which the data doesn't have: UNKNOWN, the XP alone. How a
     * done one shows is UNKNOWN too (the wiki's are all undone): COMPLETED under it.
     */
    private static Icon once(Task task, LevelingView view) {
        List<String> lore = new ArrayList<>(List.of("&8XP Task", ""));
        if (task.description() != null) lore.addAll(LevelingText.wrap("&7" + task.description()));
        lore.addAll(List.of("&b+" + task.steps().getFirst().xp() + " XP", "", "&eThis task can only be", "&ecompleted once!"));
        if (done(view, task)) lore.addAll(List.of("", "&a&lCOMPLETED"));
        return task.look().icon("&a" + task.name(), lore);
    }

    /** A Recently Viewed item: the category's or task's own; null if it's gone from the data. */
    static Icon viewedIcon(LevelingView view, String id) {
        LevelingData data = view.data();
        Category c = data.category(id);
        if (c != null) return WaysMenu.category(c, view, true);
        Task t = data.task(id);
        return t == null ? null : task(t, view);
    }
}
