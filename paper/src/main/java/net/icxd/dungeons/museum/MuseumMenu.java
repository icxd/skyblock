package net.icxd.dungeons.museum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.museum.MuseumData.Category;
import net.icxd.dungeons.user.User;

/**
 * Your Museum, as recorded (2026-10-25, 00:58.4): the Museum's head with how much is donated, the seven categories
 * (each opens its pages, see {@link MuseumCategoryMenu}), Special Items, Museum Milestones ({@link MilestonesMenu}),
 * Close, Museum Search and the Museum Appraisal Service. A click on an item in their inventory donates it.
 *
 * <p>Not here: the head's Top Items (the five donated items worth most; Hypixel's item values aren't known), Museum
 * Search (a click does nothing), the appraisal (Madame Goldsworth's, for 10M once at Milestone 3), Special Items'
 * donations (their page is empty). Main thread.
 */
public final class MuseumMenu extends MuseumGui {
    static final String TITLE = "Your Museum";
    static final int HEAD = 4;
    static final int SPECIAL = 32;
    static final int MILESTONES = 48;
    static final int CLOSE = 49;
    static final int SEARCH = 50;
    static final int APPRAISAL = 51;
    static final String HEAD_TEXTURE = "438cf3f8e54afc3b3f91d20a49f324dca1486007fe545399055524c17941f4dc";
    /** The Appraisal Service's milestone. */
    static final int APPRAISAL_MILESTONE = 3;

    /** How Your Museum shows a category, as recorded: its slot, item and colour, and its lines as Hypixel wraps them. */
    record Look(int slot, Material material, String texture, String color, List<String> description) {
    }

    static final Map<String, Look> LOOKS = Map.of(
            "COMBAT", new Look(20, Material.STONE_SWORD, null, "&c", lines("&cCombat Skill &7that you have donated", "&7to the &9Museum&7!")),
            "FARMING", new Look(21, Material.GOLDEN_HOE, null, "&a", lines("&aFarming Skill &7that you have donated", "&7to the &9Museum&7!")),
            "MINING", new Look(22, Material.STONE_PICKAXE, null, "&6", lines("&6Mining Skill &7that you have donated to", "&7the &9Museum&7!")),
            "FISHING", new Look(23, Material.FISHING_ROD, null, "&b", lines("&bFishing Skill &7that you have donated", "&7to the &9Museum&7!")),
            "FORAGING", new Look(24, Material.JUNGLE_SAPLING, null, "&2", lines("&2Foraging Skill &7that you have donated", "&7to the &9Museum&7!")),
            "DUNGEONEERING", new Look(30, Material.PLAYER_HEAD, "9b56895b9659896ad647f58599238af532d46db9c1b0389b8bbeb70999dab33d", "&4",
                    lines("&4Dungeoneering Skill &7that you have", "&7donated to the &9Museum&7!")),
            "HUNTING", new Look(31, Material.LEAD, null, "&e", lines("&eHunting Skill &7that you have donated", "&7to the &9Museum&7!")));

    private static List<String> lines(String second, String third) {
        return List.of("&7View all of the items related to the", second, third);
    }

    private MuseumMenu(Player viewer) {
        super(TITLE, Size.SIX, viewer);
    }

    /** Opens Your Museum (from Madame Goldsworth, or another Museum menu: with a click, as recorded). */
    public static void show(Player player, MenuSound sound) {
        new MuseumMenu(player).open(player, sound);
    }

    @Override
    void reopen(Player player) {
        show(player, MenuSound.CLICK);
    }

