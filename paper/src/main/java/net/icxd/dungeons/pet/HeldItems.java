package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * What a pet's held item (a Pet Item, by its id) shows and does, read from its own lines in the items data: its
 * description (what the Pets menu shows under "Held Item:", as recorded), what it does as that says (see {@link Effect}:
 * the Dwarf Turtle Shelmet's "Grants &f+10❂ True Defense" is in the pet's True Defense line, recorded) and the pet XP it
 * adds ("Gives &a+40% &7pet exp for &aMining&7."). No server needed past the item registry.
 */
public final class HeldItems {
    /** The Super Scrubber, which takes a pet's item off ("&eRight-click on your summoned pet to &eremove its item!"). */
    public static final String SCRUBBER = "SUPER_SCRUBBER";
    /** The lines every Pet Item starts with, up to "The pet must be visible to apply the item!". */
    private static final String HEADER_END = "The pet must be visible";
    private static final String FOOTER = "Right-click on your summoned pet";
    /** What a pet's item gives it isn't in its text: "&cThis item is not compatible with your pet!" (the wiki's Unalloyed Speed and Kuudra Pet). */
    private static final Map<String, Set<String>> NOT_FOR = Map.of("UNALLOYED_SPEED", Set.of("SNAIL"), "PET_ITEM_EXP_SHARE", Set.of("KUUDRA"));

