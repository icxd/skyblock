package net.icxd.dungeons.dungeons.instance;

/**
 * Hypixel's chat lines for deaths and revives in a dungeon. No recording has one: these are the
 * legacy-coloured shapes the mods match (Skytils' {@code DungeonListener} death, reconnect and revive
 * patterns, BetterMap's, SkyHanni's list of death reasons and its fairy lines; research critic.md 3.1).
 */
final class DeathText {
    /** Why someone died: how Hypixel words it for yourself and for someone else. */
    enum Reason {
        KILLED_BY("You were killed by ", " was killed by "),
        CRUSHED("You were crushed", " was crushed"),
        SUFFOCATED("You suffocated", " suffocated"),
        DEEP_HOLE("You fell into a deep hole", " fell into a deep hole"),
        TRAP("You died to a trap", " died to a trap"),
        MOB("You died to a mob", " died to a mob"),
        BURNT("You burnt to death", " burnt to death"),
        /** Only others see it ("Someone fell to their death with help from Someone"); yourself: UNKNOWN, "You died". */
        FELL_WITH_HELP("You died", " fell to their death with help from "),
        /** Only others see it: the player is gone. */
        DISCONNECTED("You disconnected from the Dungeon", " disconnected from the Dungeon"),
        OTHER("You died", " died");

        final String self;
        final String other;

        Reason(String self, String other) {
            this.self = self;
            this.other = other;
        }

        /** Whether the words go on with who did it. */
        boolean namesKiller() {
            return this == KILLED_BY || this == FELL_WITH_HELP;
        }
    }

    private DeathText() {
    }

    /**
     * Why they died, from the vanilla damage type that finished them ("fall", "lava", "generic" when a
     * SkyBlock hit did) and their last SkyBlock hit: {@code hitNow} if one took the last of their health,
     * with {@code trap} if nothing was behind it (a trap's share of max health), {@code hitLately} if a
     * mob hit them in the seconds before (a fall "with help from" it).
     */
    static Reason reason(String damageType, boolean hitNow, boolean trap, boolean hitLately) {
        if (hitNow) return trap ? Reason.TRAP : Reason.KILLED_BY;
        return switch (damageType) {
            case "fall", "fly_into_wall", "ender_pearl" -> hitLately ? Reason.FELL_WITH_HELP : Reason.OTHER;
            case "out_of_world", "outside_border" -> Reason.DEEP_HOLE;
            case "in_fire", "on_fire", "lava", "hot_floor", "campfire", "fireball", "unattributed_fireball" -> Reason.BURNT;
            case "in_wall", "cramming" -> Reason.SUFFOCATED;
            case "falling_block", "falling_anvil", "falling_stalactite", "stalagmite" -> Reason.CRUSHED;
            case "mob_attack", "mob_attack_no_aggro", "mob_projectile", "arrow", "trident", "explosion", "sonic_boom", "sting", "thorns",
                 "indirect_magic", "wither_skull" -> Reason.KILLED_BY;
            // "generic" (a SkyBlock death with no hit to blame), drowning, cactus, /kill...
            default -> Reason.OTHER;
        };
    }

    /**
     * "&c ☠ &7You were killed by Zombie Grunt and became a ghost&7." for the one who died, and for the
     * others "&c ☠ &7&bAlice&r&7 was killed by Zombie Grunt and became a ghost&7." ({@code name} in its
     * rank's colour, without the rank: the mods' patterns take colour codes, then the name). The
     * killer's colour is UNKNOWN: it's left grey.
     */
    static String death(Reason reason, String killer, boolean self, String name) {
        Reason words = reason;
        // With nobody to name: a kill is a mob's, a fall just a death.
        if (reason.namesKiller() && killer == null) words = reason == Reason.KILLED_BY ? Reason.MOB : Reason.OTHER;
        String by = words.namesKiller() ? killer : "";
        String what = self ? words.self + by : name + "&r&7" + words.other + by;
        return "&c ☠ &7" + what + " and became a ghost&7.";
    }

    /** "&c ☠ &7&bAlice &7reconnected&7.": a ghost back in the run. */
    static String reconnected(String name) {
        return "&c ☠ &7" + name + " &7reconnected&7.";
    }

    /**
     * "&a ❣ &7&bAlice&r&a was revived!". What follows "was revived" (by whom) is UNKNOWN; nothing does
     * here.
     */
    static String revived(String name) {
        return "&a ❣ &7" + name + "&r&a was revived!";
    }

    /** A Revive Stone with nobody to revive (MCW Revive Stone, "Game Messages"). */
    static final String NOBODY_TO_REVIVE = "&cThere are no players available to revive right now!";

    /** What a Fairy says when killed (SkyHanni's {@code DungeonChatFilter}). */
    static String fairyGivesStone(String fairy) {
        return "&d" + fairy + " the Fairy&f: You killed me! Take this &6Revive Stone &fso that my death is not in vain!";
    }

    static String fairyRevivesYou(String fairy) {
        return "&d" + fairy + " the Fairy&f: You killed me! I'll revive you so that my death is not in vain!";
    }

    static String fairyRevivesFriend(String fairy, String name) {
        return "&d" + fairy + " the Fairy&f: You killed me! I'll revive your friend &r" + name + " &fso that my death is not in vain!";
    }

    static String fairyGoodbye(String fairy) {
        return "&d" + fairy + " the Fairy&f: Have a great life!";
    }
}
