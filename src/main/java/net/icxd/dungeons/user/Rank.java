package net.icxd.dungeons.user;

import net.icxd.dungeons.utils.Utils;

public enum Rank {
    DEFAULT("&7", "&7", 0, 'Z'),
    VIP_PLUS("&a[VIP&6+&a] ", "&a", 1, 'H'),
    MVP_PLUS("&b[MVP&6+&b] ", "&b", 2, 'G'),
    MVP_PLUS_PLUS("&6[MVP&f++&6] ", "&6", 3, 'F'),
    YOUTUBE("&c[&fYOUTUBE&c] ", "&c", 4, 'E'),
    STAFF("&c[&6ዞ&c] ", "&c", 5, 'A');

    private String prefix;
    private String color;
    private final int priority;
    private final char character;

    Rank(String prefix, String color, int priority, char c) {
        this.prefix = Utils.color(prefix);
        this.color = Utils.color(color);
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

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getColor() {
        return this.color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getPriority() {
        return this.priority;
    }

    public char getCharacter() {
        return this.character;
    }
}
