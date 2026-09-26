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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionType;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.RefreshingGUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * Mort's menu before a run starts, as on Hypixel: the party along the top (ready or not), the
 * ready toggle, the five classes with how many picked each, and the auto ready up toggle. It
 * reopens after every click, like Hypixel's, and keeps up with the rest of the party.
 *
 * <p>Left out until the class system: each class's stats, the Class Details menu (right click) and
 * the Dungeon Orb.
 */
final class ReadyUpMenu extends GUI implements RefreshingGUI {
    private static final int READY = 13;
    private static final int CLOSE = 49;
    private static final int AUTO_READY = 53;
    private static final int FIRST_CLASS = 29;
    /** Under each class, how many picked it. */
    private static final int FIRST_COUNT = 38;
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
            set(button(FIRST_CLASS + i, classItem(dungeonClass, dungeonClass == mine), event -> {
                if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) run.selectClass(viewer, dungeonClass);
            }));
            set(FIRST_COUNT + i, countItem(dungeonClass, members));
        }

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
        List<String> lore = new ArrayList<>();
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

    private static ItemStack filler() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return pane;
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.color(name));
        meta.setLore(Utils.colorList(List.of(lore)));
        item.setItemMeta(meta);
        return item;
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
