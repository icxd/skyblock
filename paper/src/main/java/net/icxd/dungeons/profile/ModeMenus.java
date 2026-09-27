package net.icxd.dungeons.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;

/** The menus on the way to a new profile: pick its mode, then confirm its name. */
final class ModeMenus {
    private static final int BACK = 30;
    private static final int CLOSE = 31;

    private ModeMenus() {
    }

    /** "Choose a SkyBlock Mode": Classic (a Normal profile) or the special modes. */
    static final class ChooseModeMenu extends Menu {
        ChooseModeMenu(Player viewer) {
            super("Choose a SkyBlock Mode", Size.FOUR, viewer);
        }

        @Override
        void items(User user) {
            fill(filler());
            // Hypixel's starts you "on a new tiny island"; there are no islands here.
            set(button(11, item(Material.GRASS_BLOCK, "&aClassic Profile", "&8SkyBlock Mode", "", "&7A SkyBlock adventure with the",
                    "&7default rules.", "", "&7Start from scratch, without", "&7gear and build your way up to",
                    "&7become the &agreatest player &7in the", "&7universe!", "", "&eClick to play this mode!"),
                    () -> new CreateProfileMenu(viewer, ProfileMode.NORMAL, () -> new ChooseModeMenu(viewer)).open(viewer)));
            set(button(15, item(Material.BLAZE_POWDER, "&6Special Modes", "&7Choose a SkyBlock mode with special",
                    "&7rules and unique mechanics.", "", "&eClick to choose a mode!"), () -> new SpecialModeMenu(viewer).open(viewer)));
            set(goBack(BACK, "Profile Management", () -> new ProfileManagementMenu(viewer)));
            set(GUIClickableItem.close(CLOSE));
        }
    }

    /** "Select a Special Mode": Sandbox, this server's own, where Hypixel has Ironman and Stranded. */
    static final class SpecialModeMenu extends Menu {
        SpecialModeMenu(Player viewer) {
            super("Select a Special Mode", Size.FOUR, viewer);
        }

        @Override
        void items(User user) {
            fill(filler());
            set(button(13, item(Material.COMMAND_BLOCK, "&d⚒ Sandbox", "&8Special Mode", "", "&a✔ Every item in /item!",
                    "&a✔ Edit your items freely!", "&c✖ Items can't leave this profile!", "", "&eClick to start new profile!"),
                    () -> new CreateProfileMenu(viewer, ProfileMode.SANDBOX, () -> new SpecialModeMenu(viewer)).open(viewer)));
            set(goBack(BACK, "Choose a SkyBlock Mode", () -> new ChooseModeMenu(viewer)));
            set(GUIClickableItem.close(CLOSE));
        }
    }

    /**
     * "Create a Profile" (or "Create a Sandbox Profile"): the new profile's name, a random fruit none
     * of theirs has, rolled again each time the menu opens, as on Hypixel. Cancel goes back to where
     * they came from.
     */
    static final class CreateProfileMenu extends Menu {
        private final ProfileMode mode;
        private final Supplier<Menu> back;
        private String name;

        CreateProfileMenu(Player viewer, ProfileMode mode, Supplier<Menu> back) {
            super(mode == ProfileMode.SANDBOX ? "Create a Sandbox Profile" : "Create a Profile", Size.THREE, viewer);
            this.mode = mode;
            this.back = back;
        }

        @Override
        void items(User user) {
            fill(filler());
            name = ProfileActions.newName(user);
            List<String> lore = new ArrayList<>(List.of("&7You are creating a new SkyBlock", "&7profile.", ""));
            lore.add(name == null ? "&cThere are no profile names left!" : "&7Profile name: &e" + name);
            // Hypixel's is "&7Mode: ♲ Ironman", in Ironman's own grey. Sandbox keeps its pink here as in its name
            // everywhere else (Hypixel's Stranded keeps its green in Profile Management); our choice.
            if (mode != ProfileMode.NORMAL) lore.add("&7Mode: " + mode.prefix().trim());
            lore.addAll(List.of("", "&7You won't lose any progress.", "&7You can switch between profiles.", ""));
            lore.add(Profiles.cooldownLeft(user.getDocument(), System.currentTimeMillis()) > 0 ? "&cAction on cooldown!" : "&eClick to confirm new profile!");
            set(button(11, item(Material.GREEN_TERRACOTTA, "&aCreate New Profile", lore), () -> ProfileActions.create(viewer, mode, name)));
            set(button(15, item(Material.RED_TERRACOTTA, "&cCancel"), () -> back.get().open(viewer)));
        }
    }
}
