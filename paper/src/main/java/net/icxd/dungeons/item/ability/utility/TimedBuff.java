package net.icxd.dungeons.item.ability.utility;

import java.util.OptionalDouble;

import org.bukkit.entity.Player;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.stats.Stats;

/**
 * Stats for a while, as the ability's text gives them ("Grants +100✦ Speed for 30s.", "You and 4 nearby
 * players gain: +30❁ Strength +30✦ Speed for 20 seconds. Effect doesn't stack."), under the ability's name so
 * that using it again (or another item with it) starts them again: the Rogue Sword's Speed Boost on the one
 * using it; the Weird and Weirder Tubas' Howl on them and the 4 nearest players; the Wand of Strength's Life
 * Blood ("Use 10% of your max health to boost your nearby allies by +30❁ Strength for 10 seconds") on them
 * and everyone near (the wiki's use of it for solo Bingo goals has it on the caster too), for a tenth of their
 * max health, which the ability takes itself (its data's health cost isn't that). How near "nearby" is is
 * UNKNOWN ({@link #NEARBY} blocks). The sounds are UNKNOWN: plain ones.
 */
final class TimedBuff implements AbilityHandler {
    /** "Nearby" players: UNKNOWN, taken as within 20 blocks. */
    static final double NEARBY = 20;

    enum Who {
        /** The one using it. */
        SELF,
        /** "You and 4 nearby players": the number is the text's. */
        YOU_AND_NEARBY,
        /** "Use 10% of your max health to boost your nearby allies": everyone near, for health. */
        ALLIES_FOR_HEALTH
    }

    private final Who who;
    /** Its sound's key ("entity.bat.takeoff"), not a {@link org.bukkit.Sound}: those need a server to exist. */
    private final String sound;

    TimedBuff(Who who, String sound) {
        this.who = who;
        this.sound = sound;
    }

    /** "Use 10% of your max health": the share of max health it takes (0.1); 0 if it doesn't say. */
    static double healthShare(String plain) {
        OptionalDouble percent = AbilityText.after(plain, "Use");
        return plain.contains("% of your max health") && percent.isPresent() ? percent.getAsDouble() / 100 : 0;
    }

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        if (AbilityText.millis(plain).isEmpty()) return false;
        // Life Blood can't take the last of their health (UNKNOWN on Hypixel: here it just doesn't cast).
        return who != Who.ALLIES_FOR_HEALTH || PlayerHealth.get(player) > healthShare(plain) * PlayerHealth.max(player);
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        Stats stats = AbilityText.stats(plain);
        long millis = (long) AbilityText.millis(plain).orElse(0);
        if (millis <= 0) return;
        if (who == Who.ALLIES_FOR_HEALTH) PlayerHealth.damage(player, healthShare(plain) * PlayerHealth.max(player));
        int others = switch (who) {
            case SELF -> 0;
            case YOU_AND_NEARBY -> (int) AbilityText.players(plain).orElse(4);
            case ALLIES_FOR_HEALTH -> -1;
        };
        for (Player target : Buffs.youAndNearby(player, NEARBY, others)) Buffs.give(target, block.name(), stats, millis);
        player.getWorld().playSound(player.getLocation(), sound, 1, 1);
    }
}
