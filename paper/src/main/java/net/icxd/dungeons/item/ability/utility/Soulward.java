package net.icxd.dungeons.item.ability.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The Soul Esoward's Soulward: "Become invulnerable for 5s, but can't deal damage. Halves your damage for 2s
 * afterwards." (see {@link Protection}). Its 10 Soulflow isn't charged: Soulflow doesn't exist yet (UNKNOWN
 * what having none would do). The sound and particles are UNKNOWN: plain ones.
 */
final class Soulward implements AbilityHandler {
    static final String NAME = "Soulward";
    private static final long INVULNERABLE_MILLIS = 5_000;
    private static final long HALVED_MILLIS = 2_000;

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        long millis = (long) AbilityText.after(plain, "invulnerable for").orElse(INVULNERABLE_MILLIS / 1000.0) * 1000;
        long afterwards = (long) AbilityText.after(plain, "damage for").orElse(HALVED_MILLIS / 1000.0) * 1000;
        long now = System.currentTimeMillis();
        Protection.immunity(player, NAME, millis);
        Protection.noAttack(player, NAME, millis);
        Protection.dealt(player, NAME, 0.5, now + millis, afterwards);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 1, 0.6f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4);
    }
}