    /** "&cCombat": a category's name in its colour (white for one Your Museum doesn't know). */
    static String colored(String category) {
        Category found = Museum.data().category(category);
        Look look = LOOKS.get(category);
        return (look == null ? "&f" : look.color()) + (found == null ? category : found.name());
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        Document profile = user == null ? new Document() : user.profile();
        MuseumData data = Museum.data();
        set(HEAD, head(Museum.counted(profile, data), data.count()).stack());
        for (Category category : data.categories()) {
            Look look = LOOKS.get(category.id());
            if (look == null) continue;
            Icon icon = category(category, look, Museum.counted(profile, data, category), true);
            set(GUIClickableItem.button(look.slot(), icon.stack(), viewer, () -> new MuseumCategoryMenu(viewer, category.id(), 1).open(viewer, MenuSound.CLICK)));
        }
        set(GUIClickableItem.button(SPECIAL, special(true).stack(), viewer, () -> new MuseumCategoryMenu(viewer, null, 1).open(viewer, MenuSound.CLICK)));
        int xp = Museum.xp(profile, data);
        set(GUIClickableItem.button(MILESTONES, milestones(xp, data.totalXp()).stack(), viewer, () -> new MilestonesMenu(viewer).open(viewer, MenuSound.CLICK)));
        set(GUIClickableItem.close(CLOSE));
        set(SEARCH, search().stack());
        set(APPRAISAL, appraisal(Museum.milestone(xp)).stack());
    }

    // The items, as recorded

    static Icon head(int counted, int of) {
        List<String> lore = new ArrayList<>(List.of("&7The &9Museum &7is a compendium of all of", "&7your items in SkyBlock. Donate items",
                "&7to your Museum to unlock rewards.", "", "&7Other players can visit your Museum", "&7at any time! Display your best items",
                "&7proudly for all to see.", ""));
        lore.addAll(MuseumText.donated(counted, of));
        return new Icon(Material.PLAYER_HEAD, "&9Museum", lore, HEAD_TEXTURE);
    }

    /** A category's item: on Your Museum with "Click to view!", atop its own pages without. */
    static Icon category(Category category, Look look, int counted, boolean click) {
        List<String> lore = new ArrayList<>(look.description());
        lore.add("");
        lore.addAll(MuseumText.donated(counted, category.entries().size()));
        if (click) {
            lore.add("");
            lore.add(MuseumText.CLICK_TO_VIEW);
        }
        return new Icon(look.material(), look.color() + category.name(), lore, look.texture());
    }

    /** Special Items, with none donated (none can be, here). */
    static Icon special(boolean click) {
        List<String> lore = new ArrayList<>(List.of("&7View all of the &dSpecial Items &7that you", "&7have donated to the &9Museum&7!", "",
                "&7These items don't count towards", "&7Museum progress and rewards, but", "&7are cool nonetheless. Items that are",
                "&9rare &7and &6prestigious &7fit into this", "&7category, and can be displayed in", "&7the Museum.", "", "&7Items Donated: &b0"));
        if (click) {
            lore.add("");
            lore.add(MuseumText.CLICK_TO_VIEW);
        }
        return new Icon(Material.CAKE, "&dSpecial Items", lore);
    }

    static Icon milestones(int xp, int total) {
        List<String> lore = new ArrayList<>(MuseumText.milestonesLore(xp, total));
        lore.add("");
        lore.add(MuseumText.CLICK_TO_VIEW);
        return new Icon(Material.GOLD_BLOCK, "&6Museum Milestones", lore);
    }

    static Icon search() {
        return new Icon(Material.OAK_SIGN, "&aMuseum Search", "&7Search your entire Museum for a", "&7specific item or armor set!", "",
                "&eClick to search for an item!");
    }

    /** Locked till Milestone 3, as recorded; past it, what it says is UNKNOWN (the appraisal isn't here): its first lines. */
    static Icon appraisal(int milestone) {
        List<String> lore = new ArrayList<>(List.of("&7Eleanor will evaluate the total value", "&7of your Museum."));
        if (milestone < APPRAISAL_MILESTONE) lore.addAll(List.of("", "&cUnlocked at Museum Milestone " + APPRAISAL_MILESTONE));
        return new Icon(Material.DIAMOND, "&6Museum Appraisal Service", lore);
    }
}
