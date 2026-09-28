package net.icxd.dungeons.item.modifier;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.stats.Stat;

/**
 * The accessory Enrichments, in the order the Hex lists them (the wiki's The Hex/UI, accessories): each gives a
 * Legendary or better accessory one stat, as much as its item says ("Enriches an accessory with the power of
 * &e+0.5⚔ Attack Speed"), one Enrichment an accessory. An accessory keeps its name in {@code talisman_enrichment}
 * in lower case, as live items do ("magic_find"); a few live ones have it in upper case, which reads the same.
 * The Accessory Enrichment Swapper isn't one.
 */
public enum Enrichment {
    WALK_SPEED(Stat.SPEED),
    INTELLIGENCE(Stat.INTELLIGENCE),
    CRITICAL_DAMAGE(Stat.CRIT_DAMAGE),
    CRITICAL_CHANCE(Stat.CRIT_CHANCE),
    STRENGTH(Stat.STRENGTH),
    DEFENSE(Stat.DEFENSE),
    HEALTH(Stat.HEALTH),
    MAGIC_FIND(Stat.MAGIC_FIND),
    FEROCITY(Stat.FEROCITY),
    SEA_CREATURE_CHANCE(Stat.SEA_CREATURE_CHANCE),
    ATTACK_SPEED(Stat.ATTACK_SPEED);

    /** "power of &e+0.5⚔ Attack Speed", without its colours. */
    private static final Pattern AMOUNT = Pattern.compile("power of \\+([\\d.]+)");

    private final Stat stat;

    Enrichment(Stat stat) {
        this.stat = stat;
    }

    public Stat stat() {
        return stat;
    }

    /** "magic_find", as the accessory keeps it. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** "TALISMAN_ENRICHMENT_MAGIC_FIND". */
    public String itemId() {
        return "TALISMAN_ENRICHMENT_" + name();
    }

    /** How much of its stat it gives, as its item says; 0 without the item data. */
    public double amount() {
        SkyBlockItem item = ItemRegistry.get(itemId());
        return item == null ? 0 : amount(item.lore());
    }

    /** The number after "power of" in an Enrichment's text; 0 if there's none. */
    static double amount(List<String> lore) {
        String plain = String.join(" ", lore).replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
        Matcher m = AMOUNT.matcher(plain);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    /** The one with this key ("magic_find", any case); null for none. */
    public static Enrichment of(String key) {
        if (key == null || key.isEmpty()) return null;
        for (Enrichment enrichment : values()) if (enrichment.name().equalsIgnoreCase(key)) return enrichment;
        return null;
    }
}
