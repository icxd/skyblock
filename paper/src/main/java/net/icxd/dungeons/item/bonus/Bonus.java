package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

/**
 * What a bonus does where it's more than its text (see {@link SetBonuses}), found by the name its blocks
 * carry: a set's (FULL_SET, TIERED) while enough of the set is worn, a piece's own (PIECE, EXTRA) while
 * that piece is, or what an item's own text says it does while it's worn ({@link #ITEM}, found by item
 * id: Wither Armor's "Reduces the damage you take from withers by 10%"). Each hook gets the bonus as
 * it's worn now ({@link Active}); they all do nothing unless the bonus says.
 */
public interface Bonus {
    /** The kind of a bonus that's in an item's own text rather than a block: it's found by {@link #item}. */
    String ITEM = "ITEM";

    /** The bonus as it's worn now: which set (null for a piece's or an item's own), how many pieces, and which. */
    record Active(Bonus bonus, SetKey set, int count, List<Worn.Piece> pieces) {
    }

    /** Its blocks' kind: FULL_SET, TIERED, PIECE, EXTRA, or {@link #ITEM}. */
    String kind();

    /** The name its blocks carry ("Shadow Assassin"); for an {@link #ITEM} one, a name for it. */
    String name();

    /** For an {@link #ITEM} bonus, whether it's the item with this id's. */
    default boolean item(String id) {
        return false;
    }

    /**
     * How many of its pieces it takes: all of a full set's (its header's count; a bonus whose header has
     * none says how many), a tiered one's least (1 unless the bonus says), one of a piece's.
     */
    default int needs(SetKey set) {
        return set != null && !set.tiered() && set.pieces() > 0 ? set.pieces() : 1;
    }

    /**
     * Stats, on top of the rest (see {@link net.icxd.dungeons.stats.PlayerStats#addModifier}). They're
     * the stats so far: nothing here may ask for the player's stats themselves.
     */
    default void stats(Player player, Active active, Stats stats) {
    }

    /** Stats worked out from the others (Crit Damage per Strength), after every bonus's {@link #stats}. */
    default void derivedStats(Player player, Active active, Stats stats) {
    }

    /** How far its {@link #aura} reaches from whoever it counts for; 0 for none. */
    default double auraRange() {
        return 0;
    }

    /**
     * What it gives everyone within {@link #auraRange} of someone it counts for, them included: once,
     * however many of them there are (Holy Blood's "Effect only applies once!").
     */
    default void aura(Player player, Stats stats) {
    }

    /**
     * How many wearers' auras count for someone near them all (Nutcracker Armor's Battalion: "Stacks up to 5
     * times"); 1 for an aura that applies once.
     */
    default int auraStacks() {
        return 1;
    }

    /** What {@code wearers} of it near them give (at most {@link #auraStacks}); by default the aura, once. */
    default void aura(Player player, Stats stats, int wearers) {
        aura(player, stats);
    }

    /** A buff on a hit of theirs that landed on {@code target} (see {@link Combat.HitBuff}); null for none. */
    default Combat.HitBuff hit(Player player, Active active, boolean ranged, Damage.Target target) {
        return null;
    }

    /**
     * The same, for a bonus that needs more of the hit than whether it's an arrow ({@code attacker}: how
     * far the arrow flew, for the Sniper Helmet); by default the one above.
     */
    default Combat.HitBuff hit(Player player, Active active, Damage.Attacker attacker, Damage.Target target) {
        return hit(player, active, attacker.ranged(), target);
    }

    /**
     * The same, for a bonus that needs the mob or the kind of hit ({@code landing}: Static Charge's lightning on
     * its target, melee only); by default the one above.
     */
    default Combat.HitBuff hit(Player player, Active active, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
        return hit(player, active, attacker, target);
    }

    /**
     * A hit of theirs has landed and done its damage (see {@link Combat#addHitListener}): a melee hit, an arrow, a
     * Ferocity strike or an ability's hit, on {@code landing}'s mob.
     */
    default void landed(Player player, Active active, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
    }

    /** Defense they have against what hit them ({@code by}: a mob, or its projectile). */
    default double defenseAgainst(Player player, Active active, Entity by) {
        return 0;
    }

