package net.icxd.dungeons.pet.ability;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mining.MinableBlock;
import net.icxd.dungeons.mob.KillingBlow;
import net.icxd.dungeons.mob.MobDrop;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.pet.OwnedPet;
import net.icxd.dungeons.stats.Stats;

/**
 * What a pet's ability does where it's more than its text (PETS.md), found by its pet's type and its block's name
 * ("BEE", "Hive"): while the pet is summoned, or for one that says so ({@link #always}) while it's in the Pets menu.
 * Its numbers are its text's, at the pet's level (see {@link #text}). Each hook gets the ability as it counts now
 * ({@link Active}); they all do nothing unless the ability says. They're the armor bonuses' hooks
 * ({@link net.icxd.dungeons.item.bonus.Bonus}), for a pet's.
 */
public interface PetAbility {
    /** An ability as it counts for someone now: whose pet it is, at what level, and its text's numbers there. */
    record Active(PetAbility ability, OwnedPet pet, int level, double[] numbers) {
        /** The {@code i}th {@code #} of its {@link PetAbility#text} (from 0), at the pet's level; 0 past the last. */
        public double n(int i) {
            return i < numbers.length ? numbers[i] : 0;
        }
    }

    /** The pet's type ("BEE"). */
    String pet();

    /** Its block's name ("Hive"). */
    String name();

    /**
     * Its text where its numbers are, as every rarity of it has it, a {@code #} for each number and a {@code *} for
     * anything ("within # blocks: +#✎ Intelligence +#❁ Strength"); empty for one with no numbers. A rarity whose text
     * doesn't have it doesn't count (the log says so, and PetAbilitiesDataTest checks the data).
     */
    String text();

    /** It counts while the pet is in the Pets menu too ("This pet's perks are active even when the pet is not summoned!"). */
    default boolean always() {
        return false;
    }

    // ---------- stats ----------

    /**
     * Stats it gives the pet itself ("Grants ... to your pet"): they're the pet's stats with its own, which is what
     * copies its stats copy (Chimera).
     */
    default void petStats(Player player, Active active, Stats pet) {
    }

    /**
     * What it does to their summoned pet's stats, whichever pet's ability it is (the Bingo's Chimera, from the Pets menu:
     * "Increases the base stats of your active pet by 40%"): {@code base} is that pet's own at its level.
     */
    default void summonedStats(Player player, Active active, OwnedPet summoned, Stats base, Stats pet) {
    }

    /** Stats, on top of the rest (see {@link net.icxd.dungeons.stats.PlayerStats#addModifier}): the stats so far. */
    default void stats(Player player, Active active, Stats stats) {
    }

    /**
     * Stats worked out from the others (Defense as Strength), after every ability's {@link #stats} and their flat buffs
     * (see {@link net.icxd.dungeons.stats.PlayerStats#addBuffedModifier}).
     */
    default void derivedStats(Player player, Active active, Stats stats) {
    }

    /** How far its {@link #aura} reaches from its owner (by its numbers: "to players within 20 blocks"); 0 for none. */
    default double auraRange(Active active) {
        return 0;
    }

    /** What it gives {@code near}, a player within {@link #auraRange} of its owner (the owner too, unless it says). */
    default void aura(Player owner, Active active, Player near, Stats stats) {
    }

    // ---------- their hits ----------

    /** A buff on a hit of theirs on a mob (see {@link Combat.HitBuff}); null for none. */
    default Combat.HitBuff hit(Player player, Active active, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
        return null;
    }

    /** A hit of theirs has landed and done its damage (see {@link Combat#addHitListener}). */
    default void landed(Player player, Active active, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
    }

    // ---------- hits on them ----------

    /** A factor on what a hit by {@code by} (a mob, or its projectile) takes from them. */
    default double takenFrom(Player player, Active active, Entity by) {
        return 1;
    }

    /** A factor on what every hit takes from them, a trap's too. */
    default double taken(Player player, Active active) {
        return 1;
    }

    /** The share of knockback from {@code by} they don't take. */
    default double knockbackResistance(Player player, Active active, Entity by) {
        return 0;
    }

    /** A factor on vanilla damage of this cause (see {@link VanillaDamage}). */
    default double vanillaDamage(Player player, Active active, VanillaDamage.Cause cause) {
        return 1;
    }

