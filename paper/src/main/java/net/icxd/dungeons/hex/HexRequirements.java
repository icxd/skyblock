package net.icxd.dungeons.hex;

import org.bukkit.entity.Player;

import net.icxd.dungeons.item.requirement.skill.SkillRequirement;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.User;

/**
 * What Hypixel asks of a player before the Hex (the wiki's The Hex and Commands): Museum Milestone 8 to use it
 * at all, a Booster Cookie's buff for {@code /hex}, and Carpentry 20 for Books and Modifiers, 25 for Item
 * Upgrades and Gemstones. A Sandbox profile needs none of it (the owner: "On sandbox mode, there should be no
 * requirements to use The Hex"). Neither the Museum nor the Cookie Buff is here yet, so on a Normal profile both
 * count as met until they are (the owner's rule for what's missing: "all can be unlocked for now since the NPCs
 * are missing"); Carpentry is. Main thread.
 */
public final class HexRequirements {
    private HexRequirements() {
    }

    /** On a Sandbox profile: no requirements, and nothing costs anything (see HexCosts). False while their data isn't loaded. */
    public static boolean sandbox(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        return user != null && user.mode() == ProfileMode.SANDBOX;
    }

    /** Whether they may use the Hex at all: Sandbox, or the Museum and (for /hex) the Cookie Buff. */
    public static boolean mayOpen(Player player) {
        return sandbox(player) || museumMilestone(player) && boosterCookie(player);
    }

    /** Museum Milestone 8 ("Prosperous"). LATER: there's no Museum yet, so everyone has it. */
    public static boolean museumMilestone(Player player) {
        return true;
    }

    /** A Booster Cookie's buff, which {@code /hex} needs. LATER: there's no Cookie Buff yet, so everyone has it. */
    public static boolean boosterCookie(Player player) {
        return true;
    }

    /** Carpentry at {@code level} or above (0: nothing needed); always on a Sandbox profile. */
    public static boolean carpentry(Player player, int level) {
        return level <= 0 || sandbox(player) || Skills.level(player, Skill.CARPENTRY) >= level;
    }

    /** "&4❣ &cRequires &aCarpentry Skill 20&c.", the plugin's requirement line (SkillRequirement). */
    public static String carpentryLine(int level) {
        return new SkillRequirement(Skill.CARPENTRY, level).lore().getFirst();
    }
}
