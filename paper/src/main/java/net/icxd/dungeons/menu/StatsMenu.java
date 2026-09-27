package net.icxd.dungeons.menu;

import static net.icxd.dungeons.stats.Stat.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Stats & Equipment ({@code /stats}, or the head in the SkyBlock Menu), as recorded (the SkyBlock
 * Menu tour, 02:18.4): what they hold and wear, and their stats by category, each listing the stats
 * they have. The recording's player had every slot filled; the empty slots are the fandom wiki's
 * (Equipment Menu/UI). This plugin has no equipment, pets or potion effects yet, so those slots are
 * empty (none active). Stats that Hypixel turns off on the Private Island are struck through there;
 * no server here is one, so none are. The categories' breakdowns ("Your Stats Breakdown") and the
 * SkyBlock Achievements aren't here yet. Main thread.
 */
public final class StatsMenu extends GUI {
    public static final String TITLE = "Stats & Equipment";
    static final int HAND = 2;
    static final int NECKLACE = 10;
    static final int HELMET = 11;
    static final int CLOAK = 19;
    static final int CHESTPLATE = 20;
    static final int BELT = 28;
    static final int LEGGINGS = 29;
    static final int GLOVES = 37;
    static final int BOOTS = 38;
    static final int PET = 47;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int EFFECTS = 50;

    /**
     * A stats item: where it goes, its item, name and description, and its stats in order. The
     * recording's order, with the stats it didn't have (at 0) placed as the fandom wiki's Stats page
     * lists them; Charm Chance and Mining Spread, which it had, aren't stats here yet.
     */
    enum Category {
        COMBAT(14, Material.STONE_SWORD, "&cCombat Stats", List.of("&7Stats that influence how much", "&7damage you take and deal when in",
                "&7combat."), HEALTH, DEFENSE, TRUE_DEFENSE, STRENGTH, CRIT_CHANCE, CRIT_DAMAGE, ATTACK_SPEED, FEROCITY, SWING_RANGE,
                INTELLIGENCE, ABILITY_DAMAGE, HEALTH_REGEN, VITALITY, MENDING),
        MINING(15, Material.STONE_PICKAXE, "&6Mining Stats", List.of("&7Stats that influence what you can",
                "&7break, how quickly you can break it,", "&7and how many drops you receive", "&7when mining."), BREAKING_POWER, MINING_SPEED,
                PRISTINE, MINING_FORTUNE, ORE_FORTUNE, BLOCK_FORTUNE, DWARVEN_METAL_FORTUNE, GEMSTONE_FORTUNE),
        FARMING(16, Material.GOLDEN_HOE, "&aFarming Stats", List.of("&7Stats that influence how many drops", "&7you receive and how many pests",
                "&7spawn when farming."), BONUS_PEST_CHANCE, FARMING_FORTUNE, WHEAT_FORTUNE, CARROT_FORTUNE, POTATO_FORTUNE, PUMPKIN_FORTUNE,
                SUGAR_CANE_FORTUNE, MELON_FORTUNE, CACTUS_FORTUNE, COCOA_BEANS_FORTUNE, MUSHROOM_FORTUNE, NETHER_STALK_FORTUNE),
        FORAGING(23, Material.JUNGLE_SAPLING, "&2Foraging Stats", List.of("&7Stat that includes how many drops", "&7you receive when foraging."),
                SWEEP, FORAGING_FORTUNE, FIG_FORTUNE, MANGROVE_FORTUNE, HELIX_FORTUNE),
        FISHING(24, Material.FISHING_ROD, "&bFishing Stats", List.of("&7Stats that influence what you catch", "&7and how quickly you catch it while",
                "&7fishing."), FISHING_SPEED, SEA_CREATURE_CHANCE, DOUBLE_HOOK_CHANCE, TROPHY_FISH_CHANCE, TREASURE_CHANCE),
        MISC(25, Material.CLOCK, "&dMiscellaneous Stats", List.of("&7Stats that augment various aspects", "&7of your gameplay."), SPEED, MAGIC_FIND,
                PET_LUCK, HEAT_RESISTANCE, COLD_RESISTANCE, RESPIRATION, PRESSURE_RESISTANCE, FEAR, TRACKING),
        HUNTING(32, Material.LEAD, "&eHunting Stats", List.of("&7Stats that influence how quickly you", "&7hunt mobs and how many shards you",
                "&7get for doing so."), PULL, HUNTING_FORTUNE),
        WISDOM(34, Material.BOOK, "&3Wisdom Stats", List.of("&7Stats that influence how much &3Skill", "&3XP &7you gain."), COMBAT_WISDOM,
                FARMING_WISDOM, FISHING_WISDOM, MINING_WISDOM, FORAGING_WISDOM, ENCHANTING_WISDOM, ALCHEMY_WISDOM, CARPENTRY_WISDOM,
                RUNECRAFTING_WISDOM, TAMING_WISDOM, SOCIAL_WISDOM, HUNTING_WISDOM);

