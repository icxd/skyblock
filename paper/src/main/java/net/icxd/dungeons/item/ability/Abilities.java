package net.icxd.dungeons.item.ability;

import net.icxd.dungeons.item.ability.abilities.InstantTransmission;
import net.icxd.dungeons.item.ability.abilities.InstantlyShoots;
import net.icxd.dungeons.item.ability.utility.UtilityAbilities;
import net.icxd.dungeons.item.ability.weapons.WeaponAbilities;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

/**
 * What items' abilities do, by ability name: an item's ABILITY blocks are text from data, and the ones with a
 * handler here can be used. Every other ability is only text for now. A shortbow (an item with a SHORTBOW
 * block) shoots, on either click.
 */
public final class Abilities {
    private static final Map<String, AbilityHandler> HANDLERS = new HashMap<>();
    private static final AbilityHandler SHORTBOW = new InstantlyShoots();
    private static final List<ToDoubleFunction<Player>> COST_FACTORS = new ArrayList<>();
    private static final List<ToDoubleBiFunction<Player, NBTTagCompound>> ITEM_COST_FACTORS = new ArrayList<>();
    private static final List<ToDoubleBiFunction<Player, ItemBlock>> COOLDOWN_FACTORS = new ArrayList<>();
    private static final List<ToDoubleFunction<Player>> HEALTH_COST_FACTORS = new ArrayList<>();

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
     * for that click; not sneaking, only a plain one. A LEFT_RIGHT_CLICK or CLICK block is a plain one for
     * either click, a HOLD_RIGHT_CLICK one for a right click. Null for none.
     */
    public static ItemBlock forClick(List<ItemBlock> blocks, boolean right, boolean sneaking, Predicate<String> handled) {
        String shift = (right ? AbilityActivation.SHIFT_RIGHT_CLICK : AbilityActivation.SHIFT_LEFT_CLICK).name();
        boolean shiftBlock = sneaking && blocks.stream().anyMatch(block -> block.isAbility() && shift.equals(block.activation()));
        for (ItemBlock block : blocks) {
            if (!block.isAbility() || !handled.test(block.name())) continue;
            if (sneaking && shift.equals(block.activation())) return block;
            if (!shiftBlock && plainClick(block.activation(), right)) return block;
        }
        return blocks.stream().filter(ItemBlock::isShortbow).findFirst().orElse(null);
    }

    /** Whether an ABILITY block with this activation is a plain (not sneaking) left or right click's. */
    static boolean plainClick(String activation, boolean right) {
        AbilityActivation of = AbilityActivation.of(activation);
        if (of == null) return false;
        return switch (of) {
            case RIGHT_CLICK, HOLD_RIGHT_CLICK -> right;
            case LEFT_CLICK -> !right;
            case LEFT_RIGHT_CLICK, CLICK -> true;
            default -> false;
        };
    }

    /**
     * An item's ABILITY blocks with this activation whose names {@code handled} has a handler for, in their
     * order (a worn piece's SNEAK ones, a bow's ON_SHOOT ones, a pickaxe's DIG one).
     */
    public static List<ItemBlock> withActivation(List<ItemBlock> blocks, AbilityActivation activation, Predicate<String> handled) {
        List<ItemBlock> found = new ArrayList<>();
        for (ItemBlock block : blocks) {
            if (block.isAbility() && activation.name().equals(block.activation()) && handled.test(block.name())) found.add(block);
        }
        return found;
    }

    /** What using the block costs: its mana, and its share of the player's max mana. */
    public static int manaCost(ItemBlock block, int maxMana) {
        return (int) Math.round(block.mana() + block.manaPercent() * maxMana / 100);
    }

    /**
     * Adds what makes a player's abilities cost less mana, as a factor (Wise Dragon Armor's 2/3): "All
     * abilities that reduce Mana cost of items are multiplicative with each other" (the wiki's Wise
     * Dragon Armor).
     */
    public static void addManaCostFactor(ToDoubleFunction<Player> factor) {
        COST_FACTORS.add(factor);
    }

