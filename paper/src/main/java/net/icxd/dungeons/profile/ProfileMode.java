package net.icxd.dungeons.profile;

/**
 * A profile's game mode, fixed when it's made (staff can change it with {@code /pd <player> mode}).
 * Classic is Hypixel's; Sandbox is this server's own special mode, where anyone may use the item
 * browser and the commands that edit the held item, and whose items stay on the profile.
 */
public enum ProfileMode {
    NORMAL("", ""),
    SANDBOX("&d⚒ Sandbox ", " &b(Sandbox)");

    /** Before "Profile: Kiwi" in the menus, as Hypixel shows "♲ Ironman ". */
    private final String prefix;
    /** After the name in "You are playing on profile: Kiwi (Sandbox)", as Hypixel's "(Type)". */
    private final String suffix;

    ProfileMode(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String prefix() {
        return prefix;
    }

    public String suffix() {
        return suffix;
    }

    /** NORMAL for anything that isn't a mode (a profile made before modes, or a typo in the database). */
    public static ProfileMode parse(Object value) {
        if (value instanceof String s) {
            for (ProfileMode mode : values()) if (mode.name().equalsIgnoreCase(s)) return mode;
        }
        return NORMAL;
    }
}
