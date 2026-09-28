package net.icxd.dungeons.item.ability.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.utils.Utils;

/**
 * The Wither Cloak Sword's Creeper Veil (0.26.1's rework, as its text has it): "Spawns a layered veil that
 * negates damage for 10s": each hit takes a layer, "consuming 30♨ Vitality and block up to 1,000 damage",
 * that much times 1 + the Catacombs boost in a run ("The damage blocked scales with your Dungeon Stat Boost
 * while in The Catacombs"), more layers for a bigger hit; with too little Vitality left for a layer it
 * consumes all of it, they take slight knockback and the veil is gone (the wiki: "the veil will
 * automatically toggle off"), the rest of that hit getting through (UNKNOWN). They can't attack, and their
 * Vitality doesn't regenerate ({@link Vitality#addRegenPause}), while it's up. A right click with the sword while it's up takes it down
 * ({@link UtilityListener}), and "Cooldown is halved on deactivation": the cooldown runs from when it's
 * down, 10 seconds after it expires, 5 after they take it down (the reading that makes halving mean
 * something). The cast costs the 30 Vitality its data gives, and each layer its own 30 (UNKNOWN whether the
 * cast does). It's gone when the sword leaves their inventory (the wiki's trivia). What it looks like is
 * the wiki's: 6 invisible charged creepers on them (their aura is the veil). The messages are the wiki's
 * and SkyHanni's; the one for running out of Vitality is UNKNOWN (the old mana one with Vitality for mana).
 */
final class CreeperVeil implements AbilityHandler {
    static final String NAME = "Creeper Veil";
    private static final int CREEPERS = 6;
    /** "consuming 30♨ Vitality", "block up to 1,000 damage": if the text doesn't say. */
    private static final double LAYER_VITALITY = 30;
    private static final double LAYER_DAMAGE = 1_000;
    private static final double KNOCKBACK = 0.3;
    private static final double KNOCKBACK_UP = 0.2;

    private static final class Veil {
        long until;
        long cooldownMillis;
        double layerVitality;
        double layerDamage;
        final List<Creeper> creepers = new ArrayList<>();
        int ticks;
    }

    private static final Map<UUID, Veil> VEILS = new HashMap<>();

    static boolean isUp(Player player) {
        return VEILS.containsKey(player.getUniqueId());
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        Veil veil = new Veil();
        long millis = (long) AbilityText.millis(plain).orElse(10_000);
        veil.until = System.currentTimeMillis() + millis;
        veil.cooldownMillis = (long) (block.cooldown() * 1000);
        veil.layerVitality = AbilityText.after(plain, "consuming").orElse(LAYER_VITALITY);
        double boost = RunManager.inRun(player) ? ItemBuilder.catacombsBoost(player) : 0;
        // At least a point a layer, so a hit always runs out of layers or Vitality.
        veil.layerDamage = Math.max(1, AbilityText.after(plain, "block up to").orElse(LAYER_DAMAGE) * (1 + boost));
        // Its cooldown runs from when it's down.
        PlayerSession.of(player).startCooldown(cooldownKey(), 0);
        forget(player.getUniqueId());
        for (int i = 0; i < CREEPERS; i++) veil.creepers.add(creeper(player.getLocation()));
        VEILS.put(player.getUniqueId(), veil);
        Protection.noAttack(player, NAME, millis);
        player.sendMessage(Utils.color("&dCreeper Veil &aActivated!"));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1, 0.5f);
    }

    private static String cooldownKey() {
        return "ability:" + NAME;
    }

    private static Creeper creeper(Location at) {
        return at.getWorld().spawn(at, Creeper.class, c -> {
            c.setPowered(true);
            c.setInvisible(true);
            c.setAI(false);
            c.setSilent(true);
            c.setInvulnerable(true);
            c.setGravity(false);
            c.setCollidable(false);
            c.setPersistent(false);
            c.addScoreboardTag(UtilityListener.NOT_A_MOB);
        });
    }

    /** A shield: what's left of a hit once the veil has taken its layers. */
    static double absorb(Player player, double taken, Entity by) {
        Veil veil = VEILS.get(player.getUniqueId());
        if (veil == null) return taken;
        double left = taken;
        while (left > 0) {
            if (!Vitality.spend(player, veil.layerVitality)) {
                PlayerSession.of(player).setVitality(0);
                knockBack(player, by);
                takeDown(player, false, "&cNot enough Vitality! &dCreeper Veil &cDe-activated!");
                return left;
            }
            left -= veil.layerDamage;
        }
        return 0;
    }

    private static void knockBack(Player player, Entity by) {
        if (by instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) by = shooter;
        Vector away = by == null ? new Vector() : player.getLocation().toVector().subtract(by.getLocation().toVector()).setY(0);
        if (away.lengthSquared() > 0) away.normalize().multiply(KNOCKBACK);
        player.setVelocity(away.setY(KNOCKBACK_UP));
    }

    /** A right click with the sword while it's up: down it comes, and the cooldown is half. */
    static boolean deactivate(Player player) {
        if (!isUp(player)) return false;
        takeDown(player, true, "&dCreeper Veil &cDe-activated!");
        return true;
    }

    /**
     * Takes their veil down, if there is one, with {@code message} (null for none): its creepers go, and its
     * cooldown starts, halved if they took it down themselves.
     */
    private static void takeDown(Player player, boolean byThem, String message) {
        Veil veil = VEILS.remove(player.getUniqueId());
        if (veil == null) return;
        for (Creeper creeper : veil.creepers) creeper.remove();
        Protection.endNoAttack(player, NAME);
        PlayerSession.of(player).startCooldown(cooldownKey(), cooldownAfter(veil.cooldownMillis, byThem));
        if (message != null) player.sendMessage(Utils.color(message));
    }

    /** The cooldown from when it's down: all of it, or half when they took it down. */
    static long cooldownAfter(long cooldownMillis, boolean byThem) {
        return byThem ? cooldownMillis / 2 : cooldownMillis;
    }

    /**
     * Every tick: the creepers keep up, and the veil goes when it's over, the sword has gone or they've died
     * (in a run that's being a ghost: the death itself is called off).
     */
    static void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Veil> entry : List.copyOf(VEILS.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Veil veil = entry.getValue();
            if (player == null) {
                forget(entry.getKey());
            } else if (player.isDead() || ghost(player)) {
                // Its no attacking goes with it, and its cooldown starts.
                takeDown(player, false, null);
            } else if (now >= veil.until) {
                takeDown(player, false, "&dCreeper Veil &cDe-activated! &8(Expired)");
            } else if (++veil.ticks % 20 == 0 && !carries(player)) {
                takeDown(player, false, null);
            } else {
                Location at = player.getLocation();
                for (Creeper creeper : veil.creepers) if (creeper.isValid()) creeper.teleport(at);
            }
        }
    }

    private static boolean ghost(Player player) {
        DungeonRun run = RunManager.of(player);
        return run != null && run.isGhost(player.getUniqueId());
    }

    /** Whether something in their inventory has Creeper Veil. */
    private static boolean carries(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) if (Worn.ability(stack, NAME) != null) return true;
        return false;
    }

    /** They've left: their veil goes with them. */
    static void forget(UUID player) {
        Veil veil = VEILS.remove(player);
        if (veil != null) for (Creeper creeper : veil.creepers) creeper.remove();
    }
}
