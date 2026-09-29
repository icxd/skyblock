package net.icxd.dungeons.item.enchanting.weapon;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.Debuffs;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobDebuffs;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.combat.Shots;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.utility.Heals;
import net.icxd.dungeons.item.ability.weapons.Hits;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.reforge.CombatReforges;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.CHAMPION;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.CLEAVE;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.COMBO;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.DRAIN;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.DUPLEX;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.FATAL_TEMPO;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.FIRE_ASPECT;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.FLAME;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.INFERNO;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.INFINITE_QUIVER;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.KNOCKBACK;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.LETHALITY;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.LIFE_STEAL;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.MANA_STEAL;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.PIERCING;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.PUNCH;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.REND;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.SOUL_EATER;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.SWARM;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.THUNDERBOLT;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.THUNDERLORD;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.TOXOPHILITE;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.VAMPIRISM;
import static net.icxd.dungeons.item.enchanting.weapon.WeaponEnchant.VENOMOUS;

/**
 * What swords', longswords', gauntlets' and bows' enchantments do (ENCHANTS_WEAPONS.md), on the core's hooks
 * (EFFECTS.md): once a hit has landed ({@link Combat#addHitListener}: Cleave, Life Steal, Drain, Mana Steal,
 * Lethality, Venomous, Fire Aspect, Flame, Thunderlord, Thunderbolt, Knockback, Punch, Champion's second hit,
 * Inferno, Fatal Tempo's count, Duplex's fire mark, Rend's arrows), on the hit as it lands ({@link
 * Combat#addHitBuffs}: Swarm, Combo, Soul Eater), on a kill (Vampirism, Combo's kills, Soul Eater's soul, the tiers
 * of Champion and Toxophilite), on a shot (Piercing, Duplex's second arrow, Infinite Quiver) and Rend's left click.
 * Their numbers are their texts' ({@link EnchantText}); what counts for one is the weapon the hit was dealt with
 * ({@link Combat.Landing#weapon}: the bow for an arrow), for a kill what they hold. The on-hit ones count melee hits
 * (and arrows for the bow ones), not Ferocity's strikes ("they don't trigger Life Steal or Drain", the wiki's
 * Ferocity) or abilities' hits (UNKNOWN whether Hypixel's abilities set them off: they don't here). Main thread.
 */
public final class WeaponEnchants implements Listener {
    /** The counts and store items keep, under Hypixel's own keys (live items carry them). */
    static final String CHAMPION_XP = "champion_combat_xp";
    static final String TOXOPHILITE_XP = "toxophilite_combat_xp";
    static final String SOUL = "ultimateSoulEaterData";
    /** Their damages over time and debuffs on mobs (see MobDebuffs): fire is one, Fire Aspect's or Flame's. */
    static final String FIRE_DOT = "fire";
    static final String VENOM_DOT = "venomous";
    static final String VENOM_SLOW = "venomous_slow";
    static final String LETHALITY_DEBUFF = "lethality";
    static final String INFERNO_DOT = "inferno";
    /** "The walk speed reduction from Venomous is reduced by -80% while in the Catacombs" (the wiki's Venomous). */
    static final double VENOM_SLOW_IN_DUNGEONS = 0.2;
    /** "Healing from Vampirism is 10x less effective in the Catacombs" (the wiki's Vampirism). */
    static final double VAMPIRISM_IN_DUNGEONS = 0.1;
    /** Inferno "traps it for 5s and deals ... that hit's damage over the trap's duration" (the wiki's Inferno; its text has no time). */
    static final int INFERNO_SECONDS = 5;
    /** Flame's per second, a level (the wiki's Flame: 3% and 6%; its text has only the time). */
    static final double FLAME_PERCENT = 3;
    /**
     * How many more mobs a Piercing arrow goes through after the first: vanilla Piercing I's one (UNKNOWN: the text
     * says only "Arrows travel through enemies").
     */
    static final int PIERCED = 1;
    /** How far Rend reaches for its arrows ("nearby enemies": UNKNOWN how far; 10 blocks around them). */
    static final double REND_RADIUS = 10;
    /** How many kills a player's Combo remembers (Combo V's "up to 10 kills"). */
    private static final int COMBO_KILLS = 10;

    /** What a player's hits on one mob have counted (Thunderlord's every 3rd, Inferno's every 10th, Rend's arrows). */
    private static final class Counts {
        int melee;
        int all;
        int arrows;
        double lastCrit;
    }

    /** What's on one mob: each player's hits on it, its Venomous stacks, Lethality's first level, Duplex's mark. */
    private static final class MobState {
        final Map<UUID, Counts> hits = new HashMap<>(2);
        final WeaponRules.Venom venom = new WeaponRules.Venom();
        double lethality;
        double fireTaken = 1;
        long fireTakenUntil;
    }

