package net.icxd.dungeons.item.ability;

/** How an ABILITY block is used, as items.json names it; passive ones aren't (see {@link Abilities#forClick}). */
public enum AbilityActivation {
    LEFT_CLICK,
    RIGHT_CLICK,
    SHIFT_LEFT_CLICK,
    SHIFT_RIGHT_CLICK,
    PASSIVE
}
