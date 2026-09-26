package net.icxd.dungeons.mob;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * A kind of SkyBlock mob: what it is, and what it does. {@link Mobs} spawns them and keeps their
 * health, which is SkyBlock health (millions, where a vanilla mob has at most 1024).
 */
public interface SkyBlockMob {
    /** What it's registered as ("MAGMA_CUBE"). */
    String getId();
    EntityType getEntityType();

    /** With {@code &} colours. */
    default String getName() { return ""; }
    default int getLevel() { return 1; }
    default double getMaxHealth() { return 100; }
    /** What its hits do to a player, in SkyBlock damage (before their defense). */
    default double getDamage() { return 0; }
    /** A boss's name tag is framed: "﴾ [Lv200] Bladesoul 50M/50M❤ ﴿". */
    default boolean isBoss() { return false; }
    /** Nothing hurts it (a passenger that's part of how its mob looks, say). */
    default boolean isInvulnerable() { return false; }
    default boolean isInvisible() { return false; }
    /** Shown upside down (named "Dinnerbone"), without a name tag. */
    default boolean isUpsideDown() { return false; }
    default boolean hasNameTag() { return !isUpsideDown(); }

    default ItemStack getHelmet() { return null; }
    default ItemStack getChestplate() { return null; }
    default ItemStack getLeggings() { return null; }
    default ItemStack getBoots() { return null; }
    default ItemStack getItemInHand() { return null; }

    /** Each rolls on its own when a player kills it. */
    default List<MobDrop> getDrops() { return List.of(); }
    /** A mob riding it, spawned and removed with it. */
    default SkyBlockMob getPassenger() { return null; }

    default void onSpawn(LivingEntity entity) {}
    default void onDeath(LivingEntity entity, Player killer) {}
    default void onDamaged(LivingEntity entity, Player by, double damage) {}
    /** After its hit (or its projectile's) on a player. */
    default void onAttack(LivingEntity entity, Player target) {}
    default void onTick(LivingEntity entity) {}
}
