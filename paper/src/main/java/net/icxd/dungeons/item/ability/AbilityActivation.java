package net.icxd.dungeons.item.ability;

import lombok.Getter;

/** How an ability is used, as its lore line names it ("RIGHT CLICK"); passive ones say nothing. */
@Getter
public enum AbilityActivation {
    LEFT_CLICK("LEFT CLICK"),
    RIGHT_CLICK("RIGHT CLICK"),
    SHIFT_LEFT_CLICK("SNEAK LEFT CLICK"),
    SHIFT_RIGHT_CLICK("SNEAK RIGHT CLICK"),
    PASSIVE("");

    private final String display;

    AbilityActivation(String display) {
        this.display = display;
    }
}