    /** What's kept for a player: Fatal Tempo's boost and when they last hit, and Combo's kills. */
    private static final class PlayerState {
        double tempo;
        long tempoLast;
        long tempoWindow;
        final long[] kills = new long[COMBO_KILLS];
        int nextKill;
    }

    private static final Map<UUID, MobState> MOBS = new HashMap<>();
    private static final Map<UUID, PlayerState> PLAYERS = new HashMap<>();
    /** A Flame bow's arrows in flight: the shooter as they were when it left, which its fire is worked out from. */
    private static final Map<UUID, Damage.Attacker> FLAME_SHOTS = new HashMap<>();
    /** Knockback's and Punch's extra push, for the knockback the hit that set it is about to give (see {@link #onKnockback}). */
    private static final Map<UUID, Double> PUSHES = new HashMap<>();
    private static boolean clearingPushes;

    public WeaponEnchants() {
        Combat.addHitListener(WeaponEnchants::landed);
        Combat.addHitBuffs(WeaponEnchants::buff);
        Shots.addShotListener(WeaponEnchants::shot);
        WeaponStats.enable();
    }

    private static MobState mob(Entity mob) {
        return MOBS.computeIfAbsent(mob.getUniqueId(), id -> new MobState());
    }

    private static Counts hits(Entity mob, Player player) {
        return mob(mob).hits.computeIfAbsent(player.getUniqueId(), id -> new Counts());
    }

    private static PlayerState player(Player player) {
        return PLAYERS.computeIfAbsent(player.getUniqueId(), id -> new PlayerState());
    }

    // ---------- once a hit has landed ----------

    static void landed(Player player, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
        WeaponEnchant.Levels levels = WeaponEnchant.levels(landing.weapon());
        if (levels.none() || landing.entity() == null) return;
        if (levels.has(FATAL_TEMPO) && landing.kind() != HitKind.ABILITY) countTempo(player, levels.of(FATAL_TEMPO));
        switch (landing.kind()) {
            case MELEE -> melee(player, landing, levels, target, damage, killed);
            case ARROW -> arrow(player, landing, levels, target, damage, killed);
            default -> {
            }
        }
    }

    private static void melee(Player player, Combat.Landing landing, WeaponEnchant.Levels levels, Damage.Target target, double damage,
                              boolean killed) {
        LivingEntity mob = landing.entity();
        NBTTagCompound weapon = landing.weapon();
        Counts hits = hits(mob, player);
        hits.melee++;
        hits.all++;
        // The mob's hits before this one (every player's), as First Strike counts them: this is its text's "2nd".
        if (levels.has(CHAMPION) && target.hitsTaken() + 1 == (int) EnchantText.at(CHAMPION, levels.of(CHAMPION), 1)) {
            championHit(player, mob, levels.of(CHAMPION));
        }
        int blood = levels.has(LIFE_STEAL) || levels.has(DRAIN) ? bloodSoaked(player) : 0;
        if (levels.has(LIFE_STEAL)) {
            Heals.give(player, player, EnchantText.linear(LIFE_STEAL, levels.of(LIFE_STEAL) + blood, 0));
        }
        if (levels.has(DRAIN)) drain(player, levels.of(DRAIN), blood);
        if (levels.has(MANA_STEAL)) manaSteal(player, EnchantText.at(MANA_STEAL, levels.of(MANA_STEAL), 0));
        if (levels.has(CLEAVE)) cleave(player, mob, weapon, damage, levels.of(CLEAVE));
        if (levels.has(LETHALITY)) lethality(player, mob, levels.of(LETHALITY));
        if (levels.has(FIRE_ASPECT) || levels.has(VENOMOUS)) {
            Damage.Attacker attacker = Combat.attacker(player, weapon, false, 0);
            double postAdditive = WeaponRules.postAdditive(attacker, target, landing.critical());
            if (levels.has(FIRE_ASPECT)) {
                int level = levels.of(FIRE_ASPECT);
                fire(player, mob, target, postAdditive, EnchantText.at(FIRE_ASPECT, level, 1),
                        (int) EnchantText.at(FIRE_ASPECT, level, 0));
            }
            if (levels.has(VENOMOUS)) venomous(player, mob, target, attacker, postAdditive, levels.of(VENOMOUS));
        }
        if (levels.has(THUNDERLORD) || levels.has(THUNDERBOLT)) thunder(player, mob, weapon, damage, levels, hits.melee);
        if (levels.has(INFERNO)) inferno(player, mob, target, damage, levels.of(INFERNO), hits.all);
        if (levels.has(KNOCKBACK) && !killed) {
            push(mob, WeaponRules.knockback(EnchantText.at(KNOCKBACK, levels.of(KNOCKBACK), 0), false));
        }
    }

