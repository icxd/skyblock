package net.icxd.dungeons.entity;

import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.utils.RandomCollection;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public interface CustomEntity {
    EntityType getEntityType();
    String getId();

    default String getName() { return ""; }
    default int getLevel() { return 1; }
    default RegionType getSpawnRegion() { return RegionType.NONE; }

    default ItemStack getHelmet() { return null; }
    default ItemStack getChestplate() { return null; }
    default ItemStack getLeggings() { return null; }
    default ItemStack getBoots() { return null; }
    default ItemStack getItemInHand() { return null; }

    default RandomCollection<EntityDrop> getDrops() { return null; }

    default CustomEntity getPassenger() { return null; }

    default double getMaxHealth() { return 100; }
    default double getDamage() { return 100; }

    default boolean isBoss() { return false; }
    default boolean isBaby() { return false; }
    default boolean isInvisible() { return false; }
    default boolean isUpsideDown() { return false; }
    default boolean isCustomNameVisible() { return true; }
    default boolean isSilent() { return false; }

    default void onSpawn(LivingEntity entity) {}
    default void onDeath(LivingEntity entity) {}
    default void onDamaged(LivingEntity entity, double damage) {}
    default void onAttack(LivingEntity entity, LivingEntity target) {}
    default void onTick(LivingEntity entity) {}

    // should not be overwritten
    default boolean hasPassenger() { return getPassenger() != null; }
    default boolean canSpawn() { return getSpawnRegion() != RegionType.NONE; }
    default void updateNameTag(LivingEntity armorStand, LivingEntity entity) {
        if (this.isBoss()) {
            armorStand.setCustomName(Utils.color(String.format("%s﴾ %s[%sLv%s%s] %s%s %s%s%s/%s%s%s❤ %s﴿",
                    ChatColor.YELLOW, ChatColor.DARK_GRAY,
                    ChatColor.GRAY, this.getLevel(),
                    ChatColor.DARK_GRAY, ChatColor.RED,
                    this.getName(), ChatColor.GREEN,
                    Utils.formatNumber((int) entity.getHealth()), ChatColor.WHITE,
                    ChatColor.GREEN, Utils.formatNumber((int) this.getMaxHealth()),
                    ChatColor.RED, ChatColor.YELLOW)));
        } else {
            armorStand.setCustomName(Utils.color(String.format("%s[%sLv%s%s] %s%s %s%s%s/%s%s%s❤",
                    ChatColor.DARK_GRAY, ChatColor.GRAY,
                    this.getLevel(), ChatColor.DARK_GRAY,
                    ChatColor.RED, this.getName(),
                    ChatColor.GREEN, Utils.formatNumber((int) entity.getHealth()),
                    ChatColor.WHITE, ChatColor.GREEN,
                    Utils.formatNumber((int) this.getMaxHealth()), ChatColor.RED)));
        }
    }
}
