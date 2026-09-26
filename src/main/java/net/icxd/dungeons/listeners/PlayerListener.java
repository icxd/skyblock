package net.icxd.dungeons.listeners;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.entity.CustomEntity;
import net.icxd.dungeons.entity.EntityBuilder;
import net.icxd.dungeons.entity.EntityRegistry;
import net.icxd.dungeons.entity.enums.EntityDropType;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.enums.DungeonStar;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.user.Rank;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.user.UserStore;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.*;
import net.icxd.dungeons.item.nbt.ItemNBT;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();

        User user = User.cached(player.getUniqueId());
        if (user == null || !user.isLoaded()) {
            player.kick(Component.text("Couldn't load your profile, please rejoin.", NamedTextColor.RED));
            return;
        }
        // Here, before anything else can see the inventory this server's own player file had.
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
        player.sendMessage(Utils.color("&aSuccessfully loaded player data. &8(took " + user.getLoadMillis() + "ms)"));

//        player.teleport(new Location(Bukkit.getWorld("world"), 0, 100, 0));
//        World world = Bukkit.getWorld(Dungeons.getSkyBlockServer().getServerType().getWorldName());
//        player.teleport(world.getSpawnLocation());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        User user = User.cached(event.getPlayer().getUniqueId());
        if (user == null) return;
        // Vanilla drops the cursor and crafting grid after this event; they're saved with the rest instead.
        Dungeons.getUserStore().rescueLooseItems(event.getPlayer(), user);
        Dungeons.getUserStore().leave(user);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        User user = User.getUser(player.getUniqueId());
        Rank rank = Rank.valueOf(user.getDocument().getString("rank"));

        event.setCancelled(true);

        event.getRecipients().forEach(recipient -> recipient.sendMessage(Utils.color(rank.getPrefix() + player.getName() + (rank == Rank.DEFAULT ? "&7" : "&f") + ": " + event.getMessage())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        User user = User.getUser(player.getUniqueId());
        ItemStack item = event.getItem().getItemStack();
        if (item == null) return;
        if (item.getType() == Material.AIR) return;
        ItemNBT craftItem = ItemNBT.of(item);
        if (!craftItem.hasTag()) return;
        NBTTagCompound tag = craftItem.getTag();
        String id = tag.getString("id");
        SkyBlockItem sbItem = ItemRegistry.get(id);
        if (sbItem.isOwnable())
            tag.setString("owner", user.getUuid().toString());
        ItemStack builtItem = ItemBuilder.build(sbItem, tag, item.getAmount());
        player.getInventory().addItem(builtItem);
        event.setCancelled(true);
        event.getItem().remove();
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        User user = User.getUser(player.getUniqueId());
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null) return;
        if (item.getType() == Material.AIR) return;
        ItemNBT craftItem = ItemNBT.of(item);
        if (!craftItem.hasTag()) return;
        NBTTagCompound tag = craftItem.getTag();
        String id = tag.getString("id");
        SkyBlockItem sbItem = ItemRegistry.get(id);
        if (sbItem.isOwnable())
            tag.setString("owner", user.getUuid().toString());
        ItemStack builtItem = ItemBuilder.build(sbItem, tag, item.getAmount());
        player.getInventory().setItem(event.getNewSlot(), builtItem);
    }

    private final HashMap<UUID, Long> lastAbilityUse = new HashMap<>();

    @EventHandler
    public void onAbilityUse(PlayerInteractEvent event) {
        // Fired once per hand since 1.9; the ability is on the main hand item.
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        User user = User.getUser(player.getUniqueId());
        ItemStack item = player.getItemInHand();
        if (item == null) return;
        if (item.getType() == Material.AIR) return;
        ItemNBT craftItem = ItemNBT.of(item);
        if (!craftItem.hasTag()) return;
        NBTTagCompound tag = craftItem.getTag();
        tag.setString("owner", user.getUuid().toString());
        String id = tag.getString("id");
        SkyBlockItem sbItem = ItemRegistry.get(id);
        if (sbItem == null) return;
        Ability ability = sbItem.ability();
        if (ability == null) return;
        switch (ability.getActivation()) {
            case RIGHT_CLICK -> {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (lastAbilityUse.containsKey(player.getUniqueId())) {
                        long cooldown = ability.getCooldown() * 1000L;
                        if (System.currentTimeMillis() - lastAbilityUse.get(player.getUniqueId()) < cooldown) {
                            player.sendMessage(ChatColor.RED + "You currently have a cooldown for this ability!");
                            return;
                        }
                    }
                    if (ability.getCooldown() > 0) lastAbilityUse.put(player.getUniqueId(), System.currentTimeMillis());

                    int mana = StatsRunnable.MANA_MAP.get(player.getUniqueId());
                    int cost = ability.getManaCost();
                    int resMana = mana - cost;
                    if (resMana < 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, -4f);

                        long c = System.currentTimeMillis();
                        StatsRunnable.MANA_REPLACEMENT_MAP.put(player.getUniqueId(), new Replacement() {
                            @Override
                            public String getReplacement() {
                                return "" + ChatColor.RED + ChatColor.BOLD + "NOT ENOUGH MANA";
                            }

                            @Override
                            public long getEnd() {
                                return c + 2000;
                            }
                        });
                        return;
                    }

                    StatsRunnable.MANA_MAP.put(player.getUniqueId(), resMana);
                    ability.activate(player, sbItem);

                    if (ability.isShowManaCost()) {
                        long cms = System.currentTimeMillis();
                        StatsRunnable.DEFENSE_REPLACEMENT_MAP.put(player.getUniqueId(), new Replacement() {
                            @Override
                            public String getReplacement() {
                                return ChatColor.AQUA + "-" + cost + " Mana (" + ChatColor.GOLD + ability.getName() + ChatColor.AQUA + ")";
                            }

                            @Override
                            public long getEnd() {
                                return cms + 400;
                            }
                        });
                    }
                }
            }
            case LEFT_CLICK -> {
                if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (lastAbilityUse.containsKey(player.getUniqueId())) {
                        long cooldown = ability.getCooldown() * 1000L;
                        if (System.currentTimeMillis() - lastAbilityUse.get(player.getUniqueId()) < cooldown) {
                            player.sendMessage(ChatColor.RED + "You currently have a cooldown for this ability!");
                            return;
                        }
                    }
                    if (ability.getCooldown() > 0) lastAbilityUse.put(player.getUniqueId(), System.currentTimeMillis());

                    int mana = StatsRunnable.MANA_MAP.get(player.getUniqueId());
                    int cost = ability.getManaCost();
                    int resMana = mana - cost;
                    if (resMana < 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, -4f);

                        long c = System.currentTimeMillis();
                        StatsRunnable.MANA_REPLACEMENT_MAP.put(player.getUniqueId(), new Replacement() {
                            @Override
                            public String getReplacement() {
                                return "" + ChatColor.RED + ChatColor.BOLD + "NOT ENOUGH MANA";
                            }

                            @Override
                            public long getEnd() {
                                return c + 2000;
                            }
                        });
                        return;
                    }

                    StatsRunnable.MANA_MAP.put(player.getUniqueId(), resMana);
                    ability.activate(player, sbItem);

                    if (ability.isShowManaCost()) {
                        long cms = System.currentTimeMillis();
                        StatsRunnable.DEFENSE_REPLACEMENT_MAP.put(player.getUniqueId(), new Replacement() {
                            @Override
                            public String getReplacement() {
                                return ChatColor.AQUA + "-" + cost + " Mana (" + ChatColor.GOLD + ability.getName() + ChatColor.AQUA + ")";
                            }

                            @Override
                            public long getEnd() {
                                return cms + 400;
                            }
                        });
                    }
                }
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        if (!(damaged instanceof LivingEntity)) return;
        if (event.getDamager() instanceof Player player) {
            User user = User.getUser(player.getUniqueId());
            ItemStack item = player.getItemInHand();
            if (item == null) return;
            if (item.getType() == Material.AIR) return;
            ItemNBT craftItem = ItemNBT.of(item);
            if (!craftItem.hasTag()) return;
            NBTTagCompound tag = craftItem.getTag();
            tag.setString("owner", user.getUuid().toString());
            String id = tag.getString("id");
            SkyBlockItem sbItem = ItemRegistry.get(id);
            if (sbItem == null) return;

            Stats stats = Stats.STATS_CACHE.get(player.getUniqueId());
            double boost1 = DungeonStar.valueOf(tag.getString("dungeon_star")).getBoost();
            double damage = stats.getDamage();
            double strength = stats.getStrength();
            double critChance = stats.getCriticalChance();
            double critDamage = stats.getCriticalDamage();
            double magicFind = stats.getMagicFind();
            double armor = 0;

            if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() != Material.AIR) {
                ItemStack helmet = player.getInventory().getHelmet();
                ItemNBT craft1 = ItemNBT.of(helmet);
                if (craft1.hasTag()) {
                    NBTTagCompound tag1 = craft1.getTag();
                    String id1 = tag1.getString("id");
                    SkyBlockItem item1 = ItemRegistry.get(id1);
                    double boost = DungeonStar.valueOf(tag.getString("dungeon_star")).getBoost();
                    if (item1 != null) {
                        damage += item1.stats().getDamage();
                        strength += item1.stats().getStrength();
                        critChance += item1.stats().getCriticalChance();
                        critDamage += item1.stats().getCriticalDamage();
                        magicFind += item1.stats().getMagicFind();
                    }
                }
            }
            if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() != Material.AIR) {
                ItemStack chestplate = player.getInventory().getChestplate();
                ItemNBT craft1 = ItemNBT.of(chestplate);
                if (craft1.hasTag()) {
                    NBTTagCompound tag1 = craft1.getTag();
                    String id1 = tag1.getString("id");
                    SkyBlockItem item1 = ItemRegistry.get(id1);
                    double boost = DungeonStar.valueOf(tag.getString("dungeon_star")).getBoost();
                    if (item1 != null) {
                        damage += item1.stats().getDamage();
                        strength += item1.stats().getStrength();
                        critChance += item1.stats().getCriticalChance();
                        critDamage += item1.stats().getCriticalDamage();
                        magicFind += item1.stats().getMagicFind();
                    }
                }
            }
            if (player.getInventory().getLeggings() != null && player.getInventory().getLeggings().getType() != Material.AIR) {
                ItemStack leggings = player.getInventory().getLeggings();
                ItemNBT craft1 = ItemNBT.of(leggings);
                if (craft1.hasTag()) {
                    NBTTagCompound tag1 = craft1.getTag();
                    String id1 = tag1.getString("id");
                    SkyBlockItem item1 = ItemRegistry.get(id1);
                    double boost = DungeonStar.valueOf(tag.getString("dungeon_star")).getBoost();
                    if (item1 != null) {
                        damage += item1.stats().getDamage();
                        strength += item1.stats().getStrength();
                        critChance += item1.stats().getCriticalChance();
                        critDamage += item1.stats().getCriticalDamage();
                        magicFind += item1.stats().getMagicFind();
                    }
                }
            }
            if (player.getInventory().getBoots() != null && player.getInventory().getBoots().getType() != Material.AIR) {
                ItemStack boots = player.getInventory().getBoots();
                ItemNBT craft1 = ItemNBT.of(boots);
                if (craft1.hasTag()) {
                    NBTTagCompound tag1 = craft1.getTag();
                    String id1 = tag1.getString("id");
                    SkyBlockItem item1 = ItemRegistry.get(id1);
                    double boost = DungeonStar.valueOf(tag.getString("dungeon_star")).getBoost();
                    if (item1 != null) {
                        damage += item1.stats().getDamage();
                        strength += item1.stats().getStrength();
                        critChance += item1.stats().getCriticalChance();
                        critDamage += item1.stats().getCriticalDamage();
                        magicFind += item1.stats().getMagicFind();
                    }
                }
            }

            double initialDamage = (5 + damage) * (1 + strength / 100);
            double damageMultiplier = 1;

            for (int i = 0; i < tag.getList("enchantments", 10).size(); i++) {
                NBTTagCompound ench = tag.getList("enchantments", 10).get(i);
                String name = ench.getString("name");
                int level = ench.getInt("lvl");
                if (name.equalsIgnoreCase("one_for_all")) {
                    damageMultiplier = 5;
                }
            }

            double finalDamage = (initialDamage * damageMultiplier * (1 + armor));
            boolean criticalHit = Math.random() * 100 < critChance;
            if (criticalHit)
                finalDamage *= (1 + critDamage / 100);

            LivingEntity target = (LivingEntity) damaged;
            CustomEntity customEntity = EntityRegistry.get(target);
            if (customEntity == null) {
                event.setCancelled(true);
                return;
            }

            if (target.getHealth() - finalDamage <= 0) {
                customEntity.onDeath(target);
                target.remove();
                if (customEntity.hasPassenger()) {
                    EntityBuilder.passengers.get(target).remove();
                    EntityBuilder.passengers.remove(target);
                }
                EntityBuilder.nameTags.get(target).remove();
                EntityBuilder.nameTags.remove(target);
                EntityBuilder.entities.remove(target);
                event.setCancelled(true);

                if (!customEntity.isBoss()) {
                    double finalMagicFind = magicFind;
                    customEntity.getDrops().forEach(drop -> {
                        double chance = drop.getChance() / 100;
                        double finalChance = chance * (1 + finalMagicFind / 100);
                        if (Math.random() < finalChance) {
                            ItemStack i = ItemBuilder.build(drop.getItem());
                            i.setAmount(Utils.random(drop.getMinAmount(), drop.getMaxAmount()));
                            target.getWorld().dropItemNaturally(target.getLocation(), i);
                        }
                        player.sendMessage((drop.getType() == EntityDropType.RNGESUS_INCARNATE ? drop.getType().getColor() + "" + ChatColor.BOLD + "INSANE DROP! " :
                                drop.getType().getColor() + "" + ChatColor.BOLD +
                                        (drop.getType() == EntityDropType.CRAZY_RARE ? "CRAZY " : "") + "RARE DROP! ") + "" + drop.getItem().rarity().getColor() + drop.getItem().name() + " " + ChatColor.AQUA + "(+" + Utils.round(finalMagicFind, 0) + "% ✯ Magic Find)");
                    });
                }

                return;
            }
            else customEntity.onDamaged(target, finalDamage);

            event.setDamage(finalDamage);
            if (customEntity.isBoss()) {
                target.setCustomName(Utils.color(String.format("%s﴾ %s[%sLv%s%s] %s%s %s%s%s/%s%s%s❤ %s﴿",
                        ChatColor.YELLOW, ChatColor.DARK_GRAY,
                        ChatColor.GRAY, customEntity.getLevel(),
                        ChatColor.DARK_GRAY, ChatColor.RED,
                        customEntity.getName(), ChatColor.GREEN,
                        Utils.formatNumber((int) target.getHealth()), ChatColor.WHITE,
                        ChatColor.GREEN, Utils.formatNumber((int) customEntity.getMaxHealth()),
                        ChatColor.RED, ChatColor.YELLOW)));
            } else {
                target.setCustomName(Utils.color(String.format("%s[%sLv%s%s] %s%s %s%s%s/%s%s%s❤",
                        ChatColor.DARK_GRAY, ChatColor.GRAY,
                        customEntity.getLevel(), ChatColor.DARK_GRAY,
                        ChatColor.RED, customEntity.getName(),
                        ChatColor.GREEN, Utils.formatNumber((int) target.getHealth()),
                        ChatColor.WHITE, ChatColor.GREEN,
                        Utils.formatNumber((int) customEntity.getMaxHealth()), ChatColor.RED)));
            }

            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(damaged.getLocation().clone().add(Utils.random(-0.5, 0.5), 2, Utils.random(-0.5, 0.5)), EntityType.ARMOR_STAND);
            stand.setCustomName(criticalHit ?
                    Utils.rainbowize("✧" + ((int) finalDamage) + "✧") : "" + ChatColor.GRAY + (int) finalDamage);
            stand.setCustomNameVisible(true);
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setMarker(true);
            new BukkitRunnable() {
                public void run() {
                    stand.remove();
                }
            }.runTaskLater(Dungeons.getInstance(), 30);
            /*player.sendMessage(ChatColor.RED + "Damage: " + ChatColor.GRAY + finalDamage);
            player.sendMessage(ChatColor.RED + "Critical Hit: " + ChatColor.GRAY + criticalHit + " (" + critChance + "%)");*/
        }
    }

}
