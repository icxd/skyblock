package net.icxd.dungeons.item.ability.utility;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * The Archfiend Dice's (and the High Class one's) Try Your Luck: "Costs 666.7k coins to roll between 1-6. Gain
 * between -120❤ and +120❤ for 24h. Overwritten by new rolls! If you roll a 6, earn 15M coins but lose this dice."
 * (its text's numbers). The wiki's Archfiend Dice: it "attempts to roll for a 6 (4.17%, 1 in 24 rolls). If this
 * fails ... it rolls for a number between 1 and 5 with equal chances", and 1 to 6 give -120, -80, -40, +40, +80 and
 * +120 (a third of the most a step, none for no roll). Its 7 (an Archfiend Dye, 0.015%) isn't built: there are no
 * dyes. The Health is kept on their profile until it runs out (a new roll takes its place), and counts in their stats
 * ({@link #stats}). The chat lines are UNKNOWN. Main thread.
 */
final class ArchfiendDice implements AbilityHandler {
    /** Where the roll's Health is kept on a profile: how much, and until when. */
    static final String FIELD = "archfiendDice";
    /** "it attempts to roll for a 6 (4.17%, 1 in 24 rolls)" (the wiki). */
    static final double SIX = 1.0 / 24;
    private static final Pattern COSTS = Pattern.compile("Costs ([\\d.]+)([kMB]?) coins to roll");
    private static final Pattern GAIN = Pattern.compile("Gain between -([\\d,]+)❤ and \\+[\\d,]+❤ for (\\d+)h");
    private static final Pattern EARN = Pattern.compile("If you roll a 6, earn ([\\d.]+)([kMB]?) coins");

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        User user = User.ifLoaded(player.getUniqueId());
        double cost = coins(COSTS, AbilityText.plain(block.text()));
        if (user != null && cost >= 0 && Purse.has(user, cost)) return true;
        player.sendMessage(Utils.color("&cYou don't have enough coins to roll!"));
        return false;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        User user = User.ifLoaded(player.getUniqueId());
        String plain = AbilityText.plain(block.text());
        double cost = coins(COSTS, plain);
        if (user == null || cost < 0 || !Purse.take(user, cost)) return;
        Matcher gain = GAIN.matcher(plain);
        boolean said = gain.find();
        double most = said ? Double.parseDouble(gain.group(1).replace(",", "")) : 120;
        long hours = said ? Long.parseLong(gain.group(2)) : 24;
        int roll = roll(ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextInt(5));
        double health = health(roll, most);
        user.profile().put(FIELD, new Document("health", health).append("until", System.currentTimeMillis() + hours * 3_600_000));
        player.sendMessage(Utils.color("&eYou rolled a &c" + roll + "&e! " + (health >= 0 ? "&c+" : "&c") + (int) health + "❤ Health &efor " + hours + "h."));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1, roll == 6 ? 2 : 1);
        if (roll == 6) {
            double earned = coins(EARN, plain);
            if (earned > 0) Purse.add(user, earned);
            ItemStack held = player.getInventory().getItemInMainHand();
            held.setAmount(held.getAmount() - 1);
            player.getInventory().setItemInMainHand(held.getAmount() <= 0 ? null : held);
        }
        user.save();
    }

    /** A roll: a 6 when {@code sixRoll} is under 1 in 24, else 1 to 5 by {@code oneToFive} (0 to 4). */
    static int roll(double sixRoll, int oneToFive) {
        return sixRoll < SIX ? 6 : 1 + Math.max(0, Math.min(4, oneToFive));
    }

    /** What a roll gives, of the most there is: -most, -2/3, -1/3, +1/3, +2/3, +most for 1 to 6. */
    static double health(int roll, double most) {
        int step = roll <= 3 ? roll - 4 : roll - 3;
        return Math.round(most * step / 3.0);
    }

    /** "666.7k", "15M" in the text as coins; -1 if it doesn't say. */
    static double coins(Pattern pattern, String plain) {
        Matcher m = pattern.matcher(plain);
        if (!m.find()) return -1;
        double number = Double.parseDouble(m.group(1));
        double unit = switch (m.group(2)) {
            case "k" -> 1_000;
            case "M" -> 1_000_000;
            case "B" -> 1_000_000_000;
            default -> 1;
        };
        return Math.round(number * unit);
    }

    /** Their last roll's Health, while it lasts. */
    static void stats(Player player, Stats stats) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null) return;
        stats.add(Stat.HEALTH, rolled(user.profile(), System.currentTimeMillis()));
    }

    /** The Health a profile's last roll gives at {@code now}: 0 once it has run out, or with none. */
    static double rolled(Document profile, long now) {
        if (profile == null || !(profile.get(FIELD) instanceof Document roll)) return 0;
        Object until = roll.get("until");
        Object health = roll.get("health");
        if (!(until instanceof Number u) || !(health instanceof Number h) || u.longValue() <= now) return 0;
        return h.doubleValue();
    }
}
