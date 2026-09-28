package net.icxd.dungeons.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;

/**
 * A menu of these parts drawn from {@link MenuSlot}s for the viewer's profile, anew each time it opens: a
 * slot with an action is a button (a left or right click, run on the next tick), Close closes it, and the
 * rest only shows. Main thread.
 */
public abstract class CollectionGUI extends GUI {
    protected final Player viewer;
    private final Map<Integer, Consumer<ClickType>> actions = new HashMap<>();

    protected CollectionGUI(String title, Player viewer) {
        super(title, Size.SIX);
        this.viewer = viewer;
    }

    /** What each slot shows for this profile. */
    protected abstract Map<Integer, MenuSlot> slots(Document profile);

    /** Adds the buttons (see {@link #on}); called after {@link #slots}. */
    protected abstract void buttons(Document profile);

    /** Makes a slot a button. */
    protected void on(int slot, Runnable action) {
        actions.put(slot, click -> action.run());
    }

    protected void on(int slot, Consumer<ClickType> action) {
        actions.put(slot, action);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        actions.clear();
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) {
            fill(filler());
            return;
        }
        Document profile = user.profile();
        Map<Integer, MenuSlot> slots = slots(profile);
        buttons(profile);
        for (Map.Entry<Integer, MenuSlot> e : slots.entrySet()) {
            int slot = e.getKey();
            if (slot == CollectionMenus.CLOSE) {
                set(GUIClickableItem.close(slot));
                continue;
            }
            Consumer<ClickType> action = actions.get(slot);
            if (action == null) set(slot, e.getValue().stack());
            else set(GUIClickableItem.button(slot, e.getValue().stack(), viewer, action));
        }
    }

    /** This menu again, as the profile has it now (after a claim). */
    protected void reopen() {
        open(viewer);
    }
}
