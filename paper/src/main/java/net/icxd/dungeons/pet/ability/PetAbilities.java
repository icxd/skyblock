package net.icxd.dungeons.pet.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dwarven.Powder;
import net.icxd.dungeons.economy.DeathCoins;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.accessory.AccessoryEffects;
import net.icxd.dungeons.item.enchanting.armor.ArmorEnchants;
import net.icxd.dungeons.mining.BlockListener;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.pet.OwnedPet;
import net.icxd.dungeons.pet.PetData;
import net.icxd.dungeons.pet.PetData.Block;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetData.Variant;
import net.icxd.dungeons.pet.Pets;
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.user.User;

/**
 * Pets' abilities (PETS.md): which count for a player (their summoned pet's at its rarity, and the ones of the Pets
 * menu's pets that count from there, see {@link PetAbility#always}), their numbers at the pet's level, and what they do,
 * through the plugin's hooks as armor bonuses do theirs (SetBonuses): stats, auras, their hits, hits on them, kills,
 * drops, experience, mana and cooldowns, and a tick a second. What counts is worked out at most once a tick. An
 * ability whose text isn't as its {@link PetAbility#text} says doesn't count ({@link #problems}). Main thread.
 */
public final class PetAbilities implements Listener {
    /** A number in an ability's text: written in it, or one of its block's {n}s. */
    private static final String NUMBER = "(\\{\\d+\\}|\\d[\\d,]*(?:\\.\\d+)?)";
    /** By pet type, then block name: the abilities blocks of that name may be (a pet with several blocks of one name tells them apart by their text). */
    private static final Map<String, Map<String, List<PetAbility>>> BY_PET = new HashMap<>();
    private static final List<PetAbility> ALL = new ArrayList<>();
    private static final Map<PetAbility, Pattern> PATTERNS = new IdentityHashMap<>();
    /** The pet types with an ability that counts from the Pets menu. */
    private static final Set<String> FROM_MENU = new LinkedHashSet<>();
    private static final Map<UUID, Now> NOW = new HashMap<>();
    /** What counted at the last second's tick, so an ability that stops counting can end what it started (see {@link PetAbility#ended}). */
    private static final Map<UUID, Set<PetAbility>> LAST_SECOND = new HashMap<>();
    /** Of each player's Pets menu, the pets whose abilities count from there: worked out once a second. */
    private static final Map<UUID, List<OwnedPet>> MENU = new HashMap<>();
    /** Where a block's numbers are for an ability's text (see {@link #slots}); empty when its text doesn't have it. */
    private static final Map<Block, Map<PetAbility, Optional<Slot[]>>> SLOTS = new IdentityHashMap<>();
    private static PetData slotsFor;
    /** What abilities give anyone (see {@link #addAnyoneStats}). */
    private static final List<BiConsumer<Player, Stats>> ANYONE = new ArrayList<>();

    /** What counts for a player at a tick. */
    private record Now(int tick, List<PetAbility.Active> active) {
    }

    /** Where a {@code #} of an ability's text is in a block's: one of its numbers ({@code placeholder}, from 0), or a number written there. */
    record Slot(int placeholder, double literal) {
        double at(Variant variant, int level) {
            if (placeholder < 0) return literal;
            return placeholder < variant.numbers().size() ? variant.numbers().get(placeholder).at(level) : 0;
        }
    }

    static {
        for (PetAbility ability : StatAbilities.all()) add(ability);
        for (PetAbility ability : FightAbilities.all()) add(ability);
        for (PetAbility ability : RewardAbilities.all()) add(ability);
        for (PetAbility ability : GatheringAbilities.all()) add(ability);
    }

    private PetAbilities() {
    }

    private static void add(PetAbility ability) {
        ALL.add(ability);
        PATTERNS.put(ability, pattern(ability.text()));
        BY_PET.computeIfAbsent(ability.pet(), pet -> new HashMap<>()).computeIfAbsent(ability.name(), name -> new ArrayList<>()).add(ability);
        if (ability.always()) FROM_MENU.add(ability.pet());
    }

    /** Every ability that does something. */
    static List<PetAbility> all() {
        return List.copyOf(ALL);
    }

