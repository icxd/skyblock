package net.icxd.dungeons.item.ability.utility;

import java.util.OptionalDouble;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * Heals at once: the Zombie Swords' Instant Heal ("Heal for 320❤ and heal players within 7 blocks for 64❤.",
 * the others' part times the caster's Mending, see {@link Heals}) and the Gloomlock Grimoire's Extreme
 * Measures ("Heal for 1,000❤."). The numbers are the item's text. The zombie swords' charges are gone since
 * 0.26.1 (Vitality pays instead). Their sound and particles are UNKNOWN: a plain one and hearts.
 */
final class InstantHeal implements AbilityHandler {
    /** What it heals the caster, how far its heal on others reaches and what that heals; 0s where it doesn't say. */
    record Amounts(double self, double radius, double others) {
    }

    static Amounts amounts(String plain) {
        OptionalDouble self = AbilityText.after(plain, "Heal for");
        OptionalDouble radius = AbilityText.blocks(plain);
        OptionalDouble others = AbilityText.after(plain, "blocks for");
        return new Amounts(self.orElse(0), radius.orElse(0), others.orElse(0));
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Amounts heal = amounts(AbilityText.plain(block.text()));
        Heals.give(player, player, heal.self());
        if (heal.others() > 0 && heal.radius() > 0) {
            for (Player other : Buffs.youAndNearby(player, heal.radius(), -1)) {
                if (!other.equals(player)) Heals.give(player, other, heal.others());
            }
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.6f, 1.6f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.4, 0.2, 0.4);
    }
}
