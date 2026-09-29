package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.mob.MobType;

/** The weapons' passives, as their text (items.json's lines, colours and all) says. */
class WeaponPassivesTest {
    private static final double EPS = 1e-9;

    @Test
    void typeFactorsAreMultipliersFromTheText() {
        WeaponLore hyperion = WeaponLore.of(List.of("&7Deals &c+50% &7damage to &8☠ Wither &7mobs.",
                "&7Grants &c+1 ❁ Damage &7and &a+2 &b✎", "&bIntelligence &7per &cCatacombs &7level."));
        assertEquals(1.5, hyperion.types.get(MobType.WITHER), EPS);
        assertEquals(1.5, hyperion.factor(Set.of(MobType.UNDEAD, MobType.WITHER), 0, false), EPS);
        assertEquals(1, hyperion.factor(Set.of(MobType.UNDEAD), 0, false), EPS);

        WeaponLore dagger = WeaponLore.of(List.of("&7Deal &c2.5x &7damage to &4♨ Infernal &7mobs.", "&7Deal &c1.5x &7damage to &f🦴 Skeletal &7mobs.",
                "&7Gain &3+30☯ Combat Wisdom &7against &4♨", "&4Infernal &7mobs."));
        assertEquals(2.5, dagger.types.get(MobType.INFERNAL), EPS);
        assertEquals(1.5, dagger.types.get(MobType.SKELETAL), EPS);
        assertEquals(30, dagger.wisdomAgainst(Set.of(MobType.INFERNAL)), EPS);
        assertEquals(0, dagger.wisdomAgainst(Set.of(MobType.SKELETAL)), EPS);

        // "Deal +300% ❁ Damage to and gain +45☯ Combat Wisdom against Ж Arthropod mobs!"
        WeaponLore sting = WeaponLore.of(List.of("&7Deal &c+300% ❁ Damage &7to and gain", "&3+45☯ Combat Wisdom &7against &4Ж", "&4Arthropod &7mobs!"));
        assertEquals(4, sting.types.get(MobType.ARTHROPOD), EPS);
        assertEquals(45, sting.wisdomAgainst(Set.of(MobType.ARTHROPOD)), EPS);

        // Endermen and Sea Creatures aren't mob types here: nothing.
        assertSame(WeaponLore.NONE, WeaponLore.of(List.of("&7Deal &c+300% &7damage to Endermen.")));
    }

    @Test
    void missingHealthWaterAndLava() {
        WeaponLore spirit = WeaponLore.of(List.of("&7Deals &c+2% &7damage to &2༕ Undead Mobs", "&7for every &e1% &7of your missing health."));
        assertEquals(MobType.UNDEAD, spirit.missingType);
        assertTrue(spirit.types.isEmpty(), "not also a flat factor");
        // Half their health gone: +100%, x2 (the wiki's 1 + 2 x missing share).
        assertEquals(2, spirit.factor(Set.of(MobType.UNDEAD), 0.5, false), EPS);
        assertEquals(1, spirit.factor(Set.of(MobType.SKELETAL), 0.5, false), EPS);

        WeaponLore prismarine = WeaponLore.of(List.of("&7Deals &c+200% &7damage while in water."));
        assertEquals(3, prismarine.factor(Set.of(), 0, true), EPS);
        assertEquals(1, prismarine.factor(Set.of(), 0, false), EPS);

        assertEquals(2, WeaponLore.of(List.of("&7All damage dealt with this bow is", "&adoubled&7.")).always, EPS);

        WeaponLore volcano = WeaponLore.of(List.of("&7Gain &c+100❁ Strength &7against enemies", "&7who are in lava."));
        assertEquals(100, volcano.strengthInLava, EPS);
        // 200 Strength and 100 more: (1 + 3) / (1 + 2).
        assertEquals(4.0 / 3, WeaponLore.strengthFactor(200, 100), EPS);
    }