    /** Registers the hooks, the listener and the second's tick, once, as the plugin starts (after the armor's: see Dungeons). */
    public static void enable(Plugin plugin) {
        PlayerStats.addModifier(PetAbilities::stats);
        PlayerStats.addBuffedModifier(PetAbilities::derivedStats);
        DungeonRun.setHalfLife(member -> {
            Player player = Bukkit.getPlayer(member);
            return player != null && active(player, "SPIRIT", "Half Life");
        });
        Combat.addHitBuffs((Combat.LandingBuffs) (player, attacker, target, landing) -> {
            if (!MobHits.hittable(landing.entity())) return null;
            double additive = 0;
            double multiplier = 1;
            double added = 0;
            for (PetAbility.Active a : active(player)) {
                Combat.HitBuff buff = a.ability().hit(player, a, attacker, target, landing);
                if (buff == null) continue;
                additive += buff.additive();
                multiplier *= buff.multiplier();
                added += buff.added();
            }
            return new Combat.HitBuff(additive, multiplier, added);
        });
        Combat.addHitListener((player, landing, target, damage, killed) -> {
            for (PetAbility.Active a : active(player)) a.ability().landed(player, a, landing, target, damage, killed);
        });
        PlayerDamage.addTakenFrom((player, by) -> {
            double factor = 1;
            for (PetAbility.Active a : active(player)) factor *= a.ability().takenFrom(player, a, by);
            return factor;
        });
        PlayerDamage.addTakenMultiplier(player -> {
            double factor = 1;
            for (PetAbility.Active a : active(player)) factor *= a.ability().taken(player, a);
            return factor;
        });
        PlayerDamage.addKnockbackResistance((player, by) -> {
            double share = 0;
            for (PetAbility.Active a : active(player)) share += a.ability().knockbackResistance(player, a, by);
            return share;
        });
        PlayerDamage.addHurtListener((player, by, kind, taken) -> {
            for (PetAbility.Active a : active(player)) a.ability().hurt(player, a, by, taken);
        });
        VanillaDamage.addFactor((player, cause) -> {
            double factor = 1;
            for (PetAbility.Active a : active(player)) factor *= a.ability().vanillaDamage(player, a, cause);
            return factor;
        });
        SkillGains.addKillWisdom((player, event) -> {
            double wisdom = 0;
            for (PetAbility.Active a : active(player)) wisdom += a.ability().killWisdom(player, a, event);
            return wisdom;
        });
        SkillGains.addKillXpFactor((player, event) -> {
            double factor = 1;
            for (PetAbility.Active a : active(player)) factor *= a.ability().killXp(player, a, event);
            return factor;
        });
        Mobs.addDropChance((killer, blow, drop) -> {
            double factor = 1;
            for (PetAbility.Active a : active(killer)) factor *= a.ability().dropChance(killer, a, blow, drop);
            return factor;
        });
        ExpOrbs.addBonus((player, source) -> {
            double percent = 0;
            for (PetAbility.Active a : active(player)) percent += a.ability().experience(player, a, source);
            return percent;
        });
        Abilities.addManaCostFactor(player -> {
            double factor = 1;
            for (PetAbility.Active a : active(player)) factor *= a.ability().manaCost(player, a);
            return factor;
        });
        Abilities.addItemManaCostFactor((player, item) -> {
            double factor = 1;
            for (PetAbility.Active a : active(player)) factor *= a.ability().itemManaCost(player, a, item);
            return factor;
        });
        Abilities.addCooldownFactor((player, block) -> {
            double factor = 1;
            for (PetAbility.Active a : active(player)) factor *= a.ability().cooldown(player, a, block);
            return factor;
        });
        StatsRunnable.addManaRegenBonus(player -> {
            double bonus = 0;
            for (PetAbility.Active a : active(player)) bonus += a.ability().manaRegen(player, a);
            return bonus;
        });
        PlayerAttributes.addSpeedCap(player -> {
            double raise = 0;
            for (PetAbility.Active a : active(player)) raise = Math.max(raise, a.ability().speedCap(player, a));
            return raise;
        });
        PlayerDamage.addShield((player, taken, by) -> {
            for (PetAbility.Active a : active(player)) {
                if (taken <= 0) return 0;
                taken = a.ability().shield(player, a, taken, by);
            }
            return taken;
        });
        PlayerSession.addMaxManaFactor(player -> {
            double factor = 1;
            for (PetAbility.Active a : active(player)) factor *= a.ability().maxMana(player, a);
            return factor;
        });
        Mana.addSpentListener((player, amount, source) -> {
            for (PetAbility.Active a : active(player)) a.ability().manaSpent(player, a, amount, source);
        });
        DeathCoins.addSaver(player -> {
            double share = 0;
            for (PetAbility.Active a : active(player)) share = Math.max(share, a.ability().deathCoins(player, a));
            return share;
        });
        BlockListener.addMiningSpeed((player, block) -> {
            double bonus = 0;
            for (PetAbility.Active a : active(player)) bonus += a.ability().miningSpeed(player, a, block);
            return bonus;
        });
        DungeonRun.addAllyHealing((player, healing) -> {
            for (PetAbility.Active a : active(player)) a.ability().healedAllies(player, a, healing);
        });
        ArmorEnchants.addLastStandBoost(player -> {
            double percent = 0;
            for (PetAbility.Active a : active(player)) percent += a.ability().lastStand(player, a);
            return percent;
        });
        Powder.addBonus((player, type) -> {
            double percent = 0;
            for (PetAbility.Active a : active(player)) percent += a.ability().powder(player, a, type);
            return percent;
        });
        AccessoryEffects.addIntimidationLevels(player -> {
            double levels = 0;
            for (PetAbility.Active a : active(player)) levels += a.ability().intimidation(player, a);
            return levels;
        });
        plugin.getServer().getPluginManager().registerEvents(new PetAbilities(), plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, PetAbilities::second, 20, 20);
    }

