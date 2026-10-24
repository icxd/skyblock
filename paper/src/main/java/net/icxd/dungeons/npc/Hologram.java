package net.icxd.dungeons.npc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import net.icxd.dungeons.utils.Text;

/**
 * Floating lines of text, as Hypixel's are: an invisible armor stand per line, its name shown, 0.375
 * blocks apart (recorded: "Travel to:" at y 100.9375 over "Hub Island" at 100.5625). Like NPCs,
 * they aren't saved with the world.
 */
public final class Hologram {
    static final double LINE_SPACING = 0.375;
    private static final List<Hologram> SPAWNED = new ArrayList<>();

    private final List<Entity> lines;

    private Hologram(List<Entity> lines) {
        this.lines = lines;
    }

    /** Lines top to bottom, the bottom one's stand at {@code bottom}. */
    public static Hologram spawn(Location bottom, List<String> text) {
        List<Entity> stands = new ArrayList<>();
        for (int i = 0; i < text.size(); i++) {
            Location at = bottom.clone().add(0, (text.size() - 1 - i) * LINE_SPACING, 0);
            stands.add(label(at, text.get(i)));
        }
        Hologram hologram = new Hologram(stands);
        SPAWNED.add(hologram);
        return hologram;
    }

    /** One line: an invisible armor stand at {@code at} with the text as its shown name. */
    static ArmorStand label(Location at, String name) {
        return at.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            stand.customName(Text.line(name));
            stand.setCustomNameVisible(true);
        });
    }

    public void remove() {
        lines.forEach(Entity::remove);
        SPAWNED.remove(this);
    }

    /** When the plugin stops (as {@link Npcs#removeAll}). */
    public static void removeAll() {
        for (Hologram hologram : List.copyOf(SPAWNED)) hologram.remove();
    }
}
