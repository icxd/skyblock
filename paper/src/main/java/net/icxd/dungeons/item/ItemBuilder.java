package net.icxd.dungeons.item;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.dungeons.DungeonLevels;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enchanting.Enchantment;
import net.icxd.dungeons.item.enums.DungeonStar;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.Soulbound;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.rune.Rune;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Makes the item stack for a SkyBlock item: its data (kept with {@link ItemNBT}), and its name and
 * lore laid out the way Hypixel's item tooltips are today. The rules come from Hypixel's own items:
 * the recordings, the NEU item repository's dumps and the live auction house (see tools/items):
 * <ol>
 *   <li>name: rarity colour, reforge, name, stars ({@code ✪}, master stars {@code ➊}-{@code ➎})</li>
 *   <li>dark gray lines: breaking power, categories ("Collection Item")</li>
 *   <li>gear score, then stats in Hypixel's order, each with its bonuses: {@code &e(hot potato books)
 *       &6[Art of War] &9(reforge) &8(in a dungeon)}, the last on dungeon items</li>
 *   <li>gemstone slots</li>
 *   <li>enchantments: with descriptions when there are up to 5 (and it isn't a dungeon item), one
 *       a line up to 9, else three a line</li>
 *   <li>attributes, the item's own text, rune, ability, then text from the item's data</li>
 *   <li>"This item can be reforged!", requirements the owner doesn't meet, soulbound, rarity line</li>
 * </ol>
 * Every line is one {@code &}-coded string turned into a component (see {@link Text#line}).
 */
public final class ItemBuilder {
    /** The vanilla tooltip lines Hypixel hides on its items (loaded when first needed: it takes a server). */
    private static final class Hidden {
        static final DataComponentType[] COMPONENTS = {
                DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.UNBREAKABLE, DataComponentTypes.DYED_COLOR,
                DataComponentTypes.TRIM, DataComponentTypes.JUKEBOX_PLAYABLE, DataComponentTypes.MAP_ID,
                DataComponentTypes.FIREWORKS, DataComponentTypes.WRITTEN_BOOK_CONTENT, DataComponentTypes.BANNER_PATTERNS,
                DataComponentTypes.POTION_CONTENTS, DataComponentTypes.CHARGED_PROJECTILES, DataComponentTypes.ENCHANTMENTS};
    }

    /** Stats Hypixel scales by Catacombs level in dungeons (the rest only by stars, or not at all). */
    private static final Set<Stat> CATACOMBS_SCALED = EnumSet.of(Stat.HEALTH, Stat.DEFENSE, Stat.STRENGTH, Stat.DAMAGE,
            Stat.CRIT_DAMAGE, Stat.INTELLIGENCE, Stat.SPEED, Stat.SEA_CREATURE_CHANCE, Stat.MINING_SPEED, Stat.MINING_FORTUNE,
            Stat.FARMING_FORTUNE);
    /** Stats dungeons don't boost at all (health regen and vitality since 0.26.1). */
    private static final Set<Stat> NOT_SCALED = EnumSet.of(Stat.HEALTH_REGEN, Stat.VITALITY, Stat.MENDING, Stat.SWING_RANGE);

    private static final String MASTER_STARS = "➊➋➌➍➎";

    private ItemBuilder() {
    }

    public static ItemStack build(SkyBlockItem item) {
        return build(item, null);
    }

    public static ItemStack build(SkyBlockItem item, int amount) {
        ItemStack itemStack = build(item);
        itemStack.setAmount(amount);
        return itemStack;
    }

    public static ItemStack build(SkyBlockItem item, NBTTagCompound tag, int amount) {
        ItemStack itemStack = build(item, tag);
        itemStack.setAmount(amount);
        return itemStack;
    }

    /** A new item of this kind if {@code tag} is null, else the item that data describes. */
    public static ItemStack build(SkyBlockItem item, NBTTagCompound tag) {
        if (tag == null) tag = newData(item);
        addGemstoneSlots(item, tag);
        ItemNBT nbt = ItemNBT.of(new ItemStack(item.material()));
        nbt.setTag(tag);
        ItemStack stack = nbt.toItemStack();
        if (item.material() == Material.PLAYER_HEAD && item.skin() != null) Utils.skull(stack, item.skin());

        stack.setData(DataComponentTypes.CUSTOM_NAME, Text.line(name(item, tag)));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(Text.lines(lore(item, tag))));
        stack.setData(DataComponentTypes.UNBREAKABLE);
        stack.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(Hidden.COMPONENTS).build());
        if (item.color() != null) stack.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(item.color()));
        if (item.glowing() || !enchantments(tag).isEmpty()) stack.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /**
     * The SkyBlock item made again from its data, so it looks as items do now (and its data is kept
     * the current way); anything else as it is.
     */
    public static ItemStack refresh(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return item == null ? stack : build(item, tag, stack.getAmount());
    }

    /** Every SkyBlock item a player has, {@link #refresh refreshed}. */
    public static void refreshInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) contents[i] = refresh(contents[i]);
        player.getInventory().setContents(contents);
    }

    /** What every new item starts with, plus the item's own {@link SkyBlockItem#nbt()}. */
    static NBTTagCompound newData(SkyBlockItem item) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", item.id());
        tag.setString("name", item.name());
        tag.setString("rarity", item.rarity().name());
        tag.setString("dungeon_star", DungeonStar.ZERO.name());
        tag.setString("specific_item_type", item.specificItemType().name());
        GenericItemType generic = item.genericItemType();
        tag.setString("generic_item_type", generic == null ? GenericItemType.OTHER.name() : generic.name());
        tag.setString("soulbound", item.soulbound().name());
        tag.setBoolean("dungeon_item", item.dungeonItem());
        tag.setBoolean("can_have_attributes", item.canHaveAttributes());
        if (item.canHaveAttributes()) {
            Attribute attribute1 = Attribute.random(item.genericItemType(), null);
            Attribute attribute2 = Attribute.random(item.genericItemType(), attribute1);
            tag.setString("attribute_1", attribute1.name());
            tag.setInt("attribute_1_level", Utils.random(1, 2));
            tag.setString("attribute_2", attribute2.name());
            tag.setInt("attribute_2_level", Utils.random(1, 2));
        }
        tag.setBoolean("recombobulated", false);
        tag.setInt("hot_potato_books", 0);
        tag.setBoolean("art_of_war", false);
        tag.setString("owner", "");
        tag.setString("reforge", "");
        tag.setString("rune", "");
        tag.setInt("rune_level", 0);
        tag.set("gemstone_slots", new NBTTagList());
        tag.set("enchantments", new NBTTagList());
        tag.setInt("upgrade_count", 0);
        if (item.nbt() != null) {
            for (String key : item.nbt().keySet()) tag.set(key, item.nbt().get(key));
        }
        if (item.unstackable()) tag.setString("uuid", UUID.randomUUID().toString());
        return tag;
    }

    /** The item's slots, each locked if it has an unlock cost, the first time the item is built. */
    private static void addGemstoneSlots(SkyBlockItem item, NBTTagCompound tag) {
        if (item.gemstoneSlots() == null || !tag.getList("gemstone_slots", 10).isEmpty()) return;
        NBTTagList slots = new NBTTagList();
        for (GemstoneSlot slot : item.gemstoneSlots().getSlots()) {
            NBTTagCompound slotTag = new NBTTagCompound();
            boolean locked = !slot.getCosts().isEmpty();
            slotTag.setBoolean("locked", locked);
            if (locked) {
                NBTTagList costs = new NBTTagList();
                for (Cost cost : slot.getCosts()) {
                    NBTTagCompound costTag = new NBTTagCompound();
                    if (cost instanceof CoinCost coinCost) {
                        costTag.setString("type", "coin");
                        costTag.setInt("amount", coinCost.getAmount());
                    } else if (cost instanceof ItemCost itemCost) {
                        costTag.setString("type", "item");
                        costTag.setString("item_id", itemCost.getItemId());
                        costTag.setInt("amount", itemCost.getAmount());
                    } else if (cost instanceof EssenceCost essenceCost) {
                        costTag.setString("type", "essence");
                        costTag.setString("essence", essenceCost.getEssenceType().name());
                        costTag.setInt("amount", essenceCost.getAmount());
                    }
                    costs.add(costTag);
                }
                slotTag.set("costs", costs);
            }
            slots.add(slotTag);
        }
        tag.set("gemstone_slots", slots);
    }

    // ---------- the name ----------

    static String name(SkyBlockItem item, NBTTagCompound tag) {
        Reforge reforge = reforge(tag);
        return rarity(item, tag).getColor() + (reforge == null ? "" : reforge.getName() + " ") + item.name() + stars(item, tag);
    }

    /** How many stars it has: its upgrades (older items kept dungeon stars separately). */
    public static int starCount(NBTTagCompound tag) {
        String legacy = tag.getString("dungeon_star");
        int dungeonStars = legacy.isEmpty() ? 0 : DungeonStar.valueOf(legacy).ordinal();
        return Math.max(tag.getInt("upgrade_count"), dungeonStars);
    }

    /**
     * " &6✪✪✪": dungeon items show five gold stars, then a red master star (➊ to ➎); others turn their
     * stars purple from the left after five (and aqua after ten).
     */
    static String stars(SkyBlockItem item, NBTTagCompound tag) {
        int stars = starCount(tag);
        if (stars <= 0) return "";
        if (item.dungeonItem()) {
            return " &6" + "✪".repeat(Math.min(stars, 5)) + (stars > 5 ? "&c" + MASTER_STARS.charAt(Math.min(stars, 10) - 6) : "");
        }
        if (stars <= 5) return " &6" + "✪".repeat(stars);
        if (stars <= 10) return " &d" + "✪".repeat(stars - 5) + (stars < 10 ? "&6" + "✪".repeat(10 - stars) : "");
        return " &b" + "✪".repeat(Math.min(stars, 15) - 10) + (stars < 15 ? "&d" + "✪".repeat(15 - stars) : "");
    }

    // ---------- the lore ----------

    static List<String> lore(SkyBlockItem item, NBTTagCompound tag) {
        Rarity rarity = rarity(item, tag);
        Player owner = owner(tag);
        List<List<String>> sections = new ArrayList<>();

        List<String> header = new ArrayList<>();
        if (item.stats().has(Stat.BREAKING_POWER)) header.add("&8Breaking Power " + (int) item.stats().get(Stat.BREAKING_POWER));
        for (String category : item.categories()) header.add("&8" + category);
        sections.add(header);

        List<String> stats = new ArrayList<>(statLines(item, tag, rarity, owner));
        String gemstones = gemstoneLine(item, tag);
        if (gemstones != null) stats.add(gemstones);
        sections.add(stats);

        sections.add(enchantmentLines(item, tag));
        sections.add(attributeLines(tag, owner));
        sections.add(item.lore());
        sections.add(runeLines(tag));
        if (item.ability() != null) sections.add(abilityLore(item.ability(), rarity));
        List<String> fromData = item.nbtLore(tag);
        if (fromData != null) sections.add(fromData);

        List<String> lore = new ArrayList<>();
        for (List<String> section : sections) {
            if (section.isEmpty()) continue;
            if (!lore.isEmpty()) lore.add("");
            lore.addAll(section);
        }

        List<String> footer = new ArrayList<>();
        if (item.reforgeable() && reforge(tag) == null) footer.add("&8This item can be reforged!");
        footer.addAll(requirementLines(item, owner));
        String soulbound = tag.getString("soulbound");
        if (!soulbound.isEmpty() && Soulbound.valueOf(soulbound) != Soulbound.NONE) {
            footer.add("&8&l* &8" + (Soulbound.valueOf(soulbound) == Soulbound.COOP ? "Co-op " : "") + "Soulbound &8&l*");
        }
        if (!lore.isEmpty()) lore.add("");
        lore.addAll(footer);
        lore.add(rarityLine(item, tag, rarity));
        return lore;
    }

    /**
     * Each stat the item has, in Hypixel's order: the total (with {@code %} for percentage stats), then
     * what the upgrades and reforge add, then (on dungeon items) what it comes to in a dungeon.
     */
    static List<String> statLines(SkyBlockItem item, NBTTagCompound tag, Rarity rarity, Player owner) {
        List<String> lines = new ArrayList<>();
        if (item.gearScore() > 0) lines.add("&7Gear Score: &d" + item.gearScore());
        Stats base = item.stats();
        Reforge reforge = reforge(tag);
        GenericItemType generic = item.genericItemType();
        int books = tag.getInt("hot_potato_books");
        int stars = item.dungeonItem() ? Math.min(starCount(tag), 5) : 0;
        double catacombs = item.dungeonItem() ? catacombsBoost(owner) : 0;
        Stats enchanted = new Stats();
        for (Enchantment enchantment : enchantments(tag)) enchanted.add(enchantment.getType().getStats(enchantment.getLevel()));
        for (Stat stat : Stat.values()) {
            if (stat == Stat.BREAKING_POWER || stat == Stat.WEAPON_ABILITY_DAMAGE) continue;
            double potatoBooks = generic == GenericItemType.WEAPON && (stat == Stat.DAMAGE || stat == Stat.STRENGTH) ? books * 2
                    : generic == GenericItemType.ARMOR && stat == Stat.HEALTH ? books * 4
                    : generic == GenericItemType.ARMOR && stat == Stat.DEFENSE ? books * 2 : 0;
            double artOfWar = stat == Stat.STRENGTH && tag.getBoolean("art_of_war") ? 5 : 0;
            double reforged = reforge == null || reforge.getStats().get(stat) == null ? 0 : reforge.getStats().get(stat).at(rarity);
            // Stars add 2% of the base stat each out of a dungeon; in one, the dungeon boost replaces that.
            double starBonus = base.get(stat) * 0.02 * stars;
            // What enchantments grant counts in the total, with no bracket of its own.
            double shown = base.get(stat) + starBonus + potatoBooks + artOfWar + reforged + enchanted.get(stat);
            if (shown == 0) continue;
            String percent = stat.isPercent() ? "%" : "";
            StringBuilder line = new StringBuilder("&7").append(stat.getDisplayName()).append(": &").append(stat.getLoreColor())
                    .append(Text.signed(shown)).append(percent);
            if (potatoBooks != 0) line.append(" &e(").append(Text.signed(potatoBooks)).append(")");
            if (artOfWar != 0) line.append(" &6[").append(Text.signed(artOfWar)).append("]");
            if (reforged != 0) line.append(" &9(").append(Text.signed(reforged)).append(percent).append(")");
            if (item.dungeonItem() && shown > 0) {
                double factor = NOT_SCALED.contains(stat) ? 1 : 1 + 0.1 * stars + (CATACOMBS_SCALED.contains(stat) ? catacombs : 0);
                line.append(" &8(").append(Text.signed((shown - starBonus) * factor)).append(percent).append(")");
            }
            lines.add(line.toString());
        }
        if (item.shotCooldown() > 0) lines.add("&7Shot Cooldown: &a" + Text.number(item.shotCooldown()) + "s");
        return lines;
    }

    /**
     * The Catacombs stat boost (0.26.1, July 2026): 10% at level 0, +5% for each of levels 1-3, then
     * +6, +7, +8 and +9% for levels 4-7 and +10% for each level from 8 up to 50.
     */
    static double catacombsBoost(int level) {
        int[] early = {10, 15, 20, 25, 31, 38, 46, 55};
        level = Math.max(0, Math.min(level, 50));
        return (level < early.length ? early[level] : 55 + (level - 7) * 10) / 100.0;
    }

    /** The owner's; level 0's while there's no owner to go by. */
    private static double catacombsBoost(Player owner) {
        User user = owner == null ? null : User.ifLoaded(owner.getUniqueId());
        Number experience = user == null ? null : user.get("dungeons.catacombsExp", Number.class);
        return catacombsBoost(experience == null ? 0 : DungeonLevels.level(experience.doubleValue()));
    }

    /** "&7Gemstones: &8[✎] [⚔]": locked slots all dark gray, open ones with a gray symbol. */
    private static String gemstoneLine(SkyBlockItem item, NBTTagCompound tag) {
        if (item.gemstoneSlots() == null) return null;
        List<GemstoneSlot> kinds = item.gemstoneSlots().getSlots();
        NBTTagList slots = tag.getList("gemstone_slots", 10);
        StringBuilder line = new StringBuilder("&7Gemstones:");
        for (int i = 0; i < kinds.size(); i++) {
            boolean locked = i < slots.size() ? slots.get(i).getBoolean("locked") : !kinds.get(i).getCosts().isEmpty();
            char icon = kinds.get(i).getType().getIcon();
            line.append(locked ? " &8[" + icon + "]" : " &8[&7" + icon + "&8]");
        }
        return line.toString();
    }

    /** The item's enchantments that this version knows, ultimate first, then by name. */
    private static List<Enchantment> enchantments(NBTTagCompound tag) {
        List<Enchantment> enchantments = new ArrayList<>();
        NBTTagList list = tag.getList("enchantments", 10);
        for (int i = 0; i < list.size(); i++) {
            Enchantment enchantment = Enchantment.getByIdentifiable(list.get(i).getString("name") + "." + list.get(i).getInt("lvl"));
            if (enchantment.getType() != null) enchantments.add(enchantment);
        }
        enchantments.sort(Comparator.comparing((Enchantment e) -> !e.getType().isUltimate()).thenComparing(e -> e.getType().getName()));
        return enchantments;
    }

    /**
     * Up to 5 on an item that isn't a dungeon item: each with its description. Up to 9: one a line.
     * More, or any on a dungeon item (a lone one still gets its own line): three a line.
     */
    static List<String> enchantmentLines(SkyBlockItem item, NBTTagCompound tag) {
        List<Enchantment> enchantments = enchantments(tag);
        List<String> lines = new ArrayList<>();
        int count = enchantments.size();
        if (count == 0) return lines;
        if (!item.dungeonItem() && count <= 5) {
            for (Enchantment enchantment : enchantments) {
                lines.add(enchantment.getDisplayName());
                lines.addAll(enchantment.getDescription());
            }
        } else if ((!item.dungeonItem() && count <= 9) || count == 1) {
            for (Enchantment enchantment : enchantments) lines.add(enchantment.getDisplayName());
        } else {
            for (int i = 0; i < count; i += 3) {
                List<String> names = new ArrayList<>();
                for (Enchantment enchantment : enchantments.subList(i, Math.min(i + 3, count))) names.add(enchantment.getDisplayName());
                lines.add(String.join(", ", names));
            }
        }
        return lines;
    }

    /**
     * Attributes, as items showed them before Hypixel moved attributes off items: "&bVeteran I" and
     * what it grants, in red with ✖ while the owner doesn't meet its requirement.
     */
    private static List<String> attributeLines(NBTTagCompound tag, Player owner) {
        List<String> lines = new ArrayList<>();
        for (String key : List.of("attribute_1", "attribute_2")) {
            if (!tag.hasKey(key)) continue;
            Attribute attribute = Attribute.of(tag.getString(key));
            int level = tag.getInt(key + "_level");
            boolean met = owner != null && attribute.requirement().test(owner);
            lines.add((met ? "&b" : "&c") + attribute.getName() + " " + Utils.getRomanNumeral(level) + (met ? "" : " ✖"));
            lines.addAll(attribute.getLore(level));
        }
        return lines;
    }

    private static List<String> runeLines(NBTTagCompound tag) {
        if (tag.getString("rune").isEmpty()) return List.of();
        Rune rune = Rune.valueOf(tag.getString("rune"));
        return List.of(rune.getColor() + "◆ " + rune.getName() + " Rune " + Utils.getRomanNumeral(tag.getInt("rune_level")));
    }

    /**
     * "&6Ability: Instant Transmission  &e&lRIGHT CLICK" (two spaces; a passive one ends in one), its
     * description, then what it costs and its cooldown. A shortbow's is just "Shortbow: Instantly shoots!".
     */
    public static List<String> abilityLore(Ability ability, Rarity rarity) {
        List<String> lines = new ArrayList<>();
        switch (ability.getType()) {
            case SHORTBOW -> {
                lines.add(rarity.getColor() + "Shortbow: Instantly shoots!");
                return lines;
            }
            case PIECE_BONUS -> lines.add("&6Piece Bonus: " + ability.getName());
            case FULL_SET_BONUS -> lines.add("&6Full Set Bonus: " + ability.getName() + " &7(0/4)");
            default -> lines.add("&6Ability: " + ability.getName() + (ability.getActivation() == AbilityActivation.PASSIVE ? " "
                    : "  &e&l" + ability.getActivation().getDisplay()));
        }
        lines.addAll(ability.descriptionLines());
        if (ability.getManaCost() > 0) lines.add("&8Mana Cost: &b" + ability.getManaCost() + "✎");
        if (ability.getSoulflowCost() > 0) lines.add("&8Soulflow Cost: &3" + ability.getSoulflowCost() + "⸎");
        if (ability.getCooldown() > 0) lines.add("&8Cooldown: &a" + ability.getCooldown() + "s");
        return lines;
    }

    /** The requirements its owner doesn't meet (none while it has no owner). */
    private static List<String> requirementLines(SkyBlockItem item, Player owner) {
        List<String> lines = new ArrayList<>();
        if (item.requirements() == null || owner == null) return lines;
        for (Requirement requirement : item.requirements().getRequirements()) {
            if (requirement.requirement() != null && requirement.requirement().test(owner)) continue;
            lines.addAll(requirement.lore());
        }
        return lines;
    }

    /** "&6&lLEGENDARY DUNGEON SWORD", between obfuscated letters once recombobulated. */
    static String rarityLine(SkyBlockItem item, NBTTagCompound tag, Rarity rarity) {
        String bold = rarity.getBoldedColor();
        SpecificItemType type = item.specificItemType();
        String kind = type == SpecificItemType.NONE ? (item.dungeonItem() ? " DUNGEON ITEM" : "")
                : (item.dungeonItem() ? " DUNGEON" : "") + " " + type.name().replace('_', ' ');
        String line = bold + rarity.name().replace('_', ' ') + kind;
        return tag.getBoolean("recombobulated") ? bold + "&ka&r " + line + " " + bold + "&ka" : line;
    }

    private static Rarity rarity(SkyBlockItem item, NBTTagCompound tag) {
        String rarity = tag.getString("rarity");
        return rarity.isEmpty() ? item.rarity() : Rarity.valueOf(rarity);
    }

    private static Reforge reforge(NBTTagCompound tag) {
        String reforge = tag.getString("reforge");
        return reforge.isEmpty() ? null : Reforge.valueOf(reforge);
    }

    /** The online player who owns it; null if it has no owner or they aren't here. */
    private static Player owner(NBTTagCompound tag) {
        String owner = tag.getString("owner");
        if (owner.isEmpty()) return null;
        try {
            return Bukkit.getPlayer(UUID.fromString(owner));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
