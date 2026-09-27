package net.icxd.dungeons.item.ability.weapons;

import org.bukkit.event.Listener;

import net.icxd.dungeons.combat.PlayerDamage;

/**
 * What weapon abilities need to hear of: damage to players (Wither Shield's 10% less while it's up).
 * Registered once, when the plugin starts.
 */
public final class WeaponEvents implements Listener {
    public WeaponEvents() {
        PlayerDamage.addTakenMultiplier(WitherBlade::takenFactor);
    }
}
