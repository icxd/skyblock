package net.icxd.dungeons.item.ability.utility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerSession;

/**
 * Mobs around go for them: the Enrager's Enrage ("Taunt enemies in a 10 block radius and reduce their damage
 * against you by 10% for 10s", those mobs' hits on them less by the text's share while it lasts: see {@link
 * Protection}) and Jingle Bells' ("Angers all monsters in a 10 block range, consuming 50% of your max mana and
 * causing them to run towards you": the item's data has no mana cost, so the ability takes half their max mana
 * itself, down to none if they have less: UNKNOWN whether it then works). The mobs are SkyBlock's (the Blood
 * Room's undead choose for themselves, as for a Tank's Castle of Stone). The sounds are UNKNOWN: plain ones.
 */
final class Taunt implements AbilityHandler {
    private static final double RADIUS = 10;
    /** Its sound's key ("entity.bat.takeoff"), not a {@link org.bukkit.Sound}: those need a server to exist. */
    private final String sound;

    Taunt(String sound) {
        this.sound = sound;
    }

    /** "reduce their damage against you by 10%": the factor on their hits (0.9); 1 if it doesn't say. */
    static double factor(String plain) {
        return 1 - AbilityText.after(plain, "damage against you by").orElse(0) / 100;
    }

    /** "consuming 50% of your max mana": the share it takes (0.5); 0 if it doesn't say. */
    static double manaShare(String plain) {
        return plain.contains("% of your max mana") ? AbilityText.after(plain, "consuming").orElse(0) / 100 : 0;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        double radius = AbilityText.blocks(plain).orElse(RADIUS);
        double factor = factor(plain);
        long millis = (long) AbilityText.millis(plain).orElse(0);
        double manaShare = manaShare(plain);
        if (manaShare > 0) {
            PlayerSession session = PlayerSession.of(player);
            int mana = session.getMana() < 0 ? session.maxMana() : session.getMana();
            session.setMana((int) Math.max(0, mana - Math.round(manaShare * session.maxMana())));
        }
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Mob mob) || entity.getLocation().distanceSquared(player.getLocation()) > radius * radius) continue;
            if (Mobs.of(mob) == null && DungeonMobs.of(mob) == null) continue;
            mob.setTarget(player);
            if (factor < 1 && millis > 0) Protection.taunted(player, mob, factor, millis);
        }
        player.getWorld().playSound(player.getLocation(), sound, 1, 1);
    }
}
