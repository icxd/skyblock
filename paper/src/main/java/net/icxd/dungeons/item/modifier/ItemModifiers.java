package net.icxd.dungeons.item.modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Text;

/**
 * The modifiers an item is given one by one (the Hex's Modifiers, Hypixel's anvil upgrades), kept in its data, and
 * what they change about it: its stats, its lore and its abilities. The Recombobulator 3000 ({@code recombobulated})
 * and the stars ({@code upgrade_count}: a dungeon item's past five are its master stars) are the item's older data,
 * the Wither Scrolls are Necron's Blade's (see NecronsBlade), and a Silex is a level of Efficiency in its
 * enchantments. The rest are under Hypixel's own keys, as live items carry them:
 * <ul>
 *   <li>{@value #TUNERS}: Transmission Tuners, +1 block each on a Transmission ability, and on its lore's number
 *       (a live Aspect of the Void with 4 says "&a12 blocks", the Etherwarp Conduit "&a61 blocks")</li>
 *   <li>{@value #MANA_DISINTEGRATORS}: Mana Disintegrators, 1% off an ability's mana cost each and 2% off a cost
 *       that's a share of max mana (the wiki's: power orbs' 50% is 40% with 10)</li>
 *   <li>{@value #JALAPENO}: a Jalapeno Book, +5 Crit Damage and +1 Crit Chance in a deployable's buff</li>
 *   <li>{@value #WOOD_SINGULARITY}: a Wood Singularity, +25 Foraging Fortune on an axe</li>
 *   <li>{@value #POWER_SCROLL}: the Power Scroll's item id (see {@link PowerScroll})</li>
 *   <li>{@value #ENRICHMENT}: the Enrichment's stat (see {@link Enrichment})</li>
 * </ul>
 * ItemBuilder shows what they do to the lore and ItemStats counts their stats; the abilities they change read them
 * from the held item's data.
 */
public final class ItemModifiers {
    public static final String TUNERS = "tuned_transmission";
    public static final String MANA_DISINTEGRATORS = "mana_disintegrator_count";
    public static final String JALAPENO = "jalapeno_count";
    public static final String WOOD_SINGULARITY = "wood_singularity_count";
    public static final String POWER_SCROLL = "power_ability_scroll";
    public static final String ENRICHMENT = "talisman_enrichment";

    /** The Transmission Tuner's "Can be applied up to 4 times!". */
    public static final int MAX_TUNERS = 4;
    /** The Mana Disintegrator's "Can be applied up to 10 times.". */
    public static final int MAX_DISINTEGRATORS = 10;
    /** The Wood Singularity's "grants +25☘ Foraging Fortune". */
    static final double SINGULARITY_FORTUNE = 25;
    /**
     * The Jalapeno Book's buff (the wiki's Jalapeno Book, and a live Overflux Power Orb with one: "Grants +5 Crit
     * Damage. ⒥", "Grants +1 Crit Chance. ⒥"; the book's own text has no numbers).
     */
    static final Stats JALAPENO_BUFF = new Stats().set(Stat.CRIT_DAMAGE, 5).set(Stat.CRIT_CHANCE, 1);

    /** "&5• ": a deployable's buff line starts with its colour and a bullet. */
    private static final Pattern BULLET = Pattern.compile("^(?:&[0-9a-fk-or])*• ");
    /** "&a8 blocks", "&54 &7blocks": a Transmission's range in its text, with the colours between. */
    private static final Pattern RANGE = Pattern.compile("(\\d+)((?:\\s*&[0-9a-fk-or])*\\s*blocks?\\b)");
    /** "Costs 50% of max mana": a Lantern's cost in its text (see Deployables). */
    private static final Pattern SHARE = Pattern.compile("(Costs )([\\d.]+)(% of max mana)");

    private ItemModifiers() {
    }

    // Stats

    /** What its modifiers add to its stats: a Wood Singularity's Foraging Fortune and an Enrichment's stat. */
    public static Stats stats(NBTTagCompound tag) {
        Stats stats = new Stats();
        if (tag.getInt(WOOD_SINGULARITY) > 0) stats.add(Stat.FORAGING_FORTUNE, SINGULARITY_FORTUNE);
        Enrichment enrichment = Enrichment.of(tag.getString(ENRICHMENT));
        if (enrichment != null) stats.add(enrichment.stat(), enrichment.amount());
        return stats;
    }

    /**
     * The Wood Singularity's part of a stat, for its bracket: "&7Foraging Fortune: &6+91 &6(+25) &9(+12)" (a live
     * Moonglade Treecapitator's). An Enrichment has none: its stat is in the total only.
     */
    public static double woodSingularity(NBTTagCompound tag, Stat stat) {
        return stat == Stat.FORAGING_FORTUNE && tag.getInt(WOOD_SINGULARITY) > 0 ? SINGULARITY_FORTUNE : 0;
    }