    // ---------- what counts ----------

    /** The abilities that count for them now. */
    public static List<PetAbility.Active> active(Player player) {
        int tick = Bukkit.getCurrentTick();
        Now now = NOW.get(player.getUniqueId());
        if (now != null && now.tick() == tick) return now.active();
        List<PetAbility.Active> active = activeNow(player);
        if (player.isOnline()) NOW.put(player.getUniqueId(), new Now(tick, active));
        return active;
    }

    /** Whether the ability with this pet type and name counts for them now (for what other parts do with it). */
    public static boolean active(Player player, String pet, String name) {
        for (PetAbility.Active a : active(player)) if (a.ability().pet().equals(pet) && a.ability().name().equals(name)) return true;
        return false;
    }

    private static List<PetAbility.Active> activeNow(Player player) {
        if (ALL.isEmpty()) return List.of();
        User user = User.ifLoaded(player.getUniqueId());
        PetData data = Pets.data();
        if (user == null || user.isReleased() || data.count() == 0) return List.of();
        List<PetAbility.Active> active = new ArrayList<>();
        OwnedPet summoned = Pets.active(user.profile());
        if (summoned != null) add(active, summoned, data, false);
        for (OwnedPet pet : MENU.getOrDefault(player.getUniqueId(), List.of())) {
            if (summoned == null || !summoned.type().equals(pet.type())) add(active, pet, data, true);
        }
        return active;
    }

    /** The abilities of this pet that count while it's summoned, at its rarity and level (what {@link #active} has for it). */
    public static List<PetAbility.Active> of(OwnedPet pet, PetData data) {
        List<PetAbility.Active> active = new ArrayList<>();
        add(active, pet, data, false);
        return active;
    }

    /** The pet's abilities that count, at its rarity and level ({@code fromMenu}: only those that count from the Pets menu). */
    static void add(List<PetAbility.Active> active, OwnedPet pet, PetData data, boolean fromMenu) {
        Map<String, List<PetAbility>> abilities = BY_PET.get(pet.type());
        Pet kind = abilities == null ? null : data.pet(pet.type());
        Variant variant = kind == null ? null : kind.variant(Pets.tier(pet, data));
        if (variant == null) return;
        int level = Pets.level(pet, data);
        // A dragon's egg has none of its abilities yet, as it shows none.
        if (level < kind.statsFrom()) return;
        List<Block> blocks = variant.blocks();
        for (int b = 0; b < blocks.size(); b++) {
            for (PetAbility ability : abilities.getOrDefault(name(blocks.get(b)), List.of())) {
                if (fromMenu && !ability.always()) continue;
                Slot[] slots = slots(data, variant, b, ability);
                if (slots == null) continue;
                double[] numbers = new double[slots.length];
                for (int i = 0; i < slots.length; i++) numbers[i] = slots[i].at(variant, level);
                active.add(new PetAbility.Active(ability, pet, level, numbers));
            }
        }
    }