    /** A factor on what a hit by {@code by} takes from them. */
    default double takenFrom(Player player, Active active, Entity by) {
        return 1;
    }

    /**
     * A factor on what any hit (a trap's too) takes from {@code hurt}, a player who may be near
     * {@code wearer}, whom it counts for (or {@code wearer} themselves): 1 for nothing. Of all wearers',
     * the least counts (the Stone Chestplate's "players within 10 blocks of you take 5% less damage").
     */
    default double takenNear(Player wearer, Active active, Player hurt) {
        return 1;
    }

    /** The share of knockback from {@code by} they don't take. */
    default double knockbackResistance(Player player, Active active, Entity by) {
        return 0;
    }

    /** A factor on what every hit takes from them, a trap's too (the DOJO belts' "Reduces damage taken by 6%"). */
    default double taken(Player player, Active active) {
        return 1;
    }

    /** A factor on vanilla damage of this cause (see {@link VanillaDamage}: the Chicken Head's "reduces fall damage by 5%"). */
    default double vanillaDamage(Player player, Active active, VanillaDamage.Cause cause) {
        return 1;
    }

    /** A hit by {@code by} took {@code taken} health, and they're alive. */
    default void hurt(Player player, Active active, Entity by, double taken) {
    }

    /** They killed a mob. */
    default void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
    }

    /** They teleported (an ability's teleport or an ender pearl, see {@link SetBonuses#teleported}). */
    default void teleported(Player player, Active active) {
    }

    /** They started ({@code sneaking}) or stopped sneaking. */
    default void sneaked(Player player, Active active, boolean sneaking) {
    }

    /** They shot a bow. */
    default void shot(Player player, Active active, EntityShootBowEvent event) {
    }

    /**
     * An arrow of theirs left a bow, recorded (see {@link net.icxd.dungeons.combat.Shots#addShotListener}): a drawn
     * bow's, or each of a shortbow's ({@code shortbow}).
     */
    default void arrowShot(Player player, Active active, Projectile projectile, NBTTagCompound bow, boolean shortbow) {
    }

    /** Whether a potion effect of this type can't be given them (the Obsidian Chestplate's "Immune to Wither effect"). */
    default boolean blocksEffect(Player player, Active active, PotionEffectType type) {
        return false;
    }

    /** Whether they take no damage of this kind (it's cancelled before it becomes SkyBlock health). */
    default boolean immune(Player player, Active active, EntityDamageEvent.DamageCause cause) {
        return false;
    }

    /** Once a second. */
    default void second(Player player, Active active) {
    }

    /** It counted for them at the last second's tick and doesn't now (what it started while it did can stop). */
    default void ended(Player player) {
    }

    /** A factor on their damages over time that look like this (see MobDebuffs#addDotFactor: Rekindle's burning damage). */
    default double dotFactor(Player player, Active active, DamageIndicators.Look look) {
        return 1;
    }

    /** A factor on their abilities' mana costs. */
    default double manaCost(Player player, Active active) {
        return 1;
    }

    /** How much it raises their Speed cap. */
    default double speedCap(Player player, Active active) {
        return 0;
    }

    /** How much it raises their Attack Speed cap (see {@link Combat#addAttackSpeedCap}). */
    default double attackSpeedCap(Player player, Active active) {
        return 0;
    }

    /** A factor on their abilities' health costs (Berserk's "reduces the cost of health-consuming abilities by 20%"). */
    default double healthCost(Player player, Active active) {
        return 1;
    }

    /** Percent more experience from this source (see {@link ExpOrbs#addBonus}: Shimmer's "by 200%"). */
    default double experience(Player player, Active active, ExpOrbs.Source source) {
        return 0;
    }

    /** Its block's text for someone wearing {@code count} of its set (a tiered bonus's numbers grow). */
    default List<String> text(List<String> text, int count) {
        return text;
    }

    /**
     * The same for {@code holder}, for text that shows something of theirs (Crystal Armor's "Current Light Level");
     * by default the one above.
     */
    default List<String> text(List<String> text, int count, Player holder) {
        return text(text, count);
    }

    /** They've left: whatever's kept about them goes. */
    default void forget(UUID player) {
    }
}