    /** A dungeon item's master stars: its stars past five ("➊" to "➎"), five at most. */
    public static int masterStars(NBTTagCompound tag) {
        return Math.max(0, Math.min(ItemBuilder.starCount(tag), 10) - 5);
    }

    /** What its Jalapeno Book adds to the buff of the deployable it's on; nothing without one. */
    public static Stats jalapeno(NBTTagCompound tag) {
        return tag.getInt(JALAPENO) > 0 ? JALAPENO_BUFF.copy() : new Stats();
    }

    // Abilities

    /** How many blocks its Transmission Tuners add to a Transmission ability's range. */
    public static int tuners(NBTTagCompound tag) {
        return Math.max(0, tag.getInt(TUNERS));
    }

    /** Whether it's a Transmission ability, which Transmission Tuners reach further with (the wiki's Transmission Tuner). */
    public static boolean transmission(ItemBlock block) {
        return block.isAbility() && block.name() != null && block.name().endsWith(" Transmission");
    }

    /**
     * How many Transmission Tuners an item with these abilities takes: 4, but for the Aspect of the Leech's Weird
     * Transmission, 1 (the wiki's Transmission Tuner). 0 with no Transmission ability.
     */
    public static int maxTuners(List<ItemBlock> blocks) {
        int max = 0;
        for (ItemBlock block : blocks) {
            if (transmission(block)) max = Math.max(max, block.name().equals("Weird Transmission") ? 1 : MAX_TUNERS);
        }
        return max;
    }

    private static int disintegrators(NBTTagCompound tag) {
        return Math.max(0, Math.min(tag.getInt(MANA_DISINTEGRATORS), MAX_DISINTEGRATORS));
    }

    /** What an ability's mana cost is multiplied by: 1% less for each Mana Disintegrator. */
    public static double manaFactor(NBTTagCompound tag) {
        return 1 - 0.01 * disintegrators(tag);
    }

    /**
     * What a cost that's a share of max mana is multiplied by: 2% less for each (the wiki's Mana Disintegrator, for
     * power orbs; live Umberellas and Will-o'-wisps with 10 cost 40% of max mana too, not 50%).
     */
    public static double shareFactor(NBTTagCompound tag) {
        return 1 - 0.02 * disintegrators(tag);
    }

    // Lore

    /**
     * "&7&8Enriched with Magic Find": an enriched accessory's first line, then a blank one (live lore); none
     * without an Enrichment.
     */
    public static List<String> enrichmentLines(NBTTagCompound tag) {
        Enrichment enrichment = Enrichment.of(tag.getString(ENRICHMENT));
        return enrichment == null ? List.of() : List.of("&7&8Enriched with " + enrichment.stat().getDisplayName());
    }

    /** The item's own text with its modifiers: a Jalapeno Book's buff lines on a deployable whose buff is there (the power orbs'). */
    public static List<String> lore(NBTTagCompound tag, List<String> lore) {
        return tag.getInt(JALAPENO) > 0 ? withJalapeno(lore) : lore;
    }

    /**
     * Its abilities and bonuses, for the lore, with its modifiers: a Transmission ability's range counts its tuners,
     * each RIGHT CLICK ability's header has its Power Scroll's "&b&l⦾ " before it (live lore: an Aspect of the Void's
     * "&b&l⦾ &6Ability: Instant Transmission  &e&lRIGHT CLICK"; its SNEAK RIGHT CLICK one has none), a share of max
     * mana is its Mana Disintegrators' less, and a Jalapeno Book's lines go in a buff that's in an ability's text (a
     * Lantern's). What the abilities do reads the data itself, not these.
     */
    public static List<ItemBlock> blocks(NBTTagCompound tag, List<ItemBlock> blocks) {
        int tuners = tuners(tag);
        PowerScroll scroll = PowerScroll.of(tag.getString(POWER_SCROLL));
        double share = shareFactor(tag);
        boolean jalapeno = tag.getInt(JALAPENO) > 0;
        if (tuners == 0 && scroll == null && share == 1 && !jalapeno) return blocks;
        List<ItemBlock> out = new ArrayList<>(blocks.size());
        for (ItemBlock block : blocks) {
            List<String> text = block.text();
            if (tuners > 0 && transmission(block)) text = farther(text, tuners);
            if (share != 1) text = cheaper(text, share);
            if (jalapeno) text = withJalapeno(text);
            String header = block.header();
            // UNKNOWN: an item with two RIGHT CLICK abilities (none live has a scroll); each gets it.
            if (scroll != null && header != null && block.isAbility() && "RIGHT_CLICK".equals(block.activation())) header = scroll.colour() + "&l⦾ " + header;
            out.add(new ItemBlock(block.kind(), block.name(), header, block.activation(), text, block.mana(), block.manaPercent() * share,
                    block.cooldown(), block.soulflow(), block.healthCost(), block.vitality(), block.pieces()));
        }
        return out;
    }

