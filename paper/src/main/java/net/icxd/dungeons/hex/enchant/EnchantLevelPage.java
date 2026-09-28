package net.icxd.dungeons.hex.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.category.Enchantments;
import net.icxd.dungeons.hex.enchant.EnchantRules.Action;
import net.icxd.dungeons.hex.enchant.EnchantRules.Change;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.requirement.skill.SkillRequirement;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * "The Hex ➜ Enchant Item ➜ <Enchant>" (SkyblockAPI's and NEU's name for it): an enchantment's levels, one enchanted
 * book a level from its lowest to its highest, row by row from 12 (UNKNOWN: their order on Hypixel, U8), with the
 * header as on the list, and no Sort. Go Back is "To Enchant Item".
 *
 * <p>A book says what its level does, what it takes off the item, and what it costs, and a click does it (see
 * EnchantRules#choose): on the item's level it takes the enchantment off, above it puts this one on. It costs its
 * Exp levels (NEU's table; the wiki's where NEU has none), taken from their vanilla levels, and gives Enchanting XP
 * for them (3.5 X^1.5, the wiki's Enchanting), also for taking one off (the wiki's Enchantments). A level the
 * Enchantment Table doesn't sell needs its enchanted book too, as on Hypixel; there are no enchanted books of each
 * enchantment here yet, so on a Normal profile those can't be had (LATER). And it needs the Enchanting level the
 * wiki gives it. On a Sandbox profile all of it is free and nothing is needed, and no Enchanting XP comes of it
 * (nothing was spent). Main thread.
 */
public final class EnchantLevelPage extends HexPage {
    /** UNKNOWN: Hypixel's action lines (U9); the Enchantment Table's words, ours. */
    static final String ENCHANT = "&eClick to enchant!";
    static final String UPGRADE = "&eClick to upgrade!";
    static final String REMOVE = "&eClick to remove!";
    /** UNKNOWN (U10): what Hypixel does on a lower level than the item's; ours. */
    static final String LOWER = "&cThis item has a higher level!";
    static final List<String> NO_BOOK = List.of("&cYou don't have that in your", "&cinventories!");
    static final String NO_LEVELS = "&cYou don't have enough Exp Levels!";

    private final EnchantmentData.Entry enchantment;
    private final EnchantItemPage.Shown list;

    EnchantLevelPage(HexSession session, EnchantmentData.Entry enchantment, EnchantItemPage.Shown list) {
        super(session, Enchantments.TITLE + " ➜ " + enchantment.name());
        this.enchantment = enchantment;
        this.list = list;
    }

    /** A level as it stands for the item: what choosing it does, what it costs, and why it can't be had (null if it can). */
    record Offer(int level, Change change, HexCosts cost, int levels, boolean book, List<String> blocked) {
    }

    @Override
    protected Icon header() {
        return Enchantments.header();
    }

    @Override
    protected List<Entry> entries() {
        HexItem item = session.hexItem();
        EnchantmentData data = EnchantmentData.current();
        Map<String, Integer> on = EnchantRules.on(item.tag());
        List<Entry> entries = new ArrayList<>();
        for (int level = enchantment.min(); level <= enchantment.max(); level++) {
            Offer offer = offer(data, on, level);
            entries.add(Entry.of(icon(data, offer), () -> choose(offer)));
        }
        return entries;
    }

    @Override
    protected String backTo() {
        return "&7To Enchant Item";
    }

    @Override
    protected void back() {
        session.open(list.open(session));
    }

    // A level

    private Offer offer(EnchantmentData data, Map<String, Integer> on, int level) {
        Change change = EnchantRules.choose(data, on, enchantment, level);
        Integer xp = enchantment.xp(level);
        int levels = xp == null ? 0 : xp;
        HexCosts cost = change.action() == Action.LOWER || levels <= 0 ? HexCosts.NOTHING : HexCosts.of(new HexCosts.Levels(levels));
        boolean free = session.sandbox();
        // Taking one off needs neither its book nor the Enchanting level.
        boolean adding = change.action() == Action.APPLY || change.action() == Action.UPGRADE;
        boolean book = !free && adding && !enchantment.fromTable(level);
        List<String> blocked = null;
        if (change.action() == Action.LOWER) blocked = List.of(LOWER);
        else if (!free && adding && enchanting(session.player()) < enchantment.enchanting()) blocked = List.of(requirement(enchantment.enchanting()));
        else if (book) blocked = NO_BOOK;
        return new Offer(level, change, cost, levels, book, blocked);
    }

    private static int enchanting(Player player) {
        return Skills.level(player, Skill.ENCHANTING);
    }

    /** "&4❣ &cRequires &aEnchanting Skill 20&c.": the plugin's requirement line (SkillRequirement). */
    static String requirement(int level) {
        return new SkillRequirement(Skill.ENCHANTING, level).lore().getFirst();
    }

    /** The book for a level, with HexCosts' Cost block for them; none when there's nothing to show in it (see {@link #icon}). */
    private Icon icon(EnchantmentData data, Offer offer) {
        boolean nothing = offer.change().action() == Action.LOWER || offer.cost().parts().isEmpty() && !offer.book() && !session.sandbox();
        return icon(data, enchantment, offer, nothing ? List.of() : offer.cost().lore(session, action(offer.change().action())));
    }

    /**
     * A level's book: "&aSharpness VI", what the level does, what it takes off the item (UNKNOWN: Hypixel's words;
     * "&cReplaces Life Steal", ours), then HexCosts' Cost block ending in what a click does, or why it can't: the
     * Enchanting level it needs (in the plugin's requirement line), or its enchanted book (above the Enchantment
     * Table's levels: "&9Enchanted Book (Sharpness VI) &c✖" in its rarity's colour, UNKNOWN wording, and "You don't
     * have that in your inventories!"). With no {@code block} (nothing to pay: no source gives its Exp levels, UNKNOWN,
     * so none are taken), only the action or why not. A lower level than the item's: only that it has a higher one.
     */
    static Icon icon(EnchantmentData data, EnchantmentData.Entry enchantment, Offer offer, List<String> block) {
        int level = offer.level();
        List<String> lore = new ArrayList<>(EnchantItemPage.description(enchantment, level));
        if (!offer.change().replaced().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String id : offer.change().replaced()) {
                EnchantmentData.Entry other = data.get(id);
                names.add(other == null ? id : other.name());
            }
            lore.add("");
            lore.addAll(Text.wrap("&cReplaces " + String.join(", ", names), Text.LORE_WIDTH));
        }
        lore.add("");
        if (offer.change().action() == Action.LOWER) {
            lore.add(LOWER);
        } else if (block.isEmpty()) {
            lore.addAll(offer.blocked() != null ? offer.blocked() : List.of(action(offer.change().action())));
        } else {
            List<String> cost = new ArrayList<>(block);
            if (offer.book()) cost.add(1, bookLine(enchantment, level) + " &c✖");
            if (offer.blocked() != null) {
                // What stops it, in place of the action (after the block's blank line).
                cost = new ArrayList<>(cost.subList(0, cost.indexOf("") + 1));
                cost.addAll(offer.blocked());
            }
            lore.addAll(cost);
        }
        return new Icon(Material.ENCHANTED_BOOK, EnchantRules.bookName(enchantment, level), lore);
    }

    /** The enchanted book a level needs, in its rarity's colour: "&9Enchanted Book (Sharpness VI)" (UNKNOWN: Hypixel's words). */
    static String bookLine(EnchantmentData.Entry enchantment, int level) {
        EnchantmentData.Level at = enchantment.level(level);
        char colour = at == null || at.rarity() == null ? 'f' : at.rarity().getCode();
        return "&" + colour + "Enchanted Book (" + enchantment.name() + " " + Utils.getRomanNumeral(level) + ")";
    }

    static String action(Action action) {
        return switch (action) {
            case APPLY -> ENCHANT;
            case UPGRADE -> UPGRADE;
            case REMOVE -> REMOVE;
            case LOWER -> LOWER;
        };
    }

    /**
     * A level clicked: said why not, or done. It's paid for and the item made again (HexSession#upgrade, which says
     * "You applied a Sharpness VI to your ..."; taking one off says so in its own words, UNKNOWN), and what was spent
     * gives Enchanting XP.
     */
    private void choose(Offer offer) {
        if (offer.blocked() != null) {
            say(String.join(" ", offer.blocked()));
            return;
        }
        if (!offer.cost().affordable(session)) {
            say(NO_LEVELS);
            return;
        }
        HexItem before = session.hexItem();
        if (before == null) return;
        boolean remove = offer.change().action() == Action.REMOVE;
        String name = EnchantRules.displayName(enchantment, offer.level());
        if (!session.upgrade(offer.cost(), EnchantRules.with(before.tag(), offer.change().after()), remove ? null : name)) return;
        if (!session.sandbox() && offer.levels() > 0) SkillGains.give(viewer, Skill.ENCHANTING, EnchantRules.enchantingXp(offer.levels()));
        if (remove) {
            HexItem after = session.hexItem();
            say("&aYou removed " + name + " &afrom your " + (after == null ? "item" : after.name()) + "&a!");
        }
    }
}
