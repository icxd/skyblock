package net.icxd.dungeons.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.collection.CollectionMenus;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.collection.CollectionsMenu;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.profile.ProfileManagementMenu;
import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.recipe.CraftingTable;
import net.icxd.dungeons.recipe.RecipeBook;
import net.icxd.dungeons.recipe.RecipeMenus;
import net.icxd.dungeons.recipe.Recipes;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.SkillText;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.skill.SkillsMenu;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.storage.LoadoutsMenu;
import net.icxd.dungeons.storage.StorageMenu;
import net.icxd.dungeons.storage.YourBagsMenu;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.SkyBlockTime;
import net.icxd.dungeons.utils.Text;

/**
 * The SkyBlock Menu ({@code /sbmenu}, or the nether star in hotbar slot 9), as recorded on Hypixel
 * (the SkyBlock Menu tour, 2026-09-27: Banana's at 00:18.2 and the new profile Lemon's at 04:29.1).
 * What this plugin has is filled in: the stats, the skill average, the SkyBlock level, the profiles,
 * the date, and the collections and recipes found. What it doesn't have yet (pets, the bank's banker,
 * fast travel) is shown as Lemon's menu showed it, a profile with none of it; the Booster Cookie, which
 * Lemon's didn't have, is left out, and so are the calendar's events. Stats & Equipment, Your Skills,
 * Storage, Your Bags, Loadouts, Collections, the Recipe Book, the Crafting Table and Profile Management
 * open; the rest do nothing yet. Stat icons are the classic symbols (Hypixel's are its resource pack's
 * glyphs). Main thread.
 */
public final class SkyBlockMenu extends GUI {
    public static final String TITLE = "SkyBlock Menu";
    static final int STATS = 13;
    static final int SKILLS = 19;
    static final int COLLECTIONS = 20;
    static final int RECIPES = 21;
    static final int LEVELING = 22;
    static final int QUESTS = 23;
    static final int CALENDAR = 24;
    static final int STORAGE = 25;
    static final int BAGS = 29;
    static final int PETS = 30;
    static final int CRAFTING = 31;
    static final int LOADOUTS = 32;
    static final int BANK = 33;
    static final int FAST_TRAVEL = 47;
    static final int PROFILES = 48;
    static final int CLOSE = 49;
    static final int SETTINGS = 50;

    /** SkyBlock levels are 100 XP each. */
    static final int XP_PER_LEVEL = 100;
    /** The level's colour in "[88]", from 0, 40, 80 and on every 40 levels (the fandom wiki's SkyBlock Levels, Prefix Color). */
    private static final String[] LEVEL_COLORS = {"&7", "&f", "&e", "&a", "&2", "&b", "&3", "&9", "&d", "&5", "&6", "&c", "&4"};
    /** The seven stats Stats & Equipment lists, in the recorded order. */
    private static final List<Stat> SUMMARY = List.of(Stat.SPEED, Stat.STRENGTH, Stat.DEFENSE, Stat.CRIT_DAMAGE, Stat.CRIT_CHANCE,
            Stat.HEALTH, Stat.INTELLIGENCE);

    // Head skins, from the recording's packets (both menus have the same ones).
    static final String BAGS_HEAD = "1a11a7f11bcd5784903c5201d08261c4df8379109d6e611c1cd3ededf031afed";
    static final String LEVELING_HEAD = "3255327dd8e90afad681a19231665bea2bd06065a09d77ac1408837f9e0b242";
    static final String FAST_TRAVEL_HEAD = "35f4b40cef9e017cd4112d26b62557f8c1d5b189da2e99534222bc8cec7d9196";

    /**
     * What the menu shows of a player: their stats, the mean of their skill levels, their SkyBlock XP,
     * how many profiles they have of how many slots, the one they play on, the SkyBlock date, and how many
     * collections they've found and recipes they've unlocked, of how many.
     */
    record View(Stats stats, double skillAverage, int skyBlockXp, int profiles, int profileSlots, String profileName, SkyBlockTime time,
                int collectionsFound, int collections, int recipesUnlocked, int recipes) {
        static View of(Player player, User user) {
            int[] found = Collections.foundOfAll(user.profile());
            return new View(PlayerSession.of(player).stats(), Skills.average(user.profile()), user.getSkyBlockXp(),
                    Profiles.ordered(user.getDocument()).size(), Profiles.slots(user.getRank()), user.profileName(), SkyBlockTime.now(),
                    found[0], found[1], Recipes.unlocked(user.profile(), Recipes.data().book()), Recipes.data().book().size());
        }
    }