    /**
     * A block's lines with its Mana Disintegrators after its mana cost: "&8Mana Cost: &b300✎&8 (&910&9ᛃ&8)" (live
     * lore). The cost is as it was (live Alert Flares and Gyrokinetic Wands with 10 show theirs; UNKNOWN: a live Fire
     * Veil Wand's shows less); a share of max mana shows less instead, with no mark (see {@link #blocks}).
     */
    public static List<String> blockLore(NBTTagCompound tag, List<String> lines) {
        int count = disintegrators(tag);
        if (count == 0) return lines;
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            boolean cost = line.startsWith("&8Mana Cost: &b") && line.endsWith("✎");
            out.add(cost ? line + "&8 (&9" + count + "&9ᛃ&8)" : line);
        }
        return out;
    }

    /** The first number of blocks in the text, {@code tuners} more. */
    static List<String> farther(List<String> text, int tuners) {
        List<String> out = new ArrayList<>(text);
        for (int i = 0; i < out.size(); i++) {
            Matcher m = RANGE.matcher(out.get(i));
            if (!m.find()) continue;
            String line = out.get(i);
            out.set(i, line.substring(0, m.start()) + (Integer.parseInt(m.group(1)) + tuners) + m.group(2) + line.substring(m.end()));
            break;
        }
        return out;
    }

    /** "Costs 50% of max mana" in the text, times {@code factor}. */
    private static List<String> cheaper(List<String> text, double factor) {
        List<String> out = new ArrayList<>(text.size());
        for (String line : text) {
            Matcher m = SHARE.matcher(line);
            out.add(m.find() ? line.substring(0, m.start()) + m.group(1) + Text.number(Double.parseDouble(m.group(2)) * factor) + m.group(3)
                    + line.substring(m.end()) : line);
        }
        return out;
    }

    /**
     * A deployable's buff with the Jalapeno Book's two lines at its end, in its bullets' colour and marked "&a⒥" (a
     * live Overflux Power Orb's: "&5• &7Grants &9+5☠ Crit Damage&7. &a⒥"): after the last bullet and the lines that
     * go on from it, before the blank line. As it was with no bullets.
     */
    static List<String> withJalapeno(List<String> lines) {
        int last = -1;
        for (int i = 0; i < lines.size(); i++) if (BULLET.matcher(lines.get(i)).lookingAt()) last = i;
        if (last < 0) return lines;
        Matcher m = BULLET.matcher(lines.get(last));
        String bullet = m.lookingAt() ? m.group() : "";
        int end = last + 1;
        while (end < lines.size() && !lines.get(end).isEmpty() && !BULLET.matcher(lines.get(end)).lookingAt()) end++;
        List<String> out = new ArrayList<>(lines);
        List<String> added = new ArrayList<>();
        for (Stat stat : List.of(Stat.CRIT_DAMAGE, Stat.CRIT_CHANCE)) {
            added.add(bullet + "&7Grants &" + stat.getLoreColor() + Text.signed(JALAPENO_BUFF.get(stat)) + stat.getSymbol() + " "
                    + stat.getDisplayName() + "&7. &a⒥");
        }
        out.addAll(end, added);
        return out;
    }

    // Silex

    /** Its Efficiency level (which each Silex raises), 0 for none. */
    public static int efficiency(NBTTagCompound tag) {
        NBTTagList enchantments = tag.getList("enchantments", 10);
        for (int i = 0; i < enchantments.size(); i++) {
            if (enchantments.get(i).getString("name").equalsIgnoreCase("efficiency")) return enchantments.get(i).getInt("lvl");
        }
        return 0;
    }

    /** Its data with Efficiency at this level, in its enchantments as /addenchantment keeps them ({name, lvl}). */
    public static NBTTagCompound withEfficiency(NBTTagCompound tag, int level) {
        NBTTagList enchantments = tag.getList("enchantments", 10);
        for (int i = enchantments.size() - 1; i >= 0; i--) {
            if (enchantments.get(i).getString("name").equalsIgnoreCase("efficiency")) enchantments.remove(i);
        }
        NBTTagCompound efficiency = new NBTTagCompound();
        efficiency.setString("name", "efficiency");
        efficiency.setShort("lvl", (short) level);
        enchantments.add(efficiency);
        tag.set("enchantments", enchantments);
        return tag;
    }
}
