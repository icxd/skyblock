package net.icxd.dungeons.item;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.dungeons.DungeonLevels;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.item.behaviour.ItemBehaviour;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.bonus.SetBonusLore;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enchanting.Enchantment;
import net.icxd.dungeons.item.enchanting.weapon.WeaponStats;
import net.icxd.dungeons.item.enums.DungeonStar;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.Soulbound;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.item.upgrade.Book;
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
import java.util.Map;
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
 *       &6[Art of War]} and the other books' (see {@link Book}), {@code &6(Wood Singularity) &9(reforge) &d(gems)
 *       &8(in a dungeon)}, the last on dungeon items (see {@link DungeonItems})</li>
 *   <li>gemstone slots</li>
 *   <li>enchantments: with descriptions when there are up to 5 (and it isn't a dungeon item), one
 *       a line up to 9, else three a line</li>
 *   <li>attributes, the item's own text, rune, then its abilities and bonuses (text and abilities as its
 *       {@link ItemBehaviour} has them with the item's data; set bonuses count what the item's holder
 *       wears, see {@link SetBonusLore}), then its Book of Stats' count, then its reforge's bonus ("&9Withered Bonus")</li>
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
            Stat.CRIT_DAMAGE, Stat.INTELLIGENCE, Stat.SPEED, Stat.SEA_CREATURE_CHANCE, Stat.PRISTINE, Stat.MINING_SPEED,
            Stat.MINING_FORTUNE, Stat.FARMING_FORTUNE);
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
        return build(item, tag, amount, null);
    }

    /** {@link #build(SkyBlockItem, NBTTagCompound, Player)}, this many. */
    public static ItemStack build(SkyBlockItem item, NBTTagCompound tag, int amount, Player holder) {
        ItemStack itemStack = build(item, tag, holder);
        itemStack.setAmount(amount);
        return itemStack;
    }

    /** A new item of this kind if {@code tag} is null, else the item that data describes. */
    public static ItemStack build(SkyBlockItem item, NBTTagCompound tag) {
        return build(item, tag, (Player) null);
    }

    /**
     * {@link #build(SkyBlockItem, NBTTagCompound)} for {@code holder}'s inventory: its set bonuses show how
     * many of their pieces the holder wears ("(2/4)", see {@link SetBonusLore}); null for nobody's, as
     * the data has it ("(0/4)").
     */
    public static ItemStack build(SkyBlockItem item, NBTTagCompound tag, Player holder) {
        if (tag == null) tag = newData(item);
        addGemstoneSlots(item, tag);
        ItemNBT nbt = ItemNBT.of(new ItemStack(item.material()));
        nbt.setTag(tag);
        ItemStack stack = nbt.toItemStack();
        if (item.material() == Material.PLAYER_HEAD && item.skin() != null) Utils.skull(stack, item.skin());

        stack.setData(DataComponentTypes.CUSTOM_NAME, Text.line(name(item, tag)));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(Text.lines(lore(item, tag, holder))));
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
        return refresh(stack, null);
    }

    /** {@link #refresh(ItemStack)} for {@code holder}'s inventory (see {@link #build(SkyBlockItem, NBTTagCompound, Player)}). */
    public static ItemStack refresh(ItemStack stack, Player holder) {
        if (stack == null || stack.isEmpty()) return stack;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return item == null ? stack : build(item, tag, stack.getAmount(), holder);
    }

    /** Every SkyBlock item a player has, {@link #refresh refreshed} for them. */
    public static void refreshInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) contents[i] = refresh(contents[i], player);
        player.getInventory().setContents(contents);
    }

    /**
     * What every new item starts with, plus what its behaviour adds (see {@link ItemBehaviour#nbt}). Its rarity
     * and soulbound are written as they always were, but what shows is the item's own (see {@link #rarity}).
     */
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
        NBTTagCompound extra = ItemBehaviours.of(item).nbt(item);
        if (extra != null) {
            for (String key : extra.keySet()) tag.set(key, extra.get(key));
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

    /** "&6Fabled Hyperion &6✪✪✪✪✪": rarity colour, reforge, name and stars, as the item's name shows it. */
    public static String name(SkyBlockItem item, NBTTagCompound tag) {
        Reforge reforge = reforge(tag);
        return rarity(item, tag).getColor() + (reforge == null ? "" : reforge.prefix(item.name()) + " ") + item.name() + stars(item, tag);
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
    public static String stars(SkyBlockItem item, NBTTagCompound tag) {
        int stars = starCount(tag);
        if (stars <= 0) return "";
        if (DungeonItems.is(item, tag)) {
            return " &6" + "✪".repeat(Math.min(stars, 5)) + (stars > 5 ? "&c" + MASTER_STARS.charAt(Math.min(stars, 10) - 6) : "");
        }
        if (stars <= 5) return " &6" + "✪".repeat(stars);
        if (stars <= 10) return " &d" + "✪".repeat(stars - 5) + (stars < 10 ? "&6" + "✪".repeat(10 - stars) : "");
        return " &b" + "✪".repeat(Math.min(stars, 15) - 10) + (stars < 15 ? "&d" + "✪".repeat(15 - stars) : "");
    }

    // ---------- the lore ----------

    static List<String> lore(SkyBlockItem item, NBTTagCompound tag) {
        return lore(item, tag, null);
    }

    /** The lore in {@code holder}'s inventory: set bonuses count what they wear (null: as the data has it). */
    static List<String> lore(SkyBlockItem item, NBTTagCompound tag, Player holder) {
        Rarity rarity = rarity(item, tag);
        Player owner = owner(tag);
        Reforge reforge = reforge(tag);
        List<List<String>> sections = new ArrayList<>();
        sections.add(ItemModifiers.enrichmentLines(tag));

        List<String> header = new ArrayList<>();
        // With the reforge's (live Scraped Gemstone Gauntlets: 9, their own 8 and Scraped's 1).
        double breakingPower = item.stats().get(Stat.BREAKING_POWER) + (reforge == null ? 0 : reforge.stat(Stat.BREAKING_POWER, rarity, 0));
        if (breakingPower != 0) header.add("&8Breaking Power " + (int) breakingPower);
        for (String category : item.categories()) header.add("&8" + category);
        sections.add(header);

        List<String> stats = new ArrayList<>(statLines(item, tag, rarity, owner, holder));
        String gemstones = gemstoneLine(item, tag);
        if (gemstones != null) stats.add(gemstones);
        sections.add(stats);

        sections.add(enchantmentLines(item, tag));
        sections.add(attributeLines(tag, owner));
        ItemBehaviour behaviour = ItemBehaviours.of(item);
        sections.add(ItemModifiers.lore(tag, behaviour.lore(item, tag, item.lore())));
        sections.add(runeLines(tag));
        // Ultimate Wise's cheaper mana shows in the Mana Cost lines (live lore).
        for (ItemBlock block : WeaponStats.withWise(tag, ItemModifiers.blocks(tag, behaviour.blocks(item, tag, item.blocks())))) {
            sections.add(ItemModifiers.blockLore(tag, blockLore(SetBonusLore.shown(block, holder), rarity)));
        }
        sections.add(Book.statsLines(item, tag));
        // Last, as on live items: "&9Withered Bonus" and its text (after the Book of Stats' count).
        if (reforge != null) sections.add(reforge.bonusSection(rarity));

        List<String> lore = new ArrayList<>();
        for (List<String> section : sections) {
            if (section.isEmpty()) continue;
            if (!lore.isEmpty()) lore.add("");
            lore.addAll(section);
        }

        List<String> footer = new ArrayList<>();
        if (item.reforgeable() && reforge == null) footer.add("&8This item can be reforged!");
        footer.addAll(requirementLines(item, owner));
        if (item.soulbound() != Soulbound.NONE) {
            footer.add("&8&l* &8" + (item.soulbound() == Soulbound.COOP ? "Co-op " : "") + "Soulbound &8&l*");
        }
        if (!lore.isEmpty()) lore.add("");
        lore.addAll(footer);
        lore.add(rarityLine(item, tag, rarity));
        return lore;
    }

    /**
     * Each stat the item has, in Hypixel's order: the total (with its unit, {@code %} or Rift Time's {@code s}),
     * then what the upgrades and reforge add, then (on dungeon items) what it comes to in a dungeon.
     */
    static List<String> statLines(SkyBlockItem item, NBTTagCompound tag, Rarity rarity, Player owner) {
        return statLines(item, tag, rarity, owner, null);
    }

    /** {@link #statLines(SkyBlockItem, NBTTagCompound, Rarity, Player)} in {@code holder}'s inventory (null: nobody's). */
    static List<String> statLines(SkyBlockItem item, NBTTagCompound tag, Rarity rarity, Player owner, Player holder) {
        List<String> lines = new ArrayList<>();
        if (item.gearScore() > 0) lines.add("&7Gear Score: &d" + item.gearScore());
        Stats base = item.stats();
        Reforge reforge = reforge(tag);
        Map<Book, Stats> books = Book.bonuses(item, tag);
        boolean dungeon = DungeonItems.is(item, tag);
        int stars = dungeon ? Math.min(starCount(tag), 5) : starCount(tag);
        double catacombs = dungeon ? catacombsBoost(owner) : 0;
        // Withered's and Ancient's stat a Catacombs level is the owner's, as the dungeon boost is; on an item nobody
        // owns, its holder's (whose stats count it, ItemStats).
        int catacombsLevel = reforge == null || reforge.perLevel().isEmpty() ? 0 : catacombsLevel(owner != null ? owner : holder);
        Stats enchanted = new Stats();
        for (Enchantment enchantment : enchantments(tag)) enchanted.add(enchantment.getType().getStats(enchantment.getLevel()));
        Stats gems = GemSlots.stats(item, tag);
        Stats modified = ItemModifiers.stats(tag);
        for (Stat stat : Stat.values()) {
            if (stat == Stat.BREAKING_POWER || stat == Stat.WEAPON_ABILITY_DAMAGE) continue;
            // Each book's in its own bracket, in the live order: potato books, then The Art of War or Peace, ...
            double fromBooks = 0;
            StringBuilder bookBrackets = new StringBuilder();
            for (Map.Entry<Book, Stats> book : books.entrySet()) {
                double value = book.getValue().get(stat);
                if (value == 0) continue;
                fromBooks += value;
                bookBrackets.append(" ").append(book.getKey().bracket(value));
            }
            double reforged = reforge == null ? 0 : reforge.stat(stat, rarity, catacombsLevel);
            // Stars add 2% of the base stat each out of a dungeon (with no bracket); in one, the dungeon boost replaces that.
            double starBonus = starBonus(stat, base.get(stat), stars);
            double gemstones = gems.get(stat);
            // What enchantments and an Enrichment grant counts in the total, with no bracket of its own.
            double shown = base.get(stat) + starBonus + fromBooks + reforged + enchanted.get(stat) + gemstones + modified.get(stat);
            if (shown == 0) continue;
            double singularity = ItemModifiers.woodSingularity(tag, stat);
            String unit = stat.getUnit();
            StringBuilder line = new StringBuilder("&7").append(stat.getDisplayName()).append(": &").append(stat.getLoreColor())
                    .append(Text.signed(shown)).append(unit);
            line.append(bookBrackets);
            if (singularity != 0) line.append(" &6(").append(Text.signed(singularity)).append(")");
            if (reforged != 0) line.append(" &9(").append(Text.signed(reforged)).append(unit).append(")");
            if (gemstones != 0) line.append(" &d(").append(Text.signed(gemstones)).append(unit).append(")");
            if (dungeon && shown > 0) {
                line.append(" &8(").append(Text.signed((shown - starBonus) * dungeonFactor(stat, stars, catacombs))).append(unit).append(")");
            }
            lines.add(line.toString());
        }
        if (item.shotCooldown() > 0) lines.add("&7Shot Cooldown: &a" + Text.number(item.shotCooldown()) + "s");
        return lines;
    }

    /**
     * What an item's stars add to one of its own stats out of a dungeon: 2% of it a star, but nothing to
     * Swing Range (the recorded 5-star Giant's Sword showed its 1 as {@code +1} in the Hub), Health Regen or
     * Vitality (live: a 10-star Gillsplash Belt's 2 Health Regen shows +2, a 5-star Reaper Mask's 60 Vitality +60).
     */
    public static double starBonus(Stat stat, double base, int stars) {
        return stat == Stat.SWING_RANGE || stat == Stat.HEALTH_REGEN || stat == Stat.VITALITY ? 0 : base * 0.02 * stars;
    }

    /**
     * What a dungeon item's stat is multiplied by in a dungeon (in place of the 2% a star it has
     * elsewhere): +10% a star, and the Catacombs boost for the stats it applies to. Crit Chance, Attack
     * Speed and the like only get the stars'; health regen, vitality, mending and swing range nothing.
     */
    public static double dungeonFactor(Stat stat, int stars, double catacombsBoost) {
        return dungeonFactor(stat, stars, 0, catacombsBoost);
    }

    /**
     * {@link #dungeonFactor(Stat, int, double)} in Master Mode, where each master star adds 5% (its item: "increasing
     * all stats by an additional +5% in Master Mode"). UNKNOWN how Hypixel adds it up: here to the stars' 10% each.
     */
    public static double dungeonFactor(Stat stat, int stars, int masterStars, double catacombsBoost) {
        if (NOT_SCALED.contains(stat)) return 1;
        return 1 + 0.1 * stars + 0.05 * masterStars + (CATACOMBS_SCALED.contains(stat) ? catacombsBoost : 0);
    }

    /**
     * The Catacombs stat boost (0.26.1, July 2026): 10% at level 0, +5% for each of levels 1-3, then
     * +6, +7, +8 and +9% for levels 4-7 and +10% for each level from 8 up to 50.
     */
    static double catacombsBoost(int level) {
        int[] early = {10, 15, 20, 25, 31, 38, 46, 55};
        level = Math.max(0, Math.min(level, DungeonLevels.STAT_CAP));
        return (level < early.length ? early[level] : 55 + (level - 7) * 10) / 100.0;
    }

    /** The owner's; level 0's while there's no owner to go by. */
    public static double catacombsBoost(Player owner) {
        return catacombsBoost(catacombsLevel(owner));
    }

    /** Their Catacombs level for stats (at most 50); 0 for nobody, or while their data isn't loaded. */
    public static int catacombsLevel(Player owner) {
        User user = owner == null ? null : User.ifLoaded(owner.getUniqueId());
        return user == null ? 0 : DungeonProfile.catacombsStatLevel(user);
    }

    /** "&7Gemstones: &8[✎] &9[&d⚔&9]": locked slots all dark gray, open ones with a gray symbol, gems in their colours (GemSlots). */
    private static String gemstoneLine(SkyBlockItem item, NBTTagCompound tag) {
        return GemSlots.line(item, tag);
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
        boolean dungeon = DungeonItems.is(item, tag);
        if (!dungeon && count <= 5) {
            for (Enchantment enchantment : enchantments) {
                lines.add(enchantment.getDisplayName());
                lines.addAll(enchantment.getDescription());
            }
        } else if ((!dungeon && count <= 9) || count == 1) {
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
     * An ability or bonus: its header as Hypixel shows it ("&6Ability: Instant Transmission  &e&lRIGHT CLICK"),
     * its text, then what it costs and its cooldown. A shortbow's line is written here: it's in the rarity's
     * colour, which recombobulating changes.
     */
    public static List<String> blockLore(ItemBlock block, Rarity rarity) {
        if (block.isShortbow()) {
            return List.of(rarity.getColor() + "Shortbow: " + (block.name() != null ? block.name() : "Instantly shoots!"));
        }
        List<String> lines = new ArrayList<>();
        if (block.header() != null) lines.add(block.header());
        lines.addAll(block.text());
        costLines(lines, block.mana(), block.manaPercent(), block.soulflow(), block.healthCost(), block.vitality(), block.cooldown());
        return lines;
    }

    /**
     * "&8Mana Cost: &b45✎", "&8Health Cost: &c1,000❤", "&8Cooldown: &a30s" and the like, in the order
     * Hypixel lists them (NEU's dumps: soulflow, mana, health, vitality, cooldown); 0 for none. A share
     * of max mana has no ✎ ("&b50% of max", the power orbs).
     */
    private static void costLines(List<String> lines, double mana, double manaPercent, double soulflow, double health,
                                  double vitality, double cooldown) {
        // No symbol after soulflow (7 of NEU's 8 captures).
        if (soulflow > 0) lines.add("&8Soulflow Cost: &3" + Text.number(soulflow));
        if (mana > 0) lines.add("&8Mana Cost: &b" + Text.number(mana) + "✎");
        if (manaPercent > 0) lines.add("&8Mana Cost: &b" + Text.number(manaPercent) + "% of max");
        if (health > 0) lines.add("&8Health Cost: &c" + Text.number(health) + "❤");
        if (vitality > 0) lines.add("&8Vitality Cost: &4" + Text.number(vitality) + "♨");
        if (cooldown > 0) lines.add("&8Cooldown: &a" + cooldown(cooldown));
    }

    /** "30s", "1m", "2h": Hypixel writes whole minutes and hours as such. */
    static String cooldown(double seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) return Text.number(seconds / 3600) + "h";
        if (seconds >= 60 && seconds % 60 == 0) return Text.number(seconds / 60) + "m";
        return Text.number(seconds) + "s";
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
        String words = item.typeLabel() != null ? item.typeLabel() : type == SpecificItemType.NONE ? null : type.name().replace('_', ' ');
        // An empty label means just the rarity, as on Hypixel's consumables and sacks.
        boolean dungeon = DungeonItems.is(item, tag);
        String kind = words == null ? (dungeon ? " DUNGEON ITEM" : "")
                : words.isEmpty() ? "" : (dungeon ? " DUNGEON" : "") + " " + words;
        String line = bold + rarity.name().replace('_', ' ') + kind;
        return tag.getBoolean("recombobulated") ? bold + "&ka&r " + line + " " + bold + "&ka" : line;
    }

    /**
     * The item's rarity, one up while it's recombobulated. Items kept a copy of their rarity in their
     * data (and /recombobulate changed it); that isn't read, so a change to the item shows on every copy.
     */
    public static Rarity rarity(SkyBlockItem item, NBTTagCompound tag) {
        return tag.getBoolean("recombobulated") ? item.rarity().upgrade() : item.rarity();
    }

    /** Its reforge; null for none (one the reforge table doesn't know keeps its name, see Reforge#of). */
    private static Reforge reforge(NBTTagCompound tag) {
        return Reforge.of(tag);
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
