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
import net.icxd.dungeons.leveling.LevelingData.RewardKind;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.SkillText;
import net.icxd.dungeons.user.User;

/**
 * "Rewards": one kind of reward, every one of it with its level and where it stands for them, as recorded (the
 * features at 01:55.1, prefix colours 01:57.0, emblems 01:59.8, bonuses 02:04.4). Rows of seven in the middle, three
 * rows from the second (21 emblems), fewer from the third; a last row short of seven is centred, an even one with a
 * gap in its middle (the bonuses' last two, as recorded; four or six are UNKNOWN, done the same way). Main thread.
 */
public final class RewardListMenu extends GUI {
    public static final String TITLE = "Rewards";
    static final int TOP = 4;
    static final int BACK = 48;
    static final int CLOSE = 49;
    /** The emblems' list also opens the Emblems menu. */
    static final int EMBLEMS = 50;
    /** The bonuses' size: 0.01% a level, at most 5% (at 500: the wiki's Book of Progression). */
    static final double BONUS_PER_LEVEL = 0.01;
    static final int BONUS_MAX_LEVEL = 500;

    private final Player viewer;
    private final RewardKind kind;

    public RewardListMenu(Player viewer, RewardKind kind) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
        this.kind = kind;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        for (Map.Entry<Integer, Icon> entry : icons(LevelingView.of(viewer, user), kind).entrySet()) {
            if (entry.getKey() == EMBLEMS) {
                set(GUIClickableItem.button(EMBLEMS, entry.getValue().stack(), viewer, () -> new EmblemsMenu(viewer).open(viewer)));
            } else {
                set(entry.getKey(), entry.getValue().stack());
            }
        }
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Leveling Rewards").stack(), viewer,
                () -> new RewardsMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    /** Where n items go, rows of seven in the middle (see the class). */
    static int[] slots(int n) {
        int rows = (n + 6) / 7;
        int firstRow = rows >= 3 ? 1 : 2;
        int[] slots = new int[n];
        for (int i = 0; i < n; i++) {
            int row = i / 7, column = i % 7;
            int inRow = Math.min(7, n - row * 7);
            if (inRow < 7) {
                int start = (7 - inRow) / 2;
                // An even row leaves its middle empty: two go in the 3rd and 5th columns.
                column = inRow % 2 == 1 ? start + column : column < inRow / 2 ? 3 - inRow / 2 + column : 4 + column - inRow / 2;
            }
            slots[i] = (firstRow + row) * 9 + 1 + column;
        }
        return slots;
    }

    static Map<Integer, Icon> icons(LevelingView view, RewardKind kind) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(TOP, RewardsMenu.kind(view, kind, false));
        List<Reward> rewards = view.data().rewards(kind);
        int[] slots = slots(Math.min(rewards.size(), 28));
        for (int i = 0; i < slots.length; i++) icons.put(slots[i], icon(rewards.get(i), view));
        if (kind == RewardKind.EMBLEM) {
            icons.put(EMBLEMS, new Icon(Material.NAME_TAG, "&aPrefix Emblems", "&7Add some spice by having an emblem",
                    "&7next to your name in chat and in tab!", "", "&7Emblems are unlocked through", "&7various activities such as leveling up",
                    "&7or completing achievements!", "", "&7Emblems also show important data", "&7associated with them in chat!", "", "&eClick to view!"));
        }
        return icons;
    }

    /**
     * A reward's item: its level, what it says (a prefix colour's preview with their own rank and name, a bonus's
     * text with its size at their level) and where it stands for them.
     */
    static Icon icon(Reward reward, LevelingView view) {
        List<String> lore = new ArrayList<>(List.of("&8Level " + reward.level(), ""));
        if (reward.kind() == RewardKind.PREFIX) {
            lore.addAll(List.of("&7Preview: &8[&" + reward.color() + reward.level() + "&8] " + view.rank().getPrefix() + view.name(), ""));
        }
        for (String paragraph : reward.text()) {
            lore.addAll(LevelingText.wrap(paragraph.replace("{bonus}", bonus(view.level()))));
            lore.add("");
        }
        if (!reward.lore().isEmpty()) {
            lore.addAll(reward.lore());
            lore.add("");
        }
        lore.addAll(LevelingText.state(reward.level(), view.total()));
        return reward.look().icon(reward.name(), lore);
    }

    /** "0.9": a bonus's size in percent at this level (0.88 at 88, shown to one decimal as recorded). */
    static String bonus(int level) {
        return SkillText.number(BONUS_PER_LEVEL * Math.min(level, BONUS_MAX_LEVEL));
    }
}
