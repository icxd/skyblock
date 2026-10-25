package net.icxd.dungeons.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.skill.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Against the recordings: Saving Up's two rewards (2026-10-24), Marco's and the Bartender's (10-25), as chat said them. */
class QuestRewardsTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** 05:17.8, with the Banker's last line. */
    @Test
    void talkingToTheBanker() {
        Quest.Reward reward = Quests.SAVING_UP.objective(Quests.TALK_TO_THE_BANKER).reward();
        assertEquals(Quest.Reward.skyBlockXp(5), reward);
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
        assertEquals(Quest.Reward.coins(10), reward);
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

    /** 01:17.7: Marco's Spray Can, to claim, its line opening the Claim Reward menu again. */
    @Test
    void marcosSprayCan() {
        Quest.Reward reward = Quests.MARCO_THE_ARTIST.objective(Quests.GIVE_ROSE_RED).reward();
        assertEquals(Quest.Reward.item("artist:SPRAY_CAN"), reward);
        assertEquals("SPRAY_CAN", reward.item());
        PrivateData.load();
        try {
            List<Component> lines = QuestRewards.lines(Quests.MARCO_THE_ARTIST, 2, true, reward);
            assertEquals(List.of("", "  QUEST COMPLETE", "", "    REWARD", "    Spray Can", ""), plain(lines));
            Component item = lines.get(4).children().getFirst();
            assertEquals(ClickEvent.runCommand("/claimitem artist:SPRAY_CAN"), item.clickEvent());
            assertEquals("Spray Can\n\nClick to claim!", hover(item));
            assertEquals("&fSpray Can", QuestRewards.itemName("SPRAY_CAN"));
        } finally {
            PrivateData.unload();
        }
    }

    /** 02:21.7, 03:00.2 and 03:30.8: the Bartender's sword, the ten zombies' two (SkyBlock XP first), and his coins. */
    @Test
    void timeToStrike() {
        Quest.Reward sword = Quests.TIME_TO_STRIKE.objective(Quests.TALK_TO_THE_BARTENDER).reward();
        assertEquals("bartender:UNDEAD_SWORD", sword.claim());
        assertEquals(ClickEvent.runCommand("/claimitem bartender:UNDEAD_SWORD"),
                QuestRewards.lines(Quests.TIME_TO_STRIKE, 1, false, sword).get(4).children().getFirst().clickEvent());

        Quest.Reward zombies = Quests.TIME_TO_STRIKE.objective(Quests.KILL_ZOMBIES).reward();
        assertEquals(Quest.Reward.skyBlockXp(5).andSkillXp(Skill.COMBAT, 100), zombies);
        assertEquals(List.of("", "  OBJECTIVE COMPLETE", "", "    REWARDS", "    +5 SkyBlock XP", "    +100 Combat Experience", ""),
                plain(QuestRewards.lines(Quests.TIME_TO_STRIKE, 2, false, zombies)));
        assertEquals("&7&8+&3100 &7Combat Experience", QuestRewards.skillXpLine(zombies));

        Quest.Reward coins = Quests.TIME_TO_STRIKE.objective(Quests.TALK_TO_THE_BARTENDER_AGAIN).reward();
        assertEquals(List.of("", "  OBJECTIVE COMPLETE", "", "    REWARD", "    +100 Coins", ""),
                plain(QuestRewards.lines(Quests.TIME_TO_STRIKE, 3, false, coins)));
        // Reaching Combat Level I gives nothing (it's done without a word).
        assertNull(Quests.TIME_TO_STRIKE.objective(Quests.REACH_COMBAT_I).reward());
    }

    @Test
    void gettingStartedGivesNothing() {
        for (Quest.Objective objective : Quests.GETTING_STARTED.objectives()) assertNull(objective.reward(), objective.text());
    }

    private static List<String> plain(List<Component> lines) {
        return lines.stream().map(PLAIN::serialize).toList();
    }

    private static String hover(Component line) {
        HoverEvent<?> hover = line.hoverEvent();
        return PLAIN.serialize((Component) hover.value());
    }
}
