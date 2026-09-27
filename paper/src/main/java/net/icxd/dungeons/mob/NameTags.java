package net.icxd.dungeons.mob;

import net.icxd.dungeons.utils.Utils;

import java.util.List;

/** What mobs' name tags say, with {@code &} colours. */
public final class NameTags {
    /**
     * Past this a miniboss's health is shown short (130k): the recordings show "130k" for 130,000 and
     * "12,750" for 12,750, and nothing between, so where it switches is UNKNOWN.
     */
    static final double SHORT_FROM = 100_000;

    private NameTags() {
    }

    /** The Hub's: "[Lv75] Magma Cube 1M/1M❤", and a boss's framed in ﴾ ﴿. */
    public static String hub(int level, String name, double health, double maxHealth, boolean boss) {
        String tag = "&8[&7Lv" + level + "&8] &c" + name + " &a" + Utils.formatNumber(shown(health)) + "&f/&a" + Utils.formatNumber(maxHealth)
                + "&c❤";
        return boss ? "&e﴾ " + tag + " &e﴿" : tag;
    }

    /**
     * A dungeon mob's, as recorded on Hypixel (research mobs.md 1.6): its mob types' symbols in their
     * colours, "✯" if it's starred, its name (in its modifier's colour, after the modifier), and its
     * health in full with commas, green (or the modifier's second colour), yellow at 0:
     * "༕ ✯ Speedy Tank Zombie 1,200❤". No level and no max, except in the {@code LEVELED} style (the
     * Undead Skeleton's "[Lv40] ... 25,000/25,000❤"); a miniboss's name is bold light purple and its big
     * health short ("130k").
     */
    public static String dungeon(MobKind.NameStyle style, List<MobType> types, String name, int level, boolean starred, Modifier modifier,
                                 double health, double maxHealth) {
        StringBuilder tag = new StringBuilder();
        if (style == MobKind.NameStyle.LEVELED) tag.append("&8[&7Lv").append(level).append("&8] ");
        StringBuilder symbols = new StringBuilder();
        for (MobType type : types) {
            if (!type.symbol().isEmpty()) symbols.append('&').append(type.color()).append(type.symbol());
        }
        if (!symbols.isEmpty()) tag.append(symbols).append(' ');
        if (starred) tag.append("&6✯ ");
        tag.append("&c");
        if (modifier != null) tag.append('&').append(modifier.nameColor()).append(modifier.displayName()).append(' ');
        else if (style == MobKind.NameStyle.MINIBOSS) tag.append("&d&l");
        tag.append(name).append(' ');
        double shown = shown(health);
        tag.append(shown <= 0 ? "&e" : "&" + (modifier == null ? 'a' : modifier.healthColor()));
        tag.append(number(style, shown));
        if (style == MobKind.NameStyle.LEVELED) tag.append("&f/&a").append(number(style, maxHealth));
        return tag.append("&c❤").toString();
    }

    /** Whole health, rounded up (a mob with a sliver left isn't shown dead); never below 0. */
    private static double shown(double health) {
        return Math.max(0, Math.ceil(health));
    }

    private static String number(MobKind.NameStyle style, double value) {
        if (style == MobKind.NameStyle.MINIBOSS && value >= SHORT_FROM) return Utils.formatNumber(value);
        return Utils.getFormattedNumber((int) Math.min(Integer.MAX_VALUE, value));
    }
}
