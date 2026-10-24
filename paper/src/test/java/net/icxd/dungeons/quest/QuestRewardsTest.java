package net.icxd.dungeons.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Against the recording (2026-10-24): Saving Up's two rewards, as chat said them. */
class QuestRewardsTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** 05:17.8, with the Banker's last line. */
    @Test
    void talkingToTheBanker() {
        Quest.Reward reward = Quests.SAVING_UP.objective(Quests.TALK_TO_THE_BANKER).reward();
        assertEquals(new Quest.Reward(5, 0), reward);
        List<Component> lines = QuestRewards.lines(Quests.SAVING_UP, 1, false, reward);
        assertEquals(List.of("", "  OBJECTIVE COMPLETE", "", "    REWARD", "    +5 SkyBlock XP", ""), lines.stream().map(PLAIN::serialize).toList());
        Component headline = lines.get(1).children().getFirst();
        assertEquals(ClickEvent.runCommand("/questlog"), headline.clickEvent());
        assertEquals("Quest: Saving Up\n\n ✔ Talk to the Banker.\n ✖ Deposit Coins in the Bank.\n\nClick to view your Quest Log!", hover(headline));
        Component xp = lines.get(4).children().getFirst();
        assertEquals(ClickEvent.runCommand("/levels"), xp.clickEvent());
        assertEquals("+5 SkyBlock XP", hover(xp));
        assertEquals("&7&8+&b5 SkyBlock XP", QuestRewards.skyBlockXpLine(5));
    }

    /** 06:36.3, the first deposit: the last objective, so the quest's. */
    @Test
    void theFirstDeposit() {
        Quest.Reward reward = Quests.SAVING_UP.objective(Quests.DEPOSIT_COINS).reward();
        assertEquals(new Quest.Reward(0, 10), reward);
        List<Component> lines = QuestRewards.lines(Quests.SAVING_UP, 2, true, reward);
        assertEquals(List.of("", "  QUEST COMPLETE", "", "    REWARD", "    +10 Coins", ""), lines.stream().map(PLAIN::serialize).toList());
        assertEquals("Quest: Saving Up\n\n ✔ Talk to the Banker.\n ✔ Deposit Coins in the Bank.\n\nClick to view your Quest Log!",
                hover(lines.get(1).children().getFirst()));
        Component coins = lines.get(4).children().getFirst();
        assertNull(coins.clickEvent());
        assertEquals("These Coins have been credited to\nyour Purse.\n\nTo keep them safe and earn interest\nover time, deposit them at the Bank!",
                hover(coins));
        assertEquals("&7&8+&610 &7Coins", QuestRewards.coinsLine(10));
    }

    @Test
    void gettingStartedGivesNothing() {
        for (Quest.Objective objective : Quests.GETTING_STARTED.objectives()) assertNull(objective.reward(), objective.text());
    }

    private static String hover(Component line) {
        HoverEvent<?> hover = line.hoverEvent();
        return PLAIN.serialize((Component) hover.value());
    }
}
