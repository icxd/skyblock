package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.utils.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Numbers of a tiered bonus that grow with how many of its pieces are worn, one for each count from
 * {@code first} on (the wiki's Tiered Bonus Values: Arachne's Armor's +5, 10, 20, 35, 50, 70 and 100
 * Health from 2 pieces), the last one's past the end.
 */
public record Tiers(int first, double... values) {
    public Tiers {
        if (values.length == 0) throw new IllegalArgumentException("no values");
        values = values.clone();
    }

    /** The number at this many pieces: the first one's below {@code first}. */
    public double at(int count) {
        return values[Math.max(0, Math.min(count - first, values.length - 1))];
    }

    /**
     * The number lore shows for this many pieces when the bonus counts from {@code needs}: the first tier
     * that counts, until there are that many. Items' data (read with none worn) shows those: Snorkeling
     * Armor's "+2" Respiration of its 0, 2, 5 and 10 from 1 piece, which count from 2.
     */
    public double shown(int count, int needs) {
        return at(Math.max(count, needs));
    }

    /**
     * The text with the number that follows {@code before} ("&c+" in "&c+5❤ Health") as it is for this
     * many pieces in place of the one lore shows with none worn: the first time that number follows it,
     * and not as the start of a longer number. As it is if it isn't there.
     */
    public List<String> text(List<String> text, String before, int count, int needs) {
        return replace(text, before + Text.number(shown(0, needs)), before + Text.number(shown(count, needs)));
    }

    /** The lines with {@code from} (not followed by more of a number) changed to {@code to} the first time it's there. */
    static List<String> replace(List<String> text, String from, String to) {
        if (from.equals(to)) return text;
        List<String> out = new ArrayList<>(text);
        for (int i = 0; i < out.size(); i++) {
            String line = out.get(i);
            for (int at = line.indexOf(from); at >= 0; at = line.indexOf(from, at + 1)) {
                int end = at + from.length();
                if (end < line.length() && (Character.isDigit(line.charAt(end)) || line.charAt(end) == '.' && end + 1 < line.length()
                        && Character.isDigit(line.charAt(end + 1)))) continue;
                out.set(i, line.substring(0, at) + to + line.substring(end));
                return out;
            }
        }
        return out;
    }
}
