package net.icxd.dungeons.quest;

import java.util.List;

import org.bukkit.Material;

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
     * "Use the teleporter" came without a click or hover), and what it gives when it's done (null for
     * nothing, as for all of Getting Started's).
     */
    public record Objective(String text, boolean linked, Reward reward) {
        public Objective(String text, boolean linked) {
            this(text, linked, null);
        }

        /** As the Quest Log and the chat's hovers write it: a sentence ("Break a log."). */
        public String logText() {
            return text.endsWith(".") ? text : text + ".";
        }
    }

    /**
     * What an objective gives, said under OBJECTIVE COMPLETE (QUEST COMPLETE for the last): SkyBlock XP,
     * which SkyBlock Leveling counts as Complete Objectives (Saving Up's "Talk to the Banker": +5), or
     * coins into the purse (its "Deposit Coins in the Bank": +10), as recorded.
     */
    public record Reward(int skyBlockXp, double coins) {
        public static Reward skyBlockXp(int xp) {
            return new Reward(xp, 0);
        }

        public static Reward coins(double coins) {
            return new Reward(0, coins);
        }
    }
}
