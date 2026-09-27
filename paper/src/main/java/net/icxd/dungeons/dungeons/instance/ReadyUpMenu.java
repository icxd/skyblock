package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionType;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.classes.ClassBonus;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.RefreshingGUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * Mort's menu before a run starts, as on Hypixel: the party along the top (ready or not), the
 * ready toggle, the five classes with their stats at the viewer's levels and how many picked each
 * (a right click on one opens its {@link ClassDetailsMenu}), the Dungeon Orb and the auto ready up
 * toggle. It reopens after every click, like Hypixel's, and keeps up with the rest of the party.
 */
final class ReadyUpMenu extends GUI implements RefreshingGUI {
    private static final int READY = 13;
    private static final int CLOSE = 49;
    private static final int AUTO_READY = 53;
    private static final int FIRST_CLASS = 29;
    /** Under each class, how many picked it. */
    private static final int FIRST_COUNT = 38;
    private static final int ORB = 45;
    private static final List<String> DOUBLED = List.of("&aAll positive bonus stats are doubled if only 1",
            "&aplayer uses this class in a dungeon.");

    private final DungeonRun run;
    private final Player viewer;

    ReadyUpMenu(DungeonRun run, Player viewer) {
        super("Ready Up", Size.SIX);
        this.run = run;
        this.viewer = viewer;
        items();
    }

    @Override
    public long refreshRate() {
        return 10;
    }

    @Override
    public void items() {
        fill(filler());
        List<DungeonRun.Member> members = run.members();
        // The heads sit in the middle of the top row: slot 4 alone, 2-6 for five.
        int first = 4 - members.size() / 2;
        for (int i = 0; i < members.size() && i < 5; i++) set(first + i, head(members.get(i)));

        DungeonRun.Member me = run.member(viewer.getUniqueId());
        boolean ready = me != null && me.ready;
        set(button(READY, ready
                ? item(Material.LIME_STAINED_GLASS_PANE, "&aReady", "&7You are marked as ready! The fight",
                        "&7will start when all players have", "&7readied up and have the same tier", "&7selected!")
                : item(Material.RED_STAINED_GLASS_PANE, "&cNot Ready", "&7Click to mark yourself as ready to", "&7start!", "",
                        "&eClick to ready up!"),
                event -> run.setReady(viewer, !ready)));

        DungeonClass mine = run.classOf(viewer.getUniqueId());
        DungeonClass[] classes = DungeonClass.values();
        for (int i = 0; i < classes.length; i++) {
            DungeonClass dungeonClass = classes[i];
            set(classButton(FIRST_CLASS + i, dungeonClass, dungeonClass == mine));
            set(FIRST_COUNT + i, countItem(dungeonClass, members));
        }
        set(orb());

        set(GUIClickableItem.close(CLOSE));

        User user = User.cached(viewer.getUniqueId());
        boolean auto = user != null && user.isLoaded() && DungeonProfile.autoReadyUp(user);
        set(button(AUTO_READY, item(auto ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS, "&aToggle Auto Ready Up",
                "&7Auto ready up when joining &aThe", "&aCatacombs&7 if you have the", "&7requirements, so you don't have to!", "",
                "&7Currently: " + (auto ? "&aEnabled" : "&cDisabled"), "", "&eClick to toggle!"), event -> {
            if (user != null && user.isLoaded()) DungeonProfile.toggleAutoReadyUp(viewer, user);
        }));
    }

