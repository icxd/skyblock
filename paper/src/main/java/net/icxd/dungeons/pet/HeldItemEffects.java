package net.icxd.dungeons.pet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.user.User;

/**
 * What the summoned pet's held item does past its stats (see {@link HeldItems.Effect}): its owner takes no knockback (the
 * Shelmets' "makes the pet's owner immune to knockback"), has a higher speed cap (Unalloyed Speed's "+50 Max Speed Cap":
 * the most that raises it counts, as the plugin's speed caps do, UNKNOWN with a Black Cat's), and has its stats a while
 * after each kill (the Reaper Gem's "Gain 8⫽ Ferocity for 5s on kill.": a kill starts it again, UNKNOWN whether they
 * stack). The Grandma's Knitting Needle is Kill Combo's (see the pet abilities). Main thread.
 */
public final class HeldItemEffects implements Listener {
    private HeldItemEffects() {
    }

    /** Registers the hooks and the listener, once, as the plugin starts. */
    public static void enable(Plugin plugin) {
        PlayerDamage.addKnockbackResistance((player, by) -> held(player).noKnockback() ? 1 : 0);
        PlayerAttributes.addSpeedCap(player -> held(player).speedCap());
        Bukkit.getPluginManager().registerEvents(new HeldItemEffects(), plugin);
    }

    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer == null) return;
        HeldItems.Effect effect = held(killer);
        if (effect.onKillMillis() > 0) PlayerSession.of(killer).buff("pet_item:on_kill", effect.onKill(), effect.onKillMillis());
    }

    /** What their summoned pet's item does (nothing without one). */
    static HeldItems.Effect held(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return HeldItems.Effect.NONE;
        OwnedPet pet = Pets.active(user.profile());
        return pet == null ? HeldItems.Effect.NONE : HeldItems.effect(pet.heldItem());
    }
}
