package net.icxd.dungeons.utils;

import com.destroystokyo.paper.profile.ProfileProperty;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.user.User;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final List<String> CRIT_SPECTRUM = Arrays.asList("§f", "§f", "§e", "§6",
            "§c", "§c");

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
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static double random(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    /** A stat as SkyBlock shows it: whole numbers without decimals, the rest to one place. */
    public static String formatStat(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** "57,690,425", whatever the server's locale. */
    public static String getFormattedNumber(int n) {
        return NumberFormat.getNumberInstance(Locale.US).format(n);
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
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(color(message)));
    }

    public static List<String> splitByWordAndLength(String string, int splitLength, String separator) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\G" + separator + "*(.{1," + splitLength + "})(?=\\s|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(string);
        while (matcher.find())
            result.add(matcher.group(1));
        return result;
    }

    public static List<String> split(String s, int maxLength, String newLineStartColor) {
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
        return split(s, maxLength, "§7");
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

    /** A head texture's "textures" property value, from the texture's hash on textures.minecraft.net. */
    public static String texture(String hash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + hash + "\"}}}";
        return java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void skull(ItemStack head, String skin) {
        if (skin.isEmpty()) return;
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        // skin is the base64 "textures" property value. The profile's id comes from the skin, so the
        // same head is the same item and stacks (a random one made every head unique).
        com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(skin.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        profile.setProperty(new ProfileProperty("textures", skin));
        headMeta.setPlayerProfile(profile);
        head.setItemMeta(headMeta);
    }

    public static int doubleToInt(double d) {
        return Double.valueOf(d).intValue();
    }

    public static ArrayList<Player> getAllUsersOfRankOrHigher(Rank rank) {
        ArrayList<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (User.rankOf(player.getUniqueId()).isEqualOrStrongerThan(rank))
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

    private static final ThreadLocal<DecimalFormat> ENGINEERING =
            ThreadLocal.withInitial(() -> new DecimalFormat("##0E0", java.text.DecimalFormatSymbols.getInstance(Locale.ROOT)));

    /** "1M", "998k", "50M": at most four characters, as mob name tags show health. */
    public static String formatNumber(double number) {
        String r = ENGINEERING.get().format(number);
        r = r.replaceAll("E[0-9]", suffix[Character.getNumericValue(r.charAt(r.length() - 1)) / 3]);
        while (r.length() > MAX_LENGTH || r.matches("[0-9]+\\.[a-z]")) {
            r = r.substring(0, r.length() - 2) + r.substring(r.length() - 1);
        }
        return r;
    }

    public static String title(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