    private static void arrow(Player player, Combat.Landing landing, WeaponEnchant.Levels levels, Damage.Target target, double damage,
                              boolean killed) {
        LivingEntity mob = landing.entity();
        NBTTagCompound bow = landing.weapon();
        Counts hits = hits(mob, player);
        hits.all++;
        if (levels.has(FLAME)) {
            // The shooter as they were when it left, as the arrow's own hit is (Shots), not with what they hold now.
            int level = levels.of(FLAME);
            Damage.Attacker shooter = landing.projectile() == null ? null : FLAME_SHOTS.get(landing.projectile().getUniqueId());
            if (shooter == null) shooter = Combat.attacker(player, bow, true, 0);
            double postAdditive = WeaponRules.postAdditive(shooter, target, landing.critical());
            fire(player, mob, target, postAdditive, FLAME_PERCENT * level, (int) EnchantText.at(FLAME, level, 0));
        }
        if (levels.has(DUPLEX)) {
            int level = levels.of(DUPLEX);
            MobState state = mob(mob);
            state.fireTaken = EnchantText.at(DUPLEX, level, 1);
            state.fireTakenUntil = System.currentTimeMillis() + (long) (EnchantText.at(DUPLEX, level, 2) * 1000);
        }
        if (levels.has(REND)) {
            hits.arrows++;
            if (landing.critical()) hits.lastCrit = damage;
        }
        if (levels.has(INFERNO)) inferno(player, mob, target, damage, levels.of(INFERNO), hits.all);
        if (levels.has(PUNCH) && !killed) {
            push(mob, WeaponRules.knockback(EnchantText.at(PUNCH, levels.of(PUNCH), 0), true));
        }
    }

    /**
     * Champion's "The 2nd hit on a mob grants +1.4 coins & +7 exp orbs": the coins into their purse, the orbs the way
     * the mob's drops go (to them from a dungeon mob, else where it stands: see ExpOrbs).
     */
    private static void championHit(Player player, LivingEntity mob, int level) {
        double coins = EnchantText.at(CHAMPION, level, 2);
        User user = User.ifLoaded(player.getUniqueId());
        if (coins > 0 && user != null && !user.isReleased()) Purse.add(user, coins);
        ExpOrbs.grant(player, EnchantText.at(CHAMPION, level, 3), ExpOrbs.Source.OTHER, mob.getLocation(), dropsToThem(mob));
    }

    /** Whether a mob's drops (and so its experience) go straight to its killer: a dungeon's. */
    private static boolean dropsToThem(LivingEntity mob) {
        if (DungeonMobs.of(mob) != null) return true;
        Mobs.Live live = Mobs.of(mob);
        return live != null && live.type().dropsToInventory();
    }

    /** Drain's "Regen +0.5♨ Vitality every time you hit a mob. (1s Cooldown)", into their pool (never past it). */
    private static void drain(Player player, int level, int blood) {
        PlayerSession session = PlayerSession.of(player);
        String cooldown = "enchant:" + DRAIN.id();
        if (session.cooldownLeft(cooldown) > 0) return;
        double max = Vitality.max(player);
        session.setVitality(Math.min(max, Vitality.get(player) + EnchantText.linear(DRAIN, level + blood, 0)));
        session.startCooldown(cooldown, (long) (EnchantText.at(DRAIN, level, 1) * 1000));
    }

    /**
     * Mana Steal's "Regain 0.25% of your mana on hit": of their pool (UNKNOWN whether it's of the pool or of what they
     * have: the pool, as "regain" reads), whole points (the pool is whole, as dungeons' mana on hit drops fractions).
     */
    private static void manaSteal(Player player, double percent) {
        PlayerSession session = PlayerSession.of(player);
        int pool = session.maxMana();
        int mana = session.getMana() < 0 ? pool : session.getMana();
        session.setMana(Math.min(pool, mana + (int) (pool * percent / 100)));
    }

    /**
     * Cleave's "Deals 5% of your damage dealt to other monsters within 3.3 blocks of the target": the hit's damage's
     * share on each other mob that can be hurt (as it is: UNKNOWN whether their Defense counts), as an effect's damage.
     */
    private static void cleave(Player player, LivingEntity mob, NBTTagCompound weapon, double damage, int level) {
        double share = Math.floor(damage * EnchantText.at(CLEAVE, level, 0) / 100);
        double radius = EnchantText.at(CLEAVE, level, 1);
        if (share < 1 || radius <= 0) return;
        for (LivingEntity other : Hits.near(center(mob), radius)) {
            if (!other.equals(mob)) MobHits.deal(player, other, share, DamageIndicators.Look.NORMAL, HitKind.OTHER, weapon);
        }
    }

