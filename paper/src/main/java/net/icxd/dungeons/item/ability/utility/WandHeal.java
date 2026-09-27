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
 * The Healing Wands' Small, Medium, Big and Huge Heal: "Heal 60❤ per second for 5s. Wand heals don't
 * stack." (the numbers are the wand's text), a heal over time on themselves, the first second's at once (the
 * wiki's Wand of Healing; see {@link Heals#overTime}). Its sound and particles are UNKNOWN: a plain chime and
 * hearts.
 */
final class WandHeal implements AbilityHandler {
    /** One heal over time from any wand at a time. */
    static final String KIND = "wand";

    /** The heal a second and for how many seconds, from "Heal 60❤ per second for 5s"; null if it doesn't say. */
    static double[] perSecondFor(String plain) {
        OptionalDouble amount = AbilityText.after(plain, "Heal");
        OptionalDouble millis = AbilityText.millis(plain);
        if (amount.isEmpty() || millis.isEmpty()) return null;
        return new double[] {amount.getAsDouble(), Math.round(millis.getAsDouble() / 1000)};
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        double[] heal = perSecondFor(AbilityText.plain(block.text()));
        if (heal == null) return;
        Heals.overTime(player, player, KIND, heal[0], (int) heal[1], true);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1.6f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 3, 0.4, 0.2, 0.4);
    }
}
