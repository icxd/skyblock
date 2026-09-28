package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.LevelingData.GuideItem;
import net.icxd.dungeons.leveling.LevelingData.GuideTask;
import net.icxd.dungeons.leveling.LevelingData.Stage;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;

/**
 * The SkyBlock XP Guide ("Guide ➜ Amateur", {@code /skyblockxp}), as recorded (Starter at 01:51.3, Amateur at
 * 01:49.8): the seven stages across the top (a stage unlocks at half of the one before it done), and the chosen
 * stage's tasks inside a frame of glass in its colour, the undone ones first, each with its parts done and undone.
 * A task is done when all its parts are; the plugin can tell only skill, Catacombs and class levels, floors completed
 * and collection tiers, so the rest stay undone. What "Click to view more!" opens is UNKNOWN (never recorded), so it
 * opens nothing yet. Main thread.
 */
public final class GuideMenu extends GUI {
    static final int[] STAGES = {1, 2, 3, 4, 5, 6, 7};
    /** The tasks go in rows of seven from the third row, three rows of them. */
    static final int FIRST_TASK = 19;
    static final int PER_PAGE = 21;
    static final int PREVIOUS = 45;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int INFO = 50;
    static final int NEXT = 53;
    /** The stage's glass: the second row and the sides below it, as recorded (the top corners and the tasks' slots are empty). */
    static final int[] FRAME = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 51, 52, 53};
    /** A stage unlocks at this share of the one before it done. */
    static final double UNLOCK = 0.5;

    private final Player viewer;
    /** The stage shown; -1 for the last one they've unlocked. */
    private final int stage;
    private final int page;

    public GuideMenu(Player viewer, int stage) {
        this(viewer, stage, 0);
    }

    GuideMenu(Player viewer, int stage, int page) {
        super(title(SkyBlockLevels.data(), viewer, stage, page), Size.SIX);
        this.viewer = viewer;
        this.stage = stage;
        this.page = page;
    }

    /** Opens their current stage: the last one they've unlocked. */
    public static void openCurrent(Player viewer) {
        new GuideMenu(viewer, -1).open(viewer);
    }

    private static String title(LevelingData data, Player viewer, int stage, int page) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (data.stages().isEmpty() || user == null) return "SkyBlock Guide";
        LevelingView view = LevelingView.of(viewer, user);
        return title(data.stages().get(stage < 0 ? current(view) : Math.min(stage, data.stages().size() - 1)), page);
    }

    /**
     * "Guide ➜ Starter"; a stage with more tasks than one page holds, "(1/2) Guide ➜ Skilled", as Hypixel's paged
     * menus are titled (the recipe book's "(2/4) Combat Recipes"; UNKNOWN for the guide, never recorded past 21).
     */
    static String title(Stage stage, int page) {
        int pages = pages(stage);
        return (pages > 1 ? "(" + (Math.min(page, pages - 1) + 1) + "/" + pages + ") " : "") + "Guide ➜ " + LevelingText.plain(stage.name());
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        User user = User.ifLoaded(viewer.getUniqueId());
        List<Stage> stages = SkyBlockLevels.data().stages();
        if (user == null || stages.isEmpty()) {
            fill(filler());
            return;
        }
        LevelingView view = LevelingView.of(viewer, user);
        int shown = stage < 0 ? current(view) : Math.min(stage, stages.size() - 1);
        ItemStack glass = filler().withType(glass(stages.get(shown)));
        for (int slot : FRAME) set(slot, glass);
        int page = Math.min(this.page, pages(stages.get(shown)) - 1);
        for (Map.Entry<Integer, Icon> entry : icons(view, shown, page).entrySet()) {
            int slot = entry.getKey();
            Runnable action = null;
            for (int i = 0; i < STAGES.length && i < stages.size(); i++) {
                int index = i;
                if (STAGES[i] == slot && i != shown && unlocked(view, i)) action = () -> new GuideMenu(viewer, index).open(viewer);
            }
            if (slot == BACK) action = () -> new LevelingMenu(viewer).open(viewer);
            if (slot == PREVIOUS) action = () -> new GuideMenu(viewer, shown, page - 1).open(viewer);
            if (slot == NEXT) action = () -> new GuideMenu(viewer, shown, page + 1).open(viewer);
            if (action == null) set(slot, entry.getValue().stack());
            else set(GUIClickableItem.button(slot, entry.getValue().stack(), viewer, action));
        }
        set(GUIClickableItem.close(CLOSE));
    }

    /** The glass in a stage's colour: lime for Starter and light blue for Amateur (recorded), the rest likewise (UNKNOWN). */
    static Material glass(Stage stage) {
        return switch (color(stage).charAt(1)) {
            case 'a' -> Material.LIME_STAINED_GLASS_PANE;
            case 'b' -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case '3' -> Material.CYAN_STAINED_GLASS_PANE;
            case '9' -> Material.BLUE_STAINED_GLASS_PANE;
            case '5' -> Material.PURPLE_STAINED_GLASS_PANE;
            case '6' -> Material.ORANGE_STAINED_GLASS_PANE;
            case 'd' -> Material.MAGENTA_STAINED_GLASS_PANE;
            default -> Material.BLACK_STAINED_GLASS_PANE;
        };
    }

    /** How many pages a stage's tasks take. */
    static int pages(Stage stage) {
        return Math.max(1, (stage.tasks().size() + PER_PAGE - 1) / PER_PAGE);
    }

    // Progress

    /** How many of a task's parts they've done. */
    static int done(LevelingView view, GuideTask task) {
        if (task.items().isEmpty()) return view.done(task.unlock()) ? task.count() : 0;
        int done = 0;
        for (GuideItem item : task.items()) if (view.done(item.unlock())) done++;
        // Parts the data doesn't name (a count only) are never done.
        return Math.min(done, task.count());
    }

    static boolean complete(LevelingView view, GuideTask task) {
        return done(view, task) >= task.count();
    }

    /** How many of a stage's tasks they've done. */
    static int done(LevelingView view, Stage stage) {
        int done = 0;
        for (GuideTask task : stage.tasks()) done += done(view, task);
        return done;
    }

    static double progress(LevelingView view, Stage stage) {
        return stage.count() == 0 ? 0 : Math.min(1, (double) done(view, stage) / stage.count());
    }

    /** Whether a stage is open to them: the first always, the next at half of the one before it. */
    static boolean unlocked(LevelingView view, int stage) {
        List<Stage> stages = view.data().stages();
        for (int i = 1; i <= stage && i < stages.size(); i++) if (progress(view, stages.get(i - 1)) < UNLOCK) return false;
        return true;
    }

    /** The last stage they've unlocked. */
    static int current(LevelingView view) {
        int current = 0;
        while (current + 1 < view.data().stages().size() && unlocked(view, current + 1)) current++;
        return current;
    }

    // What each slot shows

    /**
     * Every slot but the glass and Close, by slot, with this stage and page of its tasks shown. The undone tasks
     * first, each lot in the data's order: the recorded menus' (UNKNOWN whether Hypixel orders them otherwise for
     * someone else). A stage with more than 21 has pages, with Hypixel's usual arrows (UNKNOWN for the guide).
     */
    static Map<Integer, Icon> icons(LevelingView view, int shown, int page) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        List<Stage> stages = view.data().stages();
        for (int i = 0; i < STAGES.length && i < stages.size(); i++) icons.put(STAGES[i], stage(view, i, i == shown));
        Stage stage = stages.get(shown);
        List<GuideTask> tasks = new ArrayList<>();
        for (GuideTask task : stage.tasks()) if (!complete(view, task)) tasks.add(task);
        for (GuideTask task : stage.tasks()) if (complete(view, task)) tasks.add(task);
        int from = page * PER_PAGE;
        for (int i = 0; from + i < tasks.size() && i < PER_PAGE; i++) {
            icons.put(FIRST_TASK + i / 7 * 9 + i % 7, task(view, color(stage), tasks.get(from + i)));
        }
        if (page > 0) icons.put(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + page));
        if (page + 1 < pages(stage)) icons.put(NEXT, new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)));
        icons.put(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Leveling"));
        icons.put(INFO, new Icon(Material.PAPER, "&a⚑ SkyBlock XP Guide", "&7Your &6SkyBlock XP Guide &7tracks the",
                "&7progress you have made through", "&7SkyBlock.", "", "&7Complete tasks within your current", "&7game stage to increase your",
                "&bSkyBlock Level &7and become a &dMaster", "&7of SkyBlock!", "", "&8Also accessible via /skyblockxp"));
        return icons;
    }

    /** A stage's item: unlocked, its description and progress, SELECTED or "Click to select!"; locked, what unlocks it. */
    static Icon stage(LevelingView view, int index, boolean selected) {
        List<Stage> stages = view.data().stages();
        Stage stage = stages.get(index);
        List<String> lore = new ArrayList<>(List.of(stage.subtitle(), ""));
        if (!unlocked(view, index)) {
            Stage before = stages.get(index - 1);
            lore.addAll(LevelingText.wrap("&7Reach &a50% &7completion of " + before.name() + " &7to unlock this stage!"));
            lore.addAll(List.of("", "&c&lLOCKED"));
            return new Icon(Material.GRAY_DYE, stage.name(), lore);
        }
        lore.addAll(stage.lines().isEmpty() ? LevelingText.wrap(stage.description()) : stage.lines());
        lore.add("");
        lore.addAll(progress(color(stage), "Total Progress", done(view, stage), stage.count()));
        lore.add("");
        lore.add(selected ? "&a&lSELECTED" : "&eClick to select!");
        return new Icon(Material.FILLED_MAP, stage.name(), lore);
    }

    /**
     * A task's item: done or not, how many parts, what it is, its parts (where its item lists them), what it's worth
     * and how far they are. A one-part task shows how far it is towards what it needs ("Progress to LVL 1"), or
     * nothing (Arachne's): done, all of it; undone, none, as the plugin can't tell part of one.
     */
    static Icon task(LevelingView view, String stageColor, GuideTask task) {
        boolean complete = complete(view, task);
        List<String> lore = new ArrayList<>();
        if (task.count() > 1) lore.addAll(List.of("&8" + task.count() + " tasks", ""));
        lore.addAll(task.lines().isEmpty() ? LevelingText.wrap("&7" + task.description()) : task.lines());
        lore.add("");
        if (task.listed()) {
            for (GuideItem item : task.items()) {
                lore.add(view.done(item.unlock()) ? "&a ✔ &8" + LevelingText.plain(item.text()) : "&c ✖ &f" + item.text());
            }
            lore.add("");
        }
        lore.add("&7Total Worth: &b+" + task.xp() + " XP");
        if (task.count() > 1) {
            lore.add("");
            lore.addAll(progress(stageColor, "Total Progress", done(view, task), task.count()));
            lore.addAll(List.of("", "&eClick to view more!"));
        } else if (task.progress() != null) {
            lore.add("");
            lore.addAll(progress(stageColor, "Progress to " + task.progress(), complete ? task.max() : 0, task.max()));
        }
        return task.look().icon((complete ? "&a✔ " : "&c✖ ") + task.name(), lore);
    }

    /** "Total Progress: &a57%" and its bar in the stage's colour, "73/128". */
    static List<String> progress(String color, String label, int done, int max) {
        double fraction = max == 0 ? 0 : Math.min(1, (double) done / max);
        return List.of("&7" + label + ": " + color + LevelingText.percent(fraction) + "%",
                LevelingText.strip(color, fraction) + " " + color + done + "&8/" + color + max);
    }

    /** A stage's colour: its name's ("&a" for Starter). */
    static String color(Stage stage) {
        return stage.name().length() > 1 && stage.name().charAt(0) == '&' ? stage.name().substring(0, 2) : "&a";
    }
}
