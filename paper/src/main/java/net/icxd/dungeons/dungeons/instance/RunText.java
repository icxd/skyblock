package net.icxd.dungeons.dungeons.instance;

import java.util.Locale;

import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

/** How Hypixel writes things in dungeon messages, the sidebar and the tab list. */
final class RunText {
    /** The bold green rule above and below the end-of-run messages. */
    static final String RULE = "&a&l" + "▬".repeat(64);

    private RunText() {
    }

    /**
     * Spaces in front so it sits in the middle of the chat, the way Hypixel centres its end-of-run
     * lines: around 160 pixels, with the vanilla font's widths (a pixel between letters, one more
     * for bold letters). Matches every centred line in the recordings.
     */
    static String centered(String text) {
        int spaces = (int) Math.ceil((160 - width(text) / 2) / 4.0);
        return "&f" + " ".repeat(Math.max(0, spaces)) + text;
    }

    /** Width in pixels of text with {@code &} colour codes. */
    static int width(String text) {
        int width = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                if (code == 'l') bold = true;
                else if ("0123456789abcdefr".indexOf(code) >= 0) bold = false;
                continue;
            }
            MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar(c);
            width += (sprite == null ? 5 : sprite.getWidth()) + 1 + (bold && c != ' ' ? 1 : 0);
        }
        return width;
    }

    /** "01s", "01m 49s", "01h 02m 03s"; to the nearest second, as the sidebar doesn't tick with the run. */
    static String elapsed(long millis) {
        long seconds = (millis + 500) / 1000;
        long h = seconds / 3600;
        long m = seconds / 60 % 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%02dh %02dm %02ds", h, m, s);
        if (m > 0) return String.format("%02dm %02ds", m, s);
        return String.format("%02ds", s);
    }

    /** "0", "852.7k", "1M", "1.3M", as in the tab list's "Team Damage Dealt". */
    static String compact(double n) {
        if (n < 1000) return String.valueOf((long) n);
        String[] units = {"k", "M", "B", "T"};
        int unit = -1;
        while (n >= 1000 && unit < units.length - 1) {
            n /= 1000;
            unit++;
        }
        String number = String.format(Locale.ROOT, "%.1f", Math.floor(n * 10) / 10);
        if (number.endsWith(".0")) number = number.substring(0, number.length() - 2);
        return number + units[unit];
    }

    /** "0:04", "1:57". */
    static String clock(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}