    private ItemStack head(DungeonRun.Member member) {
        ItemStack head = item(Material.PLAYER_HEAD, member.display(), member.ready ? "&aReady" : "&cNot Ready");
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        Player online = Bukkit.getPlayer(member.id);
        meta.setPlayerProfile(online != null ? online.getPlayerProfile() : Bukkit.createProfile(member.id, member.name));
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack classItem(DungeonClass dungeonClass, boolean selected) {
        List<String> lore = new ArrayList<>(ClassBonus.readyUpLines(dungeonClass, levelOf(dungeonClass)));
        lore.add("");
        lore.add("&f&lClass Passives");
        for (String passive : dungeonClass.getPassives()) lore.add("&8∙ &a" + passive);
        lore.add("");
        lore.add("&f&lDungeon Orb Abilities");
        for (String ability : dungeonClass.getOrbAbilities()) lore.add("&8∙ &6" + ability);
        lore.add("");
        lore.add("&f&lGhost Abilities");
        for (String ability : dungeonClass.getGhostAbilities()) lore.add("&8∙ &f" + ability);
        lore.add("");
        lore.addAll(DOUBLED);
        lore.add("");
        lore.add("&eLeft click to select!");
        lore.add("&eRight click for more details!");
        ItemStack item = item(dungeonClass.getIcon(), "&7[Lvl " + levelOf(dungeonClass) + "] &a" + dungeonClass.getDisplayName(),
                lore.toArray(String[]::new));
        if (item.getItemMeta() instanceof PotionMeta potion) {
            potion.setBasePotionType(PotionType.HEALING);
            item.setItemMeta(potion);
        }
        // No attack damage, armor or potion effect lines under the lore.
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.POTION_CONTENTS, DataComponentTypes.DYED_COLOR)
                .build());
        if (selected) item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return item;
    }

    private int levelOf(DungeonClass dungeonClass) {
        User user = User.cached(viewer.getUniqueId());
        return user == null || !user.isLoaded() ? 0 : DungeonProfile.classLevel(user, dungeonClass);
    }

    private ItemStack countItem(DungeonClass dungeonClass, List<DungeonRun.Member> members) {
        List<DungeonRun.Member> picked = members.stream().filter(m -> run.classOf(m.id) == dungeonClass).toList();
        if (picked.isEmpty()) return item(Material.GRAY_DYE, "&c0 Players", DOUBLED.toArray(String[]::new));
        List<String> lore = new ArrayList<>();
        lore.add("&eSelected by:");
        for (DungeonRun.Member m : picked) lore.add("&7  - " + m.display());
        lore.add("");
        lore.addAll(DOUBLED);
        ItemStack item = item(Material.LIME_DYE, "&a" + picked.size() + " Player(s)", lore.toArray(String[]::new));
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return item;
    }

    /** A class: a left click picks it (and the menu opens again), a right click shows its Class Details. */
    private GUIClickableItem classButton(int slot, DungeonClass dungeonClass, boolean selected) {
        ItemStack stack = classItem(dungeonClass, selected);
        GUIClickableItem select = button(slot, stack, event -> {
            if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) run.selectClass(viewer, dungeonClass);
        });
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                if (event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.SHIFT_RIGHT) {
                    select.run(event);
                    return;
                }
                Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
                    if (viewer.isOnline()) new ClassDetailsMenu(run, viewer, dungeonClass, levelOf(dungeonClass)).open(viewer);
                });
            }

            @Override
            public int slot() {
                return slot;
            }

            @Override
            public ItemStack stack() {
                return stack;
            }
        };
    }

    /**
     * The Dungeon Orb, as recorded ("Already claimed!" with one in the inventory). Without one it can be
     * claimed here (MCW Dungeon Orb); that item's last line is UNKNOWN.
     */
    private GUIClickableItem orb() {
        boolean claimed = RunClasses.hasOrb(viewer);
        ItemStack stack = item(Material.PLAYER_HEAD, "&6Dungeon Orb", "&7When entering a Dungeon, this stone", "&7adapts to its user and allows them to",
                "&7use their class abilities.", "", "&6&lLEGENDARY DUNGEON ITEM", "", claimed ? "&cAlready claimed!" : "&eClick to claim!");
        Utils.skull(stack, Utils.texture(RunClasses.ORB_TEXTURE));
        return button(ORB, stack, event -> {
            if (!RunClasses.hasOrb(viewer)) RunClasses.claimOrb(run, viewer);
        });
    }

    /** Does something, then opens the menu again (as Hypixel's does). */
    private GUIClickableItem button(int slot, ItemStack stack, Consumer<InventoryClickEvent> action) {
        return new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                action.accept(event);
                // Not from inside the click.
                Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
                    if (viewer.isOnline()) new ReadyUpMenu(run, viewer).open(viewer);
                });
            }

            @Override
            public int slot() {
                return slot;
            }

            @Override
            public ItemStack stack() {
                return stack;
            }
        };
    }
}
