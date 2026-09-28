package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.LevelingData.EmblemCategory;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;

/**
 * Emblems, as recorded (02:10.1): the kinds of prefix emblem, how many of each they've unlocked, each opening its
 * emblems ({@link EmblemListMenu}). The plugin can only tell skill, Catacombs, class and SkyBlock levels and floors
 * completed, so the Slayer, Achievement, MVP++ and Discord ones stay locked. Main thread.
 */
public final class EmblemsMenu extends GUI {
    public static final String TITLE = "Emblems";
    static final int FIRST = 10;
    static final int BACK = 30;
    static final int CLOSE = 31;

    private final Player viewer;

    public EmblemsMenu(Player viewer) {
        super(TITLE, Size.FOUR);
        this.viewer = viewer;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        LevelingView view = LevelingView.of(viewer, user);
        List<EmblemCategory> categories = view.data().emblemCategories();
        for (Map.Entry<Integer, Icon> entry : icons(view).entrySet()) {
            int slot = entry.getKey();
            int index = slot - FIRST;
            Runnable action = null;
            if (index >= 0 && index < categories.size() && index < 7) {
                String id = categories.get(index).id();
                action = () -> new EmblemListMenu(viewer, id, 0).open(viewer);
            }
            if (slot == BACK) action = () -> new LevelingMenu(viewer).open(viewer);
            if (action == null) set(slot, entry.getValue().stack());
            else set(GUIClickableItem.button(slot, entry.getValue().stack(), viewer, action));
        }
        set(GUIClickableItem.close(CLOSE));
    }

    /** Every slot but the glass and Close, by slot: the categories across the second row. */
    static Map<Integer, Icon> icons(LevelingView view) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        List<EmblemCategory> categories = view.data().emblemCategories();
        for (int i = 0; i < categories.size() && i < 7; i++) {
            EmblemCategory category = categories.get(i);
            List<String> lore = new ArrayList<>(List.of("&8" + Emblems.unlocked(category, view.profile(), view.level(), view.sources()) + " Unlocked", ""));
            lore.addAll(category.description());
            lore.addAll(List.of("", "&eClick to view!"));
            icons.put(FIRST + i, category.look().icon(category.name(), lore));
        }
        icons.put(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Leveling"));
        return icons;
    }
}
