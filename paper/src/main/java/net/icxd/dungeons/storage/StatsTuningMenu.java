package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Stats Tuning (from Loadouts' Stats Tuning), as the wiki's Maxwell/UI has it (its newer tab): the Stats Tuning item at
 * the top, the eight stats in two rows, each with what they have, its points, what a point gives and the points left;
 * "Right-Click to add a point! Hold shift for 10 points!". Taking points out isn't in the wiki's (it shows a stat with
 * none): a left click takes one out, or 10 with shift (UNKNOWN). The four tuning templates on the right aren't here yet
 * (glass). Go Back goes to Loadouts (Hypixel's goes to the Accessory Bag Thaumaturgy, which isn't here). Main thread.
 */
final class StatsTuningMenu extends GUI {
    static final String TITLE = "Stats Tuning";
    static final int INFO = 4;
    static final int GO_BACK = 49;

    private final Player viewer;

    StatsTuningMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        int accessoryPower = AccessoryBag.accessoryPower(viewer);
        Map<Integer, Icon> icons = icons(profile, accessoryPower, PlayerSession.of(viewer).stats());
        for (StatsTuning.Tuned tuned : StatsTuning.Tuned.values()) {
            set(GUIClickableItem.button(tuned.slot, icons.remove(tuned.slot).stack(), viewer, click -> tune(tuned, click)));
        }
        for (Map.Entry<Integer, Icon> e : icons.entrySet()) set(e.getKey(), e.getValue().stack());
        set(GUIClickableItem.button(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Loadouts").stack(), viewer,
                () -> new LoadoutsMenu(viewer, 0).open(viewer)));
    }

    /** A right click puts a point in (10 with shift), a left click takes one out; then their stats and the menu again. */
    private void tune(StatsTuning.Tuned tuned, ClickType click) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        int wanted = (click.isShiftClick() ? 10 : 1) * (click.isRightClick() ? 1 : -1);
        int has = StatsTuning.points(profile, tuned);
        int left = Accessories.tuningPoints(AccessoryBag.accessoryPower(viewer)) - StatsTuning.assigned(profile);
        int change = StatsTuning.change(has, left, wanted);
        if (change == 0) return;
        StatsTuning.set(profile, tuned, has + change);
        AccessoryBag.changed(viewer);
        PlayerSession.of(viewer).invalidateStats();
        new StatsTuningMenu(viewer).open(viewer);
    }

    /** The menu's slots but the glass and Go Back: the Stats Tuning item and the eight stats, for their stats now. */
    static Map<Integer, Icon> icons(Document profile, int accessoryPower, Stats stats) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        int points = Accessories.tuningPoints(accessoryPower);
        int left = Math.max(0, points - StatsTuning.assigned(profile));
        icons.put(INFO, info(accessoryPower, points, left));
        Stats tuning = StatsTuning.stats(profile, points);
        for (StatsTuning.Tuned tuned : StatsTuning.Tuned.values()) {
            double bonus = tuning.get(tuned.stat);
            icons.put(tuned.slot, stat(tuned, stats.get(tuned.stat) - bonus, bonus, StatsTuning.points(profile, tuned), left));
        }
        return icons;
    }

    /** The Stats Tuning item, as Loadouts has it without its click line ("Unassigned Points" while there are some). */
    static Icon info(int accessoryPower, int points, int unassigned) {
        List<String> lore = new ArrayList<>(List.of("&7Optimize your build to your liking by using", "&eTuning Points&7.", "",
                "&7Every &610 MP &7grants &e1 Tuning Point&7.", "", "&7Magical Power: &6" + accessoryPower, "&7Tuning Points: &e" + points));
        if (unassigned > 0) lore.add("&7Unassigned Points: &c" + unassigned + "!!!");
        return new Icon(Material.COMPARATOR, "&aStats Tuning", lore);
    }

    /**
     * A stat: "&7You have: &c100", then with points "&7+ &c50 ❤" (SkyHanni's pattern for the line: "You have: §b1,347 §7+
     * §b6 ✎"; whether the first number counts the tuning is UNKNOWN: it doesn't here), its points, what a point gives, the
     * points left, and how to add them.
     */
    static Icon stat(StatsTuning.Tuned tuned, double without, double bonus, int points, int left) {
        String color = "&" + tuned.stat.getColor();
        String have = "&7You have: " + color + Text.number(without) + tuned.stat.getUnit()
                + (bonus > 0 ? " &7+ " + color + Text.number(bonus) + " " + tuned.stat.getSymbol() : "");
        List<String> lore = new ArrayList<>(tuned.description);
        lore.addAll(List.of("", have, "&8&m------------------------", "&7Stat has: &c" + points + " points",
                "&7Per point: " + color + "+" + Text.number(tuned.perPoint) + tuned.stat.getSymbol(), "&7Points left: " + left, "",
                "&eRight-Click to add a point!"));
        // UNKNOWN: Hypixel's line for taking them out (the wiki's has a stat with none).
        if (points > 0) lore.add("&eLeft-Click to remove a point!");
        lore.add("&8Hold shift for 10 points!");
        return new Icon(tuned.material, color + tuned.stat.getSymbol() + " " + tuned.stat.getDisplayName(), lore);
    }
}
