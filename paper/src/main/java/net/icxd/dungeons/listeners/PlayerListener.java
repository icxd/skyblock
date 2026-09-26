package net.icxd.dungeons.listeners;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.entity.CustomEntity;
import net.icxd.dungeons.entity.EntityBuilder;
import net.icxd.dungeons.entity.EntityRegistry;
import net.icxd.dungeons.entity.enums.EntityDropType;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.user.UserStore;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

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
        event.setJoinMessage(null);
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
        player.sendMessage(Utils.color("&aSuccessfully loaded player data. &8(took " + user.getLoadMillis() + "ms)"));
    }

    /** Last, so every other quit handler still has the player's data. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        UUID id = event.getPlayer().getUniqueId();
        PlayerSession.end(id);
        User user = User.cached(id);
        if (user == null) return;
        // Vanilla drops the cursor and crafting grid after this event; they're saved with the rest instead.
        Dungeons.getUserStore().rescueLooseItems(event.getPlayer(), user);
        Dungeons.getUserStore().leave(user);
    }

    /** Off the main thread. The player's own text is sent as typed: colour codes are for ranks. */
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        User user = User.cached(player.getUniqueId());
        event.setCancelled(true);
        if (user == null || !user.isLoaded()) return;
        Rank rank = user.getRank();
        String line = Utils.color(rank.getPrefix() + player.getName() + (rank == Rank.DEFAULT ? "&7" : "&f") + ": ") + event.getMessage();
        event.getRecipients().forEach(recipient -> recipient.sendMessage(line));
    }

    /** SkyBlock items are rebuilt as they're picked up (for their owner); what doesn't fit stays on the ground. */
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
        ItemStack builtItem = ItemBuilder.build(sbItem, tag, item.getAmount());
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

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        User user = User.cached(player.getUniqueId());
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (user == null || item == null || item.getType() == Material.AIR) return;
        ItemNBT craftItem = ItemNBT.of(item);
        if (!craftItem.hasTag()) return;
        NBTTagCompound tag = craftItem.getTag();
        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        if (sbItem == null) return;
        if (sbItem.isOwnable()) tag.setString("owner", user.getUuid().toString());
        ItemStack builtItem = ItemBuilder.build(sbItem, tag, item.getAmount());
        player.getInventory().setItem(event.getNewSlot(), builtItem);
    }

    @EventHandler
    public void onAbilityUse(PlayerInteractEvent event) {
        // Fired once per hand since 1.9; the ability is on the main hand item.
        if (event.getHand() != EquipmentSlot.HAND) return;
        // Denied (e.g. while the player's data is being handed to another server).
        if (event.useItemInHand() == Event.Result.DENY) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        ItemNBT craftItem = ItemNBT.of(item);
        if (!craftItem.hasTag()) return;
        SkyBlockItem sbItem = ItemRegistry.get(craftItem.getTag().getString("id"));
        if (sbItem == null) return;
        Ability ability = sbItem.ability();
        if (ability == null) return;
        boolean right = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean left = event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK;
        switch (ability.getActivation()) {
            case RIGHT_CLICK -> { if (right) useAbility(player, sbItem, ability); }
            case LEFT_CLICK -> { if (left) useAbility(player, sbItem, ability); }
            default -> { }
        }
    }

    /** Mana first: a cast that fails for lack of it doesn't start the cooldown (cooldowns are per ability). */
    private void useAbility(Player player, SkyBlockItem sbItem, Ability ability) {
        PlayerSession session = PlayerSession.of(player);
        String cooldown = "ability:" + ability.getName();
        if (session.cooldownLeft(cooldown) > 0) {
            player.sendMessage(ChatColor.RED + "You currently have a cooldown for this ability!");
            return;
        }

        int mana = Math.max(0, session.getMana());
        int cost = ability.getManaCost();
        if (mana < cost) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, -4f);
            session.setManaReplacement(Replacement.forMillis("" + ChatColor.RED + ChatColor.BOLD + "NOT ENOUGH MANA", 2000));
            return;
        }

        if (ability.getCooldown() > 0) session.startCooldown(cooldown, ability.getCooldown() * 1000L);
        session.setMana(mana - cost);
        ability.activate(player, sbItem);

        if (ability.isShowManaCost()) {
            session.setDefenseReplacement(Replacement.forMillis(
                    ChatColor.AQUA + "-" + cost + " Mana (" + ChatColor.GOLD + ability.getName() + ChatColor.AQUA + ")", 400));
        }
    }

    /**
     * A player's melee hit with a SkyBlock item: (5 + damage) x (1 + strength / 100), crits, then
     * One For All. Their stats already include armor and the held item (see {@link PlayerSession#stats}). Arrows
     * count as the shooter's hit with what they're holding.
     * Cancelled hits (sweeps, see CombatListener) don't count.
     */
    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        // Their own hit, or an arrow from their SkyBlock bow.
        Player player = event.getDamager() instanceof Player p ? p
                : event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p ? p : null;
        if (!(event.getEntity() instanceof LivingEntity target) || player == null) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        ItemNBT craftItem = ItemNBT.of(item);
        if (!craftItem.hasTag()) return;
        NBTTagCompound tag = craftItem.getTag();
        SkyBlockItem sbItem = ItemRegistry.get(tag.getString("id"));
        if (sbItem == null) return;

        Stats stats = PlayerSession.of(player).stats();
        double damageMultiplier = 1;
        var enchantments = tag.getList("enchantments", 10);
        for (int i = 0; i < enchantments.size(); i++) {
            if (enchantments.get(i).getString("name").equalsIgnoreCase("one_for_all")) damageMultiplier = 5;
        }
        double finalDamage = (5 + stats.get(Stat.DAMAGE)) * (1 + stats.get(Stat.STRENGTH) / 100) * damageMultiplier;
        boolean criticalHit = Math.random() * 100 < stats.get(Stat.CRIT_CHANCE);
        if (criticalHit) finalDamage *= 1 + stats.get(Stat.CRIT_DAMAGE) / 100;

        DungeonMobs.Mob dungeonMob = DungeonMobs.of(target);
        if (dungeonMob != null) {
            DungeonMobs.playerHit(event, player, dungeonMob, finalDamage, criticalHit);
            return;
        }
        CustomEntity customEntity = EntityRegistry.get(target);
        if (customEntity == null) {
            event.setCancelled(true);
            return;
        }

        if (target.getHealth() - finalDamage <= 0) {
            event.setCancelled(true);
            customEntity.onDeath(target);
            if (!customEntity.isBoss()) drop(player, target, customEntity, stats.get(Stat.MAGIC_FIND));
            EntityBuilder.forget(target);
            target.remove();
            return;
        }
        customEntity.onDamaged(target, finalDamage);
        event.setDamage(finalDamage);
        // The name tag catches up on the next tick (EntityRunnable).
        DungeonMobs.showDamage(target, finalDamage, criticalHit);
    }

    /** Each drop rolls on its own (magic find raises the chance); only what drops is announced. */
    private static void drop(Player player, LivingEntity target, CustomEntity customEntity, double magicFind) {
        if (customEntity.getDrops() == null) return;
        customEntity.getDrops().forEach(drop -> {
            double chance = drop.getChance() / 100 * (1 + magicFind / 100);
            if (Math.random() >= chance) return;
            ItemStack stack = ItemBuilder.build(drop.getItem());
            stack.setAmount(Utils.random(drop.getMinAmount(), drop.getMaxAmount()));
            target.getWorld().dropItemNaturally(target.getLocation(), stack);
            String kind = drop.getType() == EntityDropType.RNGESUS_INCARNATE ? "INSANE DROP! "
                    : (drop.getType() == EntityDropType.CRAZY_RARE ? "CRAZY " : "") + "RARE DROP! ";
            player.sendMessage(drop.getType().getColor() + "" + ChatColor.BOLD + kind + drop.getItem().rarity().getColor() + drop.getItem().name()
                    + " " + ChatColor.AQUA + "(+" + Utils.round(magicFind, 0) + "% ✯ Magic Find)");
        });
    }
}
