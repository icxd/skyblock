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
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();

        long cms = System.currentTimeMillis();

        Utils.async(() -> {
            User user = User.getUser(player.getUniqueId());
            user.load();

            player.sendMessage(Utils.color("&aSuccessfully loaded player data. &8(took " + (System.currentTimeMillis() - cms) + "ms)"));
        });

//        player.teleport(new Location(Bukkit.getWorld("world"), 0, 100, 0));
//        World world = Bukkit.getWorld(Dungeons.getSkyBlockServer().getServerType().getWorldName());
//        player.teleport(world.getSpawnLocation());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();

        Utils.async(() -> {
            User user = User.getUser(player.getUniqueId());
            user.save();
        });
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        User user = User.getUser(player.getUniqueId());
        Rank rank = Rank.valueOf(user.getDocument().getString("rank"));

        event.setCancelled(true);

        event.getRecipients().forEach(recipient -> recipient.sendMessage(Utils.color(rank.getPrefix() + player.getName() + (rank == Rank.DEFAULT ? "&7" : "&f") + ": " + event.getMessage())));
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        User user = User.getUser(player.getUniqueId());
        ItemStack item = event.getItem().getItemStack();
        if (item == null) return;
        if (item.getType() == Material.AIR) return;
        net.minecraft.server.v1_8_R3.ItemStack craftItem = CraftItemStack.asNMSCopy(item);
        if (!craftItem.hasTag()) return;
        net.minecraft.server.v1_8_R3.NBTTagCompound tag = craftItem.getTag();
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
        net.minecraft.server.v1_8_R3.ItemStack craftItem = CraftItemStack.asNMSCopy(item);
        if (!craftItem.hasTag()) return;
        net.minecraft.server.v1_8_R3.NBTTagCompound tag = craftItem.getTag();
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
        Player player = event.getPlayer();
        User user = User.getUser(player.getUniqueId());
        ItemStack item = player.getItemInHand();
        if (item == null) return;
        if (item.getType() == Material.AIR) return;
        net.minecraft.server.v1_8_R3.ItemStack craftItem = CraftItemStack.asNMSCopy(item);
        if (!craftItem.hasTag()) return;
        net.minecraft.server.v1_8_R3.NBTTagCompound tag = craftItem.getTag();
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
                        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, -4f);

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
                        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, -4f);

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
            net.minecraft.server.v1_8_R3.ItemStack craftItem = CraftItemStack.asNMSCopy(item);
            if (!craftItem.hasTag()) return;
            net.minecraft.server.v1_8_R3.NBTTagCompound tag = craftItem.getTag();
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
                net.minecraft.server.v1_8_R3.ItemStack craft1 = CraftItemStack.asNMSCopy(helmet);
                if (craft1.hasTag()) {
                    net.minecraft.server.v1_8_R3.NBTTagCompound tag1 = craft1.getTag();
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
                net.minecraft.server.v1_8_R3.ItemStack craft1 = CraftItemStack.asNMSCopy(chestplate);
                if (craft1.hasTag()) {
                    net.minecraft.server.v1_8_R3.NBTTagCompound tag1 = craft1.getTag();
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
                net.minecraft.server.v1_8_R3.ItemStack craft1 = CraftItemStack.asNMSCopy(leggings);
                if (craft1.hasTag()) {
                    net.minecraft.server.v1_8_R3.NBTTagCompound tag1 = craft1.getTag();
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
                net.minecraft.server.v1_8_R3.ItemStack craft1 = CraftItemStack.asNMSCopy(boots);
                if (craft1.hasTag()) {
                    net.minecraft.server.v1_8_R3.NBTTagCompound tag1 = craft1.getTag();
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