    private static final Pattern COLOR = Pattern.compile("[&§][0-9a-fk-or]");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!])\\s+");
    /** "+30❁ Strength", "+5% ✯ Magic Find", "5⚶ Respiration": a sign, an amount, its symbol and a stat's words. */
    private static final Pattern AMOUNT = Pattern.compile("([+-]?)(\\d[\\d,]*(?:\\.\\d+)?)%?\\S*\\s+(?:[^\\sA-Za-z0-9]\\s+)?([A-Z][A-Za-z]*(?:\\s+[A-Z][A-Za-z]*)*)");
    private static final Pattern ONLY_ON = Pattern.compile("(?:may only be used on an? (.+?) pet|Can be applied to (.+?) pets)");
    private static final Pattern ALL_SHARE = Pattern.compile("Increases all pet stats by ([\\d.]+)%");
    private static final Pattern SHARE_OF = Pattern.compile("Increases (?:the|this) pet's (.+)");
    private static final Pattern SHARE = Pattern.compile("(?:[^\\sA-Za-z0-9]\\s+)?([A-Z][A-Za-z]*(?:\\s+[A-Z][A-Za-z]*)*) by ([\\d.]+)%");
    private static final Pattern EVERY = Pattern.compile("Every (\\d+) pet levels, you gain (.+)");
    private static final Pattern ON_KILL = Pattern.compile("Gain (.+?) for ([\\d.]+)s on kill");
    private static final Pattern COMBO = Pattern.compile("Grants (.+?) when getting a combo of (\\d+) or above");
    private static final Pattern SPEED_CAP = Pattern.compile("Grants \\+?([\\d.]+) Max Speed Cap");
    private static final Pattern TIERS = Pattern.compile("Boosts the rarity of your pet by (\\d+) tier");
    private static final Pattern EXP_SHARE = Pattern.compile("gains \\+([\\d.]+)% Exp Share rate");
    private static final Pattern REDUCES = Pattern.compile("reduces your (?:[^\\sA-Za-z0-9]\\s+)?([A-Z][A-Za-z]*(?:\\s+[A-Z][A-Za-z]*)*) by ([\\d.]+)");
    /** What makes a clause's stats only sometimes: "while mining Titanium Ore", "when getting a combo". */
    private static final Pattern CONDITION = Pattern.compile("\\b(?:while|when|during|per|for every|for each|on kill)\\b");
    private static final Pattern EXP = Pattern.compile("Gives \\+(\\d+(?:\\.\\d+)?)% pet exp for (all skills|[A-Z][a-z]+)");

    private static final Map<String, Effect> EFFECTS = new ConcurrentHashMap<>();

    private HeldItems() {
    }

    /**
     * What an item does, as its text says (nothing it doesn't say):
     *
     * @param grants      the stats it grants the pet's owner, flat ("Grants +30❁ Strength and +20✦ Speed."); not those that
     *                    "while" or "when" make sometimes, less what it "reduces" ("but reduces your ☂ Fishing Speed by 50")
     * @param shares      percent more of the pet's own stats, by stat ("Increases the pet's ✎ Intelligence by 100%.")
     * @param allShare    percent more of all of them ("Increases all pet stats by 33.3%.")
     * @param everyLevels each so many of the pet's levels give {@code perLevels} ("Every 2 pet levels, you gain +1⸕ Mining
     *                    Speed and +1☘ Mining Fortune."); 0 for none
     * @param onKill      what each kill gives for {@code onKillMillis} ("Gain 8⫽ Ferocity for 5s on kill.")
     * @param comboAt     a Kill Combo from which {@code combo} counts ("Grants +5% ✯ Magic Find when getting a combo of 50 or
     *                    above."); 0 for none
     * @param speedCap    how much it raises the owner's speed cap ("Grants +50 Max Speed Cap.")
     * @param noKnockback whether the owner takes no knockback ("makes the pet's owner immune to knockback")
     * @param tiers       how many rarities it raises the pet by ("Boosts the rarity of your pet by 1 tier!")
     * @param expShare    percent more Exp Share rate in an Exp Share slot
     * @param onlyFor     the pet it's only for, by name ("This pet item may only be used on a Griffin pet."); null for any
     * @param appliable   whether it's given to a pet ("Right-click on your summoned pet to give it this item!"), not used at Kat
     */
    public record Effect(Stats grants, Map<Stat, Double> shares, double allShare, int everyLevels, Stats perLevels, Stats onKill,
            long onKillMillis, int comboAt, Stats combo, double speedCap, boolean noKnockback, int tiers, double expShare, String onlyFor,
            boolean appliable) {
        static final Effect NONE = new Effect(new Stats(), Map.of(), 0, 0, new Stats(), new Stats(), 0, 0, new Stats(), 0, false, 0, 0, null, false);
    }

    public static SkyBlockItem item(String id) {
        return id == null ? null : ItemRegistry.get(id);
    }

    /** What it does, as its lines say: between the Pet Items' header and "Right-click on your summoned pet to give it this item!". */
    public static List<String> description(SkyBlockItem item) {
        if (item == null) return List.of();
        List<String> lore = item.lore();
        int start = 0;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(HEADER_END)) {
                start = i + 1;
                break;
            }
        }
        while (start < lore.size() && lore.get(start).isEmpty()) start++;
        int end = lore.size();
        for (int i = start; i < lore.size(); i++) {
            if (lore.get(i).contains(FOOTER)) {
                end = i;
                break;
            }
        }
        while (end > start && lore.get(end - 1).isEmpty()) end--;
        return new ArrayList<>(lore.subList(start, end));
    }

    /** What it does (nothing for none, or an id that isn't an item here), read once. */
    public static Effect effect(String id) {
        if (id == null) return Effect.NONE;
        Effect known = EFFECTS.get(id);
        if (known != null) return known;
        SkyBlockItem item = item(id);
        if (item == null) return Effect.NONE;
        boolean appliable = false;
        for (String line : item.lore()) if (line.contains(FOOTER)) appliable = true;
        Effect effect = parse(description(item), appliable);
        EFFECTS.put(id, effect);
        return effect;
    }

    /** The stats it grants the pet's owner, flat (none for one that doesn't, or isn't an item here). */
    public static Stats stats(String id) {
        return effect(id).grants().copy();
    }

    /**
     * What it gives a pet with these stats of its own at this level: its flat stats, its shares of the pet's ("all pet
     * stats" and the one-stat ones add up), and its stats by level.
     */
    public static Stats granted(String id, Stats pet, int level) {
        return granted(effect(id), pet, level);
    }

    static Stats granted(Effect effect, Stats pet, int level) {
        Stats granted = effect.grants().copy();
        for (Stat stat : Stat.values()) {
            if (!pet.has(stat)) continue;
            double percent = effect.allShare() + effect.shares().getOrDefault(stat, 0.0);
            if (percent != 0) granted.add(stat, pet.get(stat) * percent / 100);
        }
        if (effect.everyLevels() > 0) {
            int times = level / effect.everyLevels();
            for (Stat stat : Stat.values()) if (effect.perLevels().has(stat)) granted.add(stat, effect.perLevels().get(stat) * times);
        }
        return granted;
    }

    /**
     * Whether a pet of this type (and name) may hold it: the pet its text names, and the Snail no Unalloyed Speed, the
     * Kuudra no Exp Share ("&cThis item is not compatible with your pet!").
     */
    public static boolean compatible(String id, String type, String name) {
        Set<String> not = NOT_FOR.get(id);
        if (not != null && not.contains(type)) return false;
        String only = effect(id).onlyFor();
        return only == null || only.equalsIgnoreCase(name);
    }

    /** How much more pet XP it gives for a skill's XP, as a fraction ("+40%": 0.4); 0 for none. */
    public static double expBoost(String id, Skill skill) {
        SkyBlockItem item = item(id);
        if (item == null) return 0;
        Matcher m = EXP.matcher(plain(description(item)));
        double boost = 0;
        while (m.find()) {
            String target = m.group(2);
            if (target.equals("all skills") || (skill != null && target.toUpperCase(Locale.ROOT).equals(skill.name()))) {
                boost += Double.parseDouble(m.group(1)) / 100;
            }
        }
        return boost;
    }

    /** Its lines as plain text, one paragraph. */
    static String plain(List<String> lines) {
        return COLOR.matcher(String.join(" ", lines)).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    /** What an item whose description is these lines does (see {@link Effect}). */
    static Effect parse(List<String> description, boolean appliable) {
        Stats grants = new Stats();
        Map<Stat, Double> shares = new EnumMap<>(Stat.class);
        double allShare = 0;
        int everyLevels = 0;
        Stats perLevels = new Stats();
        Stats onKill = new Stats();
        long onKillMillis = 0;
        int comboAt = 0;
        Stats combo = new Stats();
        double speedCap = 0;
        boolean noKnockback = false;
        int tiers = 0;
        double expShare = 0;
        String onlyFor = null;
        for (String sentence : SENTENCE_END.split(plain(description))) {
            sentence = sentence.replaceAll("[.!]$", "");
            Matcher m;
            if ((m = ONLY_ON.matcher(sentence)).find()) onlyFor = m.group(1) != null ? m.group(1) : m.group(2);
            if (sentence.contains("immune to knockback")) noKnockback = true;
            if ((m = ALL_SHARE.matcher(sentence)).find()) {
                allShare += Double.parseDouble(m.group(1));
            } else if ((m = SHARE_OF.matcher(sentence)).find()) {
                Matcher share = SHARE.matcher(m.group(1));
                while (share.find()) {
                    Stat stat = stat(share.group(1));
                    if (stat != null) shares.merge(stat, Double.parseDouble(share.group(2)), Double::sum);
                }
            } else if ((m = EVERY.matcher(sentence)).find()) {
                everyLevels = Integer.parseInt(m.group(1));
                amounts(m.group(2), perLevels);
            } else if ((m = ON_KILL.matcher(sentence)).find()) {
                amounts(m.group(1), onKill);
                onKillMillis = (long) (Double.parseDouble(m.group(2)) * 1000);
            } else if ((m = COMBO.matcher(sentence)).find()) {
                amounts(m.group(1), combo);
                comboAt = Integer.parseInt(m.group(2));
            } else if ((m = SPEED_CAP.matcher(sentence)).find()) {
                speedCap += Double.parseDouble(m.group(1));
            } else if ((m = TIERS.matcher(sentence)).find()) {
                tiers += Integer.parseInt(m.group(1));
            } else if ((m = EXP_SHARE.matcher(sentence)).find()) {
                expShare += Double.parseDouble(m.group(1));
            } else if (sentence.startsWith("Grants ")) {
                for (String clause : sentence.split(",\\s*")) {
                    Matcher reduces = REDUCES.matcher(clause);
                    if (reduces.find()) {
                        Stat stat = stat(reduces.group(1));
                        if (stat != null) grants.add(stat, -Double.parseDouble(reduces.group(2)));
                    } else if (!CONDITION.matcher(clause).find()) {
                        amounts(clause, grants);
                    }
                }
            }
        }
        return new Effect(grants, shares, allShare, everyLevels, perLevels, onKill, onKillMillis, comboAt, combo, speedCap, noKnockback, tiers,
                expShare, onlyFor, appliable);
    }

    /** The stats a text's amounts are ("+13✯ Magic Find and +55❈ Defense"), added to {@code to}; words that aren't a stat, nothing. */
    private static void amounts(String text, Stats to) {
        Matcher m = AMOUNT.matcher(text);
        while (m.find()) {
            Stat stat = stat(m.group(3));
            if (stat == null) continue;
            double amount = Double.parseDouble(m.group(2).replace(",", ""));
            to.add(stat, m.group(1).equals("-") ? -amount : amount);
        }
    }

    /** The stat these words start with ("Magic Find", from "Magic Find Bonus"); null for none. */
    private static Stat stat(String words) {
        String[] split = words.trim().split("\\s+");
        for (int n = split.length; n > 0; n--) {
            String name = String.join(" ", Arrays.copyOf(split, n));
            for (Stat stat : Stat.values()) if (stat.getDisplayName().equals(name) && !stat.name().startsWith("RIFT_")) return stat;
        }
        return null;
    }
}
