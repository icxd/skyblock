package net.icxd.dungeons.reforge;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.weapons.Hits;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.enchanting.weapon.WeaponRules;
import net.icxd.dungeons.item.enchanting.weapon.EnchantText;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What reforges do past their stats in a fight (REFORGES.md's "Later", ENCHANTS_WEAPONS.md): the swords' Fabled,
 * Suspicious and Fanged, the bows' Precise and Headstrong, and the three armor ones the weapon part has: Loving
 * (ability damage), Hyper (the Endstone Geode's "warped": Speed after teleporting) and Empowered (Mending in
 * dungeons). Their numbers are their bonus text's at the item's rarity (reforges.json), as its lore shows it.
 * Withered's and Ancient's stats a Catacombs level are ItemStats'; Blood-Soaked's levels are the enchantments'
 * (WeaponEnchants). Main thread.
 */
public final class CombatReforges implements Listener {
    /**
     * Their bonus texts' numbers by rarity, worked out once each, for the table they're from (a reloaded table's
     * reforges are new ones, so the old ones are let go).
     */
    private static final Map<Reforge, double[][]> NUMBERS = new IdentityHashMap<>();
    private static ReforgeTable numbersFor;
    private static final double[] NONE = new double[0];
    /** Each player's melee hits on each mob, for Fanged's "Every 7th melee hit on an enemy". */
    private static final Map<UUID, Map<UUID, int[]>> MELEE = new HashMap<>();

    public CombatReforges() {
        Combat.addHitBuffs(CombatReforges::buff);
        PlayerStats.addModifier(CombatReforges::stats);
        Hits.addMagicMultiplier(CombatReforges::magic);
        SetBonuses.addTeleportListener(CombatReforges::teleported);
    }

    /** The numbers in this reforge's bonus on an item of this rarity ("Every 7th melee hit ... deals +100% damage." is 7, 100). */
    static double[] numbers(Reforge reforge, Rarity rarity) {
        // One the table doesn't have is made anew each time it's asked for (Reforge#unknown), with no bonus: kept, they'd pile up.
        if (reforge.bonus().isEmpty()) return NONE;
        ReforgeTable table = ReforgeTable.get();
        if (table != numbersFor) {
            numbersFor = table;
            NUMBERS.clear();
        }
        double[][] byRarity = NUMBERS.computeIfAbsent(reforge, r -> new double[Rarity.values().length][]);
        double[] numbers = byRarity[rarity.ordinal()];
        if (numbers == null) numbers = byRarity[rarity.ordinal()] = EnchantText.numbers(String.join(" ", reforge.bonusLines(rarity)));
        return numbers;
    }

    /** How many reforges' numbers are kept now (for tests). */
    static int kept() {
        return NUMBERS.size();
    }

    /** The {@code index}th number of the reforge's bonus on this item; 0 if there's none. */
    public static double number(Reforge reforge, SkyBlockItem item, NBTTagCompound tag, int index) {
        double[] numbers = item == null ? NONE : numbers(reforge, ItemBuilder.rarity(item, tag));
        return index < numbers.length ? numbers[index] : 0;
    }

    /** The reforge on an item with this data if it's this one (its modifier id); null otherwise. */
    static Reforge reforge(NBTTagCompound tag, String id) {
        Reforge reforge = Reforge.of(tag);
        return reforge != null && id.equals(reforge.id()) ? reforge : null;
    }

    // ---------- hits ----------

    /**
     * On a hit as it lands, by the weapon's reforge: Fabled's "Critical hits have a chance to deal up to 15% extra
     * damage" (a melee crit's damage times 1 + 0.15 x a roll from 0 to 1, the wiki's Multiplicative Sources, which has
     * it ConfirmationNeeded), Fanged's "Every 7th melee hit on an enemy deals +100% damage" (each player's own melee
     * hits on it; additive, as the wiki has other reforges' "+X% damage": UNKNOWN), and Precise's and Headstrong's
     * "Deal +10% extra damage when arrows hit the head of a mob" (additive, the wiki's Additive Sources).
     */
    static Combat.HitBuff buff(Player player, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
        Reforge reforge = Reforge.of(landing.weapon());
        if (reforge == null) return null;
        SkyBlockItem item = ItemRegistry.get(landing.weapon().getString("id"));
        switch (reforge.id()) {
            case "fabled" -> {
                if (landing.kind() != HitKind.MELEE || !landing.critical()) return null;
                double upTo = number(reforge, item, landing.weapon(), 0);
                return new Combat.HitBuff(0, 1 + upTo / 100 * ThreadLocalRandom.current().nextDouble());
            }
            case "fanged" -> {
                if (landing.kind() != HitKind.MELEE || landing.entity() == null) return null;
                int hits = ++MELEE.computeIfAbsent(landing.entity().getUniqueId(), id -> new HashMap<>(2))
                        .computeIfAbsent(player.getUniqueId(), id -> new int[1])[0];
                return WeaponRules.every(hits, (int) number(reforge, item, landing.weapon(), 0))
                        ? new Combat.HitBuff(number(reforge, item, landing.weapon(), 1), 1) : null;
            }
            case "precise", "headstrong" -> {
                Projectile arrow = landing.projectile();
                LivingEntity mob = landing.entity();
                if (landing.kind() != HitKind.ARROW || arrow == null || mob == null) return null;
                if (!WeaponRules.headshot(arrow.getLocation().getY(), mob.getLocation().getY(), mob.getHeight(), mob.getEyeHeight())) return null;
                return new Combat.HitBuff(number(reforge, item, landing.weapon(), 0), 1);
            }
            default -> {
                return null;
            }
        }
    }

    /** Loving's "Increases ability damage by 5%", on the chestplate they wear: a factor on their magic damage (see Hits). */
    static double magic(Player player) {
        double factor = 1;
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) {
            Reforge reforge = reforge(piece.tag(), "loving");
            if (reforge != null && piece.item().specificItemType() == SpecificItemType.CHESTPLATE) {
                factor *= 1 + number(reforge, piece.item(), piece.tag(), 0) / 100;
            }
        }
        return factor;
    }

    // ---------- stats ----------

    /**
     * What a reforge gives an item that its lore doesn't list: Suspicious's "Increases weapon damage by +15" (live
     * Twilight Daggers with it show their own Damage: REFORGES.md). Part of ItemStats (see WeaponStats#addUnlisted).
     */
    public static void unlisted(SkyBlockItem item, NBTTagCompound tag, Stats stats) {
        Reforge reforge = reforge(tag, "suspicious");
        if (reforge != null) stats.add(Stat.DAMAGE, number(reforge, item, tag, 0));
    }

    /** Empowered's "Grants +10☄ Mending while in Dungeons", a worn piece's each, while they're in a run. */
    private static void stats(Player player, Stats stats) {
        if (!RunManager.inRun(player)) return;
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) {
            Reforge reforge = reforge(piece.tag(), "empowered");
            if (reforge != null) stats.add(Stat.MENDING, number(reforge, piece.item(), piece.tag(), 0));
        }
    }

    /**
     * Hyper's "Gain +6✦ Speed for 5s after teleporting" (the Endstone Geode's reforge, "warped" in the data): each worn
     * piece's Speed, together (UNKNOWN whether Hypixel's pieces add up), for as long as it says, from any teleport the
     * set bonuses hear of (an ability's, an ender pearl's).
     */
    private static void teleported(Player player) {
        double speed = 0;
        double seconds = 0;
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) {
            Reforge reforge = reforge(piece.tag(), "warped");
            if (reforge == null) continue;
            speed += number(reforge, piece.item(), piece.tag(), 0);
            seconds = Math.max(seconds, number(reforge, piece.item(), piece.tag(), 1));
        }
        if (speed <= 0 || seconds <= 0) return;
        PlayerSession.of(player).buff("reforge:hyper", new Stats().set(Stat.SPEED, speed), (long) (seconds * 1000));
    }

    // ---------- what's forgotten ----------

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        MELEE.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        for (Map<UUID, int[]> hits : MELEE.values()) hits.remove(event.getPlayer().getUniqueId());
    }
}