    @Test
    void onHitHealsManaAndShreds() {
        WeaponLore reaper = WeaponLore.of(List.of("&7Heal &c10❤ &7per hit.", "&7Deal &c+200% &7damage to &2༕ Undead &7mobs."));
        assertEquals(10, reaper.healPerHit, EPS);
        assertEquals(3, reaper.types.get(MobType.UNDEAD), EPS);
        assertEquals(3, WeaponLore.of(List.of("&7Regens &b3 Mana &7on hit.")).manaPerHit, EPS);
        assertEquals(0.8, WeaponLore.of(List.of("&7Receive &a-20% &7damage from &a☮ Animal &7mobs.")).animalTaken, EPS);
        assertEquals(2, WeaponLore.of(List.of("&7Your Critical Hits deal &9100% &7more", "&7damage if you are behind your", "&7target.")).behindCrit, EPS);

        WeaponLore last = WeaponLore.of(List.of("&7Reduces the defense of your target", "&7by &a10% &7of their max &a❈ Defense &7on",
                "&7hit, stacking up to &a5 &7times."));
        assertEquals(0.1, last.shredShare, EPS);
        assertEquals(5, last.shredStacks);

        WeaponLore death = WeaponLore.of(List.of("&7Deals &a+100% &7damage to &2༕ Undead", "&7mobs.", "", "&7Your arrows have a &e50% &7chance to",
                "&7bounce to another target after it", "&7hits something."));
        assertEquals(0.5, death.bounceChance, EPS);
        assertEquals(2, death.types.get(MobType.UNDEAD), EPS);

        WeaponLore venom = WeaponLore.of(List.of("&7Fires a volley of &a5 &7arrows. Arrows", "&7apply venom to all enemies hit dealing",
                "&c2❁ Damage &7every second for &a3", "&7seconds."));
        assertEquals(5, venom.volley);
        assertEquals(2, venom.venomDamage, EPS);
        assertEquals(3, venom.venomSeconds);
    }

    @Test
    void cleaveFallsFromHalfToTwoFifths() {
        assertEquals(0.5, WeaponPassives.cleaveShare(0, 3), EPS);
        assertEquals(0.45, WeaponPassives.cleaveShare(1.5, 3), EPS);
        assertEquals(0.4, WeaponPassives.cleaveShare(3, 3), EPS);
        assertEquals(0.4, WeaponPassives.cleaveShare(5, 3), EPS);
    }

    /** Minecraft's yaw 0 faces +z: someone at -z is behind it, at +z in front. */
    @Test
    void behindIsTheHalfItsBackFaces() {
        assertTrue(WeaponPassives.behind(0, 0, -2));
        assertFalse(WeaponPassives.behind(0, 0, 2));
        // Facing +x (yaw -90): behind is -x.
        assertTrue(WeaponPassives.behind(-90, -2, 0.5));
        assertFalse(WeaponPassives.behind(-90, 2, 0.5));
    }

    @Test
    void extraArrowsGoOutEachSideInTurn() {
        assertEquals(List.of(12.5, -12.5), BowPassives.sides(2, 12.5));
        assertEquals(List.of(7.5), BowPassives.sides(1, 7.5));
        assertEquals(List.of(7.5, -7.5, 15.0, -15.0), BowPassives.sides(4, 7.5));
        assertTrue(BowPassives.sides(0, 5).isEmpty());
    }

    @Test
    void consolidatedAndChainNumbersFromText() {
        assertEquals(1.25, Explosions.factor("Increases all explosion damage dealt by 25%. Right-click to equip!"), EPS);
        assertEquals(1, Explosions.factor("Something else."), EPS);
        ItemBlock chain = new ItemBlock("ABILITY", Tormentor.NAME, null, "RIGHT_CLICK", List.of("&8Toggles On/Off on use",
                "&7Melee hits arc with cursed energy,", "&7chaining to up to &c5 &7nearby enemies."), 200, 0, 0.5, 0, 0, 0, 0);
        assertEquals(5, Tormentor.jumps(chain));
    }

