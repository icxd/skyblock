package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * "Throw", on two weapons (the lassos' "Throw" is the Forest's, not built: a click with one does nothing).
 * The Livid Dagger's: "Throw your dagger at your enemies!", "throw their sword forward, dealing damage to
 * enemies in its path" (the wiki): a melee hit on each, crits as their Crit Chance rolls. The Halberd of
 * the Shredded's: "Throw your Halberd, damaging all enemies in its path, dealing 10% melee damage.
 * Consecutive throws cost 2x more Mana and deal 2x more damage than the previous. (max 16x)"; "the Throw
 * ability is treated as a melee strike" (the wiki). When throws stop counting as consecutive is UNKNOWN
 * (4 seconds without one, as the Jerry-chine Gun's cost resets), and so are how far and fast they fly (20
 * blocks, 1.5 a tick, through mobs, not walls). The weapons' own passives (the dagger's crits from behind,
 * the halberd's healing and Undead damage) aren't built for their melee hits either: LATER.
 */
final class ThrownBlade implements AbilityHandler {
    static final String HALBERD = "AXE_OF_THE_SHREDDED";
    static final String DAGGER = "LIVID_DAGGER";
    static final double HALBERD_SHARE = 0.1;
    static final int MOST_DOUBLINGS = 4;
    static final long CONSECUTIVE_MILLIS = 4_000;
    private static final double SPEED = 1.5;
    private static final double RANGE = 20;

    private record Streak(int throwsBefore, long last) {
    }

    private static final Map<UUID, Streak> STREAKS = new HashMap<>();

    /** The halberd's multiplier after this many consecutive throws: 1, 2, 4, 8, then 16 for good. */
    static int multiplier(int throwsBefore) {
        return 1 << Math.min(Math.max(0, throwsBefore), MOST_DOUBLINGS);
    }

    static void forget(UUID player) {
        STREAKS.remove(player);
    }

    private static int throwsBefore(Player player, long now) {
        Streak streak = STREAKS.get(player.getUniqueId());
        return streak == null || now - streak.last() >= CONSECUTIVE_MILLIS ? 0 : streak.throwsBefore();
    }

    @Override
    public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
        return item.id().equals(HALBERD) || item.id().equals(DAGGER);
    }

    /** The halberd's mana past its block's 20: 20, 40, 80 ... in all. */
    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        if (!item.id().equals(HALBERD)) return true;
        return Hits.enoughMana(player, block, extraMana(block, throwsBefore(player, System.currentTimeMillis())));
    }

    private static int extraMana(ItemBlock block, int throwsBefore) {
        return (int) Math.round(block.mana() * (multiplier(throwsBefore) - 1));
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        double share = 1;
        if (item.id().equals(HALBERD)) {
            long now = System.currentTimeMillis();
            int before = throwsBefore(player, now);
            Hits.takeMana(player, extraMana(block, before), block.name());
            STREAKS.put(player.getUniqueId(), new Streak(before + 1, now));
            share = HALBERD_SHARE * multiplier(before);
        }
        Hits.Strike strike = Hits.Strike.melee(share);
        // Its hits are the throw's, whatever they hold when it lands.
        Damage.Attacker thrown = Hits.striker(player, tag, strike);
        Location eye = player.getEyeLocation();
        new Missile(player, eye, eye.getDirection().multiply(SPEED))
                .range(RANGE)
                .width(0.4)
                .look(Missile.display(eye, player.getInventory().getItemInMainHand().clone(), 1, 90))
                .onHit((missile, mob) -> {
                    Hits.weaponHit(missile.caster(), thrown, mob, strike);
                    return true;
                })
                .launch();
        player.getWorld().playSound(eye, Sound.ENTITY_SNOWBALL_THROW, 1, 0.7f);
    }
}
