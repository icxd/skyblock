package net.icxd.dungeons.item.enchanting;

import lombok.Getter;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class EnchantmentType {
    private static final HashMap<String, EnchantmentType> ENCHANTMENT_TYPE_CACHE = new HashMap<>();

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Sword
    public static final EnchantmentType BANE_OF_ARTHROPODS = new EnchantmentType("Bane of Arthropods", "bane_of_arthropods", 7, SpecificItemType.SWORD);
    public static final EnchantmentType CHAMPION = new EnchantmentType("Champion", "champion", 10, SpecificItemType.SWORD);
    public static final EnchantmentType CLEAVE = new EnchantmentType("Cleave", "cleave", 6, SpecificItemType.SWORD);
    public static final EnchantmentType CRITICAL = new EnchantmentType("Critical", "critical", 7, SpecificItemType.SWORD);
    public static final EnchantmentType CUBISM = new EnchantmentType("Cubism", "cubism", 6, SpecificItemType.SWORD);
    public static final EnchantmentType DRAGON_HUNTER = new EnchantmentType("Dragon Hunter", "dragon_hunter", 5, SpecificItemType.SWORD);
    public static final EnchantmentType ENDER_SLAYER = new EnchantmentType("Ender Slayer", "ender_slayer", 7, SpecificItemType.SWORD);
    public static final EnchantmentType EXECUTE = new EnchantmentType("Execute", "execute", 6, SpecificItemType.SWORD);
    public static final EnchantmentType FIRE_ASPECT = new EnchantmentType("Fire Aspect", "fire_aspect", 3, SpecificItemType.SWORD);
    public static final EnchantmentType FIRST_STRIKE = new EnchantmentType("First Strike", "first_strike", 5, SpecificItemType.SWORD);
    public static final EnchantmentType GIANT_KILLER = new EnchantmentType("Giant Killer", "giant_killer", 7, SpecificItemType.SWORD);
    public static final EnchantmentType LETHALITY = new EnchantmentType("Lethality", "lethality", 6, SpecificItemType.SWORD);
    public static final EnchantmentType LIFE_STEAL = new EnchantmentType("Life Steal", "life_steal", 5, SpecificItemType.SWORD);
    public static final EnchantmentType LOOTING = new EnchantmentType("Looting", "looting", 5, SpecificItemType.SWORD);
    public static final EnchantmentType LUCK = new EnchantmentType("Luck", "luck", 7, SpecificItemType.SWORD);
    public static final EnchantmentType MANA_STEAL = new EnchantmentType("Mana Steal", "mana_steal", 3, SpecificItemType.SWORD);
    public static final EnchantmentType PROSECUTE = new EnchantmentType("Prosecute", "prosecute", 6, SpecificItemType.SWORD);
    public static final EnchantmentType SCAVENGER = new EnchantmentType("Scavenger", "scavenger", 5, SpecificItemType.SWORD);
    public static final EnchantmentType SHARPNESS = new EnchantmentType("Sharpness", "sharpness", 7, SpecificItemType.SWORD);
    public static final EnchantmentType SMITE = new EnchantmentType("Smite", "smite", 7, SpecificItemType.SWORD);
    public static final EnchantmentType SMOLDERING = new EnchantmentType("Smoldering", "smoldering", 5, SpecificItemType.SWORD);
    public static final EnchantmentType SYPHON = new EnchantmentType("Syphon", "syphon", 5, SpecificItemType.SWORD);
    public static final EnchantmentType THUNDERBOLT = new EnchantmentType("Thunderbolt", "thunderbolt", 6, SpecificItemType.SWORD);
    public static final EnchantmentType THUNDERLORD = new EnchantmentType("Thunderlord", "thunderlord", 6, SpecificItemType.SWORD);
    public static final EnchantmentType TITAN_KILLER = new EnchantmentType("Titan Killer", "titan_killer", 7, SpecificItemType.SWORD);
    public static final EnchantmentType TRIPLE_STRIKE = new EnchantmentType("Triple-Strike", "triple_strike", 5, SpecificItemType.SWORD);
    public static final EnchantmentType VAMPIRISM = new EnchantmentType("Vampirism", "vampirism", 6, SpecificItemType.SWORD);
    public static final EnchantmentType VENOMOUS = new EnchantmentType("Venomous", "venomous", 6, SpecificItemType.SWORD);
    public static final EnchantmentType VICIOUS = new EnchantmentType("Vicious", "vicious", 5, SpecificItemType.SWORD);
    // Pickaxe
    public static final EnchantmentType FORTUNE = new EnchantmentType("Fortune", "fortune", 4, SpecificItemType.PICKAXE);
    public static final EnchantmentType PRISTINE = new EnchantmentType("Pristine", "pristine", 5, SpecificItemType.PICKAXE);
    // Axe
    // Shovel
    // Hoe
    public static final EnchantmentType CULTIVATING = new EnchantmentType("Cultivating", "cultivating", 10, SpecificItemType.HOE);
    public static final EnchantmentType DELICATE = new EnchantmentType("Delicate", "delicate", 5, SpecificItemType.HOE);
    public static final EnchantmentType HARVESTING = new EnchantmentType("Harvesting", "harvesting", 6, SpecificItemType.HOE);
    public static final EnchantmentType REPLENISH = new EnchantmentType("Replenish", "replenish", 1, SpecificItemType.HOE);
    // Bow
    public static final EnchantmentType CHANCE = new EnchantmentType("Chance", "chance", 5, SpecificItemType.BOW);
    public static final EnchantmentType INFINITE_QUIVER = new EnchantmentType("Infinite Quiver", "infinite_quiver", 10, SpecificItemType.BOW);
    public static final EnchantmentType OVERLOAD = new EnchantmentType("Overload", "overload", 5, SpecificItemType.BOW);
    public static final EnchantmentType POWER = new EnchantmentType("Power", "power", 7, SpecificItemType.BOW);
    public static final EnchantmentType SNIPE = new EnchantmentType("Snipe", "snipe", 4, SpecificItemType.BOW);
    // Fishing Rod
    public static final EnchantmentType ANGLER = new EnchantmentType("Angler", "angler", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType BLESSING = new EnchantmentType("Blessing", "blessing", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType CASTER = new EnchantmentType("Caster", "caster", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType CHARM = new EnchantmentType("Charm", "charm", 5, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType CORRUPTION = new EnchantmentType("Corruption", "corruption", 5, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType EXPERTISE = new EnchantmentType("Expertise", "expertise", 10, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType FRAIL = new EnchantmentType("Frail", "frail", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType LUCK_OF_THE_SEA = new EnchantmentType("Luck of the Sea", "luck_of_the_sea", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType LURE = new EnchantmentType("Lure", "lure", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType MAGNET = new EnchantmentType("Magnet", "magnet", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType PISCARY = new EnchantmentType("Piscary", "piscary", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType SPIKED_HOOK = new EnchantmentType("Spiked Hook", "spiked_hook", 6, SpecificItemType.FISHING_ROD);
    // Helmet
    public static final EnchantmentType BIG_BRAIN = new EnchantmentType("Big Brain", "big_brain", 5, SpecificItemType.HELMET);
    public static final EnchantmentType HECATOMB = new EnchantmentType("Hecatomb", "hecatomb", 10, SpecificItemType.HELMET);
    // Chestplate
    public static final EnchantmentType COUNTER_STRIKE = new EnchantmentType("Counter-Strike", "counter_strike", 5, SpecificItemType.CHESTPLATE);
    public static final EnchantmentType TRUE_PROTECTION = new EnchantmentType("True Protection", "true_protection", 1, SpecificItemType.CHESTPLATE);
    // Leggings
    public static final EnchantmentType SMARTY_PANTS = new EnchantmentType("Smarty Pants", "smarty_pants", 5, SpecificItemType.LEGGINGS);
    // Boots
    public static final EnchantmentType FEATHER_FALLING = new EnchantmentType("Feather Falling", "feather_falling", 10, SpecificItemType.BOOTS);
    public static final EnchantmentType SUGAR_RUSH = new EnchantmentType("Sugar Rush", "sugar_rush", 3, SpecificItemType.BOOTS);
    // Armor
    public static final EnchantmentType BLAST_PROTECTION = new EnchantmentType("Blast Protection", "blast_protection", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType FEROCIOUS_MANA = new EnchantmentType("Ferocious Mana", "ferocious_mana", 10, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType FIRE_PROTECTION = new EnchantmentType("Fire Protection", "fire_protection", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType GROWTH = new EnchantmentType("Growth", "growth", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType HARDENED_MANA = new EnchantmentType("Hardened Mana", "hardened_mana", 10, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType MANA_VAMPIRE = new EnchantmentType("Mana Vampire", "mana_vampire", 10, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType PROJECTILE_PROTECTION = new EnchantmentType("Projectile Protection", "projectile_protection", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType PROTECTION = new EnchantmentType("Protection", "protection", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType REJUVENATE = new EnchantmentType("Rejuvenate", "rejuvenate", 5, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType RESPITE = new EnchantmentType("Respite", "respite", 5, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType STRONG_MANA = new EnchantmentType("Strong Mana", "strong_mana", 10, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    // Tools
    public static final EnchantmentType COMPACT = new EnchantmentType("Compact", "compact", 10, SpecificItemType.PICKAXE, SpecificItemType.AXE, SpecificItemType.SPADE);
    public static final EnchantmentType EXPERIENCE = new EnchantmentType("Experience", "experience", 5, SpecificItemType.PICKAXE, SpecificItemType.AXE, SpecificItemType.SPADE, SpecificItemType.SWORD);
    // Other
    public static final EnchantmentType CAYENNE = new EnchantmentType("Cayenne", "cayenne", 5);
    public static final EnchantmentType PROSPERITY = new EnchantmentType("Prosperity", "prosperity", 5);
    public static final EnchantmentType TABASCO = new EnchantmentType("Tabasco", "tabasco", 3, SpecificItemType.SWORD, SpecificItemType.BOW, SpecificItemType.AXE);

    // ULTIMATE ENCHANTMENTS
    public static final EnchantmentType ONE_FOR_ALL = new EnchantmentType("One For All", "one_for_all", true, 1, SpecificItemType.SWORD, SpecificItemType.LONGSWORD);

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final String name;
    @Getter
    private final String namespace;
    private final boolean ultimate;
    private final int maxLevel;
    private final List<SpecificItemType> compatibleTypes;

    /**
     * {@code name} is only used if Hypixel's text (enchantments.json) has none for it; Hypixel's name
     * wins (some enchants were renamed, e.g. Syphon is now Drain).
     */
    public EnchantmentType(String name, String namespace, boolean ultimate, int maxLevel, SpecificItemType... compatibleTypes) {
        EnchantmentTexts.Entry texts = EnchantmentTexts.get(namespace);
        this.name = texts != null && texts.name() != null ? texts.name() : name;
        this.namespace = namespace;
        this.ultimate = ultimate;
        this.maxLevel = maxLevel;
        this.compatibleTypes = new ArrayList<>(Arrays.asList(compatibleTypes));
        ENCHANTMENT_TYPE_CACHE.put(namespace, this);
    }

    public EnchantmentType(String name, String namespace, int maxLevel, SpecificItemType... compatibleTypes) {
        this(name, namespace, false, maxLevel, compatibleTypes);
    }

    public static EnchantmentType getByNamespace(String namespace) {
        return ENCHANTMENT_TYPE_CACHE.get(namespace.toLowerCase());
    }

    public static java.util.Collection<EnchantmentType> all() {
        return java.util.Collections.unmodifiableCollection(ENCHANTMENT_TYPE_CACHE.values());
    }

    /** Hypixel's description at a level, as one paragraph with & colours; null if there's none for it. */
    public String getDescription(int level) {
        EnchantmentTexts.Entry texts = EnchantmentTexts.get(namespace);
        return texts == null ? null : texts.levels().get(String.valueOf(level));
    }

    /**
     * The stat it grants at a level, read from its description ("Grants +75 ❤ Health."); nothing if it
     * grants none, or only sometimes ("against explosions", "while out of combat").
     */
    public Stats getStats(int level) {
        Stats stats = new Stats();
        String text = getDescription(level);
        if (text == null) return stats;
        Matcher m = GRANTS.matcher(text);
        if (!m.find()) return stats;
        for (Stat stat : Stat.values()) {
            if (stat.getDisplayName().equals(m.group(2))) return stats.set(stat, Double.parseDouble(m.group(1)));
        }
        return stats;
    }

    /** "&7Grants &a+75 &c❤ Health&7." (or "…&7, which …"), and nothing more to it. */
    private static final Pattern GRANTS = Pattern.compile("^&7Grants &.\\+([\\d.]+) ?(?:&.)?\\S? ?([A-Z][a-zA-Z]*(?: [A-Z][a-zA-Z]*)*)&7(?:\\.$|, which )");

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EnchantmentType)) return false;
        return ((EnchantmentType) o).namespace.equals(namespace);
    }

    @Override
    public int hashCode() {
        return namespace.hashCode();
    }

}