    /** A hit by {@code by} took {@code taken} health, and they're alive. */
    default void hurt(Player player, Active active, Entity by, double taken) {
    }

    /**
     * What's left of a hit that would take {@code taken} from them ({@code by}: who's behind it, or null), after their
     * Defense and the factors and before their absorption (see {@link net.icxd.dungeons.combat.PlayerDamage#addShield}):
     * {@code taken} for nothing.
     */
    default double shield(Player player, Active active, double taken, Entity by) {
        return taken;
    }

    /** They landed from a fall of {@code blocks} blocks that hurts (before {@link #vanillaDamage} changes it). */
    default void fell(Player player, Active active, float blocks) {
    }

    /** The share of the coins a death would cost them that it saves (0 to 1; see {@link net.icxd.dungeons.economy.DeathCoins}). */
    default double deathCoins(Player player, Active active) {
        return 0;
    }

    // ---------- kills, experience and drops ----------

    /** They killed a mob. */
    default void killed(Player player, Active active, SkyBlockMobDeathEvent event) {
    }

    /** Combat Wisdom a kill gives them on top of theirs. */
    default double killWisdom(Player player, Active active, SkyBlockMobDeathEvent event) {
        return 0;
    }

    /** A factor on the Combat XP a kill gives them ("Gain +50☯ Combat Wisdom towards Zombies" multiplies it, see SkillGains). */
    default double killXp(Player player, Active active, SkyBlockMobDeathEvent event) {
        return 1;
    }

    /** A factor on one of their kill's drops' own chance, before Magic Find (see {@link net.icxd.dungeons.mob.Mobs#addDropChance}). */
    default double dropChance(Player killer, Active active, KillingBlow blow, MobDrop drop) {
        return 1;
    }

    /** Percent more experience from this source (see {@link ExpOrbs#addBonus}). */
    default double experience(Player player, Active active, ExpOrbs.Source source) {
        return 0;
    }

    // ---------- abilities and mana ----------

    /** A factor on their abilities' mana costs. */
    default double manaCost(Player player, Active active) {
        return 1;
    }

    /** A factor on the mana costs of this item's abilities (by its data). */
    default double itemManaCost(Player player, Active active, NBTTagCompound item) {
        return 1;
    }

    /** A factor on this ability block's cooldown. */
    default double cooldown(Player player, Active active, ItemBlock block) {
        return 1;
    }

    /** More mana regeneration, as a share of the base (0.3 for "Regenerate 30% extra mana"). */
    default double manaRegen(Player player, Active active) {
        return 0;
    }

    /** A factor on their mana pool (1.25 for "Increases your total mana by 25%"). */
    default double maxMana(Player player, Active active) {
        return 1;
    }

    /** They spent {@code amount} mana on an ability ({@code source}: its name). */
    default void manaSpent(Player player, Active active, int amount, String source) {
    }

    /** Mining Speed it gives them on this kind of block (see {@link net.icxd.dungeons.mining.BlockListener#addMiningSpeed}). */
    default double miningSpeed(Player player, Active active, MinableBlock block) {
        return 0;
    }

    /** They healed others in a dungeon run for this much (see {@link net.icxd.dungeons.dungeons.instance.DungeonRun#addAllyHealing}). */
    default void healedAllies(Player player, Active active, double healing) {
    }

    /** Percent more effective their Last Stand is (see {@link net.icxd.dungeons.item.enchanting.armor.ArmorEnchants#addLastStandBoost}). */
    default double lastStand(Player player, Active active) {
        return 0;
    }

    /** Percent more powder of this type they get from what they mine. */
    default double powder(Player player, Active active, PowderType type) {
        return 0;
    }

    /** They jumped. */
    default void jumped(Player player, Active active) {
    }

    /** Levels it adds to their Intimidation line's (see {@link net.icxd.dungeons.item.accessory.AccessoryEffects}). */
    default double intimidation(Player player, Active active) {
        return 0;
    }

    // ---------- the rest ----------

    /** How much it raises their Speed cap. */
    default double speedCap(Player player, Active active) {
        return 0;
    }

    /** Once a second. */
    default void second(Player player, Active active) {
    }

    /** It counted for them at the last second's tick and doesn't now (what it started while it did can stop). */
    default void ended(Player player) {
    }

    /** They've left: whatever's kept about them goes. */
    default void forget(UUID player) {
    }
}
