package net.icxd.dungeons.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Text as it's written in this plugin, with {@code &} colour codes ({@code §} works too), into
 * Adventure components; and the word wrapping and number formatting item lore uses.
 */
public final class Text {
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final ThreadLocal<DecimalFormat> NUMBER =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)));

    /** Widths (with the pixel between letters) of the default font's letters that aren't 6 wide. */
    private static final Map<Character, Integer> WIDTHS = Map.ofEntries(
            Map.entry(' ', 4), Map.entry('!', 2), Map.entry('"', 4), Map.entry('\'', 2), Map.entry('(', 4),
            Map.entry(')', 4), Map.entry('*', 4), Map.entry(',', 2), Map.entry('.', 2), Map.entry(':', 2),
            Map.entry(';', 2), Map.entry('<', 5), Map.entry('>', 5), Map.entry('@', 7), Map.entry('I', 4),
            Map.entry('[', 4), Map.entry(']', 4), Map.entry('`', 3), Map.entry('f', 5), Map.entry('i', 2),
            Map.entry('k', 5), Map.entry('l', 3), Map.entry('t', 4), Map.entry('|', 2), Map.entry('{', 4),
            Map.entry('}', 4), Map.entry('~', 7));

    /**
     * How wide Hypixel lets a line of generated item text (enchantment descriptions) get before wrapping
     * it: the width that best fits the live auction house's items ("Increases melee damage dealt by
     * 30%." stays on one line at 192 pixels). Text Hypixel writes by hand is kept as its lines.
     */
    public static final int LORE_WIDTH = 192;
    /**
     * Requirement lines wrap wider: Hypixel keeps "❣ Requires The Catacombs Floor VII Completion." (238
     * pixels) on one line and breaks Master Mode's (304) after "Floor VII" (247).
     */
    public static final int REQUIREMENT_WIDTH = 250;

    private Text() {
    }

    /** A line of item text: not italic unless it says so (item names and lore are italic by default). */
    public static Component line(String text) {
        return AMPERSAND.deserialize(text.replace('§', '&')).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> lines(List<String> text) {
        List<Component> out = new ArrayList<>(text.size());
        for (String line : text) out.add(line(line));
        return out;
    }

    /** "1,800", "0.05", "-18": thousands grouped, at most two decimals. */
    public static String number(double value) {
        return NUMBER.get().format(value);
    }

    /** "+1,800" or "-18". */
    public static String signed(double value) {
        return (value < 0 ? "" : "+") + number(value);
    }

    /** "3k", "100k", "1.5M": how Hypixel shortens big round numbers (a drill's fuel capacity). */
    public static String compact(double value) {
        if (Math.abs(value) >= 1_000_000) return number(value / 1_000_000) + "M";
        if (Math.abs(value) >= 1_000) return number(value / 1_000) + "k";
        return number(value);
    }

    /** How wide the text is in the default font, in pixels (colour codes take none; bold adds one a letter). */
    public static int width(String text) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length() && isCode(text.charAt(i + 1))) {
                char code = Character.toLowerCase(text.charAt(++i));
                if (code == 'l') bold = true;
                else if (code == 'r' || isColor(code)) bold = false;
                continue;
            }
            width += WIDTHS.getOrDefault(c, 6) + (bold ? 1 : 0);
        }
        return width;
    }

    /**
     * Wraps text into lines at most {@code maxWidth} pixels wide, breaking between words; each line
     * after the first starts with the colour and formatting in effect where it breaks. For text that
     * isn't written out line by line already (Hypixel's own lines are used as they are).
     */
    public static List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            String carry = "";
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? carry + word : line + " " + word;
                if (!line.isEmpty() && width(candidate) > maxWidth) {
                    lines.add(line.toString());
                    carry = formatAtEnd(carry + line);
                    // A word that sets its own colour doesn't need the one carried over.
                    line = new StringBuilder(startsWithColor(word) ? "" : carry).append(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /** The colour and formatting codes in effect at the end of the text, e.g. "&c&l". */
    static String formatAtEnd(String text) {
        String color = "";
        StringBuilder format = new StringBuilder();
        for (int i = 0; i + 1 < text.length(); i++) {
            char c = text.charAt(i);
            if ((c != '&' && c != '§') || !isCode(text.charAt(i + 1))) continue;
            char code = Character.toLowerCase(text.charAt(++i));
            if (isColor(code)) {
                color = "&" + code;
                format.setLength(0);
            } else if (code == 'r') {
                color = "";
                format.setLength(0);
            } else {
                format.append('&').append(code);
            }
        }
        return color + format;
    }

    private static boolean startsWithColor(String word) {
        return word.length() > 1 && (word.charAt(0) == '&' || word.charAt(0) == '§') && isColor(Character.toLowerCase(word.charAt(1)));
    }

    private static boolean isCode(char c) {
        return "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(c) >= 0;
    }

    private static boolean isColor(char c) {
        return "0123456789abcdef".indexOf(c) >= 0;
    }
}
