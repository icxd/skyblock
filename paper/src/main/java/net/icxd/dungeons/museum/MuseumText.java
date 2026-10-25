package net.icxd.dungeons.museum;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.icxd.dungeons.museum.Museum.Sort;
import net.icxd.dungeons.museum.MuseumData.Entry;
import net.icxd.dungeons.museum.MuseumData.Milestone;
import net.icxd.dungeons.skill.SkillText;

/**
 * The Museum menus' words, as recorded (2026-10-25, 00:58.4 to 02:54.3): the progress lines and bars, an entry's lines
 * (missing, donated, retrieved), the sort's and the milestones'. No server needed.
 */
final class MuseumText {
    /** The rule under a donated item's own lore. */
    static final String RULE = "&8&m" + " ".repeat(SkillText.BAR);
    static final String CLICK_TO_VIEW = "&eClick to view!";
    static final List<String> ITEM_HOW = List.of("&7Click on this item in your inventory to", "&7add it to your &9Museum&7!");
    static final List<String> SET_HOW = List.of("&7Click on an armor piece in your", "&7inventory that belongs to this armor",
            "&7set to donate the full set to your", "&9Museum&7.");
    static final List<String> RETRIEVED = List.of("&7You have retrieved this from your", "&7Museum but can add it back at any", "&7time.");
    static final String RETRIEVE = "&eClick to retrieve item!";
    /**
     * The dates' zone: a donation at 00:31 UTC on Oct 25 said "Oct 24, 2026", so it's behind UTC; New York's, as
     * Hypixel's other dates are (UNKNOWN beyond that).
     */
    static final ZoneId ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH).withZone(ZONE);
    private static final ThreadLocal<DecimalFormat> PERCENT = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    });
    private static final ThreadLocal<DecimalFormat> WHOLE = ThreadLocal.withInitial(() -> new DecimalFormat("#,##0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH)));

    private MuseumText() {
    }

    static String number(long n) {
        return WHOLE.get().format(n);
    }

    /** "0", "0.2" (1 of 636), "0.4" (1 of 245): one decimal at most. */
    static String percent(int part, int of) {
        return PERCENT.get().format(of <= 0 ? 0 : 100.0 * part / of);
    }

    /**
     * "&9&l&m &f&l&m                        &r &b1&9/&b636": blue for what's donated, as many of the 25 as are begun (1 of
     * 636 fills one, as a skill's bar does).
     */
    static String donatedBar(int part, int of) {
        int filled = SkillText.filled(of <= 0 ? 0 : (double) part / of);
        StringBuilder bar = new StringBuilder();
        if (filled > 0) bar.append("&9&l&m").append(" ".repeat(filled));
        if (filled < SkillText.BAR) bar.append("&f&l&m").append(" ".repeat(SkillText.BAR - filled));
        return bar.append("&r &b").append(number(part)).append("&9/&b").append(number(of)).toString();
    }

    /** "&7Items Donated: &e0.4&6%" and its bar. */
    static List<String> donated(int part, int of) {
        return List.of("&7Items Donated: &e" + percent(part, of) + "&6%", donatedBar(part, of));
    }

    /**
     * "&f&m                         &r &b1&3/&b100": the milestone bar, not bold, filling only whole 25ths (1 of 100 filled
     * none, recorded; its colour once filled is UNKNOWN: the dark aqua of its "/").
     */
    static String milestoneBar(int part, int of) {
        int filled = of <= 0 ? 0 : (int) Math.max(0, Math.min(SkillText.BAR, Math.floor((double) part * SkillText.BAR / of + 1e-9)));
        StringBuilder bar = new StringBuilder();
        if (filled > 0) bar.append("&3&m").append(" ".repeat(filled));
        if (filled < SkillText.BAR) bar.append("&f&m").append(" ".repeat(SkillText.BAR - filled));
        return bar.append("&r &b").append(number(part)).append("&3/&b").append(number(of)).toString();
    }

    /** The Museum Milestones item's lore, as recorded, without its "Click to view!" (the menu's own header has none). */
    static List<String> milestonesLore(int xp, int total) {
        int milestone = Museum.milestone(xp);
        List<String> lore = new ArrayList<>(List.of("&7Every &b100 SkyBlock XP &7obtained from", "&7your Museum, &6Eleanor &7will reward you.", "",
                "&dSpecial Items &7do not reward SkyBlock", "&7XP.", "", "&7Total XP: &b" + number(xp) + "&3/&b" + number(total),
                "&7Milestone: &e" + milestone + "&6/&e" + Museum.MILESTONES));
        if (milestone < Museum.MILESTONES) {
            int into = xp - milestone * Museum.MILESTONE_XP;
            lore.add("");
            lore.add("&7Progress to Milestone " + (milestone + 1) + ": &3" + percent(into, Museum.MILESTONE_XP) + "%");
            lore.add(milestoneBar(into, Museum.MILESTONE_XP));
        }
        return lore;
    }

    /** "&7Reward: &b+4 SkyBlock XP", with "&a✔" once it counts. */
    static String reward(Entry entry, boolean counts) {
        return "&7Reward: &b+" + entry.xp() + " SkyBlock XP" + (counts ? " &a✔" : "");
    }

    /** A missing entry's lines: how to donate it, its higher tier, its reward. */
    static List<String> missing(Entry entry) {
        List<String> lore = new ArrayList<>(entry.set() ? SET_HOW : ITEM_HOW);
        lore.add("");
        if (!entry.parentLines().isEmpty()) {
            lore.addAll(entry.parentLines());
            lore.add("");
        }
        lore.add(reward(entry, false));
        return lore;
    }

    /**
     * An entry that counts only by its higher tier's donation: UNKNOWN on Hypixel (never recorded). Its higher tier's
     * lines and its reward, done.
     */
    static List<String> credited(Entry entry) {
        List<String> lore = new ArrayList<>(entry.parentLines());
        lore.add("");
        lore.add(reward(entry, true));
        return lore;
    }

    /**
     * What goes under a donated item's own lore, as recorded with Extra Details on: the rule, the date it was donated
     * and made ("Created", for an item that knows when), its reward, and what a click does. Its value and display slot
     * aren't here (no item values, no displays). With Extra Details off the dates go (UNKNOWN: which lines stay).
     *
     * @param created when the item was made; 0 for an item that doesn't say
     */
    static List<String> donatedLines(Entry entry, long donated, long created, boolean details, boolean retrieved) {
        List<String> lore = new ArrayList<>(List.of(RULE));
        if (details) {
            lore.add("&7Donated: &b" + date(donated));
            if (created > 0) lore.add("&7Created: &a" + date(created));
        }
        lore.add(reward(entry, true));
        if (retrieved) {
            lore.add(RULE);
            lore.addAll(RETRIEVED);
            lore.add("");
            lore.addAll(entry.set() ? SET_HOW : ITEM_HOW);
        } else {
            lore.add("");
            lore.add(RETRIEVE);
        }
        return lore;
    }

    /** "Oct 24, 2026". */
    static String date(long millis) {
        return DATE.format(Instant.ofEpochMilli(millis));
    }

    /** The Sort button's lore: an empty line, the sorts with this one marked, how to switch. */
    static List<String> sortLore(Sort current) {
        List<String> lore = new ArrayList<>(List.of(""));
        for (Sort sort : Sort.values()) lore.add(sort == current ? "&b▶ " + sort.label() : "&7  " + sort.label());
        lore.addAll(List.of("", "&eClick to switch sort!", "&bRight-Click to go backwards!"));
        return lore;
    }

    static List<String> extraDetailsLore(boolean on) {
        return List.of("&7Show info on your donated items:", "&8- &7Date of the donation", "&8- &7Item created edition", "&8- &7Item Value",
                "&8- &7Clean Item Value", "&8- &7Museum Display Slot", "", "&7Toggled: " + (on ? "&a&lON" : "&c&lOFF"), "", "&eClick to toggle!");
    }

    /**
     * A milestone on the path: its XP and rewards; the next one's progress; "You can't claim this reward!" until it's
     * reached. Once reached it's unlocked: what Hypixel shows then, and whether it's claimed, is UNKNOWN (never
     * recorded; none of its rewards are here yet).
     */
    static List<String> milestoneLore(Milestone milestone, int xp) {
        List<String> lore = new ArrayList<>(List.of("&7Required XP: &e" + number(milestone.xp()), "", "&7Rewards:"));
        lore.addAll(milestone.rewards());
        lore.add("");
        if (xp >= milestone.xp()) {
            lore.add("&a&lUNLOCKED");
            return lore;
        }
        int from = milestone.xp() - Museum.MILESTONE_XP;
        if (xp >= from) {
            lore.add("&7Progress: &3" + percent(xp - from, Museum.MILESTONE_XP) + "%");
            lore.add(milestoneBar(xp - from, Museum.MILESTONE_XP));
            lore.add("");
        }
        lore.add("&cYou can't claim this reward!");
        return lore;
    }
}
