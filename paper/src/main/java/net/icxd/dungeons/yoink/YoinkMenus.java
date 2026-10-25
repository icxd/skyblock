package net.icxd.dungeons.yoink;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;

/**
 * /yoinkprofile's two menus: the real player's profiles to pick from (the one they play on first, as Hypixel's
 * profile menu has it), then what to bring over from the one picked, each part a button that picks it or leaves it,
 * and "Yoink!" to copy them onto the profile you play on (see {@link Yoink}). Main thread.
 */
public final class YoinkMenus {
    static final int[] PROFILE_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
    static final int[] PART_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private YoinkMenus() {
    }

    /** The menu of their profiles. */
    public static void profiles(Player viewer, String owner, List<HypixelProfile> profiles) {
        new Profiles(viewer, owner, profiles).open(viewer, null);
    }

    private static final class Profiles extends GUI {
        private final Player viewer;
        private final String owner;
        private final List<HypixelProfile> profiles;

        Profiles(Player viewer, String owner, List<HypixelProfile> profiles) {
            super("Yoink from " + owner, Size.FOUR);
            this.viewer = viewer;
            this.owner = owner;
            this.profiles = profiles;
        }

        @Override
        public void beforeOpen(Player player) {
            getItems().clear();
            fill(filler());
            for (int i = 0; i < profiles.size() && i < PROFILE_SLOTS.length; i++) {
                HypixelProfile profile = profiles.get(i);
                set(GUIClickableItem.button(PROFILE_SLOTS[i], profileItem(profile), viewer,
                        () -> new Parts(viewer, owner, profiles, profile).open(viewer, MenuSound.CLICK)));
            }
            set(GUIClickableItem.close(31));
        }

        /** "Profile: Banana", with its mode, members, level, purse and bank. */
        static ItemStack profileItem(HypixelProfile profile) {
            List<String> lore = new ArrayList<>();
            lore.add(profile.selected() ? "&8Selected on Hypixel" : "&8Profile");
            lore.add("");
            if (profile.modeName() != null) lore.add("&7Mode: &f" + profile.modeName());
            if (profile.members() > 1) lore.add("&7Co-op members: &a" + profile.members());
            lore.add("&7SkyBlock Level: &b" + profile.level());
            lore.add("&7Purse: &6" + Coins.format(profile.purse()) + " coins");
            lore.add("&7Bank: " + (profile.bank() == null ? "&8not in the API" : "&6" + Coins.format(profile.bank()) + " coins"));
            lore.add("");
            lore.add("&eClick to choose what to yoink!");
            Material material = profile.selected() ? Material.EMERALD_BLOCK : Material.GRASS_BLOCK;
            return item(material, "&eProfile: &a" + (profile.name() == null ? "?" : profile.name()), lore);
        }
    }

    private static final class Parts extends GUI {
        private final Player viewer;
        private final String owner;
        private final List<HypixelProfile> profiles;
        private final HypixelProfile profile;
        private final Set<YoinkPart> picked = EnumSet.noneOf(YoinkPart.class);

        Parts(Player viewer, String owner, List<HypixelProfile> profiles, HypixelProfile profile) {
            super("Yoink " + (profile.name() == null ? "profile" : profile.name()), Size.SIX);
            this.viewer = viewer;
            this.owner = owner;
            this.profiles = profiles;
            this.profile = profile;
        }

        @Override
        public void beforeOpen(Player player) {
            getItems().clear();
            fill(filler());
            YoinkPart[] parts = YoinkPart.values();
            for (int i = 0; i < parts.length && i < PART_SLOTS.length; i++) {
                YoinkPart part = parts[i];
                if (!part.available(profile)) {
                    set(PART_SLOTS[i], item(part.icon(), "&c" + part.title(), "&7" + part.description(), "",
                            "&cNot in the API: its API setting is off."));
                    continue;
                }
                set(GUIClickableItem.button(PART_SLOTS[i], partItem(part, picked.contains(part)), viewer, () -> {
                    if (!picked.remove(part)) picked.add(part);
                    reopen();
                }));
            }
            List<YoinkPart> available = YoinkPart.available(profile, List.of(parts));
            boolean all = !available.isEmpty() && picked.containsAll(available);
            set(GUIClickableItem.button(47, item(all ? Material.LIME_DYE : Material.GRAY_DYE, all ? "&aEverything" : "&7Everything",
                    all ? "&eClick to pick nothing." : "&eClick to pick all of it."), viewer, () -> {
                if (all) picked.clear();
                else picked.addAll(available);
                reopen();
            }));
            set(GUIClickableItem.button(48, item(Material.ARROW, "&aGo Back", "&7To " + owner + "'s profiles"), viewer,
                    () -> new Profiles(viewer, owner, profiles).open(viewer, MenuSound.CLICK)));
            set(GUIClickableItem.button(49, yoinkItem(), viewer, () -> {
                if (picked.isEmpty()) return;
                viewer.closeInventory();
                Yoink.run(viewer, owner, profile, picked);
            }));
            set(GUIClickableItem.close(50));
        }

        private void reopen() {
            open(viewer, null);
        }

        private ItemStack yoinkItem() {
            List<String> lore = new ArrayList<>();
            User user = User.ifLoaded(viewer.getUniqueId());
            String onto = user == null ? "your profile" : "&a" + user.profileName();
            lore.add("&7From &b" + (profile.name() == null ? "?" : profile.name()) + " &7(" + owner + ")");
            lore.add("&7onto " + onto + "&7:");
            if (picked.isEmpty()) {
                lore.add("&8 nothing picked yet");
                lore.add("");
                lore.add("&cPick what to yoink first!");
                return item(Material.GRAY_TERRACOTTA, "&7Yoink!", lore);
            }
            for (YoinkPart part : YoinkPart.values()) if (picked.contains(part)) lore.add("&8 - &f" + part.title());
            lore.add("");
            lore.add("&cThese replace yours!");
            lore.add("&eClick to yoink!");
            return item(Material.LIME_TERRACOTTA, "&aYoink!", lore);
        }

        /** A part: what it brings, and whether it's picked (with a glint). */
        static ItemStack partItem(YoinkPart part, boolean picked) {
            ItemStack item = item(part.icon(), (picked ? "&a" : "&e") + part.title(), "&7" + part.description(), "&7Replaces yours.", "",
                    picked ? "&aWill be yoinked!" : "&8Not picked", picked ? "&eClick to leave it." : "&eClick to yoink it!");
            if (picked) item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            return item;
        }
    }
}
