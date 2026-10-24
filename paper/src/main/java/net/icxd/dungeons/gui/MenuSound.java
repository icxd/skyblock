package net.icxd.dungeons.gui;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * The sounds Hypixel's menus make as they open, as recorded (2026-10-24), both at half volume: a
 * lever's click as one menu opens another (every move between menus, Go Back too), and a page
 * turning as the quest book's open from outside it (Quests & Chapters, Chapters, the Quest Log).
 * One opened from the world (by an NPC, a crafting table) makes neither.
 */
public enum MenuSound {
    CLICK(Sound.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS),
    PAGE(Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER);

    private final Sound sound;
    private final SoundCategory category;

    MenuSound(Sound sound, SoundCategory category) {
        this.sound = sound;
        this.category = category;
    }

    public void play(Player player) {
        player.playSound(player.getLocation(), sound, category, 0.5f, 1f);
    }
}
