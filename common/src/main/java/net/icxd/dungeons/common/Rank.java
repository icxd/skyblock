package net.icxd.dungeons.common;

/** Chat ranks, with their prefix and colour in section-sign codes. */

public enum Rank {
    DEFAULT("&7", "&7", 0, 'Z'),
    VIP_PLUS("&a[VIP&6+&a] ", "&a", 1, 'H'),
    MVP_PLUS("&b[MVP&6+&b] ", "&b", 2, 'G'),
    MVP_PLUS_PLUS("&6[MVP&f++&6] ", "&6", 3, 'F'),
    YOUTUBE("&c[&fYOUTUBE&c] ", "&c", 4, 'E'),
    STAFF("&c[&6ዞ&c] ", "&c", 5, 'A');

    private final String prefix;
    private final String color;
    private final int priority;
    private final char character;

    Rank(String prefix, String color, int priority, char c) {
        this.prefix = prefix.replace('&', '\u00a7');
        this.color = color.replace('&', '\u00a7');
        this.priority = priority;
        this.character = c;
    }

    public boolean isEqualOrStrongerThan(Rank rank) {
        return this.priority >= rank.priority;
    }

    public static Rank getRank(String rank) {
        for (Rank r : Rank.values()) {
            if (!r.name().equalsIgnoreCase(rank)) continue;
            return r;
        }
        return DEFAULT;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getColor() {
        return this.color;
    }

    public int getPriority() {
        return this.priority;
    }

    public char getCharacter() {
        return this.character;
    }
}
