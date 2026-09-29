package net.icxd.dungeons.dwarven;

import org.bson.Document;

import net.icxd.dungeons.user.User;

/**
 * Powder on the profile a player plays on ({@code dwarvenMines.powder.<type>}, what {@link User#getHOTMPowder} reads and
 * the Heart of the Mountain shows): what mining gives. Nothing here saves: the profile is saved with the rest of their
 * data, as their coins are. Main thread.
 */
public final class Powder {
    /** "The maximum amount of Mithril Powder a player may have is 2 billion" (the wiki's Mithril Powder); the same for each here. */
    static final int CAP = 2_000_000_000;

    private Powder() {
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