    /** A named block's text, with the unnamed blocks after it (Kill Combo's table of combos is one). */
    static List<String> text(Variant variant, int index) {
        List<Block> blocks = variant.blocks();
        List<String> lines = new ArrayList<>(blocks.get(index).text());
        for (int i = index + 1; i < blocks.size() && name(blocks.get(i)).isEmpty(); i++) lines.addAll(blocks.get(i).text());
        return lines;
    }

    /** A block's name, without its colours ("Hive"). */
    static String name(Block block) {
        return block.name() == null ? "" : AbilityText.plain(List.of(block.name()));
    }

    /** Of a profile's Pets menu, the best pet of each type with an ability that counts from there (by rarity, then level). */
    static List<OwnedPet> fromMenu(Document profile, PetData data) {
        if (FROM_MENU.isEmpty()) return List.of();
        Map<String, OwnedPet> best = new LinkedHashMap<>();
        for (OwnedPet pet : Pets.pets(profile)) {
            if (FROM_MENU.contains(pet.type())) best.merge(pet.type(), pet, (a, b) -> Pets.better(a, b, data));
        }
        return List.copyOf(best.values());
    }

    // ---------- numbers ----------

    private static Slot[] slots(PetData data, Variant variant, int index, PetAbility ability) {
        if (slotsFor != data) {
            SLOTS.clear();
            slotsFor = data;
        }
        return SLOTS.computeIfAbsent(variant.blocks().get(index), b -> new IdentityHashMap<>())
                .computeIfAbsent(ability, a -> Optional.ofNullable(slots(PATTERNS.get(a), text(variant, index))))
                .orElse(null);
    }

    /** Where this pattern's numbers are in these lines (see {@link PetAbility#text}); null if the lines don't have it. */
    static Slot[] slots(Pattern pattern, List<String> lines) {
        Matcher m = pattern.matcher(AbilityText.plain(lines));
        if (!m.find()) return null;
        Slot[] slots = new Slot[m.groupCount()];
        for (int i = 0; i < slots.length; i++) {
            String found = m.group(i + 1);
            slots[i] = found.startsWith("{") ? new Slot(Integer.parseInt(found.substring(1, found.length() - 1)), 0)
                    : new Slot(-1, Double.parseDouble(found.replace(",", "")));
        }
        return slots;
    }

