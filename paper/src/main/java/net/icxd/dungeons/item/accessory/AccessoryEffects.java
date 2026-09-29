package net.icxd.dungeons.item.accessory;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.SkyBlockServer;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.mob.goals.TargetNearestPlayerGoal;
import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.shop.Shop;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.storage.AccessoryBag;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.SkyBlockTime;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What accessories do beyond their stats (ACCESSORIES.md), for those that count: in the Accessory Bag or the
 * inventory, one of each and the best of a line, as the Accessory Bag counts them ({@link AccessoryBag#counted}).
 * Each one's numbers are its text's ({@link AccessoryText}). Through the plugin's hooks: stats (the crystals by day
 * and night, the Campfire Talismans while burning, the Reaper Orb's kills, the Blood God Crest's counter, the IQ
 * Points, the Bluetooth Rings, the island ones, the Gravity Talisman, the Master Skulls, the Haste Ring's Mining
 * Speed), hits taken (the mob-type talismans, the Burststopper), hits (the Tarantula Talisman, the Wedding Rings, the
 * Vampire Dentist Relic), kills (the Devour Ring), mobs' targets (the Intimidation line), experience (the Experience
 * Artifact), drops (the Bucket of Dye), vanilla damage (the Feather, Vaccine, Fire and Lava Talismans), a second's
 * tick (Night Vision, the Feather line's fall height), a minute's (the Emerald Ring's coins) and shops (the Shady
 * Ring's line). Main thread.
 */
public final class AccessoryEffects implements Listener {
    /** "Reduces the damage taken from ☠ Wither mobs by 20%". */
    private static final Pattern TAKEN = Pattern.compile("damage taken from \\S* ?([A-Z][a-z]+) mobs by ([\\d.]+)%");
    private static final Pattern HASTE = Pattern.compile("Haste ([IVX]+)");
    private static final NamespacedKey SAFE_FALL = new NamespacedKey("dungeons", "accessory_safe_fall");
    /**
     * Mining Speed a level of Haste is given as (the wiki's Haste Ring: "instead of the Haste being overridden, the
     * player is given +100 Mining Speed" for II; the Artifact's III as 150 is UNKNOWN).
     */
    static final double HASTE_MINING_SPEED = 50;
    /** How far from an island's spawn the Gravity Talisman's +10 drops to +1: UNKNOWN (the wiki gives no distances), a point every 10 blocks. */
    static final double GRAVITY_STEP = 10;
    /** The Blood God Crest and Sigil, whose kills are counted on them. */
    static final String CREST = "BLOOD_GOD_CREST";
    static final String SIGIL = "BLOOD_GOD_SIGIL";
    /** Where the Blood God Crest keeps its counter. */
    static final String KILLS = "blood_god_kills";

    private static final Map<UUID, Summary> SUMMARIES = new HashMap<>();
    private static final Map<UUID, Deque<Long>> REAPER_KILLS = new HashMap<>();
    private static final Map<UUID, Map<UUID, Integer>> TARANTULA_HITS = new HashMap<>();
    private static final Map<UUID, Long> DENTIST = new HashMap<>();
    private static final Map<UUID, Long> DEVOUR = new HashMap<>();
    private static final Map<UUID, Double> CREST_COUNT = new HashMap<>();
    private static final Set<UUID> NIGHT_VISION = new HashSet<>();

    /** A talisman's "Reduces the damage taken from X mobs by N%": from what, the factor, and whether on the Crimson Isle it's from all. */
    record Reduction(MobType type, double factor, boolean allOnCrimsonIsle) {
    }

    /**
     * What their counted accessories do, worked out once each time the Accessory Bag's look at them changes (it keeps
     * the same set until then).
     */
    static final class Summary {
        Set<String> ids = Set.of();
        final List<String> day = new ArrayList<>();
        final List<String> night = new ArrayList<>();
        final List<String> burning = new ArrayList<>();
        final List<Reduction> reductions = new ArrayList<>();
        double iqPercent;
        String bluetooth;
        String mine;
        String village;
        boolean gravity;
        String masterSkull;
        int haste;
        String reaperOrb;
        String bloodGod;
        String devour;
        String dentist;
        String tarantula;
        String wedding;
        String burststopper;
        String experience;
        String feather;
        String vaccine;
        String fire;
        boolean lava;
        boolean nightVision;
        String emerald;
        String discount;
        String dye;
        double intimidation = -1;
    }

    private AccessoryEffects() {
    }

    /** Registers the hooks, the listener and the ticks, once, as the plugin starts (from SetBonuses). */
    public static void enable(Plugin plugin) {
        PlayerStats.addModifier(AccessoryEffects::stats);
        PlayerDamage.addTakenFrom(AccessoryEffects::takenFrom);
        PlayerDamage.addShield((player, taken, by) -> burststopper(summary(player), taken, PlayerHealth.get(player)));
        Combat.addHitBuffs((Combat.LandingBuffs) AccessoryEffects::hit);
        Combat.addHitListener(AccessoryEffects::landed);
        ExpOrbs.addBonus((player, source) -> {
            Summary s = summary(player);
            return s.experience == null ? 0 : AccessoryText.after(s.experience, "gain by", 25);
        });
        VanillaDamage.addFactor(AccessoryEffects::vanillaFactor);
        TargetNearestPlayerGoal.addIgnored(AccessoryEffects::ignores);
        Mobs.addDropChance((killer, blow, drop) -> {
            Summary s = summary(killer);
            return s.dye == null || !dye(drop.itemIds()) ? 1 : 1 + AccessoryText.after(s.dye, "Dyes by", 0) / 100;
        });
        Shop.addDiscount(player -> {
            Summary s = summary(player);
            return s.discount == null ? 0 : AccessoryText.after(s.discount, "Get", 0);
        });
        plugin.getServer().getPluginManager().registerEvents(new AccessoryEffects(), plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, AccessoryEffects::second, 20, 20);
        Bukkit.getScheduler().runTaskTimer(plugin, AccessoryEffects::minute, 1200, 1200);
    }

    // ---------- which count ----------

    /** What their counted accessories do now. */
    static Summary summary(Player player) {
        Set<String> ids = AccessoryBag.counted(player);
        Summary s = SUMMARIES.get(player.getUniqueId());
        if (s != null && s.ids == ids) return s;
        s = summarize(ids);
        if (player.isOnline()) SUMMARIES.put(player.getUniqueId(), s);
        return s;
    }

    /** What these counted accessories do. */
    static Summary summarize(Set<String> ids) {
        Summary s = new Summary();
        s.ids = ids;
        for (String id : ids) {
            String plain = AccessoryText.plain(id);
            switch (id) {
                case "DAY_CRYSTAL", "SUNSHINE_CRYSTAL" -> s.day.add(id);
                case "NIGHT_CRYSTAL", "MOONLIGHT_CRYSTAL" -> s.night.add(id);
                case "IQ_POINT", "TWO_IQ_POINT" -> s.iqPercent += AccessoryText.after(id, "by", 0);
                case "BLUETOOTH_RING", "BLUERTOOTH_RING" -> s.bluetooth = id;
                case "MINE_TALISMAN" -> s.mine = id;
                case "VILLAGE_TALISMAN" -> s.village = id;
                case "GRAVITY_TALISMAN" -> s.gravity = true;
                case "HASTE_RING", "HASTE_ARTIFACT" -> s.haste = Math.max(s.haste, haste(plain));
                case "REAPER_ORB" -> s.reaperOrb = id;
                case CREST, SIGIL -> s.bloodGod = id;
                case "DEVOUR_RING" -> s.devour = id;
                case "VAMPIRE_DENTIST_RELIC" -> s.dentist = id;
                case "TARANTULA_TALISMAN", "TARANTULA_RING" -> s.tarantula = id;
                case "BURSTSTOPPER_TALISMAN", "BURSTSTOPPER_ARTIFACT" -> s.burststopper = id;
                case "EXPERIENCE_ARTIFACT" -> s.experience = id;
                case "FEATHER_TALISMAN", "FEATHER_RING", "FEATHER_ARTIFACT" -> s.feather = id;
                case "VACCINE_TALISMAN", "VACCINE_RING", "VACCINE_ARTIFACT" -> s.vaccine = id;
                case "FIRE_TALISMAN" -> s.fire = id;
                case "LAVA_TALISMAN" -> s.lava = true;
                case "NIGHT_VISION_CHARM" -> s.nightVision = true;
                case "EMERALD_RING", "EMERALD_ARTIFACT" -> s.emerald = id;
                case "SHADY_RING", "CROOKED_ARTIFACT", "SEAL_OF_THE_FAMILY" -> s.discount = id;
                case "BUCKET_OF_DYE" -> s.dye = id;
                case "INTIMIDATION_TALISMAN", "INTIMIDATION_RING", "INTIMIDATION_ARTIFACT", "INTIMIDATION_RELIC" ->
                        s.intimidation = Math.max(s.intimidation, AccessoryText.after(id, "Level", 0));
                default -> {
                    if (id.startsWith("CAMPFIRE_TALISMAN_") || id.startsWith("SOUL_CAMPFIRE_TALISMAN_")) s.burning.add(id);
                    else if (id.startsWith("WEDDING_RING_")) s.wedding = id;
                    else if (id.startsWith("MASTER_SKULL_TIER_") && AccessoryText.after(id, "Grants", 0) > 0) s.masterSkull = best(s.masterSkull, id);
                }
            }
            Reduction reduction = reduction(plain);
            if (reduction != null) s.reductions.add(reduction);
        }
        return s;
    }

    /**
     * Whether a drop is a Dye (the Bucket of Dye's "Increases your chance of dropping Dyes by 1%": a factor of 1.01 on
     * its chance, UNKNOWN whether it's that or a point more).
     */
    static boolean dye(List<String> itemIds) {
        if (itemIds.isEmpty()) return false;
        for (String id : itemIds) if (!id.startsWith("DYE_")) return false;
        return true;
    }

    /** Of two Master Skulls, the one that grants more. */
    private static String best(String a, String b) {
        return a == null || AccessoryText.after(b, "Grants", 0) > AccessoryText.after(a, "Grants", 0) ? b : a;
    }

    /** "Grants permanent Haste II": its level (2); 0 for none. */
    static int haste(String plain) {
        Matcher m = HASTE.matcher(plain);
        return m.find() ? roman(m.group(1)) : 0;
    }

    /** I to X. */
    static int roman(String numeral) {
        int total = 0;
        for (int i = 0; i < numeral.length(); i++) {
            int value = value(numeral.charAt(i));
            int next = i + 1 < numeral.length() ? value(numeral.charAt(i + 1)) : 0;
            total += value < next ? -value : value;
        }
        return total;
    }

    private static int value(char c) {
        return switch (c) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            default -> 0;
        };
    }

    /** A talisman's reduction on hits from a mob type ("... from ☠ Wither mobs by 20%"); null if its text has none. */
    static Reduction reduction(String plain) {
        Matcher m = TAKEN.matcher(plain);
        if (!m.find()) return null;
        MobType type;
        try {
            type = MobType.valueOf(m.group(1).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new Reduction(type, Math.max(0, 1 - Double.parseDouble(m.group(2)) / 100), plain.contains("from all mobs"));
    }

    // ---------- stats ----------

    /**
     * Their accessories' stats beyond their own: every one's rule (see the class), after the other modifiers that came
     * before (the set bonuses'); the IQ Points' share of "your total Intelligence" last of them.
     */
    private static void stats(Player player, Stats stats) {
        Summary s = summary(player);
        if (s.ids.isEmpty()) return;
        boolean day = SkyBlockTime.now().isDay();
        for (String id : day ? s.day : s.night) {
            double more = AccessoryText.after(id, "by", 0);
            stats.add(Stat.STRENGTH, more).add(Stat.DEFENSE, more);
        }
        if (!s.burning.isEmpty() && player.getFireTicks() > 0) for (String id : s.burning) stats.add(AccessoryText.stats(id));
        // No most for the Reaper Orb's stacks: the wiki's "stacking up to {{InfoNeeded}}" (UNKNOWN).
        if (s.reaperOrb != null) {
            Deque<Long> kills = REAPER_KILLS.get(player.getUniqueId());
            int recent = kills == null ? 0 : recent(kills, System.currentTimeMillis(), (long) (AccessoryText.after(s.reaperOrb, "last", 5) * 1000));
            stats.add(Stat.STRENGTH, recent * AccessoryText.after(s.reaperOrb, "stack of", 2));
        }
        if (s.bloodGod != null) {
            Double count = CREST_COUNT.get(player.getUniqueId());
            if (count != null) stats.add(Stat.STRENGTH, bloodGod(count, AccessoryText.after(s.bloodGod, "Gain", 1), AccessoryText.after(s.bloodGod, "Max", 7)));
        }
        // The Bluetooth Rings' "players on the island you're on": in their world, this server's island (UNKNOWN).
        if (s.bluetooth != null && player.getWorld().getPlayers().size() >= AccessoryText.after(s.bluetooth, "least", 6)) {
            stats.add(Stat.DAMAGE, AccessoryText.after(s.bluetooth, "Deal", 0));
        }
        if (s.mine != null && on(ServerType.DWARVEN_MINES)) stats.add(AccessoryText.stats(s.mine));
        if (s.village != null && PlayerSession.of(player).getRegion() == RegionType.VILLAGE) {
            stats.add(Stat.SPEED, AccessoryText.after(s.village, "by", 0));
        }
        if (s.gravity) {
            double more = gravity(player.getLocation(), player.getWorld().getSpawnLocation());
            stats.add(Stat.STRENGTH, more).add(Stat.DEFENSE, more);
        }
        if (s.haste > 0) stats.add(Stat.MINING_SPEED, HASTE_MINING_SPEED * s.haste);
        // The Master Skulls' and IQ Points' shares are of the stats as they are here (UNKNOWN where Hypixel's come).
        if (s.masterSkull != null) {
            DungeonRun run = RunManager.of(player);
            if (run != null && run.masterMode()) {
                double factor = 1 + AccessoryText.after(s.masterSkull, "Grants", 0) / 100;
                stats.set(Stat.HEALTH, stats.get(Stat.HEALTH) * factor).set(Stat.STRENGTH, stats.get(Stat.STRENGTH) * factor);
            }
        }
        if (s.iqPercent > 0) stats.set(Stat.INTELLIGENCE, stats.get(Stat.INTELLIGENCE) * (1 + s.iqPercent / 100));
    }

    /** How many of these kills were within {@code window} of {@code now}; the older ones go. */
    static int recent(Deque<Long> kills, long now, long window) {
        while (!kills.isEmpty() && now - kills.peekFirst() > window) kills.removeFirst();
        return kills.size();
    }

    /**
     * The Blood God Crest's Strength: "Gain +1❁ Strength for each digit on the counter. (Max 7)", "1–9 kill(s)
     * grants +1, 10–99 kills grants +2, and so on" (the wiki: floor(log10(kills)) + 1); the Sigil's +2 a digit.
     */
    static double bloodGod(double kills, double perDigit, double most) {
        if (kills < 1) return 0;
        int digits = String.valueOf((long) Math.floor(kills)).length();
        return perDigit * Math.min(digits, most);
    }

    /**
     * The Gravity Talisman's "between +1 and +10 ❁ Strength and ❈ Defense the closer you are to the spawn of an
     * Island": +10 at the world's spawn, one less every {@link #GRAVITY_STEP} blocks (across, UNKNOWN), at least +1.
     */
    static double gravity(Location at, Location spawn) {
        if (spawn == null || at.getWorld() != spawn.getWorld()) return 1;
        double dx = at.getX() - spawn.getX();
        double dz = at.getZ() - spawn.getZ();
        return Math.max(1, 10 - Math.floor(Math.sqrt(dx * dx + dz * dz) / GRAVITY_STEP));
    }

    // ---------- hits taken ----------

    /** Their talismans' factor on a hit by {@code by}: each one's for its mob type (the Nether Artifact's from all on the Crimson Isle). */
    private static double takenFrom(Player player, Entity by) {
        Summary s = summary(player);
        if (s.reductions.isEmpty()) return 1;
        return takenFrom(s.reductions, SetBonuses.types(by), on(ServerType.CRIMSON_ISLE));
    }

    /** The factor these reductions put on a hit by a mob of these types. */
    static double takenFrom(List<Reduction> reductions, Set<MobType> types, boolean crimsonIsle) {
        double factor = 1;
        for (Reduction r : reductions) if (types.contains(r.type()) || (crimsonIsle && r.allOnCrimsonIsle() && !types.isEmpty())) factor *= r.factor();
        return factor;
    }

    /**
     * The Burststopper's "If an incoming hit would deal at least 50% of your ❤, multiply its damage by 0.95": what
     * the hit would take after the rest (Defense, factors), against their health now ("above 50% of their health",
     * the wiki: UNKNOWN whether it's their max).
     */
    static double burststopper(Summary s, double taken, double health) {
        if (s.burststopper == null || taken < AccessoryText.after(s.burststopper, "at least", 50) / 100 * health) return taken;
        return taken * AccessoryText.after(s.burststopper, "damage by", 1);
    }

    // ---------- hits ----------

    /**
     * On a hit of theirs: the Tarantula Talisman's "Every 10th melee hit on the same enemy deals +10% damage" (a
     * multiplicative 1.1, the wiki's Multiplicative Sources; its hits on each mob counted apart), and a Wedding Ring's
     * "1 in 100 chance to deal +100% damage" (additive, the wiki's Additive Sources, as the Ring of Love's; its
     * "Requires quest progress!" isn't asked, there being no quests here: UNKNOWN whether it'd stop it).
     */
    private static Combat.HitBuff hit(Player player, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
        Summary s = summary(player);
        // Not on a mob that can't be hurt (the Watcher): its hits would count towards the 10th for nothing.
        if ((s.tarantula == null && s.wedding == null) || !MobHits.hittable(landing.entity())) return null;
        double multiplier = 1;
        double additive = 0;
        if (s.tarantula != null && landing.kind() == HitKind.MELEE) {
            Map<UUID, Integer> hits = TARANTULA_HITS.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
            int n = hits.merge(landing.entity().getUniqueId(), 1, Integer::sum);
            if (n % (int) Math.max(1, AccessoryText.after(s.tarantula, "Every", 10)) == 0) {
                multiplier = 1 + AccessoryText.after(s.tarantula, "deals", 0) / 100;
            }
        }
        if (s.wedding != null && ThreadLocalRandom.current().nextDouble() * AccessoryText.after(s.wedding, "1 in", Double.MAX_VALUE) < 1) {
            additive = AccessoryText.after(s.wedding, "deal", 100);
        }
        return additive == 0 && multiplier == 1 ? null : new Combat.HitBuff(additive, multiplier);
    }

    /** The Vampire Dentist Relic's "Apply a natural regen tick when you deal melee damage. (20s cooldown)". */
    private static void landed(Player player, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
        if (landing.kind() != HitKind.MELEE || damage <= 0) return;
        Summary s = summary(player);
        if (s.dentist == null || !ready(DENTIST, player, (long) (AccessoryText.after(s.dentist, "(", 20) * 1000))) return;
        Stats stats = PlayerSession.of(player).stats();
        PlayerHealth.heal(player, Damage.healthRegen(PlayerHealth.max(player), stats.get(Stat.HEALTH_REGEN)));
    }

    /** Whether their cooldown in {@code last} is over; if it is, it starts again. */
    private static boolean ready(Map<UUID, Long> last, Player player, long millis) {
        long now = System.currentTimeMillis();
        Long at = last.get(player.getUniqueId());
        if (at != null && now - at < millis) return false;
        last.put(player.getUniqueId(), now);
        return true;
    }

    // ---------- kills ----------

    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer == null || !killer.isOnline()) return;
        Summary s = summary(killer);
        if (s.reaperOrb != null) REAPER_KILLS.computeIfAbsent(killer.getUniqueId(), id -> new ArrayDeque<>()).addLast(System.currentTimeMillis());
        if (s.devour != null && event.kind() != null && event.kind().types().contains(MobType.UNDEAD)
                && ready(DEVOUR, killer, (long) (AccessoryText.after(s.devour, "Cooldown:", 0.5) * 1000))) {
            PlayerHealth.heal(killer, AccessoryText.after(s.devour, "Heal", 0));
        }
        if (s.bloodGod != null) countCrest(killer, s.bloodGod);
    }

    /**
     * The Blood God Crest "Tracks a counter for each mob killed while worn": one more on the counted one, where it is
     * (their inventory, or their Accessory Bag), rebuilt so its "Counter" line follows.
     */
    private static void countCrest(Player player, String id) {
        if (InventorySyncListener.frozen(player)) return;
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            NBTTagCompound tag = ItemNBT.read(contents[i]);
            if (tag == null || !id.equals(tag.getString("id"))) continue;
            ItemStack counted = ItemCounters.add(contents[i], player, KILLS, 1);
            if (counted != null) {
                inventory.setItem(i, counted);
                CREST_COUNT.put(player.getUniqueId(), ItemCounters.get(ItemNBT.read(counted), KILLS));
            }
            return;
        }
        AccessoryBag.rewrite(player, id, stack -> {
            ItemStack counted = ItemCounters.add(stack, player, KILLS, 1);
            if (counted != null) CREST_COUNT.put(player.getUniqueId(), ItemCounters.get(ItemNBT.read(counted), KILLS));
            return counted;
        });
    }

    // ---------- mobs' targets ----------

    /**
     * The Intimidation line's "Monsters at or below Level 5 will no longer target you" (the Talisman's "Level 1
     * monsters"): SkyBlock's mobs by the level their name shows (a dungeon mob's shows none, so it isn't one of
     * them: UNKNOWN); the Blood Room's undead choose for themselves.
     */
    private static boolean ignores(LivingEntity mob, Player player) {
        Summary s = summary(player);
        if (s.intimidation < 1) return false;
        int level = SetBonuses.shownLevel(mob);
        return level >= 0 && level <= s.intimidation;
    }

    // ---------- vanilla damage ----------

    /**
     * The Feather Ring's and Artifact's "Fall damage is also reduced by 15%", the Vaccine line's "Reduces the effect of
     * poison by 10%" (poison's damage), and on the Crimson Isle the Fire Talisman's "grants a 20% damage reduction
     * instead" (of fire's).
     */
    private static double vanillaFactor(Player player, VanillaDamage.Cause cause) {
        Summary s = summary(player);
        return switch (cause) {
            case FALL -> s.feather == null ? 1 : 1 - AccessoryText.after(s.feather, "reduced by", 0) / 100;
            case POISON -> s.vaccine == null ? 1 : 1 - AccessoryText.after(s.vaccine, "poison by", 0) / 100;
            case FIRE -> s.fire != null && on(ServerType.CRIMSON_ISLE) ? 1 - AccessoryText.after(s.fire, "grants a", 0) / 100 : 1;
            default -> 1;
        };
    }

    /**
     * The Fire Talisman's "immunity against damage from Fire" and the Lava Talisman's "from most Lava": none of it,
     * except on the Crimson Isle (the Fire Talisman's 20% instead; the Lava Talisman's "does not provide immunity
     * against damage from lava in the Crimson Isle", the wiki), before it becomes SkyBlock health.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Summary s = summary(player);
        if (s.fire == null && !s.lava) return;
        VanillaDamage.Cause cause = VanillaDamage.Cause.of(event.getCause());
        boolean immune = !on(ServerType.CRIMSON_ISLE) && (cause == VanillaDamage.Cause.FIRE && s.fire != null || cause == VanillaDamage.Cause.LAVA && s.lava);
        if (immune) event.setCancelled(true);
    }

    // ---------- every second, every minute ----------

    /**
     * Every second: the Night Vision Charm's "Grants Night Vision" (until it doesn't count; from the Accessory Bag
     * too, though its text says "in your inventory": UNKNOWN), the Feather line's
     * "Increases how high you can fall before taking fall damage by 5 blocks" (their safe fall distance), the Blood
     * God Crest's count as it is on the item, and the Tarantula Talisman's counts on mobs that are gone.
     */
    private static void second() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Summary s = summary(player);
            UUID id = player.getUniqueId();
            if (s.nightVision) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 30, 1, true, false, true));
                NIGHT_VISION.add(id);
            } else if (NIGHT_VISION.remove(id)) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
            safeFall(player, s.feather == null ? 0 : AccessoryText.after(s.feather, "damage by", 0));
            if (s.bloodGod != null) {
                ItemStack crest = AccessoryBag.countedItem(player, s.bloodGod);
                if (crest != null) CREST_COUNT.put(id, ItemCounters.get(ItemNBT.read(crest), KILLS));
            } else {
                CREST_COUNT.remove(id);
            }
            Map<UUID, Integer> hits = TARANTULA_HITS.get(id);
            if (hits != null && (s.tarantula == null || hits.size() > 64)) TARANTULA_HITS.remove(id);
        }
    }

    /** Their safe fall distance this much more than it is (ours: another's stays). */
    private static void safeFall(Player player, double more) {
        AttributeInstance attribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(SAFE_FALL);
        if (current != null && current.getAmount() == more) return;
        if (current != null) attribute.removeModifier(SAFE_FALL);
        if (more != 0) attribute.addTransientModifier(new AttributeModifier(SAFE_FALL, more, AttributeModifier.Operation.ADD_NUMBER));
    }

    /** Every minute: the Emerald Ring's "Get +5 coins every minute!" (the Artifact's +20), on any profile (as kills' coins are). */
    private static void minute() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Summary s = summary(player);
            if (s.emerald == null) continue;
            User user = User.ifLoaded(player.getUniqueId());
            if (user != null && !user.isReleased()) Purse.add(user, AccessoryText.after(s.emerald, "Get", 0));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        SUMMARIES.remove(id);
        REAPER_KILLS.remove(id);
        TARANTULA_HITS.remove(id);
        DENTIST.remove(id);
        DEVOUR.remove(id);
        CREST_COUNT.remove(id);
        NIGHT_VISION.remove(id);
    }

    /** Whether this server hosts that part of SkyBlock; false before the plugin has started. */
    static boolean on(ServerType type) {
        SkyBlockServer server = Dungeons.getSkyBlockServer();
        return server != null && server.getServerType() == type;
    }

    /**
     * The Catacombs Expert Ring's "Increases Catacombs dungeon experience by +10%" and Scarf's Studies' to
     * Grimoire's "Gain 2% more Dungeons class experience", in percent: {catacombs, class}. For a run's experience
     * boosts once there's a hook for them (the armor enchantments' RunBoosts: UNKNOWN until it's merged).
     */
    public static double[] runBoost(Player player) {
        Set<String> ids = AccessoryBag.counted(player);
        double catacombs = ids.contains("CATACOMBS_EXPERT_RING") ? AccessoryText.after("CATACOMBS_EXPERT_RING", "by", 0) : 0;
        double classes = 0;
        for (String scarf : List.of("SCARF_STUDIES", "SCARF_THESIS", "SCARF_GRIMOIRE")) {
            if (ids.contains(scarf)) classes = Math.max(classes, AccessoryText.after(scarf, "Gain", 0));
        }
        return new double[] {catacombs, classes};
    }
}
