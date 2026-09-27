package net.icxd.dungeons.dungeons.instance;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.classes.ClassDetails;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.utils.Utils;

/**
 * "Class Details", a right click on a class in Ready Up, as recorded (RUN2 00:57.6 to 01:04.3): 36
 * slots of black glass, the class's passives in 11 (on its icon), its Dungeon Orb abilities in 13 and
 * its ghost abilities in 15 (on heads), "Go Back" to Ready Up in 31. At the viewer's class level.
 */
final class ClassDetailsMenu extends GUI {
    private static final int PASSIVES = 11;
    private static final int ORB = 13;
    private static final int GHOST = 15;
    private static final int BACK = 31;

    ClassDetailsMenu(DungeonRun run, Player viewer, DungeonClass dungeonClass, int level) {
        super("Class Details", Size.FOUR);
        fill(filler());
        double intelligence = PlayerSession.of(viewer).stats().get(Stat.INTELLIGENCE);
        set(PASSIVES, icon(dungeonClass, ClassDetails.passives(dungeonClass, level, intelligence)));
        set(ORB, head(ClassDetails.orbTexture(dungeonClass), ClassDetails.orbAbilities(dungeonClass, level)));
        set(GHOST, head(ClassDetails.GHOST_TEXTURE, ClassDetails.ghostAbilities(dungeonClass)));
        ItemStack back = item(Material.ARROW, "&aGo Back", "&7To Ready Up");
        set(new GUIClickableItem() {
            @Override
            public void run(InventoryClickEvent event) {
                Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
                    if (viewer.isOnline()) new ReadyUpMenu(run, viewer).open(viewer);
                });
            }

            @Override
            public int slot() {
                return BACK;
            }

            @Override
            public ItemStack stack() {
                return back;
            }
        });
    }

    private static ItemStack icon(DungeonClass dungeonClass, ClassDetails.Item details) {
        ItemStack item = item(dungeonClass.getIcon(), details.name(), details.lore());
        if (item.getItemMeta() instanceof PotionMeta potion) {
            potion.setBasePotionType(PotionType.HEALING);
            item.setItemMeta(potion);
        }
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.POTION_CONTENTS, DataComponentTypes.DYED_COLOR)
                .build());
        return item;
    }

    private static ItemStack head(String texture, ClassDetails.Item details) {
        ItemStack head = item(Material.PLAYER_HEAD, details.name(), details.lore());
        Utils.skull(head, Utils.texture(texture));
        return head;
    }
}