    /**
     * Lethality's "Reduces the ❈ Defense of your target by 1.2% for 4s each time you hit them with melee. Stacks up
     * to 4 times." One on a mob from every player's hits (UNKNOWN how Hypixel keeps players' apart); "the level applied
     * first is kept" until it runs out ("Hitting that enemy with Lethality II or higher will not increase that 4.8%",
     * the wiki's Damage Calculation), so a stronger level only refreshes it.
     */
    private static void lethality(Player player, LivingEntity mob, int level) {
        MobState state = mob(mob);
        if (MobDebuffs.stacks(mob, LETHALITY_DEBUFF) == 0) state.lethality = EnchantText.at(LETHALITY, level, 0) / 100;
        long millis = (long) (EnchantText.at(LETHALITY, level, 1) * 1000);
        int most = (int) EnchantText.at(LETHALITY, level, 2);
        MobDebuffs.add(mob, new Debuffs.Spec(LETHALITY_DEBUFF, Debuffs.Kind.DEFENSE, state.lethality, most, millis), player);
    }

    /**
     * Fire on a mob (Fire Aspect, Flame): {@code percent} of the hit's damage after additive buffs each second for
     * {@code seconds} (see {@link WeaponRules#fireTick}), a gold number each, as the wiki's damage indicators have it;
     * put on again it keeps the greater and starts its count again (the wiki's example 5, MobDebuffs). Fire
     * particles show it ("displays fire particles instead of physically lighting the target on fire", 0.10).
     */
    private static void fire(Player player, LivingEntity mob, Damage.Target target, double postAdditive, double percent, int seconds) {
        if (seconds <= 0 || percent <= 0 || !MobHits.hittable(mob)) return;
        double tick = Math.floor(WeaponRules.fireTick(postAdditive, percent, fireTaken(mob), target.defense()));
        if (tick < 1) return;
        MobDebuffs.dot(mob, FIRE_DOT, player, tick, 20, seconds, DamageIndicators.Look.FIRE);
        Location at = center(mob);
        mob.getWorld().spawnParticle(Particle.FLAME, at, 8, mob.getWidth() / 3, mob.getHeight() / 3, mob.getWidth() / 3, 0.01);
    }

    /** What fire does to it more (Duplex's "Targets hit take 1.1x fire damage for 60s"); 1 without. */
    private static double fireTaken(LivingEntity mob) {
        MobState state = MOBS.get(mob.getUniqueId());
        return state == null || state.fireTakenUntil <= System.currentTimeMillis() ? 1 : state.fireTaken;
    }

    /**
     * Venomous's "Reduces the target's walk speed by 2% and deals +0.2% of your damage per second per hit, stacking
     * globally up to 40 hits. Lasts 5s.": its stacks save each hit's share (see {@link WeaponRules.Venom}), and each
     * second deals what they saved times the Additive Multiplier worked out again without melee's buffs, as a dark
     * green number. Worked out as the hit puts it on (UNKNOWN: the wiki's is worked out again each second, for what's
     * held then). The slow is one, 80% weaker in the Catacombs.
     */
    private static void venomous(Player player, LivingEntity mob, Damage.Target target, Damage.Attacker attacker, double postAdditive,
                                 int level) {
        if (!MobHits.hittable(mob)) return;
        long lasts = (long) (EnchantText.at(VENOMOUS, level, 3) * 1000);
        MobState state = mob(mob);
        state.venom.hit(postAdditive, EnchantText.at(VENOMOUS, level, 1) / 100, (int) EnchantText.at(VENOMOUS, level, 2),
                lasts, System.currentTimeMillis());
        double second = Damage.additive(WeaponRules.notMelee(attacker), target);
        double tick = Math.floor(WeaponRules.venomTick(state.venom.saved(), second, target.defense()));
        if (tick >= 1) MobDebuffs.dot(mob, VENOM_DOT, player, tick, 20, (int) Math.ceil(lasts / 1000.0), DamageIndicators.Look.POISON);
        double slow = EnchantText.at(VENOMOUS, level, 0) / 100 * (RunManager.inRun(player) ? VENOM_SLOW_IN_DUNGEONS : 1);
        MobDebuffs.add(mob, new Debuffs.Spec(VENOM_SLOW, Debuffs.Kind.SLOW, slow, 1, lasts), player);
    }