    /**
     * Adds what makes one item's abilities cost less mana, by the item's data (Ultimate Wise's "-10% mana cost
     * of this item's abilities" a level): a factor, multiplied with the rest, as all mana cost reductions are.
     */
    public static void addItemManaCostFactor(ToDoubleBiFunction<Player, NBTTagCompound> factor) {
        ITEM_COST_FACTORS.add(factor);
    }

    /** What using the block costs this player: {@link #manaCost(ItemBlock, int)} times their factors, rounded. */
    public static int manaCost(ItemBlock block, int maxMana, Player player) {
        return manaCost(block, maxMana, player, new NBTTagCompound());
    }

    /**
     * {@link #manaCost(ItemBlock, int, Player)} with the item's Mana Disintegrators ({@code tag} is its data): 1% off
     * its mana each, 2% off its share of max mana (see ItemModifiers), and the item's own factors ({@link
     * #addItemManaCostFactor}). UNKNOWN how Hypixel rounds it; to the nearest.
     */
    public static int manaCost(ItemBlock block, int maxMana, Player player, NBTTagCompound tag) {
        double factor = 1;
        for (ToDoubleFunction<Player> f : COST_FACTORS) factor *= f.applyAsDouble(player);
        NBTTagCompound data = tag == null ? new NBTTagCompound() : tag;
        for (ToDoubleBiFunction<Player, NBTTagCompound> f : ITEM_COST_FACTORS) factor *= f.applyAsDouble(player, data);
        double own = block.mana() * ItemModifiers.manaFactor(data) + block.manaPercent() * ItemModifiers.shareFactor(data) * maxMana / 100;
        return (int) Math.round(own * factor);
    }

    /**
     * Adds what shortens a player's ability cooldowns, as a factor on this block's cooldown (a Mage's
     * cooldown reduction: 0.75 for 25% less); they multiply (UNKNOWN whether Hypixel's add up).
     */
    public static void addCooldownFactor(ToDoubleBiFunction<Player, ItemBlock> factor) {
        COOLDOWN_FACTORS.add(factor);
    }

    /** The block's cooldown for this player, in milliseconds: its seconds times their factors (0 for none). */
    public static long cooldownMillis(ItemBlock block, Player player) {
        double factor = 1;
        for (ToDoubleBiFunction<Player, ItemBlock> f : COOLDOWN_FACTORS) factor *= f.applyAsDouble(player, block);
        return cooldownMillis(block.cooldown(), factor);
    }

    /** {@code seconds} of cooldown times {@code factor}, in milliseconds (never below none). */
    public static long cooldownMillis(double seconds, double factor) {
        return (long) (Math.max(0, seconds) * 1000 * Math.max(0, factor));
    }

    /**
     * Adds what lowers a player's abilities' health costs, as a factor (Berserker Armor's "-20% health costs"
     * a piece: 0.8 each); they multiply (UNKNOWN whether Hypixel's add up).
     */
    public static void addHealthCostFactor(ToDoubleFunction<Player> factor) {
        HEALTH_COST_FACTORS.add(factor);
    }

    /** What using the block costs this player in health: its handler's (see {@link AbilityHandler#healthCost}), times their factors. */
    public static double healthCost(AbilityHandler handler, Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        double cost = handler == null ? block.healthCost() : handler.healthCost(player, item, tag, block);
        if (cost <= 0) return 0;
        for (ToDoubleFunction<Player> f : HEALTH_COST_FACTORS) cost *= f.applyAsDouble(player);
        return Math.max(0, cost);
    }

    /**
     * Whether someone with this much health can pay this health cost: "This ability cannot be used if the user
     * does not have enough health to be consumed, so using it repeatedly cannot cause fatal damage" (the wiki's
     * Flower of Truth), so more health than it costs.
     */
    public static boolean canPayHealth(double health, double cost) {
        return cost <= 0 || health > cost;
    }

    /** Whole seconds left on a cooldown, as "on cooldown for 17s" shows 16.9 (UNKNOWN whether it rounds up or to nearest). */
    public static long cooldownSeconds(long millis) {
        return (long) Math.ceil(Math.max(0, millis) / 1000.0);
    }
}
