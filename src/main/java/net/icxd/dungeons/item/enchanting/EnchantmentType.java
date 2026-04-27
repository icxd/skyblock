package net.icxd.dungeons.item.enchanting;

import lombok.Getter;
import net.icxd.dungeons.item.enums.SpecificItemType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Getter
public class EnchantmentType {
    private static final HashMap<String, EnchantmentType> ENCHANTMENT_TYPE_CACHE = new HashMap<>();

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Sword
    public static final EnchantmentType BANE_OF_ARTHROPODS = new EnchantmentType("Bane of Arthropods", "bane_of_arthropods", "&7Increases damage dealt to Spiders, Case Spiders, and Silverfish by &a%s%&7.", 7, SpecificItemType.SWORD);
    public static final EnchantmentType CHAMPION = new EnchantmentType("Champion", "champion", "&7Gain &a%s%&7 extra Combat XP. The 2nd hit on a mod grants &6+%s coins&7 & &3+%s &7exp orbs.", 10, SpecificItemType.SWORD);
    public static final EnchantmentType CLEAVE = new EnchantmentType("Cleave", "cleave", "&7Deals &a%s%&7 of your damage dealt to other monsters within &a%s &7blocks of the target.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType CRITICAL = new EnchantmentType("Critical", "critical", "&7Increases your &9☠ Crit Damage&7 by &a%s%&7.", 7, SpecificItemType.SWORD);
    public static final EnchantmentType CUBISM = new EnchantmentType("Cubism", "cubism", "&7Increases damage dealt to Slimes, Creepers, and Magma Cubes by &a%s%&7.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType DRAGON_HUNTER = new EnchantmentType("Dragon Hunter", "dragon_hunter", "&7Increases damage dealt to Ender Dragons by &a%s%&7.", 5, SpecificItemType.SWORD);
    public static final EnchantmentType ENDER_SLAYER = new EnchantmentType("Ender Slayer", "ender_slayer", "&7Increases damage dealt to Endermen and Ender Dragons by &a%s%&7.",7, SpecificItemType.SWORD);
    public static final EnchantmentType EXECUTE = new EnchantmentType("Execute", "execute", "&7Increases damage dealt by &a%s%&7 for each percent of health missing on your target.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType FIRE_ASPECT = new EnchantmentType("Fire Aspect", "fire_aspect", "&7Ignites your enemies for &a%ss&7, dealing &a%s% &7of your damage per second.", 3, SpecificItemType.SWORD);
    public static final EnchantmentType FIRST_STRIKE = new EnchantmentType("First Strike", "first_strike", "&7Increases melee damage dealt by &a%s% &7for the first hit on a mob.", 5, SpecificItemType.SWORD);
    public static final EnchantmentType GIANT_KILLER = new EnchantmentType("Giant Killer", "giant_killer", "&7Increases damage dealt by &a%s%&7 for each percent of extra health that your target has above you up to &a%s%&7.", 7, SpecificItemType.SWORD);
    public static final EnchantmentType LETHALITY = new EnchantmentType("Lethality", "lethality", "&7Reduces the &a❈ Defense &7of your target by &a+%s%&7 for &64s&7 each time you hit them with melee. Stacks up to &a4 &7times.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType LIFE_STEAL = new EnchantmentType("Life Steal", "life_steal", "&7Heals you for &a%s%&7 of your max health each time you hit a mob.", 5, SpecificItemType.SWORD);
    public static final EnchantmentType LOOTING = new EnchantmentType("Looting", "looting", "&7Increases the chance of a Monster dropping an item by &a%s%&7.", 5, SpecificItemType.SWORD);
    public static final EnchantmentType LUCK = new EnchantmentType("Luck", "luck", "&7Increases the chance of a Monster dropping their armor by &a%s%&7.", 7, SpecificItemType.SWORD);
    public static final EnchantmentType MANA_STEAL = new EnchantmentType("Mana Steal", "mana_steal", "&7Regain &b%s% &7of your mana on hit.", 3, SpecificItemType.SWORD);
    public static final EnchantmentType PROSECUTE = new EnchantmentType("Prosecute", "prosecute", "&7Increases damage dealt by &a%s%&7 for each percent of health your target has.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType SCAVENGER = new EnchantmentType("Scavenger", "scavenger", "&7Scavenge &6%s Coins &7per monster level on kill.", 5, SpecificItemType.SWORD);
    public static final EnchantmentType SHARPNESS = new EnchantmentType("Sharpness", "sharpness", "&7Increases melee damage dealt by &a%s%&7.", 7, SpecificItemType.SWORD);
    public static final EnchantmentType SMITE = new EnchantmentType("Smite", "smite", "&7Increases damage dealt to Zombies, Zombie Pigmen, Withers, and Skeletons by &a%s%&7.", 7, SpecificItemType.SWORD);
    public static final EnchantmentType SMLODERING = new EnchantmentType("Smoldering", "smoldering", "&7Increases damage dealt to Blazes by &a%s%&7.", 5, SpecificItemType.SWORD);
    public static final EnchantmentType SYPHON = new EnchantmentType("Syphon", "syphon", "&7Heals for &a%s%&7 of your max health per &9100 ☠ Crit Damage&7 you deal per hit, up to &9100 ☠ Crit Damage&7.", 5, SpecificItemType.SWORD);
    public static final EnchantmentType THUNDERBOLT = new EnchantmentType("Thunderbolt", "thunderbolt", "&7Every &c3 &7hits on a monster, strike &elightning&7, dealing &a%s%&7 of the hit's damage to up to 10 monsters within 2 blocks.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType THUNDERLORD = new EnchantmentType("Thunderlord", "thunderlord", "&7Every &c3 &7hits on a monster, strike &elightning&7, dealing &a%s%&7 of the hit's damage.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType TITAN_KILLER = new EnchantmentType("Titan Killer", "titan_killer", "&7Increases damage dealt by &a%s%&7 for every 100 defense your target has up to &a80%&7.", 7, SpecificItemType.SWORD);
    public static final EnchantmentType TRIPLE_STRIKE = new EnchantmentType("Triple-Strike", "triple_strike", "&7Increases melee damage dealt by &a%s%&7 for the first three hits on a mob.", 5, SpecificItemType.SWORD);
    public static final EnchantmentType VAMPIRISM = new EnchantmentType("Vampirism", "vampirism", "&7Heals for &a%s%&7 of your missing health whenever you kil an enemy.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType VENOMOUS = new EnchantmentType("Venomous", "venomous", "&7Reduces the target's walk speed by &a%s% &7and deals &2+%s%&7 of your damage per second per hit, stacking globally up to &240 hits. &7Lasts &65s&7.", 6, SpecificItemType.SWORD);
    public static final EnchantmentType VICIOUS = new EnchantmentType("Vicious", "vicious", "&7Grants &c+%s⫽ Ferocity&7.", 5, SpecificItemType.SWORD);
    // Pickaxe
    public static final EnchantmentType FORTUNE = new EnchantmentType("Fortune", "fortune", "&7Grants &6+%s☘ Mining Fortune&7, which increases your chance for multiple drops.", 4, SpecificItemType.PICKAXE);
    public static final EnchantmentType PRISTINE = new EnchantmentType("Pristine", "pristine", "&7Grants &5+%s ✧ Pristine&7, which increases the chance to improve the quality of dropped &dGemstones&7.", 5, SpecificItemType.PICKAXE);
    // Axe
    // Shovel
    // Hoe
    public static final EnchantmentType CULTIVATING = new EnchantmentType("Cultivating", "cultivating", "&7Gain &3%s☯ Farming Wisdom &7and &6+%s☘ Farming Fortune&7.", 10, SpecificItemType.HOE);
    public static final EnchantmentType DELICATE = new EnchantmentType("Delicate", "delicate", "&7Avoids breaking stems and baby crops.", 5, SpecificItemType.HOE);
    public static final EnchantmentType HARVESTING = new EnchantmentType("Harvesting", "harvesting", "&7Grants &a+%s&6☘ Farming Fortune&7, which increases your chance for multiple crops.", 6, SpecificItemType.HOE);
    public static final EnchantmentType REPLENISH = new EnchantmentType("Replenish", "replenish", "&7Upon breaking crops, nether wart, or cocoa, automatically replant from material in your inventory.", 1, SpecificItemType.HOE);
    // Bow
    public static final EnchantmentType CHANCE = new EnchantmentType("Chance", "chance", "&7Increases the chance of a Monster dropping an item by &a%s%&7.", 5, SpecificItemType.BOW);
    public static final EnchantmentType INFINITE_QUIVER = new EnchantmentType("Infinite Quiver", "infinite_quiver", "&7Saves arrows &a%s%&7 of the time when you fire your bow. Disabled while sneaking.", 10, SpecificItemType.BOW);
    public static final EnchantmentType OVERLOAD = new EnchantmentType("Overload", "overload", "&7Increases your &9☠ Crit Damage&7 by &a%s%&7 and &9☣ Crit Chance &7by &a%s%&7. Having a Critical Chance about &9100%&7 grants a chance to perform a Mega Critical Hit dealing &950% &7extra damage.", 5, SpecificItemType.BOW);
    public static final EnchantmentType POWER = new EnchantmentType("Power", "power", "&7Increases bow damage by &a%s%&7.", 7, SpecificItemType.BOW);
    public static final EnchantmentType SNIPE = new EnchantmentType("Snipe", "snipe", "&7Arrows deal &a+%s%&7 damage for every &a10 &7blocks traveled.", 4, SpecificItemType.BOW);
    // Fishing Rod
    public static final EnchantmentType ANGLER = new EnchantmentType("Angler", "angler", "&7Increases chance to catch sea creatures by &a%s%&7.", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType BLESSING = new EnchantmentType("Blessing", "blessing", "&a%s%&7 chance to get double drops when fishing.", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType CASTER = new EnchantmentType("Caster", "caster", "&a%s%&7 chance to not consume bait.", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType CHARM = new EnchantmentType("Charm", "charm", "&7Increases the chance to receive higher-tiered Trophy Fish by &a%s%&7.", 5, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType CORRUPTION = new EnchantmentType("Corruption", "corruption", "&7Gain a &a%s%&7 chance to spawn a Corrupted Sea Creature.", 5, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType EXPERTISE = new EnchantmentType("Expertise", "expertise", "&7Grants &3%sα Sea Creature Chance&7 and &3+%s☯ Fishing Wisdom &7when killing Sea Creatures.", 10, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType FRAIL = new EnchantmentType("Frail", "frail", "&7Sea creatures start with &a%s% &7reduced health.", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType LUCK_OF_THE_SEA = new EnchantmentType("Luck of the Sea", "luck_of_the_sea", "&7Increases the chance of fishing treasure by &a%s%&7.", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType LURE = new EnchantmentType("Lure", "lure", "&7Shortens the maximum time it takes to catch something by &a%s%&7.", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType MAGNET = new EnchantmentType("Magnet", "magnet", "&7Grants &a%s &7additional experience orbs every time you successfully catch a fish.", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType PISCARY = new EnchantmentType("Piscary", "piscary", "&7Grants &b+%s☂ Fishing Speed&7.", 6, SpecificItemType.FISHING_ROD);
    public static final EnchantmentType SPIKED_HOOK = new EnchantmentType("Spiked Hook", "spiked_hook", "&7Fishing rod deals &a30% &7more damage to monsters.", 6, SpecificItemType.FISHING_ROD);
    // Helmet
    public static final EnchantmentType BIG_BRAIN = new EnchantmentType("Big Brain", "big_brain", "&7Grants &b+%s✎ Intelligence&7.", 5, SpecificItemType.HELMET);
    public static final EnchantmentType HECATOMB = new EnchantmentType("Hecatomb", "hecatomb", "&7Gain &a+%s% &cCatacombs &7XP & &a+%s% &3Class &7XP, doubled on &b&lS+&7 runs. Grants &c+%s❤&7 per 10 &cCatacombs &7levels.", 10, SpecificItemType.HELMET);
    // Chestplate
    public static final EnchantmentType COUNTER_STRIKE = new EnchantmentType("Counter-Strike", "counter_strike", "&7Gain &a+%s❈ Defense &7for &a7s&7 on the first hit from an enemy.", 5, SpecificItemType.CHESTPLATE);
    public static final EnchantmentType TRUE_PROTECTION = new EnchantmentType("True Protection", "true_protection", "&7Grants &f+%s❂ True Defense&7.", 1, SpecificItemType.CHESTPLATE);
    // Leggings
    public static final EnchantmentType SMARTY_PANTS = new EnchantmentType("Smarty Pants", "smarty_pants", "&7Grants &b+%s✎ Intelligence&7.", 5, SpecificItemType.LEGGINGS);
    // Boots
    public static final EnchantmentType FEATHER_FALLING = new EnchantmentType("Feather Falling", "feather_falling", "&7Increases how high you can fall before taking fall damage by &a%s &7 and reduces fall damage by &a%s%&7.", 10, SpecificItemType.BOOTS);
    public static final EnchantmentType SUGAR_RUSH = new EnchantmentType("Sugar Rush", "sugar_rush", "&7Grants &a+%s&f ✦ Speed&7.", 3, SpecificItemType.BOOTS);
    // Armor
    public static final EnchantmentType BLAST_PROTECTION = new EnchantmentType("Blast Protection", "blast_protection", "&7Grants &a+%s❈ Defense&7 against explosions.", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType FEROCIOUS_MANA = new EnchantmentType("Ferocious Mana", "ferocious_mana", "&7Gain &a%s%&7 of mana used near you as &c⫽ Ferocity&7 for 10 seconds, capped at 50 &a⫽ Ferocity&7.", 10, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType FIRE_PROTECTION = new EnchantmentType("Fire Protection", "fire_protection", "&7Grants &f+%s❂ True Defense&7 against fire and lava.", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType GROWTH = new EnchantmentType("Growth", "growth", "&7Grants &a+%s &c❤ Health&7.", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType HARDENED_MANA = new EnchantmentType("Hardened Mana", "hardened_mana", "&7Gain &a%s%&7 of mana used near you as &a❈ Defense &7for 10 seconds, capped at 400 &a❈ Defense&7.", 10, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType MANA_VAMPIRE = new EnchantmentType("Mana Vampire", "mana_vampire", "&7Heal for &a%s%&7 of mana used near you.", 10, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType PROJECTILE_PROTECTION = new EnchantmentType("Projectile Protection", "projectile_protection", "&7Grants &a+%s❈ Defense&7 against projectiles.", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType PROTECTION = new EnchantmentType("Protection", "protection", "&7Grants &a+%s❈ Defense&7.", 7, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType REJUVENATE = new EnchantmentType("Rejuvenate", "rejuvenate", "&7Gain &c+%s❣ Health Regen&7.", 5, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType RESPITE = new EnchantmentType("Respite", "respite", "&7Grants &c+%s❣ Health Regen&7 while out of combat.", 5, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    public static final EnchantmentType STRONG_MANA = new EnchantmentType("Strong Mana", "strong_mana", "&7Gain &a%s%&7 of mana used near you as &c❁ Strength &7for 10 seconds, capped at 100 &a❁ Strength&7.", 10, SpecificItemType.HELMET, SpecificItemType.CHESTPLATE, SpecificItemType.LEGGINGS, SpecificItemType.BOOTS);
    // Tools
    public static final EnchantmentType COMPACT = new EnchantmentType("Compact", "compact", "&7Gain &3%s☯ Mining Wisdom&7 per level with a &a%s%&7 chance to drop an enchanted item.", 10, SpecificItemType.PICKAXE, SpecificItemType.AXE, SpecificItemType.SPADE);
    public static final EnchantmentType EXPERIENCE = new EnchantmentType("Experience", "experience", "&Grants a &a%s%&7 chance for mobs or ores to drop double experience.", 5, SpecificItemType.PICKAXE, SpecificItemType.AXE, SpecificItemType.SPADE, SpecificItemType.SWORD);
    // Other
    public static final EnchantmentType CAYENNE = new EnchantmentType("Cayenne", "cayenne", "&7Grants &c+%s❤ Health&7 pet digit in your &7Magical Power&7.", 5);
    public static final EnchantmentType PROSPERITY = new EnchantmentType("Prosperity", "prosperity", "&7Grants &a+%s &c❤ Health&7.", 5);
    public static final EnchantmentType TABASCO = new EnchantmentType("Tabasco", "tabasco", "&7Grants &f+%s&7 weapon damage if you don't have a &5Dragon &7pet equipped.", 3, SpecificItemType.SWORD, SpecificItemType.BOW, SpecificItemType.AXE);

    // ULTIMATE ENCHANTMENTS
    public static final EnchantmentType ONE_FOR_ALL = new EnchantmentType("One For All", "one_for_all", "&7Removes all other enchants but increases your weapon damage by &a500%&7.", true, 1, SpecificItemType.SWORD, SpecificItemType.LONGSWORD);

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final String name;
    @Getter
    private final String namespace;
    private final String description;
    private final boolean ultimate;
    private final int maxLevel;
    private final org.bukkit.enchantments.Enchantment vanilla;
    private final List<SpecificItemType> compatibleTypes;

    public EnchantmentType(String name, String namespace, String description, boolean ultimate, int maxLevel, org.bukkit.enchantments.Enchantment vanilla, SpecificItemType... compatibleTypes) {
        this.name = name;
        this.namespace = namespace;
        this.description = description;
        this.ultimate = ultimate;
        this.maxLevel = maxLevel;
        this.vanilla = vanilla;
        this.compatibleTypes = new ArrayList<>(Arrays.asList(compatibleTypes));
        ENCHANTMENT_TYPE_CACHE.put(namespace, this);
    }

    public EnchantmentType(String name, String namespace, String description, boolean ultimate, int maxLevel, SpecificItemType... compatibleTypes) {
        this(name, namespace, description, ultimate, maxLevel, null, compatibleTypes);
    }

    public EnchantmentType(String name, String namespace, String description, int maxLevel, org.bukkit.enchantments.Enchantment vanilla, SpecificItemType... compatibleTypes) {
        this(name, namespace, description, false, maxLevel, vanilla, compatibleTypes);
    }

    public EnchantmentType(String name, String namespace, String description, int maxLevel, SpecificItemType... compatibleTypes) {
        this(name, namespace, description, false, maxLevel, compatibleTypes);
    }

    public static EnchantmentType getByNamespace(String namespace) {
        return ENCHANTMENT_TYPE_CACHE.get(namespace.toLowerCase());
    }

    public static List<EnchantmentType> getEnchantmentsByType(SpecificItemType type) {
        List<EnchantmentType> enchantments = new ArrayList<>();
        for (EnchantmentType enchantment : ENCHANTMENT_TYPE_CACHE.values())
            for (SpecificItemType enchantmentType : enchantment.getCompatibleTypes())
                if (enchantmentType == type)
                    enchantments.add(enchantment);
        return enchantments;
    }

    public String getDescription(Object... objects) {
        String description = this.description;
        for (Object object : objects)
            description = description.replaceFirst("%s", String.valueOf(object));
        return description;
    }

    public boolean isCompatible(SpecificItemType type) {
        return compatibleTypes.contains(type);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EnchantmentType)) return false;
        return ((EnchantmentType) o).namespace.equals(namespace);
    }

}
