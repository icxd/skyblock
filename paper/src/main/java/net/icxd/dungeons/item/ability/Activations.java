package net.icxd.dungeons.item.ability;

import net.icxd.dungeons.combat.Shots;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.modifier.PowerScroll;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.utils.Replacement;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abilities used some other way than a click (PlayerListener has the clicks, and DIG's left click on a block):
 * SNEAK ones, on starting to sneak, with the item held or worn (armor and equipment, as set bonuses count them:
 * the Aurora Armor's Homing Missiles is on every piece, and used once however many are worn); ON_SHOOT ones,
 * when a drawn bow with one shoots (after its arrow's shot is recorded, see {@link Shots#addShotListener}). And
 * every ability use's costs ({@link #use}), a click's too. Main thread.
 */
public final class Activations implements Listener {
    public Activations() {
        Shots.addShotListener(Activations::shot);
    }

    /**
     * Cooldown, then mana, then Vitality, then health, then whether it can happen at all (see {@link
     * AbilityHandler#usable}): a cast that fails for any of them doesn't start the cooldown (cooldowns are per
     * ability, and what shortens them counts: {@link Abilities#cooldownMillis}) or take anything. Its mana cost
     * is what it says, and its share of their max mana, less the item's Mana Disintegrators' and what makes
     * their abilities cheaper (see {@link Abilities#manaCost(ItemBlock, int, Player, NBTTagCompound)}); its
     * Vitality cost what it says; its health cost its handler's, less what lowers it ({@link
     * Abilities#healthCost}), which can't take the last of their health ("This ability cannot be used if the
     * user does not have enough health to be consumed", the wiki's Flower of Truth: nothing is said then). Too
     * little Vitality is recorded once: Wither Impact still casts, without the Wither Shield its 50 Vitality
     * pays for (0.26.1's release notes, and its June 10 alpha), so a handler can say its Vitality part is
     * optional ({@link AbilityHandler#vitalityOptional}) and it casts without it, spending none. For the rest
     * "Vitality is now a resource akin to Mana" (the June 10 changelog), so it's what too little mana does. Once
     * it's used, the item's Power Scroll does what it does (see {@link PowerScroll#used}). Returns whether it was
     * used.
     */
    public static boolean use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock ability, AbilityHandler.Trigger trigger) {
        PlayerSession session = PlayerSession.of(player);
        String cooldown = "ability:" + ability.name();
        long left = session.cooldownLeft(cooldown);
        if (left > 0) {
            player.sendMessage("§cThis ability is on cooldown for " + Abilities.cooldownSeconds(left) + "s.");
            return false;
        }

        int mana = Math.max(0, session.getMana());
        int cost = Abilities.manaCost(ability, session.maxMana(), player, tag);
        if (mana < cost) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, -4f);
            session.setManaReplacement(Replacement.forMillis("§c§lNOT ENOUGH MANA", 2000));
            return false;
        }
        AbilityHandler handler = Abilities.handler(ability);
        boolean vitalityPaid = Vitality.has(player, ability.vitality());
        if (!vitalityPaid && !handler.vitalityOptional()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, -4f);
            session.setManaReplacement(Replacement.forMillis("§c§lNOT ENOUGH VITALITY", 2000));
            return false;
        }
        double health = Abilities.healthCost(handler, player, item, tag, ability);
        if (!Abilities.canPayHealth(PlayerHealth.get(player), health)) return false;
        if (!handler.usable(player, item, tag, ability, trigger)) return false;

        long millis = Abilities.cooldownMillis(ability, player);
        if (millis > 0) session.startCooldown(cooldown, millis);
        Mana.spend(player, cost, ability.name());
        if (vitalityPaid) Vitality.spend(player, ability.vitality());
        if (health > 0) PlayerHealth.damage(player, health);
        handler.use(player, item, tag, ability, vitalityPaid, trigger);
        PowerScroll.used(player, tag, ability);

        if (cost > 0) {
            session.setDefenseReplacement(Replacement.forMillis(
                    "§b-" + cost + " Mana (§6" + ability.name() + "§b)", 400));
        }
        return true;
    }

    /**
     * They started sneaking: the SNEAK abilities of what they hold, then of what they wear (helmet first, then
     * the equipment), each one once. What's only worn doesn't use them from the hand (see {@link #fromHand}).
     * Not the dead's or dungeon ghosts' (who are invulnerable), as their worn bonuses do nothing either.
     */
    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking() || player.isDead() || player.isInvulnerable()) return;
        Set<String> used = new HashSet<>();
        ItemStack held = player.getInventory().getItemInMainHand();
        NBTTagCompound heldTag = held.isEmpty() ? null : ItemNBT.read(held);
        SkyBlockItem heldItem = heldTag == null ? null : ItemRegistry.get(heldTag.getString("id"));
        if (fromHand(heldItem)) sneak(player, heldItem, heldTag, ItemBehaviours.of(heldItem).blocks(heldItem, heldTag, heldItem.blocks()), used);
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) sneak(player, piece.item(), piece.tag(), piece.blocks(), used);
    }

    /**
     * Whether a held item's SNEAK abilities are used from the hand: not armor's, equipment's or an accessory's
     * (every SNEAK ability in the data is on armor: the Aurora Armor's Homing Missiles needs the piece on, not in
     * the hand), as their stats don't count there either ({@link SkyBlockItem#statsWhenHeld}).
     */
    static boolean fromHand(SkyBlockItem held) {
        return held != null && held.statsWhenHeld();
    }

    private static void sneak(Player player, SkyBlockItem item, NBTTagCompound tag, List<ItemBlock> blocks, Set<String> used) {
        for (ItemBlock block : Abilities.withActivation(blocks, AbilityActivation.SNEAK, name -> handles(name, player, item, tag))) {
            if (used.add(block.name())) use(player, item, tag, block, AbilityHandler.Trigger.of(AbilityActivation.SNEAK, null, null));
        }
    }

    /** A drawn bow shot: its ON_SHOOT abilities, with the arrow (already recorded, so they can change its damage). */
    private static void shot(Player player, Projectile projectile, NBTTagCompound bow, boolean fullyDrawn) {
        SkyBlockItem item = bow == null ? null : ItemRegistry.get(bow.getString("id"));
        if (item == null) return;
        List<ItemBlock> blocks = ItemBehaviours.of(item).blocks(item, bow, item.blocks());
        for (ItemBlock block : Abilities.withActivation(blocks, AbilityActivation.ON_SHOOT, name -> handles(name, player, item, bow))) {
            use(player, item, bow, block, AbilityHandler.Trigger.of(AbilityActivation.ON_SHOOT, projectile, null));
        }
    }

    /** Whether something does what the ability with this name says, and it's that ability's use just now. */
    static boolean handles(String name, Player player, SkyBlockItem item, NBTTagCompound tag) {
        AbilityHandler handler = Abilities.get(name);
        return handler != null && handler.casts(player, item, tag);
    }
}