    @Test
    void noTextNoPassives() {
        assertSame(WeaponLore.NONE, WeaponLore.of(List.of()));
        assertSame(WeaponLore.NONE, WeaponLore.of(List.of("&7That thing was too big to be called a", "&7sword.")));
        assertFalse(WeaponLore.NONE.any());
        assertNull(WeaponLore.type("Endermen"));
    }

    /** The Flaming Sword's "Ignites enemies for 3s." and the Spider Queen's Stinger's aura, from their text. */
    @Test
    void igniteAndAura() {
        assertEquals(3, WeaponLore.of(List.of("&7Ignites enemies for &a3s&7.")).igniteSeconds);
        WeaponLore stinger = WeaponLore.of(List.of("&7Arrows shot using this bow have an", "&7aura around them that deals &c360❁",
                "&cDamage &7to nearby enemies instead of", "&7dealing impact damage. Arrows travel", "&7through enemies.", "",
                "&4This item is Arachnal Ж!"));
        assertEquals(360, stinger.auraDamage, EPS);
        // "360 + (3.6 x Strength) True Damage" (the wiki).
        assertEquals(360 + 3.6 * 250, BowPassives.auraDamage(360, 250), EPS);
    }

    /** "Each strike of this weapon": its melee hits and their Ferocity strikes, so no strike after the hit kills either. */
    @Test
    void visTemperataCapsEveryStrike() {
        assertTrue(WeaponPassives.strikes(HitKind.MELEE));
        assertTrue(WeaponPassives.strikes(HitKind.FEROCITY));
        assertFalse(WeaponPassives.strikes(HitKind.ARROW));
        assertFalse(WeaponPassives.strikes(HitKind.ABILITY));
    }

    /** Vis Temperata: "capped at 33% of the enemy's max Health. This weapon cannot cause a fatal blow." */
    @Test
    void visTemperataCapsAndNeverKills() {
        assertEquals(330, WeaponPassives.strikeCap(0.33, true, 1_000, 1_000), EPS);
        assertEquals(99, WeaponPassives.strikeCap(0.33, true, 100, 1_000), EPS);
        assertEquals(0, WeaponPassives.strikeCap(0.33, true, 1, 1_000), EPS);
        assertEquals(Double.MAX_VALUE, WeaponPassives.strikeCap(0, false, 1, 1_000), EPS);
    }

    /** The Reaving Strike blocks' lines: the holder's missing health against Undead, the target's missing health. */
    @Test
    void reapersFromMissingHealth() {
        ItemBlock bone = new ItemBlock("ABILITY", "Reaving Strike", null, "RIGHT_CLICK", List.of("&7Slash in a huge arc, dealing &c125% &7melee",
                "&7damage to all enemies hit!", "", "&7Deals &c+1% &7damage to &aUndead", "&7monsters for every &a1% &7of your", "&7missing health."),
                0, 0, 0, 0, 0, 0, 0);
        ItemBlock felthorn = new ItemBlock("ABILITY", "Reaving Strike", null, "RIGHT_CLICK", List.of("&7Deals &c+1% &7damage for every &a1% &7of",
                "&7missing health on the target."), 0, 0, 0, 0, 0, 0, 0);
        WeaponPassives.Weapon reaver = new WeaponPassives.Weapon(new FakeItem("BONE_REAVER", List.of(), List.of(bone)));
        assertEquals(MobType.UNDEAD, reaver.ownMissingType);
        assertEquals(1, reaver.ownPerMissing, EPS);
        assertEquals(1, new WeaponPassives.Weapon(new FakeItem("FELTHORN_REAPER", List.of(), List.of(felthorn))).targetPerMissing, EPS);
    }

    /** An item with only an id, its lore and its blocks. */
    private record FakeItem(String id, List<String> lore, List<ItemBlock> blocks) implements SkyBlockItem {
        @Override
        public String name() {
            return id;
        }

        @Override
        public Material material() {
            return Material.IRON_SWORD;
        }
    }
}
