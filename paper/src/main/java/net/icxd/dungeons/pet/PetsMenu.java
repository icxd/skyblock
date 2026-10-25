package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.menu.SkyBlockMenu;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetData.Reward;
import net.icxd.dungeons.pet.PetText.Where;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * The Pets menu ({@code /pets}, or the SkyBlock Menu's Pets), as recorded (2026-10-25, 00:21.3): the Pets item with the
 * summoned pet, Exp Sharing, +28 Pet Slots, the pets (28 a page, in the sort's order), Pet Score Rewards, Go Back,
 * Close, Search, Pet visibility and Sort. A click on a pet summons it (or despawns the one out), a shift click makes it a
 * favourite, a right click makes it an item again.
 *
 * <p>Not here: Exp Sharing (a click does nothing), Search (nothing), Autopet. Main thread.
 */
public final class PetsMenu extends GUI {
    static final String TITLE = "Pets";
    static final int SUMMARY = 4;
    static final int EXP_SHARING = 7;
    static final int SLOTS = 8;
    static final int PREVIOUS = 45;
    static final int SCORE = 47;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int SEARCH = 50;
    static final int VISIBILITY = 51;
    static final int SORT = 52;
    static final int NEXT = 53;
    /** The pets' slots: four rows of seven inside the border. */
    static final int PER_PAGE = 28;

    private final Player viewer;
    private final int page;

    PetsMenu(Player viewer, int page, int pages) {
        super(pages > 1 ? "(" + page + "/" + pages + ") " + TITLE : TITLE, Size.SIX);
        this.viewer = viewer;
        this.page = page;
    }

    /** Opens its page (1 first) with this sound (null for none). */
    public static void show(Player player, int page, MenuSound sound) {
        User user = User.ifLoaded(player.getUniqueId());
        int pages = pages(user == null ? 0 : Pets.pets(user.profile()).size());
        new PetsMenu(player, Math.max(1, Math.min(page, pages)), pages).open(player, sound);
    }

    static int pages(int pets) {
        return Math.max(1, (pets + PER_PAGE - 1) / PER_PAGE);
    }

    /** The slot of the {@code i}th pet of a page (0 first): 10 to 16, 19 to 25, 28 to 34, 37 to 43. */
    static int slot(int i) {
        return 10 + (i / 7) * 9 + i % 7;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        PetData data = Pets.data();
        for (int slot = 0; slot < getSize(); slot++) {
            boolean inside = slot >= 9 && slot < 45 && slot % 9 != 0 && slot % 9 != 8;
            if (!inside) set(slot, filler());
        }
        OwnedPet active = Pets.active(profile);
        set(SUMMARY, summary(active, data).stack());
        set(GUIClickableItem.button(EXP_SHARING, expSharing().stack(), viewer, () -> {
        }));
        if (Pets.upgrades(profile) < Pets.MOST_UPGRADES) {
            set(GUIClickableItem.button(SLOTS, slots(Pets.slots(profile), user.getBits()).stack(), viewer, () -> buySlots(viewer)));
        }
        List<OwnedPet> pets = Pets.sorted(Pets.pets(profile), Pets.sort(profile), data);
        int pages = pages(pets.size());
        for (int i = 0; i < PER_PAGE; i++) {
            int index = (page - 1) * PER_PAGE + i;
            if (index >= pets.size()) break;
            OwnedPet pet = pets.get(index);
            boolean out = active != null && active.uuid().equals(pet.uuid());
            set(GUIClickableItem.button(slot(i), stack(pet, data, out), viewer, click -> clicked(viewer, pet.uuid(), click, page)));
        }
        if (page > 1) set(GUIClickableItem.button(PREVIOUS, pageItem("&aPrevious Page", page - 1).stack(), viewer, () -> show(viewer, page - 1, null)));
        if (page < pages) set(GUIClickableItem.button(NEXT, pageItem("&aNext Page", page + 1).stack(), viewer, () -> show(viewer, page + 1, null)));
        set(SCORE, score(Pets.score(profile, data), data).stack());
        set(GUIClickableItem.button(BACK, back().stack(), viewer, () -> new SkyBlockMenu(viewer).open(viewer, MenuSound.CLICK)));
        set(GUIClickableItem.close(CLOSE));
        set(SEARCH, search().stack());
        set(GUIClickableItem.button(VISIBILITY, visibility(Pets.visibility(profile), Pets.nametags(profile)).stack(), viewer,
                click -> visibilityClicked(viewer, click, page)));
        set(GUIClickableItem.button(SORT, sort(Pets.sort(profile)).stack(), viewer, click -> sortClicked(viewer, click, page)));
    }

    /** A pet, as the menu shows it: its head, name and lore. */
    static ItemStack stack(OwnedPet pet, PetData data, boolean out) {
        Pet kind = data.pet(pet.type());
        String texture = kind == null || kind.variant(pet.tier()) == null ? null : kind.variant(pet.tier()).texture();
        ItemStack stack = new Icon(Material.PLAYER_HEAD, PetText.name(pet, data, Where.MENU), PetText.lore(pet, data, Where.MENU, out)).stack();
        if (texture != null) Utils.skull(stack, Utils.texture(texture));
        return stack;
    }

    // What a click does

    private static void clicked(Player player, String uuid, ClickType click, int page) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        OwnedPet pet = Pets.find(user.profile(), uuid);
        if (pet == null) {
            show(player, page, null);
            return;
        }
        switch (click) {
            case LEFT -> PetActions.summonOrDespawn(player, user, pet);
            case SHIFT_LEFT -> {
                PetActions.favorite(player, user, pet);
                show(player, page, null);
            }
            case RIGHT, SHIFT_RIGHT -> {
                PetActions.toItem(player, user, pet);
                show(player, page, null);
            }
            default -> {
            }
        }
    }

    private static void visibilityClicked(Player player, ClickType click, int page) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Document profile = user.profile();
        if (click.isRightClick()) Pets.nametags(profile, !Pets.nametags(profile));
        else Pets.visibility(profile, Pets.visibility(profile).next());
        PetFollowers.settingsChanged(player);
        // A pling, as recorded (volume 8, pitch 4.05: the client's most is 2), and the menu again.
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 8, 2);
        show(player, page, null);
    }

    private static void sortClicked(Player player, ClickType click, int page) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Document profile = user.profile();
        Pets.sort(profile, Pets.sort(profile).step(click.isRightClick()));
        show(player, page, null);
        // A lever's click, as recorded (0.55, pitch 2).
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.55f, 2);
    }

    /** "+28 Pet Slots" for 5,000 Bits, twice (the wiki's Pets); what it says once they can is UNKNOWN. */
    private static void buySlots(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Document profile = user.profile();
        if (Pets.upgrades(profile) >= Pets.MOST_UPGRADES) return;
        if (user.getBits() < Pets.UPGRADE_BITS) {
            player.sendMessage(Text.line("&cYou don't have enough Bits!"));
            return;
        }
        profile.put("bits", user.getBits() - Pets.UPGRADE_BITS);
        Pets.upgrade(profile);
        user.save();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
        show(player, 1, null);
    }

    // The items, as recorded

    /** The SkyBlock Menu's Pets: the menu's own, and "&eClick to view!" (recorded); null with none (it's locked then). */
    public static Icon skyBlockMenuIcon(Document profile) {
        if (profile == null || Pets.pets(profile).isEmpty()) return null;
        List<String> lore = new ArrayList<>(PetText.summary(Pets.active(profile), Pets.data()));
        lore.add("");
        lore.add("&eClick to view!");
        return new Icon(Material.BONE, "&aPets", lore);
    }

    static Icon summary(OwnedPet active, PetData data) {
        return new Icon(Material.BONE, "&aPets", PetText.summary(active, data));
    }

    static Icon expSharing() {
        return new Icon(Material.YELLOW_DYE, "&6Exp Sharing", "&7Let your pets gain &aPet Exp &7without", "&7them being actively deployed!", "",
                "&eClick to view!");
    }

    static Icon slots(int slots, int bits) {
        List<String> lore = new ArrayList<>(List.of("&7Unlock a new page of pets.", "", "&7Current Max Pets: &a" + slots, "", "&7Cost",
                "&b" + Text.number(Pets.UPGRADE_BITS) + " Bits", ""));
        lore.add(bits >= Pets.UPGRADE_BITS ? "&eClick to purchase!" : "&cYou don't have enough Bits!");
        return new Icon(Material.DIAMOND, "&a+" + Pets.SLOTS_PER_UPGRADE + " Pet Slots", lore);
    }

    static Icon pageItem(String name, int page) {
        return new Icon(Material.ARROW, name, "&ePage " + page);
    }

    /** Pet Score Rewards: how it's counted, each threshold (the highest reached marked "&5«"), and theirs. */
    static Icon score(int score, PetData data) {
        List<String> lore = new ArrayList<>(List.of("&dPet Score &7is calculated based on", "&7how many &aunique &7pets you have and",
                "&7their &6rarity&7.", "", "&8SPECIAL and VERY SPECIAL Pets do", "&8not contribute towards your Pet", "&8Score.", "",
                "&7You gain an additional score for", "&7each max level pet you have!", ""));
        int reached = -1;
        List<Reward> rewards = data.scoreRewards();
        for (int i = 0; i < rewards.size(); i++) if (score >= rewards.get(i).score()) reached = i;
        for (int i = 0; i < rewards.size(); i++) {
            Reward reward = rewards.get(i);
            lore.add("&6" + reward.score() + " Score&8: +&b" + reward.magicFind() + " Magic Find" + (i == reached ? " &5«" : ""));
        }
        lore.add("");
        lore.add("&9Your Pet Score: &f" + score);
        return new Icon(Material.DIAMOND, "&dPet Score Rewards", lore);
    }

    static Icon back() {
        return new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu");
    }

    static Icon search() {
        return new Icon(Material.OAK_SIGN, "&aSearch", "&7Find pets by name!", "", "&eClick to search!");
    }

    /** As recorded; "&cDisabled" for name tags off is UNKNOWN (never recorded). */
    static Icon visibility(Pets.Visibility current, boolean nametags) {
        List<String> lore = new ArrayList<>(List.of(""));
        for (Pets.Visibility v : Pets.Visibility.values()) lore.add((v == current ? "&3▶ " : "&7  ") + v.label());
        lore.addAll(List.of("", "&7Pet name tags: " + (nametags ? "&aEnabled" : "&cDisabled"), "", "&bRight-click to toggle name tags!",
                "&eLeft-click to switch visibility!"));
        return new Icon(Material.STONE_BUTTON, "&aPet visibility", lore);
    }

    static Icon sort(Pets.Sort current) {
        List<String> lore = new ArrayList<>(List.of(""));
        for (Pets.Sort s : Pets.Sort.values()) lore.add((s == current ? "&b▶ " : "&7  ") + s.label());
        lore.addAll(List.of("", "&eClick to switch sort!", "&bRight-Click to go backwards!"));
        return new Icon(Material.HOPPER, "&aSort", lore);
    }
}
