package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.LevelingData.Emblem;
import net.icxd.dungeons.leveling.LevelingData.EmblemCategory;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * One kind of emblem, every one of it: unlocked ones to choose (and the chosen one to put away again), locked ones with
 * what unlocks them. Never recorded (the tour only opened Emblems): the title is the swofty remake's ("Emblems -
 * Leveling (8/21)"), the rest UNKNOWN: rows of seven, 28 a page, a chosen emblem's preview as chat shows it. Main
 * thread.
 */
public final class EmblemListMenu extends GUI {
    static final int PER_PAGE = 28;
    static final int FIRST = 10;
    static final int PREVIOUS = 45;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int NEXT = 53;

    private final Player viewer;
    private final String category;
    private final int page;

    public EmblemListMenu(Player viewer, String category, int page) {
        super(title(viewer, category), Size.SIX);
        this.viewer = viewer;
        this.category = category;
        this.page = page;
    }

    private static String title(Player viewer, String id) {
        EmblemCategory category = category(SkyBlockLevels.data(), id);
        User user = User.ifLoaded(viewer.getUniqueId());
        if (category == null || user == null) return "Emblems";
        LevelingView view = LevelingView.of(viewer, user);
        return title(category, Emblems.unlocked(category, view.profile(), view.level(), view.sources()));
    }

    /** "Emblems - Leveling (8/21)". */
    static String title(EmblemCategory category, int unlocked) {
        return "Emblems - " + LevelingText.plain(category.name()) + " (" + unlocked + "/" + category.emblems().size() + ")";
    }

    static EmblemCategory category(LevelingData data, String id) {
        for (EmblemCategory category : data.emblemCategories()) if (category.id().equals(id)) return category;
        return null;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        EmblemCategory shown = category(SkyBlockLevels.data(), category);
        if (user == null || shown == null) return;
        LevelingView view = LevelingView.of(viewer, user);
        List<Emblem> emblems = page(shown, page);
        for (Map.Entry<Integer, Icon> entry : icons(view, shown, page, Emblems.chosen(view.profile())).entrySet()) {
            int slot = entry.getKey();
            Runnable action = null;
            int index = index(slot);
            if (index >= 0 && index < emblems.size()) {
                Emblem emblem = emblems.get(index);
                if (Emblems.unlocked(emblem, view.profile(), view.level(), view.sources())) action = () -> choose(user, emblem);
            }
            if (slot == BACK) action = () -> new EmblemsMenu(viewer).open(viewer);
            if (slot == PREVIOUS) action = () -> new EmblemListMenu(viewer, category, page - 1).open(viewer);
            if (slot == NEXT) action = () -> new EmblemListMenu(viewer, category, page + 1).open(viewer);
            if (action == null) set(slot, entry.getValue().stack());
            else set(GUIClickableItem.button(slot, entry.getValue().stack(), viewer, action));
        }
        set(GUIClickableItem.close(CLOSE));
    }

    /** Chooses an emblem, or puts the chosen one away; it shows in chat and the tab list at once. */
    private void choose(User user, Emblem emblem) {
        if (user.isReleased() || GUI_MAP.get(viewer.getUniqueId()) != this) return;
        boolean chosen = emblem.id().equals(Emblems.chosen(user.profile()));
        Emblems.choose(user.profile(), chosen ? null : emblem.id());
        user.save();
        SkyBlockLevels.emblemChanged(viewer);
        // The swofty remake's line; UNKNOWN on Hypixel, and so is putting one away.
        viewer.sendMessage(Text.line(chosen ? "&aYou have removed your emblem!" : "&aYou have selected the " + emblem.name() + " emblem!"));
        new EmblemListMenu(viewer, category, page).open(viewer);
    }

    /** The emblems on one page. */
    static List<Emblem> page(EmblemCategory category, int page) {
        List<Emblem> all = category.emblems();
        int from = Math.max(0, Math.min(all.size(), page * PER_PAGE));
        return all.subList(from, Math.min(all.size(), from + PER_PAGE));
    }

    /** Which of a page's emblems a slot shows; -1 for none. */
    static int index(int slot) {
        int row = slot / 9 - 1, column = slot % 9 - 1;
        return row < 0 || row > 3 || column < 0 || column > 6 ? -1 : row * 7 + column;
    }

    /** Every slot but the glass and Close, by slot. */
    static Map<Integer, Icon> icons(LevelingView view, EmblemCategory category, int page, String chosen) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        List<Emblem> emblems = page(category, page);
        for (int i = 0; i < emblems.size(); i++) icons.put(FIRST + i / 7 * 9 + i % 7, emblem(view, emblems.get(i), emblems.get(i).id().equals(chosen)));
        if (page > 0) icons.put(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + page));
        if ((page + 1) * PER_PAGE < category.emblems().size()) icons.put(NEXT, new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)));
        icons.put(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Emblems"));
        return icons;
    }

    /** An emblem: unlocked, a preview of their name with it and SELECTED or "Click to select!"; locked, what unlocks it. */
    static Icon emblem(LevelingView view, Emblem emblem, boolean chosen) {
        String name = "&f" + emblem.name() + " " + emblem.symbol();
        List<String> lore = new ArrayList<>();
        if (Emblems.unlocked(emblem, view.profile(), view.level(), view.sources())) {
            lore.add("&7Preview: " + SkyBlockXp.bracket(view.level()) + " " + emblem.symbol() + " " + view.rank().getPrefix() + view.name());
            lore.add("");
            lore.add(chosen ? "&a&lSELECTED" : "&eClick to select!");
            if (chosen) lore.add("&eClick to remove!");
            return new Icon(Material.NAME_TAG, name, lore);
        }
        lore.addAll(LevelingText.wrap("&7Requires: &c" + emblem.requirement()));
        lore.addAll(List.of("", "&c&lLOCKED"));
        return new Icon(Material.GRAY_DYE, name, lore);
    }
}
