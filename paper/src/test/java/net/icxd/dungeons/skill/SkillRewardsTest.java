package net.icxd.dungeons.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Levels' reward lines against the recorded items (research skills.md 1.2, 1.3; the recordings have
 * private-use glyphs where these have the classic stat symbols).
 */
class SkillRewardsTest {
    @Test
    void combatOne() {
        assertEquals(List.of(
                "&8+&aAccess to &cSpider's Den",
                "&eWarrior I",
                "  &fDeal &a4%&f more damage to mobs.",
                "&8+&a0.5% &9☣ Crit Chance",
                "&8+&6100 &7Coins",
                "&8+&b5 SkyBlock XP"), SkillRewards.lines(Skill.COMBAT, 1));
    }

    @Test
    void combatTen() {
        assertEquals(List.of(
                "&eWarrior X",
                "  &fDeal &836➜&a40%&f more damage to mobs.",
                "&8+&a0.5% &9☣ Crit Chance",
                "&aDisplaced Leech &7Power Stone",
                "&8+&67,500 &7Coins",
                "&8+&b5 SkyBlock XP"), SkillRewards.lines(Skill.COMBAT, 10));
    }

    @Test
    void combatTwentyFive() {
        assertEquals(List.of(
                "&eWarrior XXV",
                "  &fDeal &896➜&a100%&f more damage to mobs.",
                "&8+&a0.5% &9☣ Crit Chance",
                "&5Horns of Torment &7Power Stone",
                "&6Bubba Blister &7Power Stone",
                "&6Eccentric Painting &7Power Stone",
                "&5Magma Urchin &7Power Stone",
                "&5Precious Pearl &7Power Stone",
                "&8+&6225,000 &7Coins",
                "&8+&b10 SkyBlock XP"), SkillRewards.lines(Skill.COMBAT, 25));
    }

    @Test
    void combatFiftyOne() {
        assertEquals(List.of(
                "&eWarrior LI",
                "  &fDeal &8200➜&a201%&f more damage to mobs.",
                "&8+&a0.5% &9☣ Crit Chance",
                "&8+&61,000,000 &7Coins",
                "&8+&b30 SkyBlock XP"), SkillRewards.lines(Skill.COMBAT, 51));
    }

    @Test
    void combatUnlocks() {
        assertEquals("&8+&aAccess to &dThe End", SkillRewards.lines(Skill.COMBAT, 12).get(0));
        assertEquals("&8+&aAccess to &cCrimson Isle", SkillRewards.lines(Skill.COMBAT, 22).get(0));
        List<String> twenty = SkillRewards.lines(Skill.COMBAT, 20);
        assertEquals("&8+&aAccess to &5Zealot Bruiser Hideout", twenty.get(3));
        assertEquals(List.of("&6Fang-tastic Chocolate Chip &7Power", "&7Stone"), twenty.subList(12, 14));
        assertEquals("&fDeal &8209➜&a210%&f more damage to mobs.", SkillRewards.lines(Skill.COMBAT, 60).get(1).trim());
    }

