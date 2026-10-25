package net.icxd.dungeons.quest;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.skill.Skill;

/**
 * A quest, as Hypixel's Quest Log lists it: its name, its objectives in order, where it's followed in
 * the boss bar and sidebar ({@link Home}), and the quest that starts when it's done (Getting Started
 * leads to Marco the Artist).
 *
 * @param icon the Quest Log's item for it (paper for most)
 * @param next the id of the quest it leads to; null for none
 */
public record Quest(String id, String name, Material icon, Home home, List<Objective> objectives, String next) {
    public Quest {
        objectives = List.copyOf(objectives);
    }

    public Objective objective(int index) {
        return objectives.get(index);
    }

    /**
     * One of its steps: what the boss bar, sidebar and chat call it ("Break a log"), whether its
     * NEW OBJECTIVE lines take you to the Quest Log (two of Getting Started's don't: "Talk to Jerry" and
     * "Use the teleporter" came without a click or hover), what it gives when it's done (null for
     * nothing, as for all of Getting Started's), what it counts towards (null for nothing: "Kill
     * Zombies (3/10)"), and where it's followed if not where its quest is (null: as its quest; Saving
     * Up's "Deposit Coins in the Bank" is, in the Bank, and its "Talk to the Banker" isn't).
     */
    public record Objective(String text, boolean linked, Reward reward, Goal goal, Home home) {
        public Objective(String text, boolean linked) {
            this(text, linked, null, null, null);
        }

        public Objective(String text, boolean linked, Reward reward) {
            this(text, linked, reward, null, null);
        }

        public Objective counting(Goal goal) {
            return new Objective(text, linked, reward, goal, home);
        }

        public Objective followed(Home home) {
            return new Objective(text, linked, reward, goal, home);
        }

        /** As the Quest Log and the chat's hovers write it: a sentence ("Break a log."). */
        public String logText() {
            return text.endsWith(".") ? text : text + ".";
        }
    }

    /**
     * What an objective counts towards, shown with it, as recorded (2026-10-25): kills ("Kill Zombies
     * (0/10)", counted on the profile), a skill's XP against a level's ("Reach Combat Level I
     * (150/50 XP)", read from the profile; done when next checked, silently), or what to bring (the
     * sidebar's " Rose Red x5" under "Give Rose Red Dye to Marco"; the NPC takes it).
     */
    public record Goal(Kind kind, int target, Skill skill, String item) {
        public enum Kind {
            COUNT, SKILL_XP, ITEMS
        }

        public static Goal count(int target) {
            return new Goal(Kind.COUNT, target, null, null);
        }

        public static Goal skillXp(Skill skill, int xp) {
            return new Goal(Kind.SKILL_XP, xp, skill, null);
        }

        public static Goal items(String item, int count) {
            return new Goal(Kind.ITEMS, count, null, item);
        }
    }

    /**
     * What an objective gives, said under OBJECTIVE COMPLETE (QUEST COMPLETE for the last), as recorded:
     * SkyBlock XP, which SkyBlock Leveling counts as Complete Objectives (Saving Up's "Talk to the
     * Banker": +5), coins into the purse (its "Deposit Coins in the Bank": +10), a skill's XP ("+100
     * Combat Experience" for Time to Strike's zombies), or an item to claim ({@code claim}, the chat
     * line's {@code /claimitem artist:SPRAY_CAN}: Marco's Spray Can, the Bartender's Undead Sword).
     */
    public record Reward(int skyBlockXp, double coins, String item, String claim, Skill skill, double skillXp) {
        public static Reward skyBlockXp(int xp) {
            return new Reward(xp, 0, null, null, null, 0);
        }

        public static Reward coins(double coins) {
            return new Reward(0, coins, null, null, null, 0);
        }

        /** An item to claim, under this key ("bartender:UNDEAD_SWORD"), whose part after the colon is its id. */
        public static Reward item(String claim) {
            return new Reward(0, 0, claim.substring(claim.indexOf(':') + 1), claim, null, 0);
        }

        public Reward andSkillXp(Skill skill, double xp) {
            return new Reward(skyBlockXp, coins, item, claim, skill, xp);
        }
    }
}