    /** An ability's text as a pattern: each {@code #} a number (or one of its block's {n}s), each {@code *} anything. */
    static Pattern pattern(String text) {
        StringBuilder regex = new StringBuilder();
        StringBuilder words = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c != '#' && c != '*') {
                words.append(c);
                continue;
            }
            if (!words.isEmpty()) regex.append(Pattern.quote(words.toString()));
            words.setLength(0);
            regex.append(c == '#' ? NUMBER : ".*?");
        }
        if (!words.isEmpty()) regex.append(Pattern.quote(words.toString()));
        return Pattern.compile(regex.toString());
    }

    /**
     * What's wrong with the abilities for this data: a pet an ability names that isn't there, a block of an ability's
     * name whose text is as none of that name's abilities say (it doesn't count there), and an ability whose text is
     * nowhere. Logged when the data comes in.
     */
    public static List<String> problems(PetData data) {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<PetAbility>>> byPet : BY_PET.entrySet()) {
            Pet kind = data.pet(byPet.getKey());
            if (kind == null) {
                problems.add(byPet.getKey() + ": no such pet");
                continue;
            }
            for (Map.Entry<String, List<PetAbility>> byName : byPet.getValue().entrySet()) {
                Set<PetAbility> found = new LinkedHashSet<>();
                for (Variant variant : kind.variants().values()) {
                    for (int b = 0; b < variant.blocks().size(); b++) {
                        if (!name(variant.blocks().get(b)).equals(byName.getKey())) continue;
                        boolean any = false;
                        for (PetAbility ability : byName.getValue()) {
                            if (slots(PATTERNS.get(ability), text(variant, b)) == null) continue;
                            any = true;
                            found.add(ability);
                        }
                        if (!any) problems.add(byPet.getKey() + " " + variant.rarity() + " " + byName.getKey() + ": its text isn't as any of its abilities say");
                    }
                }
                for (PetAbility ability : byName.getValue()) {
                    if (!found.contains(ability)) problems.add(byPet.getKey() + " " + byName.getKey() + ": no block has \"" + ability.text() + "\"");
                }
            }
        }
        return problems;
    }

    // ---------- what happens ----------

    /** Their abilities' stats: each one's own, then what's worked out from the rest, then what nearby players' pets' auras give. */
    private static void stats(Player player, Stats stats) {
        for (PetAbility.Active a : active(player)) a.ability().stats(player, a, stats);
    }

    /** Then, once their flat buffs are in too, the stats from stats, the auras and what anyone gets. */
    private static void derivedStats(Player player, Stats stats) {
        for (PetAbility.Active a : active(player)) a.ability().derivedStats(player, a, stats);
        auras(player, stats);
        for (BiConsumer<Player, Stats> anyone : ANYONE) anyone.accept(player, stats);
    }

    /**
     * Adds what an ability gives anyone, whatever pets they have and whoever's near (Web Battlefield's battlefields,
     * where mobs died): after the auras.
     */
    static void addAnyoneStats(BiConsumer<Player, Stats> stats) {
        ANYONE.add(stats);
    }

    /**
     * What abilities give the summoned pet itself: its own abilities' ("to your pet", see {@link PetAbility#petStats}), and
     * what any of theirs does to it ({@link PetAbility#summonedStats}); {@code base} is its own at its level, {@code stats}
     * that and its held item's so far.
     */
    public static void petStats(Player player, OwnedPet pet, Stats base, Stats stats) {
        List<PetAbility.Active> active = active(player);
        for (PetAbility.Active a : active) if (a.pet().uuid().equals(pet.uuid())) a.ability().petStats(player, a, stats);
        for (PetAbility.Active a : active) a.ability().summonedStats(player, a, pet, base, stats);
    }

    /**
     * What players' pets' auras near them give them: each ability's once, at the highest level among those in reach
     * (they say "Doesn't stack"; UNKNOWN whose counts when several are near).
     */
    private static void auras(Player player, Stats stats) {
        Map<PetAbility, PetAbility.Active> best = new IdentityHashMap<>();
        Map<PetAbility, Player> owners = new IdentityHashMap<>();
        for (Player owner : player.getWorld().getPlayers()) {
            if (!inPlay(owner)) continue;
            double distance = owner.getLocation().distanceSquared(player.getLocation());
            for (PetAbility.Active a : active(owner)) {
                double range = a.ability().auraRange(a);
                if (range <= 0 || distance > range * range) continue;
                PetAbility.Active before = best.get(a.ability());
                if (before == null || a.level() > before.level()) {
                    best.put(a.ability(), a);
                    owners.put(a.ability(), owner);
                }
            }
        }
        for (Map.Entry<PetAbility, PetAbility.Active> e : best.entrySet()) e.getKey().aura(owners.get(e.getKey()), e.getValue(), player, stats);
    }

    /** Whether their abilities act now: not the dead's, or dungeon ghosts' (who are invulnerable), as armor bonuses' don't. */
    static boolean inPlay(Player player) {
        return !player.isDead() && !player.isInvulnerable();
    }

    /** A fall that hurts: before what changes its damage (see {@link VanillaDamage}), how far they fell. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) return;
        for (PetAbility.Active a : active(player)) a.ability().fell(player, a, player.getFallDistance());
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        for (PetAbility.Active a : active(player)) a.ability().jumped(player, a);
    }

    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer == null || !killer.isOnline()) return;
        for (PetAbility.Active a : active(killer)) a.ability().killed(killer, a, event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        NOW.remove(id);
        LAST_SECOND.remove(id);
        MENU.remove(id);
        for (PetAbility ability : ALL) ability.forget(id);
    }

    /**
     * Every second: which of their menu's pets count from there, what counts does its second's work, and what stopped
     * counting ends. The dead and dungeon ghosts do no second's work (see {@link #inPlay}).
     */
    private static void second() {
        PetData data = Pets.data();
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = User.ifLoaded(player.getUniqueId());
            MENU.put(player.getUniqueId(), user == null || user.isReleased() ? List.of() : fromMenu(user.profile(), data));
            Set<PetAbility> counting = new LinkedHashSet<>();
            if (inPlay(player)) {
                for (PetAbility.Active a : active(player)) {
                    counting.add(a.ability());
                    a.ability().second(player, a);
                }
            }
            Set<PetAbility> before = LAST_SECOND.put(player.getUniqueId(), counting);
            if (before != null) for (PetAbility ability : before) if (!counting.contains(ability)) ability.ended(player);
        }
    }
}
