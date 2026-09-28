package net.icxd.dungeons.item.ability;

/**
 * How an ABILITY block is used, as items.json names it (every activation its blocks have); passive ones
 * aren't. Clicks are {@link Abilities#forClick}'s; SNEAK, ON_SHOOT and DIG come through {@link Activations}
 * and PlayerListener, each paid for as a click's is (see {@link Activations#use}).
 */
public enum AbilityActivation {
    LEFT_CLICK,
    RIGHT_CLICK,
    SHIFT_LEFT_CLICK,
    SHIFT_RIGHT_CLICK,
    /** Either click, which the use's {@link AbilityHandler.Trigger} tells apart (the Hollow Wand's "Left Click to cast ... and Right Click to cast"). */
    LEFT_RIGHT_CLICK,
    /** Either click (the Ham Radio's "Switches to the next radio channel"). */
    CLICK,
    /**
     * A right click held down: each right click the client sends while it's held (every 4 ticks, vanilla's)
     * is a use, so the handler sees it again and again, as long as its costs are paid.
     */
    HOLD_RIGHT_CLICK,
    /** Starting to sneak, with the item held or worn (armor, equipment): the Aurora Armor's Homing Missiles. */
    SNEAK,
    /** Shooting a drawn bow: the arrow is the use's {@link AbilityHandler.Trigger#projectile}. */
    ON_SHOOT,
    /** A left click on a block with the item: the block is the use's {@link AbilityHandler.Trigger#block} (UNKNOWN whether Hypixel's is per block broken). */
    DIG,
    PASSIVE;

    /** The activation with this name; null for none. */
    public static AbilityActivation of(String name) {
        if (name == null) return null;
        for (AbilityActivation activation : values()) if (activation.name().equals(name)) return activation;
        return null;
    }
}
