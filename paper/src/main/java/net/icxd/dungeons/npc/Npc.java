package net.icxd.dungeons.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import com.destroystokyo.paper.profile.ProfileProperty;

import io.papermc.paper.datacomponent.item.ResolvableProfile;

/**
 * An NPC, as Hypixel's are: most player-shaped (and Mort in a run), a Mannequin in its skin that
 * doesn't move, with two invisible armor stands carrying its name and "CLICK" (at its feet and a
 * quarter block lower, so the names float just above its head); a few villagers, like Jerry (see
 * {@link #villager}). A left or right click on any of them is its click (see {@link Npcs}). They
 * aren't saved with the world: the plugin spawns them each time it starts (an island's when it loads).
 */
public final class Npc {
    /** On the profile: the NPCs it has talked to. */
    static final String TALKED = "talkedTo";

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
        Npc npc = new Npc(id, List.of(body, Hologram.label(at, name), Hologram.label(at.clone().subtract(0, 0.25, 0), "&e&lCLICK")), click);
        Npcs.add(npc);
        return npc;
    }

    /**
     * A villager NPC, as Hypixel's Jerry is: a villager of this kind and job that doesn't move, turn or
     * get pushed, its name tags at {@code heights} over its feet (top line first: Jerry's were recorded
     * 0.35, 0.10 and -0.15 from his).
     */
    public static Npc villager(String id, Location at, Villager.Type type, Villager.Profession profession, List<String> lines, double[] heights,
                               Consumer<Player> click) {
        Villager body = at.getWorld().spawn(at, Villager.class, v -> {
            v.setVillagerType(type);
            v.setProfession(profession);
            v.setVillagerLevel(1);
            v.setAI(false);
            v.setCollidable(false);
            v.setInvulnerable(true);
            v.setGravity(false);
            v.setSilent(true);
            v.setPersistent(false);
        });
        List<Entity> entities = new ArrayList<>();
        entities.add(body);
        for (int i = 0; i < lines.size(); i++) entities.add(Hologram.label(at.clone().add(0, heights[i], 0), lines.get(i)));
        Npc npc = new Npc(id, entities, click);
        Npcs.add(npc);
        return npc;
    }

    /** Whether this profile hasn't talked to this NPC yet; it has from now on. */
    public static boolean firstTalk(Document profile, String id) {
        List<Object> talked = profile.get(TALKED) instanceof List<?> list ? new ArrayList<>(list) : new ArrayList<>();
        if (talked.contains(id)) return false;
        talked.add(id);
        profile.put(TALKED, talked);
        return true;
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
