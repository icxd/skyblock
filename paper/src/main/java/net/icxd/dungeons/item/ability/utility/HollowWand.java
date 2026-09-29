package net.icxd.dungeons.item.ability.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;

/**
 * The Hollow Wand's Hollow Spirit (LEFT/RIGHT CLICK): "Left Click to cast a stack of [✤] and Right Click to cast a
 * stack of [✦]. Combine stacks to cast different spells! [✤✤] Spirit Spark (10 ⚶), [✦✦] Hollowed Rush (15 ⚶), [✦✤]
 * Raging Wind (20 ⚶), [✤✦] Ichor Pool (30 ⚶)" (its text's costs). Two clicks make a spell; it's paid in the Hollow
 * Armor's ⚶ Spirit stacks, which are the armor bonuses' (its tiered Spirit, BONUSES.md): {@link
 * UtilityAbilities#hollowSpirit} says where they come from, and until something gives them there are none. What the
 * spells do is the wiki's Hollow Wand ('/wandinfo'): Spirit Spark heals "yourself and up to 5 players within 25
 * blocks for 8% max HP"; Hollowed Rush grants them "+1.3x" Mana Regen for 6 s; Raging Wind "+30%" Damage and +10
 * Ferocity for 20 s, "Stacks up to 3 times"; Ichor Pool is "a pool with 8 block radius. Up to 5 players standing
 * within this pool gains +1.2x Damage and +40 Ferocity. Pool lasts for 20s." "The effects of the Spells do not stack
 * when used by multiple players unless otherwise specified": each is one buff whoever cast it. How long a first click
 * waits for its second, and what's shown, are UNKNOWN (3 seconds; the clicks so far where Defense is on the action
 * bar). Main thread.
 */
final class HollowWand implements AbilityHandler {
    static final String NAME = "Hollow Spirit";
    /** The wiki's '/wandinfo' numbers. */
    static final int PLAYERS = 5;
    static final double RADIUS = 25;
    static final double SPARK_HEAL = 0.08;
    static final double RUSH_REGEN = 0.3;
    static final long RUSH_MILLIS = 6_000;
    static final double WIND_DAMAGE = 30;
    static final double WIND_FEROCITY = 10;
    static final long WIND_MILLIS = 20_000;
    static final int WIND_STACKS = 3;
    static final double POOL_RADIUS = 8;
    static final double POOL_DAMAGE = 1.2;
    static final double POOL_FEROCITY = 40;
    static final long POOL_MILLIS = 20_000;
    /** How long a first click waits for its second (UNKNOWN). */
    static final long COMBO_MILLIS = 3_000;

    /** A spell: its two clicks (L for ✤, R for ✦) and name. */
    enum Spell {
        SPIRIT_SPARK("LL", "Spirit Spark"),
        HOLLOWED_RUSH("RR", "Hollowed Rush"),
        RAGING_WIND("RL", "Raging Wind"),
        ICHOR_POOL("LR", "Ichor Pool");

        final String clicks;
        final String display;

        Spell(String clicks, String display) {
            this.clicks = clicks;
            this.display = display;
        }

        /** The spell two clicks make; null for none. */
        static Spell of(String clicks) {
            for (Spell spell : values()) if (spell.clicks.equals(clicks)) return spell;
            return null;
        }
    }

    private static ToIntFunction<Player> stacks = player -> 0;
    private static ObjIntConsumer<Player> spend = (player, amount) -> {
    };

    /** A first click waiting for its second: which, and until when. */
    private record Pending(char click, long until) {
    }

    /** A Raging Wind on someone: its stacks, until when. */
    private record Wind(int stacks, long until) {
    }

    /** An Ichor Pool: where, until when. */
    private record Pool(Location at, long until) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> RUSHED = new HashMap<>();
    private static final Map<UUID, Wind> WINDS = new HashMap<>();
    private static final List<Pool> POOLS = new ArrayList<>();

    /** Where the ⚶ Spirit stacks come from, and how they're spent (see {@link UtilityAbilities#hollowSpirit}). */
    static void spiritFrom(ToIntFunction<Player> stacks, ObjIntConsumer<Player> spend) {
        HollowWand.stacks = stacks;
        HollowWand.spend = spend;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, boolean vitalityPaid, Trigger trigger) {
        char click = trigger.right() ? 'R' : 'L';
        long now = System.currentTimeMillis();
        Pending first = PENDING.remove(player.getUniqueId());
        if (first == null || first.until() < now) {
            PENDING.put(player.getUniqueId(), new Pending(click, now + COMBO_MILLIS));
            show(player, "&8[" + symbol(click) + "&8]");
            return;
        }
        Spell spell = Spell.of("" + first.click() + click);
        if (spell == null) return;
        int cost = cost(block, spell);
        if (stacks.applyAsInt(player) < cost) {
            player.sendMessage(Utils.color("&cYou need " + cost + " ⚶ Spirit to cast " + spell.display + "!"));
            return;
        }
        spend.accept(player, cost);
        show(player, "&8[" + symbol(first.click()) + symbol(click) + "&8] &d" + spell.display);
        cast(player, spell);
    }