    /** Each other skill's recorded next level. */
    @Test
    void otherSkills() {
        assertEquals(List.of("&eFarmhand XXII",
                "  &fGrants &a+&884➜&a88&f &6☘ Farming Fortune&f,",
                "  &fwhich increases your chance for",
                "  &fmultiple crops.",
                "&8+&a4 &c❤ Health",
                "&8+&6150,000 &7Coins",
                "&8+&b10 SkyBlock XP"), SkillRewards.lines(Skill.FARMING, 22));
        assertEquals(List.of("&eTreasure Hunter XVIII",
                "  &fGrants &a+&81.7➜&a1.8&f &6⛃ Treasure Chance&f.",
                "&8+&a3 &c❤ Health",
                "&8+&665,000 &7Coins",
                "&8+&b10 SkyBlock XP"), SkillRewards.lines(Skill.FISHING, 18));
        List<String> mining = SkillRewards.lines(Skill.MINING, 30);
        assertEquals(List.of("&eSpelunker XXX",
                "  &fGrants &a+&8116➜&a120&f &6☘ Mining Fortune&f,",
                "  &fwhich increases your chance for",
                "  &fmultiple ore drops.",
                "&8+&a2 ❈ Defense",
                "&5Amber Material &7Reforge"), mining.subList(0, 6));
        assertEquals(List.of("&8+&6350,000 &7Coins", "&8+&b20 SkyBlock XP"), mining.subList(mining.size() - 2, mining.size()));
        assertEquals(List.of("&eLogger XIV",
                "  &fGrants &a+&852➜&a56&f &6☘ Foraging Fortune&f,",
                "  &fwhich increases your chance for",
                "  &fmultiple logs.",
                "&8+&a1 &c❁ Strength",
                "&8+&625,000 &7Coins",
                "&8+&b10 SkyBlock XP"), SkillRewards.lines(Skill.FORAGING, 14));
        assertEquals(List.of("&eConjurer XXXVI",
                "  &fGain &8175➜&a180%&f more experience",
                "  &forbs from any source.",
                "&8+&a0.5% &c๑ Ability Damage",
                "&8+&a2 &b✎ Intelligence",
                "&d&lSoul Eater Enchantment",
                "&8+&6500,000 &7Coins",
                "&8+&b20 SkyBlock XP"), SkillRewards.lines(Skill.ENCHANTING, 36));
        assertEquals(List.of("&eBrewer XXIII",
                "  &fPotions that you brew have a",
                "  &822➜&a23%&f longer duration.",
                "&8+&a2 &b✎ Intelligence",
                "&8+&6175,000 &7Coins",
                "&8+&b10 SkyBlock XP"), SkillRewards.lines(Skill.ALCHEMY, 23));
        assertEquals(List.of("&8+&a1 &c❤ Health",
                "&fMedium Shelves &7Furniture Recipe",
                "&8+&665,000 &7Coins",
                "&8+&b10 SkyBlock XP"), SkillRewards.lines(Skill.CARPENTRY, 18));
        assertEquals(List.of("&7Access to Level &511 &7Runes"), SkillRewards.lines(Skill.RUNECRAFTING, 11));
        assertEquals(List.of("&eZoologist XXXI",
                "  &fGain &830➜&a31%&f extra pet exp.",
                "&8+&fGain &86➜&a6.2% &fExp Share rate.",
                "&8+&a1 &d♣ Pet Luck",
                "&8+&6375,000 &7Coins",
                "&8+&b20 SkyBlock XP"), SkillRewards.lines(Skill.TAMING, 31));
        assertEquals(List.of("&fAbility to purchase &dSocial Display",
                "&ffrom Amelia",
                "&8+&6250 &7Coins"), SkillRewards.lines(Skill.SOCIAL, 2));
        assertEquals(List.of("&eCharming IX",
                "  &fGrants &a+&80.32➜&a0.36&f &b❣ Charm Chance&f.",
                "&8+&a1 &d☘ Hunting Fortune",
                "&8+&65,000 &7Coins",
                "&8+&b5 SkyBlock XP"), SkillRewards.lines(Skill.HUNTING, 9));
    }

    /** The skill's own item has the perk as it is now, with no change: "&fDeal &a96%&f more damage to mobs." at XXIV. */
    @Test
    void perkNow() {
        assertEquals(List.of("&fDeal &a96%&f more damage to mobs."), SkillRewards.perkDescription(Skill.COMBAT, 24, false));
        assertEquals("&fGrants &a+88&f &6☘ Farming Fortune&f,", SkillRewards.perkDescription(Skill.FARMING, 22, false).get(0));
        assertEquals("&fGrants &a+4&f &6☘ Farming Fortune&f,", SkillRewards.perkDescription(Skill.FARMING, 1, true).get(0));
    }
}
