package net.icxd.dungeons.item.ability.weapons;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * Reaving Strike (Bone Reaver, Felthorn Reaper): "Slash in a huge arc, dealing 125% melee damage to all
 * enemies hit!" (135% on the Felthorn Reaper: the share is read off its text), with charges: "4 / 5s" on the
 * Bone Reaver, "5 / 4s" on the Felthorn Reaper (the wiki; the data doesn't have them). With none left a click
 * does nothing (UNKNOWN what Hypixel says). The arc's size is UNKNOWN (5 blocks, 150° wide), and so are the
 * swords' bonuses for missing health (their melee hits don't have them yet either: LATER).
 */
final class ReavingStrike implements AbilityHandler {
    private static final Pattern SHARE = Pattern.compile("(\\d+)% melee damage");
    static final double LENGTH = 5;
    static final double DEGREES = 150;

    /** Each sword's charges, by item id. */
    private static final Map<String, Charges> CHARGES = Map.of(
            "BONE_REAVER", new Charges(4, 5_000),
            "FELTHORN_REAPER", new Charges(5, 4_000),
            "STARRED_FELTHORN_REAPER", new Charges(5, 4_000));
    private static final Charges OTHERS = new Charges(4, 5_000);

    /** The share of their melee damage its text gives ("dealing 125% melee damage": 1.25); 1 if it gives none. */
    static double share(List<String> text) {
        Matcher matcher = SHARE.matcher(String.join(" ", text).replaceAll("[&§].", ""));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) / 100.0 : 1;
    }

    static void forget(UUID player) {
        for (Charges charges : CHARGES.values()) charges.forget(player);
        OTHERS.forget(player);
    }

    private static Charges charges(SkyBlockItem item) {
        return CHARGES.getOrDefault(item.id(), OTHERS);
    }

    @Override
    public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
        return charges(item).left(player.getUniqueId(), System.currentTimeMillis()) > 0;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        if (!charges(item).use(player.getUniqueId(), System.currentTimeMillis())) return;
        double share = share(block.text());
        for (LivingEntity mob : Hits.inCone(player, LENGTH, DEGREES)) Hits.weaponHit(player, tag, mob, Hits.Strike.melee(share));
        slash(player);
    }

    /** A sweep's arc in front of them. */
    private static void slash(Player player) {
        Location eye = player.getEyeLocation();
        for (int i = -3; i <= 3; i++) {
            Location at = eye.clone();
            at.setYaw(eye.getYaw() + i * (float) (DEGREES / 7));
            Vector point = at.getDirection().multiply(2.5);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, eye.clone().add(point), 1, 0, 0, 0, 0);
        }
        player.getWorld().playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.7f);
    }
}