        final int slot;
        final Material material;
        final String name;
        final List<String> description;
        final List<Stat> stats;

        Category(int slot, Material material, String name, List<String> description, Stat... stats) {
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.description = description;
            this.stats = List.of(stats);
        }
    }

    private final Player viewer;

    public StatsMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        for (Map.Entry<Integer, Icon> entry : icons(PlayerSession.of(viewer).stats()).entrySet()) set(entry.getKey(), entry.getValue().stack());
        // What they hold and wear, as it is (a look only: it can't be taken out here).
        PlayerInventory inventory = viewer.getInventory();
        worn(HAND, inventory.getItemInMainHand());
        worn(HELMET, inventory.getHelmet());
        worn(CHESTPLATE, inventory.getChestplate());
        worn(LEGGINGS, inventory.getLeggings());
        worn(BOOTS, inventory.getBoots());
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu").stack(), viewer,
                () -> new SkyBlockMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    private void worn(int slot, ItemStack item) {
        if (item != null && !item.isEmpty()) set(slot, item.clone());
    }

    /** Every slot but the glass, Close and what they hold and wear (those slots show them empty). */
    static Map<Integer, Icon> icons(Stats stats) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(HAND, empty("&7Empty Hand Slot"));
        icons.put(NECKLACE, empty("&7Empty Equipment Slot", " &8> Necklace"));
        icons.put(HELMET, empty("&7Empty Helmet Slot"));
        icons.put(CLOAK, empty("&7Empty Equipment Slot", " &8> Cloak"));
        icons.put(CHESTPLATE, empty("&7Empty Chestplate Slot"));
        icons.put(BELT, empty("&7Empty Equipment Slot", " &8> Belt"));
        icons.put(LEGGINGS, empty("&7Empty Leggings Slot"));
        icons.put(GLOVES, empty("&7Empty Equipment Slot", " &8> Gloves", " &8> Bracelet"));
        icons.put(BOOTS, empty("&7Empty Boots Slot"));
        icons.put(PET, empty("&7Empty Pet Slot"));
        for (Category category : Category.values()) icons.put(category.slot, category(category, stats));
        icons.put(EFFECTS, new Icon(Material.POTION, "&aActive Effects", "&7View and manage all of your active", "&7potion effects.", "",
                "&7Drink Potions or splash them on the", "&7ground to buff yourself!", "", "&7Currently Active: &e0", "",
                "&8Also accessible via /effects.", "", "&eClick to view!"));
        return icons;
    }

    /** An empty slot, as the wiki's menu has them: light grey glass. */
    static Icon empty(String name, String... lore) {
        return new Icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, name, lore);
    }

    /**
     * A category's item: its stats that aren't 0, as " &c❤ Health &f2,206"; with none, the wiki's
     * "You do not have any stats to show in this category!".
     */
    static Icon category(Category category, Stats stats) {
        List<String> lore = new ArrayList<>(category.description);
        lore.add("");
        List<String> lines = new ArrayList<>();
        for (Stat stat : category.stats) {
            if (stats.has(stat)) lines.add(SkyBlockMenu.statLine(stat, stats.get(stat)));
        }
        if (lines.isEmpty()) {
            lore.add("&8You do not have any stats to show in");
            lore.add("&8this category!");
        } else {
            lore.addAll(lines);
            lore.add("");
            lore.add("&eClick for details!");
        }
        return new Icon(category.material, category.name, lore);
    }
}
