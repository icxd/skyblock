package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;

/**
 * The Bonemerang's Swing: "Throw the bone a short distance, dealing the damage an arrow would. Deals
 * double damage when coming back. Pierces up to 10 foes." "It travels forward up to 13.5 blocks before
 * reaching the apex of its journey and returning ... If the Bonemerang comes into contact with a block or
 * strikes more than 10 enemies while traveling, it will shatter and become unusable for 3 seconds" (the
 * wiki). Its hits are the bow's arrows' (a crit as their Crit Chance rolls: a thrown bone is a full draw,
 * UNKNOWN). One of theirs is in the air at a time (UNKNOWN), and while it's shattered the bone stays in
 * their hand rather than turning into a Ghast Tear (APPROX). How fast it flies is UNKNOWN (1 block a tick).
 */
final class Bonemerang implements AbilityHandler {
    static final double APEX = 13.5;
    static final int MOST_STRUCK = 10;
    static final long SHATTERED_MILLIS = 3_000;
    private static final double SPEED = 1;
    /** Back in their hand this close. */
    private static final double CAUGHT = 1.5;

    /** Each player's last bone thrown (in the air until it isn't). */
    private static final Map<UUID, Missile> THROWN = new HashMap<>();

    static void forget(UUID player) {
        THROWN.remove(player);
    }

    @Override
    public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
        Missile thrown = THROWN.get(player.getUniqueId());
        return (thrown == null || !thrown.flying()) && PlayerSession.of(player).cooldownLeft("bonemerang") <= 0;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Location eye = player.getEyeLocation();
        boolean[] back = {false};
        int[] struck = {0};
        // Its hits are the throw's, whatever they hold when it comes by.
        Damage.Attacker thrown = Hits.striker(player, tag, Hits.Strike.arrow(0, 1));
        Missile bone = new Missile(player, eye, eye.getDirection().multiply(SPEED))
                .range(APEX * 2 + 8)
                .width(0.4)
                .look(Missile.display(eye, player.getInventory().getItemInMainHand().clone(), 1, 90))
                .steer(missile -> {
                    if (!back[0] && missile.travelled() >= APEX) {
                        back[0] = true;
                        missile.forget();
                    }
                    if (!back[0]) return null;
                    Vector home = missile.caster().getEyeLocation().toVector().subtract(missile.at().toVector());
                    if (home.lengthSquared() <= CAUGHT * CAUGHT) missile.cancel();
                    return home;
                })
                .onHit((missile, mob) -> {
                    if (++struck[0] > MOST_STRUCK) {
                        shatter(missile.caster());
                        return false;
                    }
                    double travelled = mob.getBoundingBox().getCenter().distance(missile.caster().getEyeLocation().toVector());
                    Hits.weaponHit(missile.caster(), thrown, mob, Hits.Strike.arrow(travelled, back[0] ? 2 : 1));
                    return true;
                })
                .onEnd((missile, at, impact) -> {
                    if (impact) shatter(missile.caster());
                })
                .launch();
        THROWN.put(player.getUniqueId(), bone);
        player.getWorld().playSound(eye, Sound.ENTITY_SNOWBALL_THROW, 1, 0.5f);
    }

    /** Shattered: unusable for 3 seconds (UNKNOWN how it sounds: glass breaking). */
    private static void shatter(Player player) {
        PlayerSession.of(player).startCooldown("bonemerang", SHATTERED_MILLIS);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1.5f);
    }
}
