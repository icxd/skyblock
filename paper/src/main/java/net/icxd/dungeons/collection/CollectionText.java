package net.icxd.dungeons.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.icxd.dungeons.collection.CollectionData.Collection;
import net.icxd.dungeons.collection.CollectionData.Reward;
import net.icxd.dungeons.collection.CollectionData.Tier;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillText;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * How collections are written in the menus and chat, as recorded (the SkyBlock Menu tour, 00:40-01:04):
 * the progress lines and bars (the skills' bars, see {@link SkillText}), the tiers' names and what each
 * tier gives. No server needed.
 */
public final class CollectionText {
    /**
     * How wide a generated line of these menus gets before it wraps, in pixels: fitted to the recorded ones
     * ("View all your Rotten Flesh Collection" 183 and "  Fire Protection Exp Discount (-25%)" 188 stay on a
     * line; "View all your The Professor Collection" 195 and "Master Mode completions reward more" 191 break).
     */
    public static final int WIDTH = 190;
    /** The rule around a level-up message: SkyHanni's pattern for it ({@code §[e3]§l▬{64}}), in yellow. */
    public static final String RULE = "&e&l" + "▬".repeat(64);

    private CollectionText() {
    }

    /** "Blaze Rod VII"; tier 0 is only the name. */
    public static String named(Collection collection, int tier) {
        return tier > 0 ? collection.name() + " " + Utils.getRomanNumeral(tier) : collection.name();
    }

    /** "&e70&6%" in progress, "&a100%" once there (the tiers' "Progress:"). */
    public static String progress(double fraction) {
        return fraction >= 1 ? "&a100%" : "&e" + SkillText.percent(fraction) + "&6%";
    }

    /** "&e73.5&6%", or "&a100&6%" once complete (the Recipe Book's "Recipes Unlocked:"; the collections' is UNKNOWN at 100%). */
    public static String unlocked(double fraction) {
        return (fraction >= 1 ? "&a" : "&e") + SkillText.percent(fraction) + "&6%";
    }

    /** "&2&l&m...&r &e61&6/&e83": how many of how many, on the bar. */
    public static String bar(long of, long all) {
        double fraction = all <= 0 ? 0 : Math.min(1, (double) of / all);
        return SkillText.bar(fraction, of, all);
    }

    /** How far a count is toward a tier's amount, 0 to 1. */
    public static double fraction(long count, long amount) {
        return amount <= 0 ? 1 : Math.min(1, (double) count / amount);
    }

    /** Text wrapped at {@link #WIDTH}, as these menus wrap what they write. */
    public static List<String> wrap(String text) {
        return Text.wrap(text, WIDTH);
    }

    // What a tier gives

    /** A reward's line in the menus (without the two spaces they're indented by). */
    public static String line(Reward reward) {
        return switch (reward.type()) {
            case SKYBLOCK_XP -> "&8+&b" + reward.amount() + " SkyBlock XP";
            case SKILL_XP -> "&8+&3" + Text.number(reward.amount()) + " &7" + skillName(reward.skill()) + " Experience";
            case EXP_DISCOUNT -> "&9" + reward.name() + " &7Exp Discount &a(-" + reward.percent() + "%)";
            // Singular for one: UNKNOWN (only 9 recorded).
            case SLOTS -> "&8+&7" + reward.amount() + " &a" + reward.name() + " &7" + (reward.amount() == 1 ? "Slot" : "Slots");
            case MINION_RECIPES -> color(reward.item(), '9') + reward.name() + " Minion &7Recipes";
            case PET_RECIPE -> "&7[Lvl 1] &f" + reward.name() + " &7Recipe";
            // As the wiki's Collection UI writes it: UNKNOWN in the game.
            case FORGE_RECIPE -> "&f" + reward.name() + " &7Dwarven Forge Recipe";
            case RECIPE -> color(reward.item(), 'f') + reward.name() + " &7Recipe";
            // As the wiki's Collection UI writes it: UNKNOWN in the game.
            case TRADE -> color(reward.item(), 'f') + reward.name() + " &7Trade";
            // The recorded "&aQuiver"; the other unlocks' colour (a stat's too) is UNKNOWN, taken the same.
            case STAT, UNLOCK -> "&a" + reward.text();
            case ITEM -> color(reward.item(), 'f') + reward.name() + (reward.amount() > 1 ? "&8 x" + reward.amount() : "");
            case ESSENCE -> "&d" + capitalized(reward.essence()) + " Essence" + (reward.amount() > 0 ? " &8x" + reward.amount() : "");
        };
    }

    /**
     * A tier's rewards as the menus list them, two spaces in and wrapped; without the SkyBlock XP where the
     * menu leaves it out (a boss's category item).
     */
    public static List<String> rewardLines(Tier tier, boolean withSkyBlockXp) {
        List<String> lines = new ArrayList<>();
        for (Reward reward : tier.rewards()) {
            if (!withSkyBlockXp && reward.type() == Reward.Type.SKYBLOCK_XP) continue;
            for (String line : wrap(line(reward))) lines.add("  " + line);
        }
        return lines;
    }

    /** "Rewards:" for more than one line, "Reward:" for one (the recorded "Bonzo I Reward:"). */
    public static String rewardsWord(List<String> lines) {
        return lines.size() == 1 ? "Reward:" : "Rewards:";
    }

    /** The rarity colour of an item ("&9"), or {@code fallback}'s if there's no such item. */
    static String color(String itemId, char fallback) {
        SkyBlockItem item = ItemRegistry.get(itemId);
        return "&" + (item == null ? fallback : item.rarity().getCode());
    }

    private static String skillName(String skill) {
        try {
            return Skill.valueOf(skill).getName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return capitalized(skill);
        }
    }

    static String capitalized(String word) {
        if (word == null || word.isEmpty()) return "";
        String lower = word.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    // The level-up message

    /**
     * The chat message for reaching {@code tier}: the rules and the header are what mods match on Hypixel's
     * ("  §r§6§lCOLLECTION LEVEL UP ", SkyHanni's CompactBingoChat); the rest is UNKNOWN (none recorded), laid
     * out like the skills' level-up: the old and new tier, and the tier's rewards as the menus list them.
     */
    public static List<String> levelUp(Collection collection, int tier) {
        List<String> lines = new ArrayList<>();
        lines.add(RULE);
        String from = tier > 1 ? "&8" + Utils.getRomanNumeral(tier - 1) + "➜" : "";
        lines.add("  &6&lCOLLECTION LEVEL UP &e" + collection.name() + " " + from + "&e" + Utils.getRomanNumeral(tier));
        lines.add("");
        lines.add("  &a&lREWARDS");
        for (String line : rewardLines(collection.tier(tier), true)) lines.add("  " + line);
        lines.add(RULE);
        return lines;
    }
}
