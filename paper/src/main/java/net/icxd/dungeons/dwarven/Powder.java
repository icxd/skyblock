package net.icxd.dungeons.dwarven;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bukkit.entity.Player;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.user.User;

/**
 * Powder on the profile a player plays on ({@code dwarvenMines.powder.<type>}, what {@link User#getHOTMPowder} reads and
 * the Heart of the Mountain shows): what mining gives, with what raises it (the Mithril Drills' "Grants +20% Mithril
 * Powder."). Nothing here saves: the profile is saved with the rest of their data, as their coins are. Main thread.
 */
public final class Powder {
    /** "The maximum amount of Mithril Powder a player may have is 2 billion" (the wiki's Mithril Powder); the same for each here. */
    static final int CAP = 2_000_000_000;
    /** "Grants +20% Mithril Powder." (the Mithril Drills), "Grants +5% Gemstone Powder, and …" (the Gemstone Drills). */
    private static final Pattern HELD_BONUS = Pattern.compile("Grants \\+([\\d.]+)% (\\w+) Powder");

    private Powder() {
    }

    /**
     * A block mined: its {@code base} powder of a type, with what raises it. The one source of that here is the held
     * item's text (the Mithril Drills: "Grants &2+20% Mithril Powder&7." while held, the wiki's Mithril Powder; its
     * other sources, Core of the Mountain, Powder Buff, Sky Mall, events and pets, aren't here).
     */
    public static void mined(Player player, PowderType type, int base) {
        add(User.ifLoaded(player.getUniqueId()), type, withBonus(base, heldBonus(player, type), ThreadLocalRandom.current().nextDouble()));
    }

    /** The percent more powder of a type what they hold in their main hand gives, as its text says; 0 for none. */
    static double heldBonus(Player player, PowderType type) {
        NBTTagCompound tag = ItemNBT.read(player.getInventory().getItemInMainHand());
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return item == null ? 0 : bonus(item.lore(), type);
    }

    /** The first "Grants +20% Mithril Powder" of this type in an item's text, in percent; 0 for none. */
    static double bonus(List<String> lore, PowderType type) {
        for (String line : lore) {
            Matcher m = HELD_BONUS.matcher(line.replaceAll("[&§].", ""));
            if (m.find() && m.group(2).equalsIgnoreCase(type.name())) return Double.parseDouble(m.group(1));
        }
        return 0;
    }

    /**
     * {@code base} powder with {@code percent} more: its whole part, and the rest as the chance of one more, with
     * {@code roll} (0 to 1) as the random (UNKNOWN how Hypixel gives a share of one: 1 powder a block and +20% is 1.2).
     */
    static int withBonus(int base, double percent, double roll) {
        double exact = Math.max(0, base) * (1 + Math.max(0, percent) / 100);
        int whole = (int) exact;
        return whole + (roll < exact - whole ? 1 : 0);
    }

    /** Gives them {@code amount} powder of a type, up to the cap. Nothing while their data isn't here. */
    public static void add(User user, PowderType type, int amount) {
        if (user == null || user.isReleased() || amount <= 0) return;
        add(user.profile(), type, amount);
    }

    /** The same on a profile document (made where an older profile hasn't the fields); returns what it has now. */
    public static int add(Document profile, PowderType type, int amount) {
        if (profile == null) return 0;
        Document powder = child(child(profile, "dwarvenMines"), "powder");
        int now = sum(powder.get(type.name()) instanceof Number n ? n.intValue() : 0, amount);
        powder.put(type.name(), now);
        return now;
    }

    /** What {@code have} and {@code amount} more come to, at most the cap. */
    static int sum(int have, int amount) {
        return (int) Math.min(CAP, Math.max(0L, (long) have + Math.max(0, amount)));
    }

    private static Document child(Document parent, String key) {
        Document child = parent.get(key, Document.class);
        if (child == null) {
            child = new Document();
            parent.put(key, child);
        }
        return child;
    }
}
