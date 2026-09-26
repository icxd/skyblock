package net.icxd.dungeons.item.data;

import java.util.List;

/**
 * An ability or bonus of a data item, as Hypixel shows it: its header line as it is ("&6Ability: Instant
 * Transmission  &e&lRIGHT CLICK", a bonus's "(0/4)" included), the lines under it, then what using it costs
 * (0 for none; cooldowns in seconds). {@link net.icxd.dungeons.item.ItemBuilder} shows blocks after the item's
 * text. What an ability does is up to Java code, found by its name (see {@link net.icxd.dungeons.item.ability.Abilities}).
 *
 * @param kind ABILITY, FULL_SET, PIECE, TIERED, EXTRA or SHORTBOW
 * @param activation how an ABILITY is used (an {@link net.icxd.dungeons.item.ability.AbilityActivation} name); null if not known
 * @param pieces how many pieces a set bonus takes; 0 if it isn't one
 */
public record ItemBlock(String kind, String name, String header, String activation, List<String> text, double mana,
                        double manaPercent, double cooldown, double soulflow, double healthCost, double vitality, int pieces) {
    public ItemBlock {
        text = text == null ? List.of() : List.copyOf(text);
    }

    /** An ability, used on a click or passive (not a bonus). */
    public boolean isAbility() {
        return "ABILITY".equals(kind);
    }

    /** A shortbow's "Shortbow: Instantly shoots!": the item shoots on a click. */
    public boolean isShortbow() {
        return "SHORTBOW".equals(kind);
    }
}
