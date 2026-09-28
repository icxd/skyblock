package net.icxd.dungeons.item.ability;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

/** What an ability does (see {@link Abilities}). By the time it's used, its costs are paid and its cooldown started. */
@FunctionalInterface
public interface AbilityHandler {
    /**
     * What set a use off, for the activations that need more than that it happened (see {@link AbilityActivation}):
     * the block's activation, whether it was a right click (a LEFT_RIGHT_CLICK or CLICK ability tells the two
     * apart), the arrow an ON_SHOOT ability's bow has just shot, and the block a click (or a DIG) was on; null
     * for what doesn't apply.
     */
    record Trigger(AbilityActivation activation, boolean right, Projectile projectile, Block block) {
        /** A click, on {@code block} (null for none). */
        public static Trigger click(AbilityActivation activation, boolean right, Block block) {
            return new Trigger(activation, right, null, block);
        }

        /** Something other than a click: a sneak, a shot (its arrow), a dig (its block). */
        public static Trigger of(AbilityActivation activation, Projectile projectile, Block block) {
            return new Trigger(activation, false, projectile, block);
        }
    }

    /** {@code tag} is the held item's data; {@code block} the ability or shortbow block being used. */
    void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block);

    /**
     * The use, told whether its Vitality cost was paid: only ever not for one whose Vitality part is
     * optional (see {@link #vitalityOptional}), which then casts without that part. The rest do the use.
     */
    default void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, boolean vitalityPaid) {
        use(player, item, tag, block);
    }

    /**
     * The use with what set it off (see {@link Trigger}); for a worn piece's SNEAK ability, {@code item} and
     * {@code tag} are that piece's. By default the use without it.
     */
    default void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, boolean vitalityPaid, Trigger trigger) {
        use(player, item, tag, block, vitalityPaid);
    }

    /**
     * Whether it still casts with too little Vitality, just without what the Vitality pays for (and
     * spending none): "Not having enough Vitality does not prevent you from casting Wither Impact - it
     * still deals damage, it just doesn't put up the shield" (0.26.1's release notes, and its June 10
     * alpha). No unless it says otherwise: too little of it stops the cast, as too little mana does.
     */
    default boolean vitalityOptional() {
        return false;
    }

    /**
     * Whether a click with the item is this ability's at all just now, or the item's other use (its
     * shortbow shot): the Terminator's Salvation "Can be cast after landing 3 hits", and until then a left
     * click shoots as ever. Asked before anything else, and nothing is said or spent when it isn't. Yes
     * unless it says otherwise.
     */
    default boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
        return true;
    }

    /**
     * Whether it can happen now, asked once its costs are known to be there and before they're paid, so
     * a use that can't happen costs nothing (Instant Transmission with no room to go); if not, it tells
     * the player why. Yes unless it says otherwise.
     */
    default boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return true;
    }

    /** {@link #usable(Player, SkyBlockItem, NBTTagCompound, ItemBlock)} with what set the use off. */
    default boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block, Trigger trigger) {
        return usable(player, item, tag, block);
    }

    /**
     * The health it costs, before what lowers health costs (see {@link Abilities#addHealthCostFactor}): its
     * block's by default; one whose cost is a share of their health says so ("Use 10% of your max health").
     * It's charged with the rest of its costs: "This ability cannot be used if the user does not have enough
     * health to be consumed" (the wiki's Flower of Truth).
     */
    default double healthCost(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return block.healthCost();
    }
}
