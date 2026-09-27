package net.icxd.dungeons.dungeons.classes;

import java.util.ArrayList;
import java.util.List;

import net.icxd.dungeons.dungeons.DungeonClass;

/**
 * The Class Details menu's three items for a class (right click on it in Ready Up): its passives,
 * its Dungeon Orb abilities and its ghost abilities, verbatim as recorded (RUN2 00:57.6 to 01:04.3,
 * at Healer 15, Mage 15, Berserk 20, Archer 16 and Tank 14). Numbers that {@link ClassBonus} or the
 * wiki give per level follow the level; the rest are the recorded ones at every level (no second
 * level was recorded to tell them apart).
 */
public final class ClassDetails {
    /** What each of the three items is: its name, then its lore. */
    public record Item(String name, List<String> lore) {
    }

    /** The heads of the orb and ghost items, per class (textures as recorded). */
    public static String orbTexture(DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case HEALER -> "dc6bacd36ed60f533138e759c425946222b78eda6b616216f6dcc08e90d33e";
            case MAGE -> "96636ba6988ce9b40ddc749a09ce0fb939af526005995c18d323ac96625f0d6d";
            case BERSERK -> "64457b2d7acf522d1912f0d6c6139440ba9d333a6bf61a2e9440e7fb3788939e";
            case ARCHER -> "c3d14561bbd063f70424a8afcc37bfe9c74562ea36f7bfa3f23206830c64faf1";
            case TANK -> "96c8447a8b6b0e0c7e7629c6898ec9c749a7a0a2b452b9c3852c7847bb4dc5";
        };
    }

    /** The Ghost Abilities item's head, the same for every class. */
    public static final String GHOST_TEXTURE = "f1d26142909bde6d9ab136fe8928dd67b662843d6c1813e108842c23f1c793bb";

    private ClassDetails() {
    }

    private static String n(ClassBonus bonus, int level) {
        return ClassBonus.format(bonus.value(level, false));
    }

    /**
     * Slot 11. {@code intelligence} is the player's (the Mage Staff's share grows with it: 30% plus
     * 0.09% a point, which is MCW's formula and fits the recorded 113.7% at 930).
     */
    public static Item passives(DungeonClass dungeonClass, int level, double intelligence) {
        int l = Math.clamp(level, 0, ClassBonus.MAX_LEVEL);
        List<String> lore = switch (dungeonClass) {
            case HEALER -> List.of(
                    "&8∙ &6Class Passive: Renew",
                    "&7Grants &a" + ClassBonus.multiplier(1 + ClassBonus.HEALER_RENEW.value(l, false) / 100) + "x  Mending&7, which increases your",
                    "&7healing on others.",
                    "",
                    "&7While playing Healer, &a Mending &7also boosts",
                    "&7healing on &7&oyourself&7.",
                    "",
                    "&8∙ &aClass Passive: Healing Aura",
                    "&7Passively heals all living teammates within a &e8",
                    // MCW +0.1% every 5 levels; from 1% to fit the recorded 1.3% at 15.
                    "&7block radius for &a" + ClassBonus.format(1 + 0.1 * (l / 5)) + "% &7HP per second.",
                    "",
                    "&8∙ &aClass Passive: Revive",
                    "&7Spawns a fairy that follows you and uses it's",
                    "&7energy to revives dead teammates, while no one is",
                    "&7dead it will stick around and provide support.",
                    // MCW 100 seconds, 1 less a level (85 at 15, as recorded).
                    "&7Vanishes for &e" + (100 - l) + " &7seconds after reviving a player.",
                    "",
                    "&8∙ &cClass Passive: Orbies",
                    "&7Enemies that you help kill have a &e15% &7chance to",
                    "&7drop an orb that anyone can pick up. The orb",
                    "&7heals the player that picked it up and grants",
                    "&7them a random buff.",
                    "",
                    "&8∙ &cClass Passive: Soul Tether",
                    "&7Clicking on a teammate creates a tether",
                    "&7connecting yourself to them. Tethered teammates",
                    "&7receive &a0.5% &7of your maximum health as healing",
                    "&7every second while they are within &e30 &7blocks of",
                    "&7you. Every &e5 &7melee hits you deal while tethered",
                    "&7will heal both players for &a1% &7of their maximum",
                    "&7health.",
                    "",
                    "&8∙ &cClass Passive: Overheal",
                    "&7Healing a player that is full health will convert",
                    // MCW 20% and +0.4% a level (26% at 15, as recorded).
                    "&7the healing into an absorption shield up to &a" + ClassBonus.format(20 + 0.4 * l) + "% &7of",
                    "&7their health.");
            case MAGE -> List.of(
                    "&8∙ &6Class Passive: Mage Staff",
                    "&7All your melee weapon attacks become ranged.",
                    "&7Those attacks deal &a" + ClassBonus.format(30 + 0.09 * intelligence) + "% &7of their melee damage",
                    "&7(increased by your Intelligence) up to &a10&7 blocks,",
                    "&7after which damage is reduced.",
                    "",
                    "&8∙ &6Class Passive: Efficient Spells",
                    "&7All abilities have a &a" + n(ClassBonus.MAGE_EFFICIENT_SPELLS, l) + "%&7 shorter cooldown.");
            case BERSERK -> List.of(
                    "&8∙ &cClass Passive: Bloodlust",
                    "&7Your next hit after killing a monster deals &c" + n(ClassBonus.BERSERK_BLOODLUST_DAMAGE, l) + "%",
                    "&7increased damage. This bonus will expire after &e" + n(ClassBonus.BERSERK_BLOODLUST_DURATION, l),
                    "&7seconds. Reduces the cooldown remaining on",
                    "&7Throwing Axe by &b1 &7second on activation. Heals",
                    "&7you for &a" + n(ClassBonus.BERSERK_BLOODLUST_HEAL, l) + "% &7of your missing health every hit.",
                    "",
                    "&8∙ &cClass Passive: Lust For Blood",
                    "&7Every time you hit an enemy, increases the",
                    "&7damage you deal to that enemy by &c" + n(ClassBonus.BERSERK_LUST_PER_HIT, l) + "% &7up to a",
                    "&7maximum of &a" + n(ClassBonus.BERSERK_LUST_CAP, l) + "%&7.",
                    "",
                    "&8∙ &cClass Passive: Indomitable",
                    "&7Gain &c" + n(ClassBonus.BERSERK_INDOMITABLE, l) + "% &7of your Strength as Defense.",
                    "",
                    "&8∙ &cClass Passive: Weapon Master",
                    "&7Your melee weapons hit up to &c5 &7enemies within",
                    "&7your swing range in a frontal cone. Increases",
                    "&7your maximum swing range by &c" + n(ClassBonus.BERSERK_WEAPON_MASTER, l) + " &7blocks.");
            case ARCHER -> List.of(
                    "&8∙ &aClass Passive: Doubleshot",
                    "&a" + n(ClassBonus.ARCHER_DOUBLESHOT, l) + "% &7chance to shoot a second arrow.",
                    "",
                    "&8∙ &aClass Passive: Bone Plating",
                    // UNKNOWN formula (it scales with the level and Crit Damage): the recorded level-16 number.
                    "&7Reduces your next incoming hit by &a1,554.04",
                    "&7damage (scales with Archer Level and Critical",
                    "&7Damage).",
                    "",
                    "&8∙ &aClass Passive: Bouncy Arrows",
                    // MCW +2% a level (32% at 16, as recorded).
                    "&a" + (2 * l) + "% &7chance for your arrows to bounce to an",
                    "&7additional target.",
                    "&8Cooldown: &a15s");
            case TANK -> List.of(
                    "&8∙ &aClass Passive: Protective Barrier",
                    "&7Grants &a" + ClassBonus.multiplier(1 + ClassBonus.TANK_PROTECTIVE_BARRIER.value(l, false) / 100) + "x  Defense&7.",
                    "&7Upon falling below &a50% &7health, gain a &e10% &7health",
                    "&7absorption shield.",
                    "&8Cooldown: &a90s",
                    "",
                    "&8∙ &aClass Passive: Taunt",
                    "&7Increases the chance for mobs to target you",
                    "&7when you are above &a25% &7HP.",
                    "",
                    "&8∙ &aClass Passive: Diversion",
                    "&7Divert &c80% &7of the damage taken by your",
                    "&7teammates within &e30 &7blocks around you.",
                    "",
                    "&8∙ &aClass Passive: Defensive Stance",
                    "&7Grants immunity to knockback from mobs.");
        };
        return new Item("&aClass Passives", lore);
    }

    /** Slot 13. */
    public static Item orbAbilities(DungeonClass dungeonClass, int level) {
        int l = Math.clamp(level, 0, ClassBonus.MAX_LEVEL);
        List<String> lore = switch (dungeonClass) {
            case HEALER -> List.of(
                    "&8∙ &6Ability: Healing Circle  &e&lRIGHT CLICK",
                    "&7Launches a healing orb that creates",
                    "&7a healing circle where it lands. The",
                    "&7healing circle heals friendly players",
                    "&7inside for &a1% &7of their health every",
                    "&7second.",
                    "&8Cooldown: &a2s",
                    "",
                    "&8∙ &6Ability: Wish  &e&lRIGHT CLICK",
                    "&7Heals everyone in your group to full",
                    "&7health and grants them a shield for",
                    "&a20% &7of their maximum health. The",
                    "&7cooldown is reduced by &b10 &7seconds",
                    "&7for every player below &a25% &7health.",
                    "&8Cooldown: &a120s");
            case MAGE -> List.of(
                    "&8∙ &6Ability: Guided Sheep  &e&lRIGHT CLICK",
                    "&7Shoots a Guided Sheep, dealing",
                    "&7damage based on your Mage level!",
                    "&8Acts as Superboom TNT!",
                    "&8Cooldown: &a30s",
                    "",
                    "&8∙ &6Ability: Thunderstorm  &e&lRIGHT CLICK",
                    "&7Unleash a Thunderstorm, striking",
                    "&7enemies in a &a10&7 block radius for &a15",
                    "&7seconds, dealing damage based on",
                    "&7your Mage level!",
                    "&8Cooldown: &a500s");
            case BERSERK -> List.of(
                    "&8∙ &6Ability: Throwing Axe  &e&lRIGHT CLICK",
                    "&7Throw an Axe, dealing the same",
                    "&7damage as your highest hit in the",
                    "&7last minute.",
                    "&8Cooldown: &a10s",
                    "",
                    "&8∙ &6Ability: Ragnarok  &e&lRIGHT CLICK",
                    "&7Grants &e100 Attack Speed&7, &f400",
                    "&fSpeed &7and &c1.5x &7melee damage",
                    "&7increase for &b15 &7seconds. Also",
                    "&7summons &a3&7 Zombie minions to aid you",
                    "&7in battle!",
                    "&8Cooldown: &a60s");
            case ARCHER -> List.of(
                    "&8∙ &6Ability: Explosive Shot  &e&lRIGHT CLICK",
                    "&7Shoots a volley of 3 Arrows that",
                    "&7explodes on impact, dealing the same",
                    "&7damage as your highest bow hit in",
                    "&7the last minute in a &a4&7 block radius.",
                    "&8Acts as Superboom TNT!",
                    // MCW 40 seconds, 2 less every 5 levels (34 at 16, as recorded).
                    "&8Cooldown: &a" + (40 - 2 * (l / 5)) + "s",
                    "",
                    "&8∙ &6Ability: Rapid Fire  &e&lLEFT CLICK",
                    // 4 seconds and one more every 10 levels fits the recorded 5 at 16 (MCW says 3).
                    "&7Shoots &a5&7 Arrows per second for &a" + (4 + l / 10),
                    "&7seconds! Arrows deal &a75.0%&7 of your",
                    "&7highest Bow hit in the last minute.",
                    "&8Cooldown: &a100s");
            case TANK -> List.of(
                    "&8∙ &6Ability: Seismic Wave  &e&lRIGHT CLICK",
                    "&7A Seismic Wave emerges from",
                    "&7underneath you and travels in a",
                    "&7straight line. Deals &c20,000 +10%",
                    "&7every &a+50 Defense &7in damage to",
                    "&7any enemy in its path.",
                    "&8Acts as Superboom TNT!",
                    "&8Cooldown: &a15s",
                    "",
                    "&8∙ &6Ability: Castle of Stone  &e&lRIGHT CLICK",
                    "&7Fortifies your defence, reducing the",
                    "&7damage you take by &c70% &7for &a20",
                    "&7seconds. Also aggros all enemies in",
                    "&7a &a10&7 block radius.",
                    "&8Cooldown: &a150s");
        };
        return new Item("&aDungeon Orb Abilities", lore);
    }

    /** Slot 15. */
    public static Item ghostAbilities(DungeonClass dungeonClass) {
        List<String> lore = switch (dungeonClass) {
            case HEALER -> List.of(
                    "&8∙ &fGhost Ability: Healing Potion",
                    "&7Throw a potion which heals all teammates in a &a10",
                    "&7block radius for &a10%&7 of their max health and",
                    "&c+100 Health&7.",
                    "",
                    "&8∙ &fGhost Ability: Revive",
                    "&7Revive yourself after 50 seconds.");
            case MAGE -> List.of(
                    "&8∙ &fGhost Ability: Instant Wall",
                    "&7Create a 5x3 wall at the block you are looking at",
                    "&7which lasts for &a10&7 seconds.",
                    "",
                    "&8∙ &fGhost Ability: Fireball",
                    "&7Shoots a fireball, dealing damage based on your",
                    "&7Mage level!");
            case BERSERK -> List.of(
                    "&8∙ &fGhost Ability: Buff Potion",
                    "&7Throw a potion which temporarily gives all",
                    "&7teammates in a &a10&7 block radius &c+30 Strength&7.",
                    "",
                    "&8∙ &fGhost Ability: Ghost Axe",
                    "&7Throw an Axe, dealing damage based on your",
                    "&7Berserk level!");
            case ARCHER -> List.of(
                    "&8∙ &fGhost Ability: Stun Bow",
                    "&7Temporarily stuns any non-boss",
                    "&7dungeon monster for a short period",
                    "&7of time when shot.",
                    "&8Cooldown: &a10s",
                    "",
                    "&8∙ &fGhost Ability: Healing Bow",
                    "&7Shoot a teammate to heal them for",
                    "&a10%&7 of their max HP and grant them",
                    "&c+50 Strength&7 for 10 seconds.",
                    "&8Cooldown: &a10s");
            case TANK -> List.of(
                    "&8∙ &fGhost Ability: Stun Potion",
                    "&7Throw a potion which temporarily stuns all",
                    "&7monsters in a &a10&7 block radius.",
                    "",
                    "&8∙ &fGhost Ability: Absorption Potion",
                    "&7Throw a potion which gives all teammates in a &a10",
                    "&7block radius &a200&7 HP worth of absorption for &a3",
                    "&7seconds.");
        };
        return new Item("&aGhost Abilities", new ArrayList<>(lore));
    }
}
