package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import net.icxd.dungeons.common.DungeonFloor;

/**
 * What a secret gives its finder (the wiki's Dungeon Secrets, "List of Secret Rewards"): a blessing for the
 * team (chests and bats) or a dungeon item. The wiki has no chances ("expand on the loot pools" is on its own
 * to-do list), so they're the simplest that fit what was recorded: of the four secret chests opened on the
 * Entrance, three held a blessing (Stone I, Wisdom II, Life I) and one a Defuse Kit, so a chest (and a bat,
 * never recorded) holds a blessing 3 times in 4, of a kind and level (I or II) picked evenly. All three items
 * recorded on the Entrance (that chest's and two on the floor) were Defuse Kits, so there it's always one;
 * on the other floors, never recorded, one of the source's items picked evenly. Items by id; the Healing VIII
 * splash potion and the Secret Dye (1 in a million) aren't items here, so they're left out.
 */
final class SecretRewards {
    enum Source { CHEST, BAT, ITEM }

    /** A blessing ({@code blessing} and {@code level}) or an item ({@code item}, an id). */
    record Reward(Blessing blessing, int level, String item) {
        boolean isBlessing() {
            return blessing != null;
        }
    }

    /** UNKNOWN; fitted to the four recorded secret chests (three blessings). */
    static final double BLESSING_CHANCE = 0.75;
    /** UNKNOWN; fitted to the Entrance's three recorded item rewards, all of them this. */
    static final String ENTRANCE_ITEM = "DEFUSE_KIT";

    private SecretRewards() {
    }

    /** The items this source can give on this floor. */
    static List<String> items(Source source, DungeonFloor floor) {
        List<String> items = new ArrayList<>(List.of("DUNGEON_DECOY", "DUNGEON_TRAP", "TRAINING_WEIGHTS", "SPIRIT_LEAP", "INFLATABLE_JERRY",
                "DEFUSE_KIT"));
        if (source != Source.BAT) items.add("CANDYCOMB");
        if (source == Source.CHEST) items.add("ARCHITECT_FIRST_DRAFT");
        if (floor.getNumber() >= 4) {
            items.add("DUNGEON_CHEST_KEY");
            items.add("TREASURE_TALISMAN");
        }
        return items;
    }

    static Reward roll(Source source, DungeonFloor floor, RandomGenerator random) {
        if (source != Source.ITEM && random.nextDouble() < BLESSING_CHANCE) {
            return new Reward(Blessing.random(random), 1 + random.nextInt(2), null);
        }
        if (floor == DungeonFloor.ENTRANCE) return new Reward(null, 0, ENTRANCE_ITEM);
        List<String> items = items(source, floor);
        return new Reward(null, 0, items.get(random.nextInt(items.size())));
    }
}
