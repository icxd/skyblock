package net.icxd.dungeons.leveling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.leveling.LevelingData.Reward;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * "Level 90 Rewards": one level's rewards, each with where it stands for them, as recorded (Level 88's at 01:37.9 and
 * Level 90's at 01:40.3): a golden apple for its Health, blaze powder for its Strength, and its other rewards as the
 * Rewards lists show them. One to five go every other slot across the middle, as recorded for one and three; more
 * than five (only level 50 has six) go side by side (UNKNOWN). Main thread.
 */
public final class LevelMenu extends GUI {
    static final int BACK = 30;
    static final int CLOSE = 31;

    private final Player viewer;
    private final int level;

    public LevelMenu(Player viewer, int level) {
        super("Level " + level + " Rewards", Size.FOUR);
        this.viewer = viewer;
        this.level = level;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        for (Map.Entry<Integer, Icon> entry : icons(LevelingView.of(viewer, user), level).entrySet()) set(entry.getKey(), entry.getValue().stack());
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Leveling").stack(), viewer,
                () -> new LevelingMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    /** Where n rewards go in the middle row. */
    static int[] slots(int n) {
        int[] slots = new int[n];
        for (int i = 0; i < n; i++) slots[i] = n <= 5 ? 13 - (n - 1) + 2 * i : 9 + Math.max(0, (9 - n) / 2) + i;
        return slots;
    }

    /** The level's rewards by slot (not Go Back and Close). */
    static Map<Integer, Icon> icons(LevelingView view, int level) {
        List<Icon> items = new ArrayList<>();
        for (Reward reward : view.data().rewards(level)) items.add(RewardListMenu.icon(reward, view));
        List<String> state = LevelingText.state(level, view.total());
        if (level > 0 && level % LevelRewards.STRENGTH_EVERY == 0) {
            items.add(stat(Material.BLAZE_POWDER, LevelRewards.strengthLine(LevelRewards.STRENGTH), level, state));
        }
        if (level > 0) items.add(stat(Material.GOLDEN_APPLE, LevelRewards.healthLine(LevelRewards.HEALTH), level, state));
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        int[] slots = slots(Math.min(items.size(), 9));
        for (int i = 0; i < slots.length; i++) icons.put(slots[i], items.get(i));
        return icons;
    }

    private static Icon stat(Material material, String name, int level, List<String> state) {
        List<String> lore = new ArrayList<>(List.of("&8Level " + Text.number(level), ""));
        lore.addAll(state);
        return new Icon(material, name, lore);
    }
}
