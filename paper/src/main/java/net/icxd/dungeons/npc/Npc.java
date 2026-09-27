package net.icxd.dungeons.npc;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;

import com.destroystokyo.paper.profile.ProfileProperty;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.icxd.dungeons.utils.Text;

/**
 * A player-shaped NPC, as Hypixel's are (and as Mort is in a run): a Mannequin in its skin that
 * doesn't move, with two invisible armor stands carrying its name and "CLICK" (at its feet and a
 * quarter block lower, so the names float just above its head). A left or right click on any of
 * them is its click (see {@link Npcs}). They aren't saved with the world: the plugin spawns them
 * each time it starts.
 */
public final class Npc {
    private final String id;
    private final List<Entity> entities;
    private final Consumer<Player> click;

    private Npc(String id, List<Entity> entities, Consumer<Player> click) {
        this.id = id;
        this.entities = entities;
        this.click = click;
    }

    /**
     * Spawns one at {@code at} (its feet), facing the way it faces.
     *
     * @param id    what it's known as ("OPHELIA")
     * @param name  its name tag, in {@code &} colours
     * @param skin  its "textures" property; one without a signature shows too
     * @param click what a player's click on it does
     */
    public static Npc spawn(String id, Location at, String name, ProfileProperty skin, Consumer<Player> click) {
        Mannequin body = at.getWorld().spawn(at, Mannequin.class, m -> {
            m.setProfile(ResolvableProfile.resolvableProfile()
                    .uuid(UUID.nameUUIDFromBytes(("npc:" + id).getBytes()))
                    .addProperty(skin)
                    .build());
            m.setDescription(null);
            m.setImmovable(true);
            m.setInvulnerable(true);
            m.setGravity(false);
            m.setSilent(true);
            m.setPersistent(false);
        });
        Npc npc = new Npc(id, List.of(body, label(at, name), label(at.clone().subtract(0, 0.25, 0), "&e&lCLICK")), click);
        Npcs.add(npc);
        return npc;
    }

    private static ArmorStand label(Location at, String name) {
        return at.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            stand.customName(Text.line(name));
            stand.setCustomNameVisible(true);
        });
    }

    public String id() {
        return id;
    }

    /** It or one of its name tags (whose boxes are in the way of clicking it). */
    public boolean is(Entity entity) {
        return entities.contains(entity);
    }

    void click(Player player) {
        click.accept(player);
    }

    public void remove() {
        entities.forEach(Entity::remove);
        Npcs.forget(this);
    }
}
