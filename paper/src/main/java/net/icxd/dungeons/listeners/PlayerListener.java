package net.icxd.dungeons.listeners;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.profile.ProfileActions;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.modifier.PowerScroll;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.user.UserStore;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    /** Hypixel says which profile you're on about 3 seconds after you join. */
    private static final long PROFILE_MESSAGE_DELAY = 3 * 20;

    /** Loads the player's data before they join, waiting for another server to let go of it (see UserStore). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        try {
            Dungeons.getUserStore().claim(event.getUniqueId(), event.getName(), event.getAddress().getHostAddress());
        } catch (UserStore.HeldElsewhereException e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Your profile is still being saved on another server. Try again in a moment.", NamedTextColor.RED));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Couldn't load your profile.", NamedTextColor.RED));
        } catch (RuntimeException e) {
            Dungeons.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Couldn't load " + event.getName() + "'s data", e);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Couldn't load your profile.", NamedTextColor.RED));
        }
    }

    /** Another plugin refused the login after the data was claimed: let it go again. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLoginRefused(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            User user = User.cached(event.getUniqueId());
            if (user != null && user.getPlayer() == null) Dungeons.getUserStore().leave(user);
        });
    }

    /** First, so everything after it sees the player's stored inventory rather than this server's player file. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        Player player = event.getPlayer();

        User user = User.cached(player.getUniqueId());
        if (user == null || !user.isLoaded()) {
            player.kick(Component.text("Couldn't load your profile, please rejoin.", NamedTextColor.RED));
            return;
        }
        try {
            Dungeons.getUserStore().restoreInventory(player, user);
        } catch (StoredInventory.NewerDataException e) {
            Dungeons.getInstance().getLogger().severe("Not restoring " + player.getName() + "'s inventory: " + e.getMessage());
            player.kick(Component.text("Your items were saved by a newer version of Minecraft than this server runs.", NamedTextColor.RED));
            return;
        } catch (RuntimeException e) {
            Dungeons.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Couldn't restore " + player.getName() + "'s inventory", e);
            player.kick(Component.text("Couldn't load your inventory, please rejoin.", NamedTextColor.RED));
            return;
        }
        // Items made before an update to how items look (or are stored) are brought up to date.
        ItemBuilder.refreshInventory(player);
        if (user.getRank().isEqualOrStrongerThan(Rank.STAFF)) player.sendMessage(Utils.color("&aSuccessfully loaded player data. &8(took " + user.getLoadMillis() + "ms)"));
        // As Hypixel, a moment after joining any server.
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (player.isOnline() && User.ifLoaded(player.getUniqueId()) == user) ProfileActions.announce(player, user);
        }, PROFILE_MESSAGE_DELAY);
    }

    /** Last, so every other quit handler still has the player's data. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent event) {
        event.quitMessage(null);
        UUID id = event.getPlayer().getUniqueId();
        PlayerSession.end(id);
        User user = User.cached(id);
        if (user == null) return;
        // Vanilla drops the cursor and crafting grid after this event; they're saved with the rest instead.
        Dungeons.getUserStore().rescueLooseItems(event.getPlayer(), user);
        Dungeons.getUserStore().leave(user);
    }

    /**
     * "[88] ♦ [MVP+] name: text", in the rank's colours, the SkyBlock level and emblem only for those who
     * see them (SkyBlock Levels in Chat, see {@link SkyBlockLevels}). Off the main thread. The player's own
     * text is shown as typed: colour codes are for ranks.
     */
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        User user = User.cached(player.getUniqueId());
        if (user == null || !user.isLoaded()) {
            event.setCancelled(true);
            return;
        }
        Rank rank = user.getRank();
        Component name = Text.line(rank.getPrefix() + player.getName() + (rank == Rank.DEFAULT ? "&7" : "&f") + ": ");
        Component level = Text.line(SkyBlockLevels.chatPrefix(player.getUniqueId()));
        NamedTextColor text = rank == Rank.DEFAULT ? NamedTextColor.GRAY : NamedTextColor.WHITE;
        event.renderer((source, displayName, message, viewer) -> (viewer instanceof Player p && !SkyBlockLevels.levelsInChat(p.getUniqueId())
                ? name : level.append(name)).append(message.colorIfAbsent(text)));
    }

    /**
     * SkyBlock items are rebuilt as they're picked up (for their owner, and with the set bonus counts of what
     * they wear); what doesn't fit stays on the ground.
     */
    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        User user = User.cached(player.getUniqueId());
        ItemStack item = event.getItem().getItemStack();
        if (user == null || item.getType() == Material.AIR) return;
        ItemNBT craftItem = ItemNBT.of(item);
        if (!craftItem.hasTag()) return;
        NBTTagCompound tag = craftItem.getTag();
        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        if (sbItem == null) return;
        if (sbItem.isOwnable()) tag.setString("owner", user.getUuid().toString());
        ItemStack builtItem = ItemBuilder.build(sbItem, tag, item.getAmount(), player);
        event.setCancelled(true);
        Map<Integer, ItemStack> left = player.getInventory().addItem(builtItem);
        int leftOver = left.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (leftOver == 0) {
            event.getItem().remove();
        } else {
            item.setAmount(leftOver);
            event.getItem().setItemStack(item);
        }
    }

    /**
     * What they switch to is rebuilt from its data (with the set bonus counts of what they wear), and their
     * speed and reach follow its stats at once, not on the next second (once the switch has happened, so
     * their stats have it).
     */
    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (player.isOnline()) PlayerAttributes.apply(player, PlayerSession.of(player).stats());
        });
        User user = User.cached(player.getUniqueId());
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (user == null || item == null || item.getType() == Material.AIR) return;
        ItemNBT craftItem = ItemNBT.of(item);
        if (!craftItem.hasTag()) return;
        NBTTagCompound tag = craftItem.getTag();
        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        if (sbItem == null) return;
        if (sbItem.isOwnable()) tag.setString("owner", user.getUuid().toString());
        ItemStack builtItem = ItemBuilder.build(sbItem, tag, item.getAmount(), player);
        player.getInventory().setItem(event.getNewSlot(), builtItem);
    }

    /**
     * A click with a SkyBlock item uses the ability its blocks (with what its behaviour adds) have for that
     * click, if something does what it says (see {@link Abilities#forClick}); else a shortbow shoots, except
     * on a right click that uses the block they clicked (a chest, a door): that one is the block's.
     */
    @EventHandler
    public void onAbilityUse(PlayerInteractEvent event) {
        // Fired once per hand since 1.9; the ability is on the main hand item.
        if (event.getHand() != EquipmentSlot.HAND) return;
        // Denied (e.g. while the player's data is being handed to another server).
        if (event.useItemInHand() == Event.Result.DENY) return;
        boolean right = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean left = event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        NBTTagCompound tag = ItemNBT.read(item);
        if (tag == null) return;
        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        if (sbItem == null) return;
        List<ItemBlock> blocks = ItemBehaviours.of(sbItem).blocks(sbItem, tag, sbItem.blocks());
        ItemBlock block = Abilities.forClick(blocks, right, player.isSneaking(), name -> {
            AbilityHandler handler = Abilities.get(name);
            return handler != null && handler.casts(player, sbItem, tag);
        });
        if (block == null) return;
        if (block.isShortbow()) {
            if (right && usesBlock(event)) return;
            // It has shot, so no drawing it too.
            if (right) event.setUseItemInHand(Event.Result.DENY);
            shoot(player, sbItem, tag, block);
        } else {
            useAbility(player, sbItem, tag, block);
        }
    }

    /** Whether the click uses the block itself, as a right click on a chest or lever does unless they're sneaking. */
    private static boolean usesBlock(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        return event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()
                && clicked != null && clicked.getType().isInteractable();
    }

    /**
     * Cooldown, then mana, then Vitality, then whether it can happen at all (see
     * {@link AbilityHandler#usable}): a cast that fails for any of them doesn't start the cooldown
     * (cooldowns are per ability) or take anything. Its mana cost is what it says, and its share of their
     * max mana, less the item's Mana Disintegrators' and what makes their abilities cheaper (see {@link
     * Abilities#addManaCostFactor}); its Vitality cost what it says. Too little Vitality is recorded once:
     * Wither Impact still casts, without the Wither Shield its 50 Vitality pays for (0.26.1's release notes,
     * and its June 10 alpha), so a handler can say its Vitality part is optional ({@link
     * AbilityHandler#vitalityOptional}) and it casts without it, spending none. For the rest "Vitality is now
     * a resource akin to Mana" (the June 10 changelog), so it's what too little mana does. Once it's used, the
     * item's Power Scroll does what it does (see {@link PowerScroll#used}).
     */
    private void useAbility(Player player, SkyBlockItem sbItem, NBTTagCompound tag, ItemBlock ability) {
        PlayerSession session = PlayerSession.of(player);
        String cooldown = "ability:" + ability.name();
        long left = session.cooldownLeft(cooldown);
        if (left > 0) {
            player.sendMessage("§cThis ability is on cooldown for " + Abilities.cooldownSeconds(left) + "s.");
            return;
        }

        int mana = Math.max(0, session.getMana());
        int cost = Abilities.manaCost(ability, session.maxMana(), player, tag);
        if (mana < cost) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, -4f);
            session.setManaReplacement(Replacement.forMillis("§c§lNOT ENOUGH MANA", 2000));
            return;
        }
        AbilityHandler handler = Abilities.handler(ability);
        boolean vitalityPaid = Vitality.has(player, ability.vitality());
        if (!vitalityPaid && !handler.vitalityOptional()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, -4f);
            session.setManaReplacement(Replacement.forMillis("§c§lNOT ENOUGH VITALITY", 2000));
            return;
        }
        if (!handler.usable(player, sbItem, tag, ability)) return;

        if (ability.cooldown() > 0) session.startCooldown(cooldown, (long) (ability.cooldown() * 1000));
        Mana.spend(player, cost, ability.name());
        if (vitalityPaid) Vitality.spend(player, ability.vitality());
        handler.use(player, sbItem, tag, ability, vitalityPaid);
        PowerScroll.used(player, tag, ability);

        if (cost > 0) {
            session.setDefenseReplacement(Replacement.forMillis(
                    "§b-" + cost + " Mana (§6" + ability.name() + "§b)", 400));
        }
    }

    /**
     * A shortbow's shot, at most one per its shot cooldown (whichever shortbow they shot last), which their
     * Attack Speed shortens.
     */
    private void shoot(Player player, SkyBlockItem sbItem, NBTTagCompound tag, ItemBlock shortbow) {
        PlayerSession session = PlayerSession.of(player);
        if (session.cooldownLeft("shortbow") > 0) return;
        if (sbItem.shotCooldown() > 0) {
            int ticks = Damage.shotCooldownTicks(sbItem.shotCooldown(), session.stats().get(Stat.ATTACK_SPEED), Combat.attackSpeedCap(player));
            session.startCooldown("shortbow", ticks * 50L);
        }
        Abilities.handler(shortbow).use(player, sbItem, tag, shortbow);
    }

    /**
     * A player's hit, or their arrow's: on SkyBlock's mobs it does SkyBlock damage, worked out by
     * {@link Combat} (with whatever they hold, fists too; an arrow with the bow it left). Cancelled hits
     * (sweeps, see CombatListener) don't count.
     */
    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Combat.playerHit(event);
    }
}
