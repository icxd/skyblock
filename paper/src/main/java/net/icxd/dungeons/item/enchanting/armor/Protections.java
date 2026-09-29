package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.enums.SpecificItemType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The armor enchantments against what hurts a player other than a mob's hit, and those that change how they
 * move: Projectile, Blast and Fire Protection, Feather Falling (and Old Dragon Armor's Old Blood on it), Depth
 * Strider and Frost Walker. Vanilla damage (a fall, fire, lava, an explosion) is SkyBlock health 1:1 without
 * them (see {@link VanillaDamage}); they give a factor on it by its cause. Main thread.
 */
final class Protections {
    static final String PROJECTILE = "projectile_protection";
    static final String BLAST = "blast_protection";
    static final String FIRE = "fire_protection";
    static final String FEATHER_FALLING = "feather_falling";
    static final String DEPTH_STRIDER = "depth_strider";
    static final String FROST_WALKER = "frost_walker";
    /**
     * Old Blood's "Increases the strength of ... Feather Falling": "+3% Fall damage reduction Per Level" (the
     * wiki's Old Dragon Armor, Enchantment Buffs), the set's own boots' (with the full set on, the boots are the
     * set's).
     */
    static final double OLD_BLOOD_PER_LEVEL = 3;
    static final String OLD_BLOOD = "Old Blood";
    /**
     * How long Frost Walker's ice lasts once they've left it. UNKNOWN (the wiki's Frost Walker has none; vanilla's
     * starts melting after 3 to 6 seconds): 5 seconds.
     */
    static final int FROST_TICKS = 100;
    /** Transient, so they aren't saved with the player and go away with the plugin. */
    private static final NamespacedKey SAFE_FALL = new NamespacedKey("dungeons", "feather_falling");
    private static final NamespacedKey WATER_MOVEMENT = new NamespacedKey("dungeons", "depth_strider");
    /** The ice Frost Walker has made, and the tick it goes back to water at. */
    private static final Map<Block, Integer> ICE = new HashMap<>();

    private Protections() {
    }

    static void register() {
        // "Grants +49 Defense against projectiles": a mob's arrow's hit, where Defense counts.
        PlayerDamage.addDefenseAgainst((player, by) -> by instanceof Projectile ? sum(WornEnchants.of(player), PROJECTILE) : 0);
        VanillaDamage.addFactor(Protections::factor);
    }

    /** The first number of each piece's text of this enchantment, added up (its Defense, True Defense or blocks). */
    static double sum(List<WornEnchants.Piece> pieces, String id) {
        double sum = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(id);
            if (level > 0) sum += EnchantNumbers.get(id, level, 0);
        }
        return sum;
    }

    /**
     * What their enchantments leave of vanilla damage of this cause: Feather Falling's "reduces fall damage by
     * 50%"; Fire Protection's "+14 True Defense against fire and lava", Blast Protection's "+210 Defense against
     * explosions" and Projectile Protection's against a vanilla projectile, each as Defense (or True Defense)
     * lessens a hit, 100 / (100 + it). Vanilla damage has no Defense of its own here, so theirs is all that
     * lessens it (UNKNOWN how SkyBlock's fire and explosions otherwise go: the wiki's Damage Calculation says
     * "Differs").
     */
    private static double factor(Player player, VanillaDamage.Cause cause) {
        List<WornEnchants.Piece> pieces = WornEnchants.of(player);
        if (pieces.isEmpty()) return 1;
        return switch (cause) {
            case FALL -> fallFactor(fallReduction(pieces, SetBonuses.active(player, OLD_BLOOD)));
            case FIRE, LAVA -> Damage.defenseMultiplier(sum(pieces, FIRE));
            case EXPLOSION -> Damage.defenseMultiplier(sum(pieces, BLAST));
            case PROJECTILE -> Damage.defenseMultiplier(sum(pieces, PROJECTILE));
            default -> 1;
        };
    }

    /** Feather Falling's cut of fall damage, in percent: its text's (the second number), and Old Blood's 3% a level. */
    static double fallReduction(List<WornEnchants.Piece> pieces, boolean oldBlood) {
        double percent = 0;
        for (WornEnchants.Piece piece : pieces) {
            int level = piece.level(FEATHER_FALLING);
            if (level <= 0) continue;
            percent += EnchantNumbers.get(FEATHER_FALLING, level, 1) + (oldBlood ? OLD_BLOOD_PER_LEVEL * level : 0);
        }
        return percent;
    }

    /** What's left of fall damage with this much cut off, in percent: never below none. */
    static double fallFactor(double reduction) {
        return Math.max(0, 1 - reduction / 100);
    }

    /**
     * Once a second, their vanilla attributes: Feather Falling's "Increases how high you can fall before taking
     * fall damage by 10" is that many more blocks of safe fall distance, and Depth Strider's "Reduces how much you
     * are slowed in the water by 100%" is water movement efficiency (1 for all of it, as vanilla's Depth Strider
     * III has it).
     */
    static void second(Player player, List<WornEnchants.Piece> pieces) {
        setBonus(player.getAttribute(Attribute.SAFE_FALL_DISTANCE), SAFE_FALL, sum(pieces, FEATHER_FALLING));
        setBonus(player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY), WATER_MOVEMENT, waterMovement(sum(pieces, DEPTH_STRIDER)));
    }

    /** Depth Strider's percent as water movement efficiency: a share, at most all of it. */
    static double waterMovement(double percent) {
        return Math.max(0, Math.min(1, percent / 100));
    }

    /** One modifier of ours on the attribute, at this amount (none for 0); left alone if it's that already. */
    private static void setBonus(AttributeInstance attribute, NamespacedKey key, double amount) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(key);
        if (current != null && current.getAmount() == amount) return;
        if (current != null) attribute.removeModifier(key);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    // ---------- Frost Walker ----------

    /**
     * "Ice blocks will be created below you when you walk above water in a radius of 2 blocks": as they walk (on
     * something solid, as vanilla's does only on the ground), still water with air above it within the radius
     * under their feet turns to frosted ice, which goes back to water {@link #FROST_TICKS} after they've last been
     * near it. Not in a dungeon run, whose rooms have water of their own (a puzzle's). Hypixel removed it in 0.22
     * (the wiki's Frost Walker), so its items' text is all there is to go on (UNKNOWN: whether it should do
     * anything; see ENCHANTS_ARMOR.md).
     */
    static void moved(Player player, Location to) {
        if (player.isFlying() || player.isInsideVehicle() || RunManager.inRun(player)) return;
        List<WornEnchants.Piece> pieces = WornEnchants.of(player);
        WornEnchants.Piece boots = WornEnchants.ofType(pieces, SpecificItemType.BOOTS);
        int level = boots == null ? 0 : boots.level(FROST_WALKER);
        if (level <= 0) return;
        int radius = (int) EnchantNumbers.get(FROST_WALKER, level, 0);
        World world = to.getWorld();
        int x = to.getBlockX(), y = (int) Math.floor(to.getY() - 0.01), z = to.getBlockZ();
        if (radius <= 0 || !world.getBlockAt(x, y, z).getType().isSolid()) return;
        int until = Bukkit.getCurrentTick() + FROST_TICKS;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                Block block = world.getBlockAt(x + dx, y, z + dz);
                if (ICE.containsKey(block)) {
                    ICE.put(block, until);
                } else if (freezes(block)) {
                    block.setType(Material.FROSTED_ICE, false);
                    ICE.put(block, until);
                }
            }
        }
    }

    /** Still water (a source) with air above it. */
    private static boolean freezes(Block block) {
        return block.getType() == Material.WATER && block.getBlockData() instanceof Levelled water && water.getLevel() == 0
                && block.getRelative(0, 1, 0).getType().isAir();
    }

    /** Every second: the ice whose time is up goes back to water (unless it isn't frosted ice any more). */
    static void melt() {
        if (ICE.isEmpty()) return;
        int tick = Bukkit.getCurrentTick();
        ICE.entrySet().removeIf(e -> {
            if (e.getValue() > tick) return false;
            unfreeze(e.getKey());
            return true;
        });
    }

    /** All of it back to water at once (the plugin is stopping). */
    static void meltAll() {
        for (Block block : ICE.keySet()) unfreeze(block);
        ICE.clear();
    }

    private static void unfreeze(Block block) {
        if (block.getType() == Material.FROSTED_ICE) block.setType(Material.WATER, false);
    }
}
