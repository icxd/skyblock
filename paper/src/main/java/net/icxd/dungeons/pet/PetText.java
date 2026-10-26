package net.icxd.dungeons.pet;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.pet.PetData.Block;
import net.icxd.dungeons.pet.PetData.Formula;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetData.Variant;
import net.icxd.dungeons.skill.SkillText;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Text;

/**
 * A pet's words, as recorded (2026-10-25: the Pets menu at 00:21.3, a bought Bee as an item at 00:59.1): its name,
 * and its lore in the Pets menu or as an item. The lore is its type line, its stats (with its held item's, in the
 * stats' order), its ability text at its level (wrapped at 188 pixels, as every recorded paragraph is), its held item,
 * the Pet Candy it's had, its progress, and what a click does. No server needed past the item registry.
 */
public final class PetText {
    /** How wide its ability lines get (build_pets.py's WIDTH: every recorded one wraps the same at it). */
    static final int WIDTH = 188;
    static final String SUMMON = "&eLeft-click to summon!";
    static final String DESPAWN = "&cClick to despawn!";
    static final String FAVORITE = "&eShift Left-click to toggle as favorite!";
    static final String CONVERT = "&eRight-click to convert to an item!";
    static final String ADD = "&eRight-click to add this pet to your pet menu!";
    static final String KAT = "&8Can be upgraded at Kat in The Hub!";
    /** Before a favourite's name in the menu (recorded: "&e⭐ &7[Lvl 100] &dEnderman&5 ✦"). */
    static final String FAVORITE_MARK = "&e⭐ ";
    static final String MAX_LEVEL = "&b&lMAX LEVEL";
    private static final Pattern NUMBER = Pattern.compile("\\{(\\d+)}");
    private static final List<ThreadLocal<DecimalFormat>> FORMATS = new ArrayList<>();

