package net.icxd.dungeons.profile;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.SkyBlockMenu;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * "Profile Management" ({@code /profiles}): the five profile slots, as Hypixel's. Profiles come first,
 * oldest first, then the empty slots their rank has, then the locked ones; Go Back is to the SkyBlock
 * Menu. Left out: the co-op lines.
 */
public final class ProfileManagementMenu extends Menu {
    private static final int FIRST = 11;
    private static final int BACK = 30;
    private static final int CLOSE = 31;

    public ProfileManagementMenu(Player viewer) {
        super("Profile Management", Size.FOUR, viewer);
    }

    @Override
    void items(User user) {
        fill(filler());
        List<Profiles.Entry> profiles = Profiles.ordered(user.getDocument());
        int unlocked = Profiles.slots(user.getRank());
        for (int i = 0; i < Profiles.MAX_SLOTS; i++) {
            int slot = FIRST + i;
            if (i < profiles.size()) {
                String id = profiles.get(i).id();
                set(button(slot, profileItem(user, profiles.get(i)), () -> new ManageProfileMenu(viewer, id).open(viewer)));
            } else if (i < unlocked) {
                set(button(slot, emptySlot(), () -> {
                    String reason = ProfileActions.cantChange(viewer, User.ifLoaded(viewer.getUniqueId()));
                    if (reason == null) {
                        new ModeMenus.ChooseModeMenu(viewer).open(viewer);
                        return;
                    }
                    viewer.closeInventory();
                    viewer.sendMessage(Text.line(reason));
                }));
            } else {
                set(slot, lockedSlot(i + 1));
            }
        }
        set(button(BACK, item(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu"), () -> new SkyBlockMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    private ItemStack profileItem(User user, Profiles.Entry entry) {
        boolean selected = entry.id().equals(user.profileId());
        ProfileMode mode = entry.mode();
        List<String> lore = new ArrayList<>();
        lore.add(selected ? "&8Selected slot" : "&8Slot in use");
        lore.add("");
        lore.addAll(summary(user, entry));
        lore.add("");
        if (mode == ProfileMode.SANDBOX) {
            // Ours, in the style of Ironman's warning.
            lore.add("&6Items can't leave this");
            lore.add("&6profile in any way.");
            lore.add("");
        }
        if (selected) {
            lore.add("&aYou are playing on this profile!");
            lore.add("");
        }
        lore.add(selected ? "&eClick to manage profile!" : "&eClick to manage!");
        // Hypixel keeps a special mode's own item on its profile; the one you're on is an emerald block.
        Material material = selected ? Material.EMERALD_BLOCK : mode == ProfileMode.SANDBOX ? Material.COMMAND_BLOCK : Material.GRASS_BLOCK;
        return item(material, mode.prefix() + "&eProfile: &a" + entry.name(), lore);
    }

    /**
     * Hypixel's, without the island (there are none here), the Ender Chest (vanilla's is one per
     * player, and closed; see SandboxStorage) and the co-op lines.
     */
    private static ItemStack emptySlot() {
        return item(Material.OAK_BUTTON, "&eEmpty Profile Slot",
            "&8Available",
            "",
            "&7Use this slot if you want to",
            "&7start a new SkyBlock adventure.",
            "",
            "&7Each profile has its own:",
            "&8• &7Inventory",
            "&8• &7Bank & Purse",
            "&8• &7Quests",
            "&8• &7Collections",
            "",
            "&4&lWARNING:&c Creation of profiles",
            "&cwhich boost other profiles will",
            "&cbe considered abusive and",
            "&cpunished.",
            "",
            "&eClick to create a profile!");
    }

    /** Hypixel sells the 5th slot for gems; here the slots come with ranks. */
    private static ItemStack lockedSlot(int number) {
        ItemStack item = item(Material.BEDROCK, "&6Profile Slot #" + number, "&8Unavailable", "",
                "&7Requires " + label(Profiles.slotRank(number)) + "&7.");
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return item;
    }

    private static String label(Rank rank) {
        return switch (rank) {
            case VIP_PLUS -> "&aVIP&6+";
            case MVP_PLUS -> "&bMVP&6+";
            case STAFF -> "&cSTAFF";
            default -> "&7" + rank.name();
        };
    }
}
