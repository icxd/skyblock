package net.icxd.dungeonscanner.world;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where SkyBlock's sidebar says you are, from its lines top to bottom without formatting: the
 * server after the date ("10/24/26 m35DV"), and the area on the line after the time ("8:00am ☀",
 * then "Your Island").
 */
public final class Sidebar {
  private static final Pattern SERVER = Pattern.compile("^\\d\\d/\\d\\d/\\d\\d\\s+(\\S+)");
  private static final Pattern TIME = Pattern.compile("\\b\\d{1,2}:\\d\\d\\s?(am|pm)\\b", Pattern.CASE_INSENSITIVE);
  /** The area's icon: a resource pack glyph now (U+E067), "⏣" on older clients (the Rift's "ф"). */
  private static final Pattern ICON = Pattern.compile("[\\uE000-\\uF8FF⏣ф]");

  private Sidebar() {
  }

  /** The server's id, or null. */
  public static String server(List<String> lines) {
    for (String line : lines) {
      Matcher m = SERVER.matcher(line.strip());
      if (m.find()) return m.group(1);
    }
    return null;
  }

  /** The area, or null. Of the lines after a time, the one with the area's icon if any has it. */
  public static String area(List<String> lines) {
    String plain = null;
    for (int i = 0; i + 1 < lines.size(); i++) {
      if (!TIME.matcher(lines.get(i)).find()) continue;
      String next = lines.get(i + 1).strip();
      String area = ICON.matcher(next).replaceAll("").strip();
      if (area.isEmpty()) continue;
      if (ICON.matcher(next).lookingAt()) return area;
      if (plain == null) plain = area;
    }
    return plain;
  }
}
