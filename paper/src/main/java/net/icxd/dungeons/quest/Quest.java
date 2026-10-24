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
     * One of its steps: what the boss bar, sidebar and chat call it ("Break a log"), and whether its
     * NEW OBJECTIVE lines take you to the Quest Log (two of Getting Started's don't: "Talk to Jerry" and
     * "Use the teleporter" came without a click or hover).
     */
    public record Objective(String text, boolean linked) {
        /** As the Quest Log and the chat's hovers write it: a sentence ("Break a log."). */
        public String logText() {
            return text.endsWith(".") ? text : text + ".";
        }
    }
}
