package net.icxd.dungeons.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.user.Rank;
import net.icxd.dungeons.user.User;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
  private static final List<ChatColor> CRIT_SPECTRUM = Arrays.asList(ChatColor.WHITE, ChatColor.WHITE, ChatColor.YELLOW, ChatColor.GOLD,
      ChatColor.RED, ChatColor.RED);

  public static String color(String string) {
    return string.replace("&", "§");
  }
  public static ArrayList<String> colorList(List<String> list) {
    ArrayList<String> colored = new ArrayList<>();
    for (String s : list)
      colored.add(color(s));
    return colored;
  }

  private static final String[] ROMAN_NUMERALS = new String[] { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
  private static final int[] ROMAN_VALUES = new int[] { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
  public static String getRomanNumeral(int number) {
    if (number < 1 || number > 3999)
      return String.valueOf(number);
    StringBuilder roman = new StringBuilder();
    for (int i = 0; i < ROMAN_NUMERALS.length; i++) {
      while (number >= ROMAN_VALUES[i]) {
        number -= ROMAN_VALUES[i];
        roman.append(ROMAN_NUMERALS[i]);
      }
    }
    return roman.toString();
  }

  public static int random(int min, int max) {
    return new Random().nextInt((max - min) + 1) + min;
  }

  public static double random(double min, double max) {
    return Math.random() * (max - min) + min;
  }

  public static String getDateFormatted(Date date) { return new SimpleDateFormat("dd/MM/yy").format(date); }
  public static String getFormattedNumber(int n) {
    return NumberFormat.getNumberInstance().format(n);
  }

  public static String rainbowize(String string) {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    for (String c : string.split("")) {
      if (i > CRIT_SPECTRUM.size() - 1) i = 0;
      builder.append(CRIT_SPECTRUM.get(i)).append(c);
      i++;
    }
    return builder.toString();
  }

  public static void async(Runnable runnable) {
    new Thread(runnable).start();
  }

  public static void delay(Runnable runnable, long delay) {
    new BukkitRunnable() {
      @Override public void run() { runnable.run(); }
    }.runTaskLater(Dungeons.getInstance(), delay);
  }

  public static void sendActionText(Player player, String message) {
    PacketPlayOutChat packet = new PacketPlayOutChat(new ChatComponentText(color(message)), (byte) 2);
    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
  }

  public static String formatTime(long time) {
    return String.format("%02d:%02d:%02d", time / 3600000, (time / 60000) % 60, (time / 1000) % 60);
  }

  public static String commaFormat(double number) {
    return new DecimalFormat("#,###.0").format(number);
  }

  public static List<String> splitByWordAndLength(String string, int splitLength, String separator) {
    List<String> result = new ArrayList<>();
    Pattern pattern = Pattern.compile("\\G" + separator + "*(.{1," + splitLength + "})(?=\\s|$)", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(string);
    while (matcher.find())
      result.add(matcher.group(1));
    return result;
  }

  public static List<String> split(String s, int maxLength, ChatColor newLineStartColor) {
    String[] words = s.split(" ");
    List<String> lines = new ArrayList<>();
    StringBuilder line = new StringBuilder();
    for (String word : words) {
      if (line.length() > maxLength) {
        lines.add(color(line.toString()));
        line = new StringBuilder(newLineStartColor.toString());
      }
      line.append(word).append(" ");
    }
    lines.add(color(line.toString()));
    return lines;
  }

  public static List<String> split(String s, int maxLength) {
    return split(s, maxLength, ChatColor.GRAY);
  }

  public static List<String> combineElements(List<String> list, String separator, int perElement) {
    List<String> n = new ArrayList<>();
    for (int i = 0; i < list.size(); i += perElement) {
      StringBuilder builder = new StringBuilder();
      for (int j = 0; j < perElement; j++) {
        if (i + j > list.size() - 1)
          break;
        builder.append(j != 0 ? separator : "").append(list.get(i + j));
      }
      n.add(builder.toString());
    }
    return n;
  }

  public static void skull(ItemStack head, String skin) {
    if (skin.isEmpty()) return;
    SkullMeta headMeta = (SkullMeta) head.getItemMeta();
    GameProfile profile = new GameProfile(UUID.randomUUID(), null);
    byte[] encodedData = skin.getBytes();
    profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
    Field profileField;
    try {
      profileField = headMeta.getClass().getDeclaredField("profile");
      profileField.setAccessible(true);
      profileField.set(headMeta, profile);
    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ignored){
    }
    head.setItemMeta(headMeta);
  }

  public static String colorize(String string) {
    return string.replace("%%black%%", "§0")
        .replace("%%dark_blue%%", "§1")
        .replace("%%dark_green%%", "§2")
        .replace("%%dark_aqua%%", "§3")
        .replace("%%dark_red%%", "§4")
        .replace("%%dark_purple%%", "§5")
        .replace("%%gold%%", "§6")
        .replace("%%gray%%", "§7")
        .replace("%%dark_gray%%", "§8")
        .replace("%%blue%%", "§9")
        .replace("%%green%%", "§a")
        .replace("%%aqua%%", "§b")
        .replace("%%red%%", "§c")
        .replace("%%light_purple%%", "§d")
        .replace("%%yellow%%", "§e")
        .replace("%%white%%", "§f")
        .replace("%%obfuscated%%", "§k")
        .replace("%%bold%%", "§l")
        .replace("%%strikethrough%%", "§m")
        .replace("%%underline%%", "§n")
        .replace("%%italic%%", "§o")
        .replace("%%reset%%", "§r");
  }

  public static int doubleToInt(double d) {
    return Double.valueOf(d).intValue();
  }

  public static ArrayList<Class<?>> getAllClassesOfSubType(Class<?> subType) {
    return new ArrayList<>(new Reflections().getSubTypesOf(subType));
  }

  public static ArrayList<Player> getAllUsersOfRankOrHigher(Rank rank) {
    ArrayList<Player> players = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      User user = User.getUser(player.getUniqueId());
      if (Rank.valueOf(user.getDocument().getString("rank")).isEqualOrStrongerThan(rank))
        players.add(player);
    }
    return players;
  }

  public static String round(double d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Double.toString(d));
    bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
    return bd.toString();
  }

  private static final String[] suffix = new String[]{"", "k", "M", "B", "T"};
  private static final int MAX_LENGTH = 4;

  public static String formatNumber(double number) {
    String r = new DecimalFormat("##0E0").format(number);
    r = r.replaceAll("E[0-9]", suffix[Character.getNumericValue(r.charAt(r.length() - 1)) / 3]);
    while (r.length() > MAX_LENGTH || r.matches("[0-9]+\\.[a-z]")) {
      r = r.substring(0, r.length() - 2) + r.substring(r.length() - 1);
    }
    return r;
  }

  public static ItemStack setSkullItem(ItemStack stack, String url) {
    if (url.isEmpty())
      return null;
    SkullMeta headMeta = (SkullMeta) stack.getItemMeta();
    GameProfile profile = new GameProfile(UUID.randomUUID(), null);
    byte[] encodedData = Base64.getEncoder().encode((String.format("{textures:{SKIN:{url:\"%s\"}}}", url)).getBytes());
    profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
    Field profileField;
    try {
      profileField = headMeta.getClass().getDeclaredField("profile");
      profileField.setAccessible(true);
      profileField.set(headMeta, profile);
    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
      e1.printStackTrace();
    }

    stack.setItemMeta(headMeta);

    return stack;
  }

  public static String title(String s) {
    return s.charAt(0) + s.substring(1).toLowerCase();
  }
}
