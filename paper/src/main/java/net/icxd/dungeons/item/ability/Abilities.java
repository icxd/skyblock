package net.icxd.dungeons.item.ability;

import net.icxd.dungeons.item.ability.abilities.InstantTransmission;
import net.icxd.dungeons.item.ability.abilities.InstantlyShoots;
import net.icxd.dungeons.item.ability.utility.UtilityAbilities;
import net.icxd.dungeons.item.ability.weapons.WeaponAbilities;
import net.icxd.dungeons.item.data.ItemBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What items' abilities do, by ability name: an item's ABILITY blocks are text from data, and the ones with a
 * handler here can be used. Every other ability is only text for now. A shortbow (an item with a SHORTBOW
 * block) shoots, on either click.
 */
public final class Abilities {
    private static final Map<String, AbilityHandler> HANDLERS = new HashMap<>();
    private static final AbilityHandler SHORTBOW = new InstantlyShoots();

    static {
        register("Instant Transmission", new InstantTransmission());
        WeaponAbilities.register(Abilities::register);
        UtilityAbilities.register(Abilities::register);
    }

    /** What the ability with this name does, from now on; one handler per name. */
    private static void register(String abilityName, AbilityHandler handler) {
        if (HANDLERS.putIfAbsent(abilityName, handler) != null) throw new IllegalStateException("Two handlers for " + abilityName);
    }

    private Abilities() {
    }

    /** What the ability with this name does; null if nothing does it (yet). */
    public static AbilityHandler get(String abilityName) {
        return abilityName == null ? null : HANDLERS.get(abilityName);
    }

    /** What using this block does: a shortbow's shot, or its ability's handler (null for none). */
    public static AbilityHandler handler(ItemBlock block) {
        return block.isShortbow() ? SHORTBOW : get(block.name());
    }

    /**
     * The block a left or right click uses, of an item's blocks (with what its behaviour adds): the first
     * ABILITY block for the click whose name {@code handled} has a handler for, else a SHORTBOW block.
     * Sneaking, a SHIFT_ block for the click is for it, and a plain one too unless the item has a SHIFT_ one
     * for that click; not sneaking, only a plain one. Null for none.
     */
    public static ItemBlock forClick(List<ItemBlock> blocks, boolean right, boolean sneaking, Predicate<String> handled) {
        String plain = (right ? AbilityActivation.RIGHT_CLICK : AbilityActivation.LEFT_CLICK).name();
        String shift = (right ? AbilityActivation.SHIFT_RIGHT_CLICK : AbilityActivation.SHIFT_LEFT_CLICK).name();
        boolean shiftBlock = sneaking && blocks.stream().anyMatch(block -> block.isAbility() && shift.equals(block.activation()));
        for (ItemBlock block : blocks) {
            if (!block.isAbility() || !handled.test(block.name())) continue;
            if (sneaking && shift.equals(block.activation())) return block;
            if (!shiftBlock && plain.equals(block.activation())) return block;
        }
        return blocks.stream().filter(ItemBlock::isShortbow).findFirst().orElse(null);
    }

    /** What using the block costs: its mana, and its share of the player's max mana. */
    public static int manaCost(ItemBlock block, int maxMana) {
        return (int) Math.round(block.mana() + block.manaPercent() * maxMana / 100);
    }

    /** Whole seconds left on a cooldown, as "on cooldown for 17s" shows 16.9 (UNKNOWN whether it rounds up or to nearest). */
    public static long cooldownSeconds(long millis) {
        return (long) Math.ceil(Math.max(0, millis) / 1000.0);
    }
}
