package net.icxd.dungeons.item.modifier;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * The Power Scrolls the Hex offers, in its order (the wiki's The Hex/UI, Power Scrolls): one at a time on an item
 * with a RIGHT CLICK ability, kept as the scroll's item id in {@code power_ability_scroll}. Using that ability then
 * does what the scroll's item says: "Heal back 1% of your missing ❤ Health ... Has a 5s cooldown", "Gain +5✎ Mana
 * ... Has a 5s cooldown", "Gain +10❁ Strength for 5s" and the like. The Jade and Topaz Power Scrolls have no text
 * in the item data, and the Hex doesn't show them, so they aren't here.
 */
public enum PowerScroll {
    RUBY('c'),
    SAPPHIRE('b'),
    JASPER('d'),
    AMETHYST('5'),
    AMBER('6'),
    OPAL('f');

    /** "Heal back 1% of your missing". */
    private static final Pattern HEAL = Pattern.compile("Heal back ([\\d.]+)% of your missing");
    /** "Gain +5✎ Mana". */
    private static final Pattern MANA = Pattern.compile("Gain \\+([\\d.]+)✎ Mana\\b");
    /** "Gain +10❁ Strength for 5s": a stat, and how long. */
    private static final Pattern BUFF = Pattern.compile("Gain \\+([\\d.]+)(\\S) ([A-Z][A-Za-z]*(?: [A-Z][A-Za-z]*)*) for ([\\d.]+)s\\b");
    /** "Has a 5s cooldown". */
    private static final Pattern COOLDOWN = Pattern.compile("Has an? ([\\d.]+)s cooldown");

    private final char colour;

    PowerScroll(char colour) {
        this.colour = colour;
    }

    /** "RUBY_POWER_SCROLL". */
    public String itemId() {
        return name() + "_POWER_SCROLL";
    }

    /**
     * "&c": its gem's colour, which its name has in the Hex (the wiki's The Hex/UI) and its "⦾" on the ability it's
     * on (live lore: Sapphire &b, Jasper &d, Opal &f, Ruby &c, Amber &6, Amethyst &5).
     */
    public String colour() {
        return "&" + colour;
    }

    /** The one with this item id; null for none (or a scroll the Hex doesn't offer). */
    public static PowerScroll of(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;
        for (PowerScroll scroll : values()) if (scroll.itemId().equalsIgnoreCase(itemId)) return scroll;
        return null;
    }

    /**
     * What a scroll does on a use, from its item's text: a share of their missing health back, mana, stats for a
     * while (milliseconds), and how long before it does it again (0 for every use).
     */
    record Effect(double heal, double mana, Stats stats, long millis, long cooldown) {
    }

    static Effect effect(List<String> lore) {
        String plain = String.join(" ", lore).replaceAll("[&§][0-9a-fk-orA-FK-OR]", "").replaceAll("\\s+", " ");
        Matcher heal = HEAL.matcher(plain);
        Matcher mana = MANA.matcher(plain);
        Matcher buff = BUFF.matcher(plain);
        Matcher cooldown = COOLDOWN.matcher(plain);
        Stats stats = new Stats();
        long millis = 0;
        if (buff.find()) {
            Stat stat = stat(buff.group(2), buff.group(3));
            if (stat != null) {
                stats.add(stat, Double.parseDouble(buff.group(1)));
                millis = (long) (Double.parseDouble(buff.group(4)) * 1000);
            }
        }
        return new Effect(heal.find() ? Double.parseDouble(heal.group(1)) / 100 : 0, mana.find() ? Double.parseDouble(mana.group(1)) : 0,
                stats, millis, cooldown.find() ? (long) (Double.parseDouble(cooldown.group(1)) * 1000) : 0);
    }

    /** The stat with this symbol whose name the words are ("Strength" with "❁", not Damage). */
    private static Stat stat(String symbol, String name) {
        for (Stat stat : Stat.values()) {
            if (!stat.name().startsWith("RIFT_") && stat.getSymbol().equals(symbol) && stat.getDisplayName().equals(name)) return stat;
        }
        return null;
    }

    /**
     * An ability of the held item was used (it's paid for and done): if it's a RIGHT CLICK one and the item has a
     * Power Scroll, the scroll does what it does, unless it's on its cooldown. Its stats are theirs for a while as
     * a buff of its own, which another use starts again (PlayerSession#buff).
     */
    public static void used(Player player, NBTTagCompound tag, ItemBlock ability) {
        if (!"RIGHT_CLICK".equals(ability.activation())) return;
        PowerScroll scroll = of(tag.getString(ItemModifiers.POWER_SCROLL));
        SkyBlockItem item = scroll == null ? null : ItemRegistry.get(scroll.itemId());
        if (item == null) return;
        Effect effect = effect(item.lore());
        PlayerSession session = PlayerSession.of(player);
        // One cooldown a scroll, whichever item it's on (UNKNOWN: whether it's the item's own).
        String key = "power_scroll:" + scroll.name();
        if (session.cooldownLeft(key) > 0) return;
        if (effect.cooldown() > 0) session.startCooldown(key, effect.cooldown());
        if (effect.heal() > 0) PlayerHealth.heal(player, (PlayerHealth.max(player) - PlayerHealth.get(player)) * effect.heal());
        if (effect.mana() > 0) session.setMana((int) Math.min(session.maxMana(), Math.max(0, session.getMana()) + effect.mana()));
        if (effect.millis() > 0) session.buff(scroll.itemId(), effect.stats(), effect.millis());
    }
}
