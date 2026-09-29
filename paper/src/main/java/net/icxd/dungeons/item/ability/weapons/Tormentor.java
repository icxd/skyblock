package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.utils.Utils;

/**
 * The Tormentor's Chain of Agony: "Toggles On/Off on use. Melee hits arc with cursed energy, chaining to up to 5
 * nearby enemies." Its 2026 addition (the wiki's Tormentor history) has the rest: "chaining to up to 5 nearby mobs
 * dealing 100% damage. Arcs jump between mobs, with a range of 3 blocks. Each jump costs 200 Mana, with a 0.5s
 * cooldown." So the block's 200 mana and 0.5 s are the chain's: a jump's mana (with what makes the item's abilities
 * cheaper, {@link Abilities#manaCost}), and the time between chains. The toggle itself is free, so a click only
 * switches it (see {@link ChainOfAgony#usable}). Each jump deals the hit's damage to the next mob, as an effect's
 * damage (no hit listeners, no Ferocity), and it stops when their mana runs short. Its look, sound and messages are
 * UNKNOWN. (Its Angered is WeaponPassives'.)
 */
final class Tormentor {
    static final String NAME = "Chain of Agony";
    /** "Arcs jump between mobs, with a range of 3 blocks." */
    static final double JUMP_RANGE = 3;
    /** "chaining to up to 5 nearby enemies", if its text doesn't say. */
    private static final int JUMPS = 5;
    private static final Pattern CHAIN = Pattern.compile("chaining to up to (\\d+)");

    private static final Set<UUID> ON = new HashSet<>();
    /** When each player's next chain may go. */
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private Tormentor() {
    }

    static void register() {
    }

    static void forget(UUID player) {
        ON.remove(player);
        NEXT.remove(player);
    }

    /** The click: on, or off again. */
    static final class ChainOfAgony implements AbilityHandler {
        /** It switches here and isn't a use: nothing is paid for the switch, and no cooldown starts. */
        @Override
        public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
            boolean on = ON.add(player.getUniqueId());
            if (!on) ON.remove(player.getUniqueId());
            player.sendMessage(Utils.color(on ? "&aChain of Agony &2Enabled" : "&aChain of Agony &cDisabled"));
            player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1, on ? 1.2f : 0.8f);
            return false;
        }

        @Override
        public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        }
    }

    /** A melee hit with the Tormentor landed: while the chain is on and ready, it jumps from what was hit. */
    static void landed(Player player, WeaponPassives.Weapon weapon, Combat.Landing landing, double damage) {
        if (!ON.contains(player.getUniqueId())) return;
        ItemBlock block = block(weapon.item);
        if (block == null) return;
        long now = System.currentTimeMillis();
        Long next = NEXT.get(player.getUniqueId());
        if (next != null && now < next) return;
        PlayerSession session = PlayerSession.of(player);
        int cost = Abilities.manaCost(block, session.maxMana(), player, landing.weapon());
        int jumps = jumps(block);
        LivingEntity from = landing.entity();
        Set<UUID> hit = new HashSet<>();
        hit.add(from.getUniqueId());
        boolean chained = false;
        for (int i = 0; i < jumps; i++) {
            int mana = session.getMana() < 0 ? session.maxMana() : session.getMana();
            if (mana < cost) break;
            LivingEntity to = nextMob(from, hit);
            if (to == null) break;
            Hits.takeMana(player, cost, NAME);
            arc(from, to);
            MobHits.deal(player, to, damage, DamageIndicators.Look.NORMAL, HitKind.OTHER, landing.weapon());
            hit.add(to.getUniqueId());
            from = to;
            chained = true;
        }
        if (chained) NEXT.put(player.getUniqueId(), now + (long) (block.cooldown() * 1000));
    }

    private static ItemBlock block(SkyBlockItem item) {
        for (ItemBlock block : item.blocks()) if (block.isAbility() && NAME.equals(block.name())) return block;
        return null;
    }

    /** "chaining to up to 5 nearby enemies": its text's number. */
    static int jumps(ItemBlock block) {
        Matcher m = CHAIN.matcher(AbilityText.plain(block.text()));
        return m.find() ? Integer.parseInt(m.group(1)) : JUMPS;
    }

    /** The nearest mob within the jump's range of {@code from} that the chain hasn't hit yet; null for none. */
    private static LivingEntity nextMob(LivingEntity from, Set<UUID> hit) {
        for (LivingEntity mob : Hits.near(from.getLocation(), JUMP_RANGE)) {
            if (!hit.contains(mob.getUniqueId())) return mob;
        }
        return null;
    }

    /** Particles along the arc (UNKNOWN how Hypixel's looks). */
    private static void arc(LivingEntity from, LivingEntity to) {
        Location a = from.getLocation().add(0, from.getHeight() / 2, 0);
        Vector step = to.getLocation().add(0, to.getHeight() / 2, 0).toVector().subtract(a.toVector());
        int points = Math.max(1, (int) (step.length() * 3));
        step.multiply(1.0 / points);
        for (int i = 0; i <= points; i++) {
            a.getWorld().spawnParticle(Particle.WITCH, a, 1, 0, 0, 0, 0);
            a.add(step);
        }
    }
}
