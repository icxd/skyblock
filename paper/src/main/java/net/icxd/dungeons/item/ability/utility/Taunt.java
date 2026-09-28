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
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;

/**
 * Mobs around go for them: the Enrager's Enrage ("Taunt enemies in a 10 block radius and reduce their damage
 * against you by 10% for 10s", those mobs' hits on them less by the text's share while it lasts: see {@link
 * Protection}) and Jingle Bells' ("Angers all monsters in a 10 block range, consuming 50% of your max mana and
 * causing them to run towards you": the item's data has no mana cost, so the ability takes half their max mana
 * itself, down to none if they have less: UNKNOWN whether it then works). The mobs are SkyBlock's (the Blood
 * Room's undead choose for themselves, as for a Tank's Castle of Stone). The sounds are UNKNOWN: plain ones.
 * Enrage costs a tenth of their max health too (see {@link #healthCost}).
 */
final class Taunt implements AbilityHandler {
    private static final double RADIUS = 10;
    /**
     * Enrage's health cost: "item_ability_health_cost = 10% of HP" (the wiki's Enrager). Its data's is 15.6,
     * which isn't health: the Wand of Strength's is 15.6 too, and its text says "Use 10% of your max health".
     */
    static final double HEALTH_SHARE = 0.1;
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

    /** A tenth of their max health for one whose data has a health cost at all (Enrage; Jingle Bells has none). */
    @Override
    public double healthCost(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return block.healthCost() > 0 ? HEALTH_SHARE * PlayerHealth.max(player) : 0;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        String plain = AbilityText.plain(block.text());
        double radius = AbilityText.blocks(plain).orElse(RADIUS);
        double factor = factor(plain);
        long millis = (long) AbilityText.millis(plain).orElse(0);
        double manaShare = manaShare(plain);
        if (manaShare > 0) Mana.spend(player, (int) Math.round(manaShare * PlayerSession.of(player).maxMana()), block.name());
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Mob mob) || entity.getLocation().distanceSquared(player.getLocation()) > radius * radius) continue;
            if (Mobs.of(mob) == null && DungeonMobs.of(mob) == null) continue;
            mob.setTarget(player);
            if (factor < 1 && millis > 0) Protection.taunted(player, mob, factor, millis);
        }
        player.getWorld().playSound(player.getLocation(), sound, 1, 1);
    }
}