    static {
        for (int d = 0; d <= 6; d++) {
            String pattern = "#,##0" + (d > 0 ? "." + "#".repeat(d) : "");
            FORMATS.add(ThreadLocal.withInitial(() -> {
                DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT));
                // DecimalFormat's own rounding, half-even on the double's value: NEU has the Zombie's 26.25 as 26.2.
                format.setRoundingMode(RoundingMode.HALF_EVEN);
                return format;
            }));
        }
    }

    /** Where the pet's lore is: the Pets menu (what a click does), or an item (to add it to the menu). */
    public enum Where {
        MENU, ITEM
    }

    private PetText() {
    }

    /** "&6Lion": its name in its rarity's colour. */
    public static String colored(OwnedPet pet, PetData data) {
        Pet kind = data.pet(pet.type());
        Rarity tier = Pets.tier(pet, data);
        return "&" + tier.getCode() + (kind == null ? pet.type() : kind.name(tier));
    }

    /** "&7[Lvl 94] &dGuardian"; in the menu, "&e⭐ " before a favourite's. */
    public static String name(OwnedPet pet, PetData data, Where where) {
        return (where == Where.MENU && pet.favorite() ? FAVORITE_MARK : "") + "&7[Lvl " + Pets.level(pet, data) + "] " + colored(pet, data);
    }

    /** "&8[&7Lv100&8] &6Lion" over a summoned pet; "&6Bea's Bee" over someone else's (recorded, Bea's in the Hub). */
    public static String floating(OwnedPet pet, PetData data, String owner) {
        Pet kind = data.pet(pet.type());
        Rarity tier = Pets.tier(pet, data);
        String name = kind == null ? pet.type() : kind.name(tier);
        return "&8[&7Lv" + Pets.level(pet, data) + "&8] &" + tier.getCode() + (owner == null ? "" : owner + "'s ") + name;
    }

    /** A number of its text at a level, with its decimals: "6.6", "1.16", "115". */
    static String number(Formula formula, int level) {
        int decimals = formula.decimals() < 0 ? 2 : Math.min(6, formula.decimals());
        return FORMATS.get(decimals).get().format(formula.at(level));
    }

    /** The lore, as recorded in the Pets menu (with what a click does; {@code active}: it's the one out) or as an item. */
    public static List<String> lore(OwnedPet pet, PetData data, Where where, boolean active) {
        Pet kind = data.pet(pet.type());
        Rarity tier = Pets.tier(pet, data);
        Variant variant = kind == null ? null : kind.variant(tier);
        int level = Pets.level(pet, data);
        List<List<String>> sections = new ArrayList<>();
        sections.add(List.of("&8" + (kind == null || kind.kind() == null ? "Pet" : kind.kind())));
        boolean hatched = kind == null || level >= kind.statsFrom();
        sections.add(statLines(hatched ? Pets.withItem(pet, data) : HeldItems.stats(pet.heldItem())));
        if (variant != null && hatched) for (Block block : variant.blocks()) sections.add(blockLines(block, variant, level));
        sections.add(heldItemLines(pet.heldItem()));
        if (pet.candyUsed() > 0) sections.add(List.of("&a(" + pet.candyUsed() + "/10) Pet Candy Used"));
        sections.add(progress(pet, data));
        boolean kat = variant != null && variant.kat();
        if (where == Where.MENU) {
            if (kat) sections.add(List.of(KAT));
            sections.add(List.of(active ? DESPAWN : SUMMON, FAVORITE, CONVERT));
        } else {
            sections.add(List.of(ADD));
            List<String> footer = new ArrayList<>();
            if (kat) footer.add(KAT);
            footer.add(rarityLine(tier));
            sections.add(footer);
        }
        List<String> lore = new ArrayList<>();
        for (List<String> section : sections) {
            if (section.isEmpty()) continue;
            if (!lore.isEmpty()) lore.add("");
            lore.addAll(section);
        }
        return lore;
    }

    /** "&7Crit Damage: &9+75%": each stat in the stats' order, its value in its lore colour. */
    static List<String> statLines(Stats stats) {
        List<String> lines = new ArrayList<>();
        for (Stat stat : Stat.values()) {
            if (!stats.has(stat)) continue;
            lines.add("&7" + stat.getDisplayName() + ": &" + stat.getLoreColor() + Text.signed(stats.get(stat)) + stat.getUnit());
        }
        return lines;
    }

    /** An ability's name and its paragraphs, numbers in, wrapped as Hypixel wraps them. */
    static List<String> blockLines(Block block, Variant variant, int level) {
        List<String> lines = new ArrayList<>();
        if (block.name() != null) lines.add(block.name());
        for (String paragraph : block.text()) lines.addAll(wrapped(numbers(paragraph, variant, level)));
        return lines;
    }

    static String numbers(String paragraph, Variant variant, int level) {
        Matcher m = NUMBER.matcher(paragraph);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            String value = n < variant.numbers().size() ? number(variant.numbers().get(n), level) : "?";
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** A paragraph's lines, a list line's leading spaces kept (" &b+1.2✎ Intelligence": wrapping drops them). */
    static List<String> wrapped(String paragraph) {
        String text = paragraph.stripLeading();
        List<String> lines = new ArrayList<>(Text.wrap(text, WIDTH));
        lines.set(0, paragraph.substring(0, paragraph.length() - text.length()) + lines.get(0));
        return lines;
    }

    /** "&6Held Item: &5Dwarf Turtle Shelmet" and what it does, as recorded; nothing for none. */
    static List<String> heldItemLines(String id) {
        if (id == null) return List.of();
        SkyBlockItem item = HeldItems.item(id);
        List<String> lines = new ArrayList<>();
        lines.add("&6Held Item: " + (item == null ? "&f" + id : "&" + item.rarity().getCode() + item.name()));
        lines.addAll(HeldItems.description(item));
        return lines;
    }

    /**
     * "&7Progress to Level 95: &e2.2%" over the skills' bar ("&e28,101&6/&e1.3M"), or at its most "&b&lMAX LEVEL"
     * over all its XP ("&8▸ 61,377,616 XP"), as recorded.
     */
    static List<String> progress(OwnedPet pet, PetData data) {
        Pet kind = data.pet(pet.type());
        int level = Pets.level(pet, data);
        if (kind != null && level >= data.maxLevel(kind)) return List.of(MAX_LEVEL, "&8▸ " + SkillText.number(Math.floor(pet.exp())) + " XP");
        Rarity tier = Pets.tier(pet, data);
        double need = data.xpFor(kind, tier, level + 1);
        double into = data.xpIntoLevel(kind, tier, pet.exp());
        double fraction = need <= 0 ? 0 : Math.min(1, into / need);
        return List.of("&7Progress to Level " + (level + 1) + ": &e" + SkillText.percent(fraction) + "%", SkillText.bar(fraction, into, need));
    }

    /** "&f&lCOMMON PET". */
    static String rarityLine(Rarity rarity) {
        return "&" + rarity.getCode() + "&l" + rarity.name().replace('_', ' ') + " PET";
    }

    /**
     * The Pets item's lines (in the Pets menu and the SkyBlock Menu): what it's for, and the summoned pet's progress
     * ("&7Selected pet: &6Lion", recorded), or "&cNone" (the wiki's menu).
     */
    static List<String> summary(OwnedPet active, PetData data) {
        List<String> lore = new ArrayList<>(List.of("&7View and manage all of your Pets.", "", "&7Level up your pets faster by gaining",
                "&7XP in their favorite skill!", "", "&7Selected pet: " + (active == null ? "&cNone" : colored(active, data))));
        if (active != null) {
            lore.add("");
            lore.addAll(progress(active, data));
        }
        lore.add("");
        lore.add("&8Also accessible via /pets.");
        return lore;
    }
}
