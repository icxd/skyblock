package net.icxd.dungeons.quest;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * An objective's reward, given and said as Saving Up's were (recorded 2026-10-24): OBJECTIVE COMPLETE
 * (QUEST COMPLETE for the last), which opens the Quest Log and has the quest's progress as its hover,
 * then REWARD and what it gave, with a firework's launch. SkyBlock XP comes with a level-up's sound
 * (its line opens /levels; SkyBlock Leveling counts it as Complete Objectives), coins go into the
 * purse (their line's hover says so). Main thread.
 */
final class QuestRewards {
    /** The coins line's hover, as recorded. */
    static final String COINS_HOVER = "&7These &6Coins &7have been credited to\n&7your &6Purse&7.\n\n&7To keep them safe and earn interest\n"
            + "&7over time, deposit them at the &6Bank&7!";

    private QuestRewards() {
    }

    /** Gives it for this objective, done: {@code done} of the quest's objectives are now, {@code completed} if that was the last. */
    static void give(Player player, User user, Quest quest, int done, boolean completed, Quest.Reward reward) {
        if (reward.coins() > 0) Purse.add(user, reward.coins());
        if (reward.skyBlockXp() > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
            SkyBlockLevels.changed(player);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT, 1, 1);
        for (Component line : lines(quest, done, completed, reward)) player.sendMessage(line);
    }

    /** A blank, the headline, a blank, "REWARD", what it gave, a blank. */
    static List<Component> lines(Quest quest, int done, boolean completed, Quest.Reward reward) {
        Component hover = Text.line(String.join("\n", QuestText.hover(quest, done)));
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("  ").append(Text.line(completed ? "&6&lQUEST COMPLETE" : "&6&lOBJECTIVE COMPLETE")
                .clickEvent(ClickEvent.runCommand("/questlog")).hoverEvent(HoverEvent.showText(hover))));
        lines.add(Component.empty());
        lines.add(Text.line("    &a&lREWARD"));
        if (reward.skyBlockXp() > 0) {
            lines.add(Component.text("    ").append(Text.line(skyBlockXpLine(reward.skyBlockXp()))
                    .clickEvent(ClickEvent.runCommand("/levels")).hoverEvent(HoverEvent.showText(Text.line("&8+&b" + reward.skyBlockXp() + " SkyBlock XP")))));
        }
        if (reward.coins() > 0) {
            lines.add(Component.text("    ").append(Text.line(coinsLine(reward.coins())).hoverEvent(HoverEvent.showText(Text.line(COINS_HOVER)))));
        }
        lines.add(Component.empty());
        return lines;
    }

    /** "&7&8+&b5 SkyBlock XP". */
    static String skyBlockXpLine(int xp) {
        return "&7&8+&b" + xp + " SkyBlock XP";
    }

    /** "&7&8+&610 &7Coins". */
    static String coinsLine(double coins) {
        return "&7&8+&6" + Coins.format(coins) + " &7Coins";
    }
}
