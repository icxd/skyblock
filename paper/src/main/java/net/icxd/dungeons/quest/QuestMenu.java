package net.icxd.dungeons.quest;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.SkyBlockMenu;
import net.icxd.dungeons.quest.QuestMenus.Layout;
import net.icxd.dungeons.user.User;

/**
 * The quest menus (see {@link QuestMenus} for what they show): Quests & Chapters, Chapters, the Quest
 * Log, Completed Quests and the Fairy Souls Guide, linked as recorded. Each opens with a page turning, as the
 * recording's did, but going back, which clicks like any menu's Go Back (UNKNOWN for Completed Quests: a page),
 * and the guide, which clicks. Chapters and the Dungeon Journals show but do nothing yet (neither is here).
 * Main thread.
 */
public final class QuestMenu extends GUI {
    private final Player viewer;
    private final Layout layout;
    private final Map<Integer, Runnable> buttons = new HashMap<>();

    private QuestMenu(String title, Player viewer, Layout layout) {
        super(title, Size.SIX);
        this.viewer = viewer;
        this.layout = layout;
    }

    public static void questsAndChapters(Player viewer) {
        questsAndChapters(viewer, MenuSound.PAGE);
    }

    private static void questsAndChapters(Player viewer, MenuSound sound) {
        QuestMenu menu = new QuestMenu(QuestMenus.QUESTS_AND_CHAPTERS, viewer, QuestMenus.questsAndChapters());
        menu.buttons.put(QuestMenus.CHAPTERS_SLOT, () -> chapters(viewer));
        menu.buttons.put(QuestMenus.QUEST_LOG_SLOT, () -> questLog(viewer));
        menu.buttons.put(QuestMenus.BACK, () -> new SkyBlockMenu(viewer).open(viewer));
        menu.open(viewer, sound);
    }

    public static void chapters(Player viewer) {
        QuestMenu menu = new QuestMenu(QuestMenus.CHAPTERS, viewer, QuestMenus.chapters());
        menu.buttons.put(QuestMenus.BACK, () -> questsAndChapters(viewer, MenuSound.CLICK));
        menu.open(viewer, MenuSound.PAGE);
    }

    public static void questLog(Player viewer) {
        questLog(viewer, MenuSound.PAGE);
    }

    private static void questLog(Player viewer, MenuSound sound) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        QuestMenu menu = new QuestMenu(QuestMenus.QUEST_LOG, viewer, QuestMenus.questLog(user.profile()));
        menu.buttons.put(QuestMenus.BACK, () -> questsAndChapters(viewer, MenuSound.CLICK));
        menu.buttons.put(QuestMenus.COMPLETED, () -> completedQuests(viewer));
        menu.buttons.put(QuestMenus.FAIRY_SOULS_SLOT, () -> fairySoulsGuide(viewer));
        menu.open(viewer, sound);
    }

    /** It opens with a click, as recorded (05:35.5); going back turns a page (05:36.5). */
    static void fairySoulsGuide(Player viewer) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        QuestMenu menu = new QuestMenu(QuestMenus.FAIRY_SOULS_GUIDE, viewer, QuestMenus.fairySoulsGuide(user.profile()));
        menu.buttons.put(QuestMenus.BACK, () -> questLog(viewer, MenuSound.PAGE));
        menu.open(viewer, MenuSound.CLICK);
    }

    static void completedQuests(Player viewer) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        QuestMenu menu = new QuestMenu(QuestMenus.COMPLETED_QUESTS, viewer, QuestMenus.completedQuests(user.profile()));
        menu.buttons.put(QuestMenus.BACK, () -> questLog(viewer, MenuSound.CLICK));
        menu.open(viewer, MenuSound.PAGE);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        for (int slot : layout.glass()) set(slot, filler());
        layout.icons().forEach((slot, icon) -> {
            Runnable action = buttons.get(slot);
            if (slot == QuestMenus.CLOSE) set(GUIClickableItem.close(slot));
            else if (action != null) set(GUIClickableItem.button(slot, icon.stack(), viewer, action));
            else set(slot, icon.stack());
        });
    }
}