    /**
     * Thunderlord's "Every 3 hits on a monster, strike lightning, dealing 8% of the hit's damage." and Thunderbolt's
     * "... to up to 10 monsters within 2 blocks": their melee hits on this mob (each player's own: UNKNOWN whether
     * Hypixel counts everyone's), a lightning strike (its look and sound) on the 3rd, 6th, ...
     */
    private static void thunder(Player player, LivingEntity mob, NBTTagCompound weapon, double damage, WeaponEnchant.Levels levels,
                                int hits) {
        WeaponEnchant counting = levels.has(THUNDERLORD) ? THUNDERLORD : THUNDERBOLT;
        if (!WeaponRules.every(hits, (int) EnchantText.at(counting, levels.of(counting), 0))) return;
        mob.getWorld().strikeLightningEffect(mob.getLocation());
        if (levels.has(THUNDERLORD)) {
            double strike = Math.floor(damage * EnchantText.at(THUNDERLORD, levels.of(THUNDERLORD), 1) / 100);
            if (strike >= 1) MobHits.deal(player, mob, strike, DamageIndicators.Look.NORMAL, HitKind.OTHER, weapon);
        }
        if (levels.has(THUNDERBOLT)) {
            int level = levels.of(THUNDERBOLT);
            double strike = Math.floor(damage * EnchantText.at(THUNDERBOLT, level, 1) / 100);
            int most = (int) EnchantText.at(THUNDERBOLT, level, 2);
            if (strike < 1) return;
            List<LivingEntity> near = Hits.near(center(mob), EnchantText.at(THUNDERBOLT, level, 3));
            for (int i = 0; i < near.size() && i < most; i++) {
                MobHits.deal(player, near.get(i), strike, DamageIndicators.Look.NORMAL, HitKind.OTHER, weapon);
            }
        }
    }

    /**
     * Inferno's "Every 10th hit on a mob spawns an inferno which traps the mob and deals 125% of your last hit": each
     * player's melee hits and arrows on it (UNKNOWN whose count), and on the 10th it can't walk for 5s and takes that
     * share of the hit spread over them, a second at a time, as magic damage its magic resistance takes from ("The
     * trap damage counts as Magic Damage", the wiki's Inferno). Flames show it.
     */
    private static void inferno(Player player, LivingEntity mob, Damage.Target target, double damage, int level, int hits) {
        if (!WeaponRules.every(hits, (int) EnchantText.at(INFERNO, level, 0)) || !MobHits.hittable(mob)) return;
        double total = damage * EnchantText.at(INFERNO, level, 1) / 100 * (1 - Math.max(0, Math.min(1, target.magicResistance())));
        double tick = Math.floor(total / INFERNO_SECONDS);
        Hits.root(mob, INFERNO_SECONDS * 20);
        if (tick >= 1) MobDebuffs.dot(mob, INFERNO_DOT, player, tick, 20, INFERNO_SECONDS, DamageIndicators.Look.NORMAL);
        mob.getWorld().spawnParticle(Particle.FLAME, center(mob), 30, mob.getWidth() / 2, mob.getHeight() / 2, mob.getWidth() / 2, 0.05);
    }

    /** Fatal Tempo's boost grows with a hit (a Ferocity strike's too: "Each Ferocity strike count as an individual hit"). */
    private static void countTempo(Player player, int level) {
        PlayerState state = player(player);
        long now = System.currentTimeMillis();
        state.tempoWindow = (long) (EnchantText.at(FATAL_TEMPO, level, 2) * 1000);
        state.tempo = WeaponRules.tempo(state.tempo, state.tempoLast, now, EnchantText.at(FATAL_TEMPO, level, 0),
                EnchantText.at(FATAL_TEMPO, level, 1), state.tempoWindow);
        state.tempoLast = now;
    }

    /**
     * Fatal Tempo's boost on their Ferocity now, in percent, with a weapon of this level: 0 once its "3 seconds after
     * your last attack" are over (see WeaponStats).
     */
    static double tempoBoost(Player player, int level) {
        PlayerState state = PLAYERS.get(player.getUniqueId());
        if (state == null || state.tempo <= 0) return 0;
        long window = (long) (EnchantText.at(FATAL_TEMPO, level, 2) * 1000);
        return System.currentTimeMillis() - state.tempoLast <= window ? state.tempo : 0;
    }

    /** Whether they may have a Fatal Tempo boost now (within the last hit's window), for their weapon to be looked at (see WeaponStats). */
    static boolean tempoRunning(Player player) {
        PlayerState state = PLAYERS.get(player.getUniqueId());
        return state != null && state.tempo > 0 && System.currentTimeMillis() - state.tempoLast <= state.tempoWindow;
    }

