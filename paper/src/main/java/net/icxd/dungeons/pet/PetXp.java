package net.icxd.dungeons.pet;

import java.util.EnumSet;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * What skill XP gives the summoned pet, and Taming. Pet XP is the skill's XP before Wisdom x the pet's share of it x
 * (1 + Taming level / 100) x (1 + its held item's boost) x its own (the Reindeer's 2): the owner's formula, and the
 * recording's (2026-10-25: a Farming Bee went from level 1 to 4 on 6.4 a wheat, the wheat's 4 Farming XP x 1.6, where
 * the action bar showed 4.8 with Wisdom). Its share (the wiki's Pets): all of its own skill's (every skill's for an
 * All Skills pet), a third of another's, and a quarter of that again for Alchemy or Enchanting; Fishing's is 1.5x;
 * Carpentry, Taming, Runecrafting and Social give none, and a pet fed instead (the Wisp) gets none.
 *
 * <p>Taming XP is 30% of what the pet gets before the Taming level, its item and its own: a 2020 measurement (Farming,
 * Combat and Foraging 30% of the skill's XP, Fishing and Mining 45%, when Mining's was 1.5x too; the Hypixel forums'
 * "Calculating the best skill for Taming EXP"). Not shown as it comes in (the recording's action bar never showed
 * any): its levels are said, as every skill's. A pet at its most level still gets its XP, and Taming its share. Main
 * thread.
 */
public final class PetXp {
    /** Skills whose XP gives pets none. */
    static final Set<Skill> NONE = EnumSet.of(Skill.CARPENTRY, Skill.TAMING, Skill.RUNECRAFTING, Skill.SOCIAL);
    static final double OTHER_SKILL = 1.0 / 3;
    static final double ALCHEMY_OR_ENCHANTING = 1.0 / 4;
    static final double FISHING = 1.5;
    static final double TAMING_SHARE = 0.3;
    /** The level-up's four notes (recorded, two ticks apart): a pling at 0.8 with an orb at 0.2, the pling again at 0.4 a tick on. */
    static final float[] PLINGS = {52 / 63f, 70 / 63f, 94 / 63f, 105 / 63f};
    static final float[] ORBS = {1, 4 / 3f, 16 / 9f, 2};

    private PetXp() {
    }

    /** A skill's XP (before Wisdom) as the pet's, before Taming's, its item's and its own multiplier: its share of it. */
    static double share(Pet pet, Skill skill, double xp) {
        if (pet == null || skill == null || NONE.contains(skill) || (pet.skill() == null && !pet.allSkills()) || !(xp > 0)) return 0;
        double out = xp;
        if (skill == Skill.FISHING) out *= FISHING;
        if (!pet.allSkills() && pet.skill() != skill) {
            out *= OTHER_SKILL;
            if (skill == Skill.ALCHEMY || skill == Skill.ENCHANTING) out *= ALCHEMY_OR_ENCHANTING;
        }
        return out;
    }

    /** The pet's XP for a skill's XP (before Wisdom), with this Taming level and its held item's boost (0.4 for +40%). */
    static double petXp(Pet pet, Skill skill, double xp, int taming, double itemBoost) {
        return share(pet, skill, xp) * (1 + taming / 100.0) * (1 + itemBoost) * (pet == null ? 1 : pet.xpMultiplier());
    }

    static double tamingXp(Pet pet, Skill skill, double xp) {
        return share(pet, skill, xp) * TAMING_SHARE;
    }

    /** "&aYour &aBee &aleveled up to level &92&a!" (recorded "&aYour Bee leveled up to level &92&a!", its green the line's). */
    static String levelUp(OwnedPet pet, PetData data, int level) {
        return "&aYour " + PetText.colored(pet, data) + " &aleveled up to level &9" + level + "&a!";
    }

    /** A player gained a skill's XP ({@code base}, before Wisdom): their summoned pet's share, and Taming's. */
    public static void gained(Player player, Skill skill, double base) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || !(base > 0)) return;
        Document profile = user.profile();
        PetData data = Pets.data();
        OwnedPet active = Pets.active(profile);
        Pet kind = active == null ? null : data.pet(active.type());
        if (kind == null) return;
        double xp = petXp(kind, skill, base, Skills.level(profile, Skill.TAMING), HeldItems.expBoost(active.heldItem(), skill));
        if (!(xp > 0)) return;
        int before = Pets.level(active, data);
        OwnedPet after = active.withExp(active.exp() + xp);
        Pets.replace(profile, after);
        double taming = tamingXp(kind, skill, base);
        if (taming > 0) SkillGains.giveQuietly(player, Skill.TAMING, taming);
        int level = Pets.level(after, data);
        if (level <= before) return;
        for (int l = before + 1; l <= level; l++) player.sendMessage(Text.line(levelUp(after, data, l)));
        chime(player);
        PetFollowers.refresh(player);
        PlayerSession.of(player).invalidateStats();
        if (Pets.raiseBest(profile, data)) SkyBlockLevels.changed(player);
        user.save();
    }

    /** The level-up's notes, as recorded. */
    static void chime(Player player) {
        for (int i = 0; i < PLINGS.length; i++) {
            float pling = PLINGS[i];
            float orb = ORBS[i];
            Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
                if (!player.isOnline()) return;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.RECORDS, 0.8f, pling);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.RECORDS, 0.2f, orb);
            }, 2L * i);
            Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
                if (player.isOnline()) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.RECORDS, 0.4f, pling);
            }, 2L * i + 1);
        }
    }
}
