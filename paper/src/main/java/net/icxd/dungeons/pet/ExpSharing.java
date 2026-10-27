package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;

import net.icxd.dungeons.skill.Skill;

/**
 * Exp Sharing, as the wiki's Pets has it: pets in its slots gain the summoned pet's XP (its share of the skill's,
 * before its own multipliers) at their Exp Share rate, and then their own multipliers (Taming's, their item's boost,
 * their own). The rate: 0.2% a Taming level, +15% for an Exp Share held, to 47%. Slot #1 is everyone's; the other two
 * only with Diana's Sharing is Caring perk ("&8Enabled by a Mayor Diana perk.", recorded), and there are no mayors
 * here, so they're disabled (their pets gain nothing). No pet summoned, none gains; the summoned pet in a slot gains
 * no more. The Kuudra pet "automatically has Exp share!" and "only gains XP on the Crimson Isle" (its lines), so there
 * it shares as though in a slot, and twice if it's in one (the wiki's Kuudra Pet), its item's boost not counting
 * while it isn't summoned ("Other pet exp boost items ... will not increase experience gain when the Kuudra Pet is not
 * equipped"). Kept with the profile's pets as a pet's uuid for each slot. No server needed.
 */
public final class ExpSharing {
    static final String FIELD = "expShare";
    static final int SLOTS = 3;
    /** Slot #1's alone (the others need Diana). */
    static final int ACTIVE_SLOTS = 1;
    static final double PER_TAMING_LEVEL = 0.2;
    static final double MOST = 47;
    static final String KUUDRA = "KUUDRA";

    private ExpSharing() {
    }

    /** Whether a slot (0 for #1) shares. */
    static boolean active(int slot) {
        return slot < ACTIVE_SLOTS;
    }

    /** Each slot's pet (0 for #1), a pet that's still theirs; null for none. */
    static List<OwnedPet> slots(Document profile) {
        List<String> uuids = uuids(profile);
        List<OwnedPet> out = new ArrayList<>(SLOTS);
        for (String uuid : uuids) out.add(uuid == null ? null : Pets.find(profile, uuid));
        return out;
    }

    /** Puts a pet in a slot (null: empties it), out of any other. */
    static void put(Document profile, int slot, String uuid) {
        List<String> uuids = uuids(profile);
        for (int i = 0; i < SLOTS; i++) if (uuid != null && uuid.equals(uuids.get(i))) uuids.set(i, null);
        uuids.set(slot, uuid);
        Pets.part(profile, true).put(FIELD, uuids);
    }

    /** Out of its slot, if it's in one (it's left the menu). */
    static void forget(Document profile, String uuid) {
        List<String> uuids = uuids(profile);
        if (!uuids.contains(uuid)) return;
        uuids.replaceAll(u -> uuid.equals(u) ? null : u);
        Pets.part(profile, true).put(FIELD, uuids);
    }

    private static List<String> uuids(Document profile) {
        Document pets = Pets.part(profile, false);
        List<String> out = new ArrayList<>(Arrays.asList(new String[SLOTS]));
        if (pets != null && pets.get(FIELD) instanceof List<?> list) {
            for (int i = 0; i < SLOTS && i < list.size(); i++) out.set(i, list.get(i) instanceof String s ? s : null);
        }
        return out;
    }

    /** A pet's Exp Share rate in a slot, in percent: 0.2 a Taming level and its item's (the Exp Share's 15), to 47. */
    static double rate(int taming, String heldItem) {
        return Math.min(MOST, taming * PER_TAMING_LEVEL + HeldItems.effect(heldItem).expShare());
    }

    /** A pet sharing a gain: so many times, with or without its item's boost. */
    record Sharer(OwnedPet pet, int times, boolean itemBoosts) {
    }

    /** Who shares a gain the {@code summoned} pet made (see the class's doc). */
    static List<Sharer> sharers(Document profile, OwnedPet summoned, boolean crimsonIsle) {
        List<Sharer> out = new ArrayList<>();
        List<String> inSlots = new ArrayList<>();
        List<OwnedPet> slots = slots(profile);
        for (int i = 0; i < slots.size(); i++) {
            OwnedPet pet = slots.get(i);
            if (pet == null || !active(i) || pet.uuid().equals(summoned.uuid())) continue;
            inSlots.add(pet.uuid());
            boolean kuudra = KUUDRA.equals(pet.type());
            if (kuudra && !crimsonIsle) continue;
            out.add(new Sharer(pet, kuudra ? 2 : 1, !kuudra));
        }
        if (crimsonIsle) {
            for (OwnedPet pet : Pets.pets(profile)) {
                if (KUUDRA.equals(pet.type()) && !pet.uuid().equals(summoned.uuid()) && !inSlots.contains(pet.uuid())) out.add(new Sharer(pet, 1, false));
            }
        }
        return out;
    }

    /**
     * A sharer's XP for the summoned pet's {@code raw} (its share of the skill's, before its multipliers): at its rate,
     * then its multipliers.
     */
    static double xp(Sharer sharer, PetData.Pet kind, Skill skill, double raw, int taming) {
        String held = sharer.pet().heldItem();
        double boost = sharer.itemBoosts() ? HeldItems.expBoost(held, skill) : 0;
        return raw * rate(taming, held) / 100 * (1 + taming / 100.0) * (1 + boost) * (kind == null ? 1 : kind.xpMultiplier()) * sharer.times();
    }
}