    private static String symbol(char click) {
        return click == 'R' ? "&b✦" : "&c✤";
    }

    private static void show(Player player, String text) {
        PlayerSession.of(player).setDefenseReplacement(Replacement.forMillis(Utils.color(text), 1500));
    }

    /** "Spirit Spark (10 ⚶)": its cost in the text (0 if it doesn't say). */
    static int cost(ItemBlock block, Spell spell) {
        Matcher m = Pattern.compile(Pattern.quote(spell.display) + " \\((\\d+) ⚶\\)").matcher(AbilityText.plain(block.text()));
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static void cast(Player player, Spell spell) {
        long now = System.currentTimeMillis();
        List<Player> them = Buffs.youAndNearby(player, RADIUS, PLAYERS);
        switch (spell) {
            case SPIRIT_SPARK -> {
                for (Player target : them) Heals.give(player, target, SPARK_HEAL * PlayerHealth.max(target));
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1.4f);
            }
            case HOLLOWED_RUSH -> {
                for (Player target : them) RUSHED.put(target.getUniqueId(), now + RUSH_MILLIS);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1.4f);
            }
            case RAGING_WIND -> {
                for (Player target : them) {
                    Wind before = WINDS.get(target.getUniqueId());
                    int stacked = before == null || before.until() < now ? 1 : Math.min(WIND_STACKS, before.stacks() + 1);
                    WINDS.put(target.getUniqueId(), new Wind(stacked, now + WIND_MILLIS));
                    PlayerSession.of(target).buffPercent(spell.display, Stat.DAMAGE, WIND_DAMAGE * stacked, WIND_MILLIS);
                    Buffs.give(target, spell.display, new Stats().set(Stat.FEROCITY, WIND_FEROCITY * stacked), WIND_MILLIS);
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1, 1);
            }
            case ICHOR_POOL -> {
                POOLS.add(new Pool(player.getLocation(), now + POOL_MILLIS));
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1, 0.8f);
            }
        }
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 20, 0.5, 0.6, 0.5, 0.02);
    }

    /** Hollowed Rush's share more mana regeneration while it lasts (see StatsRunnable#addManaRegenBonus). */
    static double manaRegen(Player player) {
        Long until = RUSHED.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis() ? RUSH_REGEN : 0;
    }

    /** The players an Ichor Pool gives to now: up to 5 standing in it, nearest its middle first. */
    private static List<Player> inPool(Pool pool) {
        List<Player> in = new ArrayList<>();
        for (Player player : pool.at().getWorld().getPlayers()) {
            if (!player.isDead() && !player.isInvulnerable() && player.getLocation().distanceSquared(pool.at()) <= POOL_RADIUS * POOL_RADIUS) in.add(player);
        }
        in.sort((a, b) -> Double.compare(a.getLocation().distanceSquared(pool.at()), b.getLocation().distanceSquared(pool.at())));
        return in.size() > PLAYERS ? in.subList(0, PLAYERS) : in;
    }

    /** Whether they stand in an Ichor Pool that gives to them now (one pool's worth, however many there are). */
    static boolean inAPool(Player player) {
        if (POOLS.isEmpty()) return false;
        long now = System.currentTimeMillis();
        for (Pool pool : POOLS) {
            if (pool.until() > now && pool.at().getWorld().equals(player.getWorld()) && inPool(pool).contains(player)) return true;
        }
        return false;
    }

    /** An Ichor Pool's Ferocity (see PlayerStats#addModifier). */
    static void stats(Player player, Stats stats) {
        if (inAPool(player)) stats.add(Stat.FEROCITY, POOL_FEROCITY);
    }

    /** An Ichor Pool's "+1.2x Damage" on their hits (see Combat#addMultiplier). */
    static double multiplier(Player player, Boolean ranged) {
        return inAPool(player) ? POOL_DAMAGE : 1;
    }

    /** Every tick: pools that are done go; the rest show. */
    static void tick() {
        if (POOLS.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Iterator<Pool> it = POOLS.iterator(); it.hasNext(); ) {
            Pool pool = it.next();
            if (pool.until() <= now) {
                it.remove();
                continue;
            }
            if (now / 50 % 10 == 0) pool.at().getWorld().spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, pool.at(), 30, POOL_RADIUS / 2, 0.1, POOL_RADIUS / 2, 0);
        }
    }

    static void forget(UUID player) {
        PENDING.remove(player);
        RUSHED.remove(player);
        WINDS.remove(player);
    }
}
