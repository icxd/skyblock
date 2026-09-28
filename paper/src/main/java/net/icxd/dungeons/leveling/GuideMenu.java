package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

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
 * stage's tasks, the undone ones first, each with its parts done and undone. A task is done when all its parts are;
 * the plugin can tell only skill, Catacombs and class levels, floors completed and collection tiers, so the rest stay
 * undone. What "Click to view more!" opens is UNKNOWN (never recorded), so it opens nothing yet. Main thread.
 */
public final class GuideMenu extends GUI {
    static final int[] STAGES = {1, 2, 3, 4, 5, 6, 7};
    /** The tasks go in rows of seven from the third row. */
    static final int FIRST_TASK = 19;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int INFO = 50;
    /** A stage unlocks at this share of the one before it done. */
    static final double UNLOCK = 0.5;

    private final Player viewer;
    /** The stage shown; -1 for the last one they've unlocked. */
    private final int stage;

    public GuideMenu(Player viewer, int stage) {
        super(title(SkyBlockLevels.data(), viewer, stage), Size.SIX);
        this.viewer = viewer;
        this.stage = stage;
    }

    /** Opens their current stage: the last one they've unlocked. */
    public static void openCurrent(Player viewer) {
        new GuideMenu(viewer, -1).open(viewer);
    }

    private static String title(LevelingData data, Player viewer, int stage) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (data.stages().isEmpty() || user == null) return "SkyBlock Guide";
        LevelingView view = LevelingView.of(viewer, user);
        int shown = stage < 0 ? current(view) : Math.min(stage, data.stages().size() - 1);
        return "Guide ➜ " + LevelingText.plain(data.stages().get(shown).name());
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        LevelingView view = LevelingView.of(viewer, user);
        List<Stage> stages = view.data().stages();
        if (stages.isEmpty()) return;
        int shown = stage < 0 ? current(view) : Math.min(stage, stages.size() - 1);
        for (Map.Entry<Integer, Icon> entry : icons(view, shown).entrySet()) {
            int slot = entry.getKey();
            Runnable action = null;
            for (int i = 0; i < STAGES.length && i < stages.size(); i++) {
                int index = i;
                if (STAGES[i] == slot && i != shown && unlocked(view, i)) action = () -> new GuideMenu(viewer, index).open(viewer);
            }
            if (slot == BACK) action = () -> new LevelingMenu(viewer).open(viewer);
            if (action == null) set(slot, entry.getValue().stack());
            else set(GUIClickableItem.button(slot, entry.getValue().stack(), viewer, action));
        }
        set(GUIClickableItem.close(CLOSE));
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

    /** Every slot but the glass and Close, by slot, with this stage shown. */
    static Map<Integer, Icon> icons(LevelingView view, int shown) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        List<Stage> stages = view.data().stages();
        for (int i = 0; i < STAGES.length && i < stages.size(); i++) icons.put(STAGES[i], stage(view, i, i == shown));
        Stage stage = stages.get(shown);
        // The undone ones first, each lot in the data's order: the recorded menus' (UNKNOWN whether Hypixel orders
        // them otherwise for someone else).
        List<GuideTask> tasks = new ArrayList<>();
        for (GuideTask task : stage.tasks()) if (!complete(view, task)) tasks.add(task);
        for (GuideTask task : stage.tasks()) if (complete(view, task)) tasks.add(task);
        for (int i = 0; i < tasks.size() && i < 21; i++) icons.put(FIRST_TASK + i / 7 * 9 + i % 7, task(view, color(stage), tasks.get(i)));
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