    /**
     * Soul Eater's soul goes with the critical hit it's added to, as the hit lands (before it can kill, so the soul of
     * the kill it makes is the next hit's): what they hold, if it's the one with it, has none left. Whether it had this
     * soul to give (see {@link WeaponRules#soulToAdd}: an arrow carries its bow as it left).
     */
    private static boolean eatSoul(Player player, double eaten) {
        NBTTagCompound held = ItemNBT.read(player.getInventory().getItemInMainHand());
        return WeaponRules.soulToAdd(eaten, SOUL_EATER.on(held) > 0, ItemCounters.get(held, SOUL)) && ItemCounters.addHeld(player, SOUL, -eaten);
    }

    /** Knockback's and Punch's extra push for the knockback this hit gives the mob, which vanilla does after its listeners. */
    private static void push(LivingEntity mob, double strength) {
        if (strength <= 0) return;
        PUSHES.merge(mob.getUniqueId(), strength, Math::max);
        if (clearingPushes) return;
        clearingPushes = true;
        // A push no knockback took (a cancelled hit) doesn't wait for the next one.
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            PUSHES.clear();
            clearingPushes = false;
        });
    }

    /**
     * The hit's knockback pushes it that much further: away from the player, or the way the arrow flew. Paper's event
     * for a living entity knocked back by another's hit (its parent, EntityPushedByEntityAttackEvent, is deprecated).
     */
    @EventHandler(ignoreCancelled = true)
    public void onKnockback(EntityKnockbackByEntityEvent event) {
        if (PUSHES.isEmpty()) return;
        Double strength = PUSHES.remove(event.getEntity().getUniqueId());
        if (strength == null) return;
        Entity by = event.getHitBy();
        Vector away = by instanceof Projectile arrow ? arrow.getVelocity().clone()
                : event.getEntity().getLocation().toVector().subtract(by.getLocation().toVector());
        away.setY(0);
        if (away.lengthSquared() < 1e-9) return;
        event.setKnockback(event.getKnockback().clone().add(away.normalize().multiply(strength)));
    }

    // ---------- on the hit as it lands ----------

    /**
     * Swarm's "Increases your damage by 2% for each enemy within 10 blocks. Maximum of 10 enemies." (the mobs that can
     * be hurt around them: UNKNOWN whether it's around them or the target), Combo's "+1% per kill up to 2 kills within
     * 2s" (both additive, the wiki's Additive Sources) and Soul Eater's soul "at the end of your next critical hit"
     * (added damage, which only the crit multiplies: the wiki's "Add Damage").
     */
    static Combat.HitBuff buff(Player player, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
        WeaponEnchant.Levels levels = WeaponEnchant.levels(landing.weapon());
        if (levels.none()) return null;
        double additive = 0;
        double added = 0;
        if (levels.has(SWARM)) {
            int level = levels.of(SWARM);
            int most = (int) EnchantText.at(SWARM, level, 2);
            additive += WeaponRules.swarm(enemies(player, EnchantText.at(SWARM, level, 1), most),
                    EnchantText.at(SWARM, level, 0), most);
        }
        if (levels.has(COMBO)) {
            PlayerState state = PLAYERS.get(player.getUniqueId());
            int level = levels.of(COMBO);
            if (state != null) {
                additive += WeaponRules.combo(state.kills, System.currentTimeMillis(), EnchantText.at(COMBO, level, 0),
                        (int) EnchantText.at(COMBO, level, 1), (long) (EnchantText.at(COMBO, level, 2) * 1000));
            }
        }
        if (levels.has(SOUL_EATER) && landing.critical()) {
            double soul = ItemCounters.get(landing.weapon(), SOUL);
            // Not on a mob it can't hurt (the Watcher), where the hit does nothing; and once, from the item they hold.
            if (soul > 0 && MobHits.hittable(landing.entity()) && eatSoul(player, soul)) added += soul;
        }
        return additive == 0 && added == 0 ? null : new Combat.HitBuff(additive, 1, added);
    }

    /** How many mobs that can be hurt are within {@code radius} of them, counting no further than {@code most}. */
    private static int enemies(Player player, double radius, int most) {
        Location at = player.getLocation();
        double squared = radius * radius;
        int count = 0;
        for (Entity entity : player.getWorld().getNearbyEntities(at, radius, radius, radius, MobHits::hittable)) {
            if (entity.getLocation().distanceSquared(at) <= squared && ++count >= most) break;
        }
        return count;
    }

    // ---------- on a kill ----------

    /**
     * What a kill does with what they hold: Vampirism's "Heal 5❤ every time you kill a mob. (0.5s Cooldown)" (10x less
     * in the Catacombs), Soul Eater takes the mob's soul, Champion and Toxophilite count its Combat XP and tier up; and
     * it counts for their Combo. Before the Book of Stats counts it (MONITOR), which then builds on this.
     */
    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer == null || !killer.isOnline()) return;
        PlayerState state = player(killer);
        state.kills[state.nextKill] = System.currentTimeMillis();
        state.nextKill = (state.nextKill + 1) % state.kills.length;

        PlayerInventory inventory = killer.getInventory();
        ItemStack held = inventory.getItemInMainHand();
        NBTTagCompound tag = ItemNBT.read(held);
        WeaponEnchant.Levels levels = WeaponEnchant.levels(tag);
        if (levels.none()) return;
        if (levels.has(VAMPIRISM)) vampirism(killer, levels.of(VAMPIRISM));
        SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
        if (item == null || InventorySyncListener.frozen(killer)) return;
        boolean changed = false;
        if (levels.has(SOUL_EATER)) changed = soul(killer, tag, levels.of(SOUL_EATER), event);
        if (levels.has(CHAMPION) || levels.has(TOXOPHILITE)) changed |= countCombatXp(killer, tag, event);
        if (changed) inventory.setItemInMainHand(ItemBuilder.build(item, tag, held.getAmount(), killer));
    }

    private static void vampirism(Player killer, int level) {
        PlayerSession session = PlayerSession.of(killer);
        String cooldown = "enchant:" + VAMPIRISM.id();
        if (session.cooldownLeft(cooldown) > 0) return;
        double heal = EnchantText.linear(VAMPIRISM, level + bloodSoaked(killer), 0);
        // UNKNOWN whether Hypixel's 10x less also takes the Catacombs boost all healing there gets (Heals): both here.
        Heals.give(killer, killer, RunManager.inRun(killer) ? heal * VAMPIRISM_IN_DUNGEONS : heal);
        session.startCooldown(cooldown, (long) (EnchantText.at(VAMPIRISM, level, 1) * 1000));
    }

    /**
     * Soul Eater's "Your weapon gains 2x the Damage of the latest monster killed ... (Max 1M outside Dungeons)": the
     * mob's Damage (a room's multiplier's included), in place of what the weapon had. Whether it changed.
     */
    private static boolean soul(Player killer, NBTTagCompound tag, int level, SkyBlockMobDeathEvent event) {
        double damage = event.mob() != null ? event.mob().getDamage() : event.variant() == null ? 0 : event.variant().damage();
        double soul = WeaponRules.soul(EnchantText.at(SOUL_EATER, level, 0), damage, EnchantText.at(SOUL_EATER, level, 1),
                RunManager.inRun(killer));
        double had = ItemCounters.get(tag, SOUL);
        if (soul == had) return false;
        ItemCounters.add(tag, SOUL, soul - had);
        return true;
    }

    /**
     * Champion's and Toxophilite's count: the kill's Combat XP, as the kill gives it (see SkillGains), onto
     * "champion_combat_xp" or "toxophilite_combat_xp", and the level up for each "&850k Combat XP to tier up!" it has
     * reached. Nothing's said when it does (UNKNOWN what Hypixel says). Whether it counted.
     */
    private static boolean countCombatXp(Player killer, NBTTagCompound tag, SkyBlockMobDeathEvent event) {
        User user = User.ifLoaded(killer.getUniqueId());
        double base = event.variant() == null ? 0 : event.variant().combatXp();
        if (user == null || user.isReleased() || base <= 0) return false;
        double wisdom = PlayerSession.of(killer).stats().get(SkillGains.wisdom(Skill.COMBAT));
        double xp = SkillGains.withWisdom(base, wisdom) * (1 + SkillGains.combatXpPercent(Combat.enchantments(tag)) / 100);
        boolean counted = tierUp(tag, CHAMPION, CHAMPION_XP, xp);
        return tierUp(tag, TOXOPHILITE, TOXOPHILITE_XP, xp) || counted;
    }

    /** Adds to a tiered enchantment's count on the item, and moves it up the tiers the count has reached; false without it. */
    static boolean tierUp(NBTTagCompound tag, WeaponEnchant enchant, String key, double xp) {
        int level = enchant.on(tag);
        if (level <= 0 || xp <= 0) return false;
        double count = ItemCounters.add(tag, key, xp);
        int tier = WeaponRules.tier(level, count, at -> EnchantText.hasLevel(enchant, at + 1) ? EnchantText.tierUp(enchant, at) : 0);
        if (tier != level) enchant.setOn(tag, tier);
        return true;
    }

    // ---------- on a shot ----------

    /**
     * Piercing's "Arrows travel through enemies. The extra targets hit take 25% of the damage." and Duplex's "Shoot a
     * second arrow dealing 4% of the first arrow's damage": one more arrow, with the first's aim and speed, from where
     * it is (at once: UNKNOWN when Hypixel's leaves), crit rolled on its own; it tells no shot listeners, so it makes
     * no third.
     */
    static void shot(Player player, Projectile projectile, NBTTagCompound bow, boolean fullyDrawn, boolean shortbow) {
        WeaponEnchant.Levels levels = WeaponEnchant.levels(bow);
        if (levels.none()) return;
        double pierce = levels.has(PIERCING) ? EnchantText.at(PIERCING, levels.of(PIERCING), 0) / 100 : 0;
        if (pierce > 0) Shots.pierce(projectile, PIERCED, pierce);
        Damage.Attacker shooter = levels.has(FLAME) ? Combat.attacker(player, bow, true, 0) : null;
        if (shooter != null) FLAME_SHOTS.put(projectile.getUniqueId(), shooter);
        if (!levels.has(DUPLEX)) return;
        double share = EnchantText.at(DUPLEX, levels.of(DUPLEX), 0) / 100;
        if (share <= 0) return;
        Vector velocity = projectile.getVelocity().clone();
        Arrow second = projectile.getWorld().spawn(projectile.getLocation(), Arrow.class, arrow -> {
            arrow.setShooter(player);
            arrow.setVelocity(velocity);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        });
        Shots.record(second, player, bow, fullyDrawn, share);
        if (pierce > 0) Shots.pierce(second, PIERCED, pierce);
        if (shooter != null) FLAME_SHOTS.put(second.getUniqueId(), shooter);
    }

    /**
     * Infinite Quiver's "Saves arrows 3% of the time when you fire your bow.": the shot takes no arrow (a drawn bow's; a
     * shortbow's take none anyway).
     */
    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        int level = INFINITE_QUIVER.on(Combat.skyBlockData(event.getBow()));
        if (level <= 0) return;
        if (ThreadLocalRandom.current().nextDouble() * 100 < EnchantText.at(INFINITE_QUIVER, level, 0)) event.setConsumeArrow(false);
    }

    // ---------- Rend ----------

    /**
     * Rend's "Use Left Click ability to rip your arrows out of nearby enemies. Each arrow deals 5% of your last critical
     * shot on the target, up to 5 arrows. 2s Cooldown.": a left click with the bow; on each mob near them its arrows
     * from this bow since the last Rend (Rend's own only: "only takes into account crits from the Rend bow itself"),
     * each that share of the last critical arrow on it, as an effect's damage. On cooldown it does nothing (UNKNOWN
     * whether Hypixel says so); the cooldown starts with any use.
     */
    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        NBTTagCompound tag = Combat.skyBlockData(held);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        int level = REND.on(tag);
        if (level <= 0 || item == null || item.specificItemType() != SpecificItemType.BOW) return;
        PlayerSession session = PlayerSession.of(player);
        String cooldown = "enchant:" + REND.id();
        if (session.cooldownLeft(cooldown) > 0 || player.isDead()) return;
        session.startCooldown(cooldown, (long) (EnchantText.at(REND, level, 2) * 1000));
        double share = EnchantText.at(REND, level, 0) / 100;
        int most = (int) EnchantText.at(REND, level, 1);
        for (LivingEntity mob : Hits.near(player.getLocation(), REND_RADIUS)) {
            MobState state = MOBS.get(mob.getUniqueId());
            Counts hits = state == null ? null : state.hits.get(player.getUniqueId());
            if (hits == null || hits.arrows <= 0) continue;
            double damage = Math.floor(Math.min(hits.arrows, most) * share * hits.lastCrit);
            hits.arrows = 0;
            if (damage >= 1) MobHits.deal(player, mob, damage, DamageIndicators.Look.NORMAL, HitKind.OTHER, tag);
        }
    }

    // ---------- the reforge that adds levels ----------

    /**
     * The Blood-Soaked reforge's "Increase the enchantment effects of Life Steal, Vampirism and Drain by 1 level" (the
     * number in its text), on what they wear (it's a cloak's); 0 without it.
     */
    static int bloodSoaked(Player player) {
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) {
            Reforge reforge = Reforge.of(piece.tag());
            if (reforge != null && "blood_soaked".equals(reforge.id())) return (int) CombatReforges.number(reforge, piece.item(), piece.tag(), 0);
        }
        return 0;
    }

    /** The middle of a mob, where what's "within N blocks of the target" is measured from. */
    private static Location center(LivingEntity mob) {
        return mob.getBoundingBox().getCenter().toLocation(mob.getWorld());
    }

    // ---------- what's forgotten ----------

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        MOBS.remove(event.getEntity().getUniqueId());
        if (event.getEntity() instanceof Projectile) FLAME_SHOTS.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PLAYERS.remove(id);
        for (MobState state : MOBS.values()) state.hits.remove(id);
    }
}
