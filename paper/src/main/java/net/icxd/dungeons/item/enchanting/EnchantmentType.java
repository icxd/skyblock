package net.icxd.dungeons.item.enchanting;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.mining.MiningTools;
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

    /**
     * Its description at a level on this item, here: Efficiency on a mining tool off the Hub says the Mining Speed it
     * grants there ({@link MiningTools#efficiencyText}); every other one as {@link #getDescription(int)}.
     */
    public String getDescription(SkyBlockItem item, int level) {
        String mining = MiningTools.efficiencyText(namespace, item, level);
        return mining != null ? mining : getDescription(level);
    }

    /** What the next tier of a stacking enchantment takes, as its text says ("&8100 blocks to tier up!"); null at its last. */
    public String getTierUp(int level) {
        EnchantmentData.Entry data = data();
        EnchantmentData.Level at = data == null ? null : data.level(level);
        return at == null ? null : at.tierUp();
    }

    /**
     * The first percentage its text gives at a level, in green: Looting's "by &a15%", Experience's "a &a12.5% &7chance",
     * Compact's "a &a0.25% &7chance"; 0 if it has none.
     */
    public double percent(int level) {
        return percent(getDescription(level));
    }

    /** The first green percentage in a description (see {@link #percent(int)}); 0 for none. */
    static double percent(String text) {
        if (text == null) return 0;
        Matcher m = PERCENT.matcher(text);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
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

    /** The stats it grants at a level on this item, here (Efficiency's Mining Speed on a mining tool: see {@link #getDescription(SkyBlockItem, int)}). */
    public Stats getStats(SkyBlockItem item, int level) {
        return stats(getDescription(item, level));
    }

    /** What a description's text grants (see {@link #getStats}); nothing for null. */
    static Stats stats(String text) {
        Stats stats = new Stats();
        if (text == null) return stats;
        Matcher m = GRANTS.matcher(text);
        if (m.find()) {
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

    /** One "&a+75 &c❤ Health" of "&7Grants … [&7and …]". */
    private static final String GRANT = "&.\\+([\\d.]+) ?(?:&.)?\\S? ?([A-Z][a-zA-Z]*(?: [A-Z][a-zA-Z]*)*)";
    /**
     * "&7Grants &a+75 &c❤ Health&7." (or "…&7, which …", or Divine Gift's "…Magic Find."), and nothing more to it; or
     * "&7Gain" (Cultivating's two, as live lore counts them), and the one before "&7and a …" (Compact's Mining Wisdom,
     * live lore's "Mining Wisdom: +8" at VIII, before its chance of an enchanted item).
     */
    private static final Pattern GRANTS = Pattern.compile("^&7(?:Grants|Gain) " + GRANT + "(?: &7and " + GRANT + ")?(?:&7)?(?:\\.$|, which | &7and a )");
    /** One "&9☠ Crit Damage &7by &a10%&7" of "&7Increases … [and …]." */
    private static final String BY = "&.\\S+ ([A-Z][a-zA-Z]*(?: [A-Z][a-zA-Z]*)*) &7by &a\\+?([\\d.]+)%?&7";
    private static final Pattern INCREASES = Pattern.compile("^&7Increases " + BY + "(?: and " + BY + ")?\\.");
    /** A green percent: "&a15%", "&a+12.5%". */
    private static final Pattern PERCENT = Pattern.compile("&a\\+?([\\d.]+)%");

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
