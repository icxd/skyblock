package net.icxd.dungeons.hex.modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.modifier.Enrichment;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.modifier.PowerScroll;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * Which modifiers the Hex offers an item, and what each does to its data (see ItemModifiers for what they do to
 * the item): its Modifiers category is for an item that has any, its page lists what they offer, and each has a
 * line in the summary. In the order of the wiki's table of The Hex, as the official screenshot's summary has them
 * (Recombobulator 3000, Master Star, Power Scroll):
 * <ol>
 *   <li>Recombobulator 3000: once, on an item with a type (items.json's), but for those the item data says can't
 *       be (the API's) and pet items (the wiki's Recombobulator 3000: "Pets and Pet Items cannot be Recombobulated";
 *       none of 925 live ones is).</li>
 *   <li>Master Stars: the next of the five, on a dungeon item with its five stars.</li>
 *   <li>Wither Scrolls: Implosion, Shadow Warp and Wither Shield, one of each, on Necron's Blade and its swords.</li>
 *   <li>Transmission Tuner: up to 4 on an item with a Transmission ability (see ItemModifiers#maxTuners).</li>
 *   <li>Silex: a level of Efficiency each on a pickaxe, drill or gauntlet with Efficiency V, up to X; not on the
 *       Stonk (the 0.19.1 fix). The gauntlet from live items (UNKNOWN: its item says "a pickaxe or a drill"):
 *       Gemstone Gauntlets have Efficiency VI to X.</li>
 *   <li>Wood Singularity: once, on an axe.</li>
 *   <li>Mana Disintegrator: up to 10 on a wand or deployable.</li>
 *   <li>Jalapeno Book: once, on a deployable.</li>
 *   <li>Power Scrolls: one of the six, on an item with a RIGHT CLICK ability (with what its data gives it: a
 *       Hyperion's scrolls) or that the item data says takes one. A new one takes the old one's place; UNKNOWN
 *       whether Hypixel gives the old one back: it's gone here.</li>
 *   <li>Enrichments: one of the eleven, on an accessory that's Legendary or better now (recombobulated too, as
 *       live Epic accessories are). A new one takes the old one's place, gone as a Power Scroll is (UNKNOWN).</li>
 * </ol>
 * The Tool Exp Capsule (farming tools' levels) and Divan's Powder Coating (not in the Hex's sources) are LATER.
 * Only the Recombobulator's, the Master Star's, the Power Scroll's and the Enrichment's summary lines are seen (the
 * screenshot, the wiki's accessory tab); the rest are ours, in their grammar: UNKNOWN. Plain functions, of the item
 * as it is: they only read it.
 */
public final class HexModifiers {
    private static final List<String> WITHER_SCROLLS = List.of("IMPLOSION_SCROLL", "SHADOW_WARP_SCROLL", "WITHER_SHIELD_SCROLL");
    /** Each Wither Scroll's flag in a Necron's blade's data (NecronsBlade's). */
    private static final List<String> WITHER_FLAGS = List.of("implosion", "shadow_warp", "wither_shield");
    private static final List<String> MASTER_STARS = List.of("FIRST_MASTER_STAR", "SECOND_MASTER_STAR", "THIRD_MASTER_STAR",
            "FOURTH_MASTER_STAR", "FIFTH_MASTER_STAR");
    private static final String STONK = "STONK_PICKAXE";

    /**
     * One thing the page offers: the item it takes (one of it), the colour its name is in on the page when it isn't
     * its rarity's (null for that), whether it's done (on, or as many as can be; still listed, as NEU's Hex lists
     * them), whether it's one of a count (for how "done" is said), and what applying it does to the item's data.
     */
    public record Offer(String itemId, String colour, boolean done, boolean counted, UnaryOperator<NBTTagCompound> apply) {
    }

    /** A modifier for the item: its line in the summary, and what the page offers of it (maybe nothing yet). */
    public record Modifier(String line, List<Offer> offers) {
    }

    private HexModifiers() {
    }

    /** Every modifier for the item, in order; none if it takes none. */
    public static List<Modifier> of(HexItem hexItem) {
        SkyBlockItem item = hexItem.item();
        NBTTagCompound tag = hexItem.tag();
        List<ItemBlock> blocks = ItemBehaviours.of(item).blocks(item, tag, item.blocks());
        List<Modifier> out = new ArrayList<>();
        add(out, recombobulator(item, tag));
        add(out, masterStars(item, tag));
        add(out, witherScrolls(item, tag));
        add(out, tuners(blocks, tag));
        add(out, silex(item, tag));
        add(out, once(item.specificItemType() == SpecificItemType.AXE, "&6Wood Singularity", "WOOD_SINGULARITY", ItemModifiers.WOOD_SINGULARITY, tag));
        add(out, counted(item.specificItemType() == SpecificItemType.WAND || item.specificItemType() == SpecificItemType.DEPLOYABLE,
                "&9Mana Disintegrator", "MANA_DISINTEGRATOR", ItemModifiers.MANA_DISINTEGRATORS, ItemModifiers.MAX_DISINTEGRATORS, tag));
        add(out, once(item.specificItemType() == SpecificItemType.DEPLOYABLE, "&5Jalapeno Book", "JALAPENO_BOOK", ItemModifiers.JALAPENO, tag));
        add(out, powerScrolls(item, blocks, tag));
        add(out, enrichments(hexItem));
        return out;
    }

    /** Their summary lines, in order. */
    public static List<String> summary(HexItem item) {
        return of(item).stream().map(Modifier::line).toList();
    }

    /** What the page offers, in order. */
    public static List<Offer> offers(HexItem item) {
        return of(item).stream().flatMap(m -> m.offers().stream()).toList();
    }

    private static void add(List<Modifier> out, Modifier modifier) {
        if (modifier != null) out.add(modifier);
    }

    // The summary's lines, as the screenshot's: "  &6Recombobulator 3000 &c✖" and "  &5Hot Potato Book &e10&7/&a10"

    static String line(String name, boolean done) {
        return "  " + name + (done ? " &a✔" : " &c✖");
    }

    /** UNKNOWN: whether a full count is green (the screenshot's "10/10" looks it). */
    static String line(String name, int count, int max) {
        return "  " + name + " " + (count >= max ? "&a" : "&e") + count + "&7/&a" + max;
    }

    // Each modifier

    private static Modifier recombobulator(SkyBlockItem item, NBTTagCompound tag) {
        boolean done = tag.getBoolean("recombobulated");
        boolean takes = !"OTHER".equals(item.typeKey()) && !"PET_ITEM".equals(item.typeKey()) && item.canRecombobulate();
        if (!takes && !done) return null;
        return new Modifier(line("&6Recombobulator 3000", done), List.of(new Offer("RECOMBOBULATOR_3000", null, done, false, t -> {
            t.setBoolean("recombobulated", true);
            return t;
        })));
    }

    /**
     * Master stars: on a dungeon item (its kind's, or made one: the tag) that takes stars; the next one is offered
     * once it has all five (FIRST_MASTER_STAR for ➊), the fifth done once it has ➎. The line is ✖ until the first
     * (the screenshot's, on a five-star Livid Dagger), then a count (UNKNOWN). UNKNOWN: whether the Hex also takes
     * Exp levels for one, as an anvil does (the wiki's Master Star: 300 to 500); here only the star.
     */
    private static Modifier masterStars(SkyBlockItem item, NBTTagCompound tag) {
        boolean dungeon = item.dungeonItem() || tag.getBoolean("dungeon_item");
        if (!dungeon || item.upgradeCosts() == null) return null;
        int stars = ItemBuilder.starCount(tag);
        int master = ItemModifiers.masterStars(tag);
        String line = master == 0 ? line("&cMaster Star", false) : line("&cMaster Star", master, MASTER_STARS.size());
        if (stars < 5) return new Modifier(line, List.of());
        boolean done = master >= MASTER_STARS.size();
        String next = MASTER_STARS.get(Math.min(master, MASTER_STARS.size() - 1));
        return new Modifier(line, List.of(new Offer(next, null, done, true, t -> {
            t.setInt("upgrade_count", Math.min(ItemBuilder.starCount(t), 9) + 1);
            return t;
        })));
    }

    private static Modifier witherScrolls(SkyBlockItem item, NBTTagCompound tag) {
        if (!ItemBehaviours.takesWitherScrolls(item)) return null;
        List<Offer> offers = new ArrayList<>();
        int on = 0;
        for (int i = 0; i < WITHER_SCROLLS.size(); i++) {
            String flag = WITHER_FLAGS.get(i);
            boolean done = tag.getBoolean(flag);
            if (done) on++;
            offers.add(new Offer(WITHER_SCROLLS.get(i), null, done, false, t -> {
                t.setBoolean(flag, true);
                return t;
            }));
        }
        return new Modifier(line("&5Wither Scrolls", on, WITHER_SCROLLS.size()), offers);
    }

    private static Modifier tuners(List<ItemBlock> blocks, NBTTagCompound tag) {
        int max = ItemModifiers.maxTuners(blocks);
        if (max == 0) return null;
        return counted(true, "&5Transmission Tuner", "TRANSMISSION_TUNER", ItemModifiers.TUNERS, max, tag);
    }

    /** UNKNOWN: its line; here how many levels past V it has given. */
    private static Modifier silex(SkyBlockItem item, NBTTagCompound tag) {
        SpecificItemType type = item.specificItemType();
        boolean tool = type == SpecificItemType.PICKAXE || type == SpecificItemType.DRILL || type == SpecificItemType.GAUNTLET;
        int efficiency = ItemModifiers.efficiency(tag);
        if (!tool || STONK.equalsIgnoreCase(item.id()) || efficiency < 5) return null;
        boolean done = efficiency >= 10;
        return new Modifier(line("&5Silex", Math.min(efficiency, 10) - 5, 5), List.of(new Offer("SIL_EX", null, done, true,
                t -> ItemModifiers.withEfficiency(t, Math.min(ItemModifiers.efficiency(t), 9) + 1))));
    }

    /** One that goes on once, kept as a count of 1 under {@code key}. */
    private static Modifier once(boolean takes, String name, String itemId, String key, NBTTagCompound tag) {
        boolean done = tag.getInt(key) > 0;
        if (!takes && !done) return null;
        return new Modifier(line(name, done), List.of(new Offer(itemId, null, done, false, t -> {
            t.setInt(key, 1);
            return t;
        })));
    }

    /** One that goes on up to {@code max} times, counted under {@code key}. */
    private static Modifier counted(boolean takes, String name, String itemId, String key, int max, NBTTagCompound tag) {
        int count = tag.getInt(key);
        if (!takes && count == 0) return null;
        return new Modifier(line(name, Math.min(count, max), max), List.of(new Offer(itemId, null, count >= max, true, t -> {
            t.setInt(key, Math.min(t.getInt(key), max - 1) + 1);
            return t;
        })));
    }

    private static Modifier powerScrolls(SkyBlockItem item, List<ItemBlock> blocks, NBTTagCompound tag) {
        PowerScroll on = PowerScroll.of(tag.getString(ItemModifiers.POWER_SCROLL));
        boolean rightClick = blocks.stream().anyMatch(b -> b.isAbility() && "RIGHT_CLICK".equals(b.activation()));
        if (!rightClick && !item.canHavePowerScroll() && on == null) return null;
        List<Offer> offers = new ArrayList<>();
        for (PowerScroll scroll : PowerScroll.values()) {
            offers.add(new Offer(scroll.itemId(), scroll.colour(), scroll == on, false, t -> {
                t.setString(ItemModifiers.POWER_SCROLL, scroll.itemId());
                return t;
            }));
        }
        return new Modifier(line("&7Power Scroll", on != null), offers);
    }

    private static Modifier enrichments(HexItem item) {
        Enrichment on = Enrichment.of(item.tag().getString(ItemModifiers.ENRICHMENT));
        boolean takes = item.accessory() && item.rarity().ordinal() >= Rarity.LEGENDARY.ordinal();
        if (!takes && on == null) return null;
        List<Offer> offers = new ArrayList<>();
        for (Enrichment enrichment : Enrichment.values()) {
            offers.add(new Offer(enrichment.itemId(), null, enrichment == on, false, t -> {
                t.setString(ItemModifiers.ENRICHMENT, enrichment.key());
                return t;
            }));
        }
        return new Modifier(line("&7Enrichment", on != null), offers);
    }
}