    private final Player viewer;

    public SkyBlockMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    /** Opens it for them on the next tick (not from inside the click or command that asks for it). */
    public static void openLater(Player player) {
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (player.isOnline()) new SkyBlockMenu(player).open(player);
        });
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        for (Map.Entry<Integer, Icon> entry : icons(View.of(viewer, user)).entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = entry.getValue().stack();
            if (slot == STATS && stack.getItemMeta() instanceof SkullMeta head) {
                // Their own head.
                head.setPlayerProfile(viewer.getPlayerProfile());
                stack.setItemMeta(head);
            }
            switch (slot) {
                case STATS -> set(GUIClickableItem.button(slot, stack, viewer, () -> new StatsMenu(viewer).open(viewer)));
                case SKILLS -> set(GUIClickableItem.button(slot, stack, viewer, () -> new SkillsMenu(viewer).open(viewer)));
                case PROFILES -> set(GUIClickableItem.button(slot, stack, viewer, () -> new ProfileManagementMenu(viewer).open(viewer)));
                case STORAGE -> set(GUIClickableItem.button(slot, stack, viewer, () -> new StorageMenu(viewer).open(viewer)));
                case BAGS -> set(GUIClickableItem.button(slot, stack, viewer, () -> new YourBagsMenu(viewer).open(viewer)));
                case LOADOUTS -> set(GUIClickableItem.button(slot, stack, viewer, () -> new LoadoutsMenu(viewer, 0).open(viewer)));
                case COLLECTIONS -> set(GUIClickableItem.button(slot, stack, viewer, () -> new CollectionsMenu(viewer).open(viewer)));
                case RECIPES -> set(GUIClickableItem.button(slot, stack, viewer, () -> new RecipeBook(viewer).open(viewer)));
                case CRAFTING -> set(GUIClickableItem.button(slot, stack, viewer, () -> new CraftingTable(viewer).open(viewer)));
                default -> set(slot, stack);
            }
        }
        set(GUIClickableItem.close(CLOSE));
    }

    // What each slot shows

    /** Every slot but the glass and Close, by slot. */
    static Map<Integer, Icon> icons(View view) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(STATS, stats(view.stats()));
        icons.put(SKILLS, new Icon(Material.DIAMOND_SWORD, "&aYour Skills", SkillsMenu.summary(view.skillAverage(), true)));
        icons.put(COLLECTIONS, CollectionMenus.summary(view.collectionsFound(), view.collections(), true));
        icons.put(RECIPES, RecipeMenus.summary(view.recipesUnlocked(), view.recipes(), true));
        icons.put(LEVELING, leveling(view.skyBlockXp()));
        icons.put(QUESTS, new Icon(Material.WRITABLE_BOOK, "&aQuests & Chapters", "&7Each island has its own series of",
                "&bChapters &7for you to complete!", "", "&7Complete tasks within a Chapter to", "&7earn small &6rewards&7, or complete",
                "&7entire Chapters to earn big ones!", "", "&7Some islands also have &aQuests &7for", "&7you to complete! Some items can only",
                "&7be obtained through Quests.", "", "&eClick to view!"));
        icons.put(CALENDAR, calendar(view.time()));
        icons.put(STORAGE, new Icon(Material.CHEST, "&aStorage", "&7Store global items that you want to", "&7access at any time from anywhere",
                "&7here.", "", "&8Also accessible via /storage", "", "&eClick to view!"));
        icons.put(BAGS, new Icon(Material.PLAYER_HEAD, "&aYour Bags", List.of("&7Different bags allow you to store",
                "&7many different items inside!", "", "&8Also accessible via /bags", "", "&eClick to open!"), BAGS_HEAD));
        // No pets yet.
        icons.put(PETS, new Icon(Material.GRAY_DYE, "&cPets", "&7View and manage all of your Pets.", "", "&cFind your first pet to unlock!"));
        icons.put(CRAFTING, new Icon(Material.CRAFTING_TABLE, "&aCrafting Table", "&7Opens the crafting grid.", "",
                "&8Also accessible via /craft", "", "&eClick to open!"));
        icons.put(LOADOUTS, new Icon(Material.BARREL, "&aLoadouts", "&7View and edit preset armor and", "&7equipment sets with other settings to",
                "&7make switching activities easy.", "", "&8Also accessible via /loadouts", "", "&eClick to view!"));
        // No Personal Bank yet, even with Emerald VI.
        icons.put(BANK, new Icon(Material.GRAY_DYE, "&cPersonal Bank", "&7Contact your Banker from anywhere.", "",
                "&cRequires &aEmerald Collection VI"));
        // Nowhere visited yet.
        icons.put(FAST_TRAVEL, new Icon(Material.PLAYER_HEAD, "&bFast Travel", List.of("&7Teleport to islands you've already", "&7visited.", "",
                "&8Also accessible via /warp", "", "&cYou haven't unlocked this yet!"), FAST_TRAVEL_HEAD));
        icons.put(PROFILES, profiles(view.profiles(), view.profileSlots(), view.profileName()));
        icons.put(SETTINGS, new Icon(Material.REDSTONE_TORCH, "&aSettings", "&7View and edit your SkyBlock settings.", "",
                "&8Also accessible via /viewsettings.", "", "&eClick to view!"));
        return icons;
    }

    /** Stats & Equipment: seven of their stats, always these (a new profile's Strength shows 0). Their own head. */
    static Icon stats(Stats stats) {
        List<String> lore = new ArrayList<>(List.of("&7View your equipment, stats,", "&7achievements, and more!", ""));
        for (Stat stat : SUMMARY) lore.add(statLine(stat, stats.get(stat)));
        lore.addAll(List.of(" &8and more...", "", "&8Also accessible via /stats", "", "&eClick to view!"));
        return new Icon(Material.PLAYER_HEAD, "&aStats & Equipment", lore);
    }

    /**
     * " &c❁ Strength &f589.25": a stat's line in the stats items, its value in white. A white stat's
     * value has no colour of its own: " &f✦ Speed 400".
     */
    static String statLine(Stat stat, double value) {
        return " " + stat.label() + " " + (stat.getColor() == 'f' ? "" : "&f") + Text.number(value) + stat.getUnit();
    }

    static int level(int skyBlockXp) {
        return skyBlockXp / XP_PER_LEVEL;
    }

    static String levelColor(int level) {
        return LEVEL_COLORS[Math.min(level / 40, LEVEL_COLORS.length - 1)];
    }

    /** Their level and the next one's progress, on a dark aqua bar: "&b34&3/&b100 XP". */
    static Icon leveling(int skyBlockXp) {
        int level = level(skyBlockXp);
        int into = skyBlockXp - level * XP_PER_LEVEL;
        String bar = SkillText.strip((double) into / XP_PER_LEVEL).replace("&2&l&m", "&3&l&m") + " &b" + into + "&3/&b" + XP_PER_LEVEL + " XP";
        return new Icon(Material.PLAYER_HEAD, "&aSkyBlock Leveling", List.of("&7Your SkyBlock Level: &8[" + levelColor(level) + level + "&8]", "",
                "&7Determine how far you've", "&7progressed in SkyBlock and earn", "&7rewards from completing unique", "&7tasks.", "",
                "&7Progress to Level " + (level + 1) + ":", bar, "", "&8Also accessible via /levels", "", "&eClick to view!"), LEVELING_HEAD);
    }

    /** The date; there are no events yet, so no Active Event and Next Event. */
    static Icon calendar(SkyBlockTime time) {
        return new Icon(Material.CLOCK, "&aCalendar and Events", "&7View the SkyBlock Calendar, upcoming", "&7events, and event rewards!", "",
                "&7Date: &a" + time.calendarDate(), "", "&8Also accessible via /calendar", "", "&eClick to view!");
    }

    /** How many profiles of their slots, and the one they're on. */
    static Icon profiles(int profiles, int slots, String playingOn) {
        return new Icon(Material.NAME_TAG, "&aProfile Management", "&7You can have multiple SkyBlock", "&7profiles at the same time.", "",
                "&7Each profile has its own island,", "&7inventory, quest log...", "", "&7Profiles: &e" + profiles + "&6/&e" + slots,
                "&7Playing on: &a" + playingOn, "", "&bPlay with friends using /coopadd <name>!", "", "&8Also accessible via /profiles", "",
                "&eClick to view!");
    }
}
