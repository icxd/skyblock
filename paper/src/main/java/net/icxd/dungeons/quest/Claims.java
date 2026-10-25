package net.icxd.dungeons.quest;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

/**
 * The items quests gave a profile that it hasn't claimed yet, by their claim keys (the reward line's
 * {@code /claimitem bartender:UNDEAD_SWORD}): {@code claims} on the profile. A reward's menu opens as
 * it's given, and its chat line opens it again until it's claimed (see {@link ClaimMenu}). Plain
 * functions on the profile's document; nothing here saves.
 */
public final class Claims {
    public static final String KEY = "claims";

    private Claims() {
    }

    public static List<String> unclaimed(Document profile) {
        List<String> keys = new ArrayList<>();
        if (profile != null && profile.get(KEY) instanceof List<?> list) for (Object o : list) if (o instanceof String s) keys.add(s);
        return keys;
    }

    public static boolean has(Document profile, String claim) {
        return unclaimed(profile).contains(claim);
    }

    /** Waiting to be claimed (once: a key there already stays one). */
    public static void add(Document profile, String claim) {
        List<String> keys = unclaimed(profile);
        if (keys.contains(claim)) return;
        keys.add(claim);
        profile.put(KEY, keys);
    }

    /** Claimed: false if it wasn't waiting. */
    public static boolean take(Document profile, String claim) {
        List<String> keys = unclaimed(profile);
        if (!keys.remove(claim)) return false;
        profile.put(KEY, keys);
        return true;
    }

    /** The item a key gives: what's after its colon ("UNDEAD_SWORD"). */
    public static String item(String claim) {
        return claim.substring(claim.indexOf(':') + 1);
    }

    /** "You claimed a Spray Can!", "You claimed an Undead Sword!" (recorded), its name as it shows. */
    static String claimed(String name) {
        String plain = name.replaceAll("[&§].", "");
        boolean vowel = !plain.isEmpty() && "AEIOUaeiou".indexOf(plain.charAt(0)) >= 0;
        return "&aYou claimed " + (vowel ? "an " : "a ") + name + "&a!";
    }
}
