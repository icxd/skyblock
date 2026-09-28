package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bson.Document;

import net.icxd.dungeons.common.Rank;

/**
 * Loadouts (the wiki's Loadouts, and the Loadouts tour recorded on 2026-09-27), which replaced the
 * Wardrobe: each one names an armor set and an equipment set (by their numbers in the Armor Sets and
 * Equipment Sets, which keep the pieces, see {@link StorageDocument}) and a power, any of them "None".
 * Equipping one puts its sets on (see {@link Wardrobe}) and selects its power; a None leaves that part as
 * it is (recorded: Loadout 1, armor only, left the necklace and the power on). Its pet, Heart of the
 * Mountain and Forest trees and Tuning Template aren't here yet (no pets, no saved trees, no tuning).
 * As plain functions on the profile's document:
 * <pre>
 * loadouts: [ { name: "Loadout 1", armor: 0, equipment: null, power: "Silky" }, ... ]
 * </pre>
 * Numbers are from 0 here and from 1 in the menus.
 */
public final class Loadouts {
    public static final String KEY = "loadouts";
    /** Three pages of twelve are shown; the most anyone has is 27 (18 with MVP+, 9 more from Account Upgrades). */
    public static final int MAX = 27;
    public static final int PER_PAGE = 12;
    public static final int ARMOR_SETS = 27;
    public static final int EQUIPMENT_SETS = 18;
    /** More slots from Account Upgrades at the Community Center, which isn't here. */
    public static final int ACCOUNT_UPGRADES = 9;

    /** A loadout; null parts are None. */
    public record Loadout(String name, Integer armor, Integer equipment, String power) {
        /** Set up with at least one part: only then can it be equipped. */
        public boolean customized() {
            return armor != null || equipment != null || power != null;
        }

        public Loadout withArmor(Integer armor) {
            return new Loadout(name, armor, equipment, power);
        }

        public Loadout withEquipment(Integer equipment) {
            return new Loadout(name, armor, equipment, power);
        }

        public Loadout withPower(String power) {
            return new Loadout(name, armor, equipment, power);
        }

        public Loadout withName(String name) {
            return new Loadout(name, armor, equipment, power);
        }

        Document toDocument() {
            return new Document("name", name).append("armor", armor).append("equipment", equipment).append("power", power);
        }
    }

    private Loadouts() {
    }

    /** "Loadout 3": a loadout's name until it's renamed. */
    public static String defaultName(int index) {
        return "Loadout " + (index + 1);
    }

    /** A loadout as it's never been set up. */
    public static Loadout fresh(int index) {
        return new Loadout(defaultName(index), null, null, null);
    }

    public static Loadout get(Document profile, int index) {
        List<?> list = profile.get(KEY) instanceof List<?> l ? l : List.of();
        if (index >= list.size() || !(list.get(index) instanceof Document d)) return fresh(index);
        String name = d.get("name") instanceof String s && !s.isBlank() ? s : defaultName(index);
        return new Loadout(name, number(d.get("armor")), number(d.get("equipment")), d.get("power") instanceof String p ? p : null);
    }

    public static void put(Document profile, int index, Loadout loadout) {
        List<Object> list = profile.get(KEY) instanceof List<?> l ? new ArrayList<>(l) : new ArrayList<>();
        while (list.size() <= index) list.add(null);
        list.set(index, loadout.toDocument());
        profile.put(KEY, list);
    }

    private static Integer number(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    /**
     * What's on them now matches the loadout: its sets are the worn ones and its power the selected one
     * (a None part matches only none). The recorded menu hides "Left-click to equip!" then, for the
     * loadout just equipped when it names every part, not for one that leaves some as they were.
     */
    public static boolean equipped(Loadout loadout, int wornArmor, int wornEquipment, String power) {
        return Objects.equals(loadout.armor(), wornArmor < 0 ? null : wornArmor)
                && Objects.equals(loadout.equipment(), wornEquipment < 0 ? null : wornEquipment)
                && Objects.equals(loadout.power(), power);
    }

    // How many a rank has (the wiki's Loadouts, Wardrobe and Equipment Wardrobe: VIP and MVP aren't ranks here)

    /** Loadouts: 4, 10 with VIP+, 18 with MVP+; staff have every one, the Account Upgrades' too. */
    public static int loadouts(Rank rank) {
        return byRank(rank, 4, 10, 18, MAX);
    }

    /** Armor sets, as the Wardrobe had them: 4, 10 with VIP+, 18 with MVP+. */
    public static int armorSets(Rank rank) {
        return byRank(rank, 4, 10, 18, ARMOR_SETS);
    }

    /** Equipment sets: 2, 5 with VIP+, 9 with MVP+. */
    public static int equipmentSets(Rank rank) {
        return byRank(rank, 2, 5, 9, EQUIPMENT_SETS);
    }

    private static int byRank(Rank rank, int none, int vipPlus, int mvpPlus, int staff) {
        if (rank.isEqualOrStrongerThan(Rank.STAFF)) return staff;
        if (rank.isEqualOrStrongerThan(Rank.MVP_PLUS)) return mvpPlus;
        if (rank.isEqualOrStrongerThan(Rank.VIP_PLUS)) return vipPlus;
        return none;
    }

    /** "&aVIP&6+": a rank as the locked slots name it (its prefix without the brackets). */
    static String rankName(Rank rank) {
        String prefix = rank.getPrefix().replace('§', '&').trim();
        return prefix.replace("[", "").replace("]", "");
    }

    /**
     * Where a locked slot's unlocked from, as the recorded locked loadouts list it ("&8▶ &aAccount
     * Upgrades &8- &69 Slots"): the ranks with more than theirs, with how many that rank has, then the
     * Account Upgrades. Only the Account Upgrades line is recorded (an MVP+ player's); the rank lines are
     * in its style, UNKNOWN.
     */
    static List<String> unlockLines(Rank rank, java.util.function.ToIntFunction<Rank> slots) {
        List<String> lines = new ArrayList<>(List.of("&7Unlock more slots from:"));
        for (Rank higher : List.of(Rank.VIP_PLUS, Rank.MVP_PLUS)) {
            if (slots.applyAsInt(higher) > slots.applyAsInt(rank)) {
                lines.add("&8▶ " + rankName(higher) + " &8- &6" + slots.applyAsInt(higher) + " Slots");
            }
        }
        lines.add("&8▶ &aAccount Upgrades &8- &6" + ACCOUNT_UPGRADES + " Slots");
        lines.addAll(List.of("", "&cUnlock more slots from &dElizabeth &cat", "&cthe &bCommunity Center"));
        return lines;
    }
}
