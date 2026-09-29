package net.icxd.dungeons.item.ability.utility;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The Secret Tracker 3000: "Always points towards the closest secret while in dungeons." (the wiki's "a compass
 * that points to the nearest Secret while in Dungeons"), and its Echolocation: "Shows you where the nearest dungeon
 * secret is." (its block's 9 mana and 5 s). The nearest secret is the nearest one the team hasn't found, of the
 * rooms someone has walked into (see RunItems#nearestSecret). The compass: while they carry one alive in a run,
 * their compass points there, once a second. The click: particles from them towards it, and a column at it for 5
 * seconds that only they see (UNKNOWN how Hypixel shows it). Outside a run, or with no secret to show, nothing
 * happens and nothing is spent (UNKNOWN what Hypixel says).
 */
final class SecretTracker implements AbilityHandler {
    static final String ID = "SECRET_TRACKER";
    /** How long the column shows, and how often it's drawn. */
    private static final int SHOW_TICKS = 100;
    private static final int EVERY = 5;
    /** How far along the way the trail goes (at most), and its steps. */
    private static final double TRAIL = 8;
    private static final double STEP = 0.5;
    private static final Particle.DustOptions COLOUR = new Particle.DustOptions(Color.fromRGB(85, 255, 255), 1.2f);

    /** Whose compass points at a secret now (so it can point home again). */
    private static final Set<UUID> POINTING = new HashSet<>();

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return RunItems.nearestSecret(player) != null;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Location secret = RunItems.nearestSecret(player);
        if (secret == null) return;
        trail(player, secret);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.6f);
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!player.isOnline() || !player.getWorld().equals(secret.getWorld()) || ticks > SHOW_TICKS) {
                    cancel();
                    return;
                }
                for (double y = 0; y < 3; y += 0.25) player.spawnParticle(Particle.END_ROD, secret.clone().add(0, y, 0), 1, 0, 0, 0, 0);
                ticks += EVERY;
            }
        }.runTaskTimer(Dungeons.getInstance(), 0, EVERY);
    }

    /** Particles from their eyes towards the secret, a few blocks of the way. */
    private static void trail(Player player, Location secret) {
        Location eye = player.getEyeLocation();
        Vector to = secret.toVector().subtract(eye.toVector());
        double length = Math.min(TRAIL, to.length());
        if (length <= 0) return;
        Vector step = to.normalize().multiply(STEP);
        Location at = eye.clone();
        for (double d = 0; d < length; d += STEP) {
            at.add(step);
            player.spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, COLOUR);
        }
    }

    /** Once a second: carriers' compasses point at their nearest secret, and the others' home again. */
    static void second() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location secret = RunItems.alive(player) && carries(player) ? RunItems.nearestSecret(player) : null;
            if (secret != null) {
                player.setCompassTarget(secret);
                POINTING.add(player.getUniqueId());
            } else if (POINTING.remove(player.getUniqueId())) {
                player.setCompassTarget(player.getWorld().getSpawnLocation());
            }
        }
    }

    /** Whether a Secret Tracker is in their inventory. */
    private static boolean carries(Player player) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            NBTTagCompound tag = ItemNBT.read(stack);
            if (tag != null && ID.equals(tag.getString("id"))) return true;
        }
        return false;
    }

    static void forget(UUID player) {
        POINTING.remove(player);
    }
}
