package net.icxd.dungeons.item.enchanting;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An enchantment, by what items store it as ({@link #getNamespace}, "sharpness", "one_for_all"): what Hypixel says
 * about it is in {@link EnchantmentData} (the Hex's private table), read when asked, so every enchantment there is
 * exists as soon as the table is in. What code does for one goes by its id (Damage, KillCoins, SkillGains, Shots,
 * DragonSets); the rest are text, and the stats their text grants ({@link #getStats}).
 */
public final class EnchantmentType {
    /** The ones code refers to. */
    public static final EnchantmentType SCAVENGER = new EnchantmentType("scavenger");

    private final String namespace;

    private EnchantmentType(String namespace) {
        this.namespace = namespace;
    }

    /** The enchantment by any name it goes by (the plugin's id, Hypixel's), in any case; null if the table has none. */
    public static EnchantmentType getByNamespace(String namespace) {
        EnchantmentData.Entry entry = EnchantmentData.current().get(namespace);
        return entry == null ? null : new EnchantmentType(entry.id());
    }

    /** Every enchantment in the table. */
    public static Collection<EnchantmentType> all() {
        return EnchantmentData.current().all().stream().map(e -> new EnchantmentType(e.id())).toList();
    }

    /** What items store it as. */
    public String getNamespace() {
        return namespace;
    }

    /** What the table says about it; null if it has nothing. */
    public EnchantmentData.Entry data() {
        return EnchantmentData.current().get(namespace);
    }

    /** Hypixel's name for it now (Syphon is Drain); its id's words while the table doesn't have it. */
    public String getName() {
        EnchantmentData.Entry data = data();
        if (data != null) return data.name();
        StringBuilder name = new StringBuilder();
        for (String word : namespace.split("_")) {
            if (word.isEmpty()) continue;
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    public boolean isUltimate() {
        EnchantmentData.Entry data = data();
        return data != null && data.ultimate();
    }

    /** The highest level the Hex offers; 0 if the table doesn't have it. */
    public int getMaxLevel() {
        EnchantmentData.Entry data = data();
        return data == null ? 0 : data.max();
    }

    /** Whether it goes on this item (the kinds its books name). */
    public boolean appliesTo(SkyBlockItem item) {
        EnchantmentData.Entry data = data();
        return data != null && data.appliesTo(item);
    }

    /** Hypixel's description at a level, as one paragraph with & colours; null if there's none for it. */
    public String getDescription(int level) {
        EnchantmentData.Entry data = data();
        EnchantmentData.Level at = data == null ? null : data.level(level);
        return at == null ? null : at.text();
    }

    /** The book's own lines at a level (as the Hex's menus show it); null if there's no text for it. */
    public List<String> getLines(int level) {
        EnchantmentData.Entry data = data();
        EnchantmentData.Level at = data == null ? null : data.level(level);
        return at == null ? null : at.lines();
    }

    /**
     * The stats it grants at a level, read from its description ("Grants +75 ❤ Health.", Absorb's "Grants +1☯
     * Foraging Wisdom and +2☘ Foraging Fortune.", Critical's "Increases ☠ Crit Damage by 10%.", Overload's two);
     * nothing if it grants none, or only sometimes ("against explosions", "while out of combat").
     */
    public Stats getStats(int level) {
        return stats(getDescription(level));
    }

    /** What a description's text grants (see {@link #getStats}); nothing for null. */
    static Stats stats(String text) {
        Stats stats = new Stats();
        if (text == null) return stats;
        Matcher m = GRANTS.matcher(text);
        if (m.find() || (m = TWO_GRANTS.matcher(text)).find()) {
            set(stats, m.group(2), m.group(1));
            return m.group(4) == null ? stats : set(stats, m.group(4), m.group(3));
        }
        m = INCREASES.matcher(text);
        if (!m.find()) return stats;
        set(stats, m.group(1), m.group(2));
        if (m.group(3) != null) set(stats, m.group(3), m.group(4));
        return stats;
    }

    private static Stats set(Stats stats, String name, String value) {
        for (Stat stat : Stat.values()) {
            if (stat.getDisplayName().equals(name)) return stats.set(stat, Double.parseDouble(value));
        }
        return stats;
    }

    /** One "&a+75 &c❤ Health" (or Small Brain's "&b-5✎ Intelligence") of "&7Grants … [&7and …]". */
    private static final String GRANT = "&.([+-][\\d.]+) ?(?:&.)?\\S? ?([A-Z][a-zA-Z]*(?: [A-Z][a-zA-Z]*)*)";
    /**
     * "&7Grants &a+75 &c❤ Health&7." (or "…&7, which …", or Divine Gift's "…Magic Find.", or Small Brain's "…
     * Intelligence&7 and …"), and nothing more to it.
     */
    private static final Pattern GRANTS = Pattern.compile("^&7Grants " + GRANT + "(?:(?: &7|&7 )and " + GRANT + ")?(?:&7)?(?:\\.$|, which )");
    /**
     * Reflection's two sentences, "&7Grants &b+2✎ Intelligence&7. Grants &f+1❂ True Defense&7. When damaged by an
     * arrow, …": both stats count for good, whatever the sentences after them do (a Turbo's "Grants …. Requires
     * Bronze in a … Contest!" is one sentence of grants, so it still grants nothing).
     */
    private static final Pattern TWO_GRANTS = Pattern.compile("^&7Grants " + GRANT + "(?:&7)?\\. Grants " + GRANT + "(?:&7)?\\. ");
    /** One "&9☠ Crit Damage &7by &a10%&7" of "&7Increases … [and …]." */
    private static final String BY = "&.\\S+ ([A-Z][a-zA-Z]*(?: [A-Z][a-zA-Z]*)*) &7by &a\\+?([\\d.]+)%?&7";
    private static final Pattern INCREASES = Pattern.compile("^&7Increases " + BY + "(?: and " + BY + ")?\\.");

    @Override
    public boolean equals(Object o) {
        return o instanceof EnchantmentType other && other.namespace.equals(namespace);
    }

    @Override
    public int hashCode() {
        return namespace.hashCode();
    }

    @Override
    public String toString() {
        return namespace;
    }
}
