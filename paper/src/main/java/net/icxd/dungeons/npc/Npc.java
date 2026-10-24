package net.icxd.dungeons.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.destroystokyo.paper.profile.ProfileProperty;

import io.papermc.paper.datacomponent.item.ResolvableProfile;

/**
 * An NPC, as Hypixel's are: most player-shaped (and Mort in a run), a Mannequin in its skin that
 * doesn't move, with invisible armor stands carrying its name tags (its name at its feet and "CLICK"
 * a quarter block lower, so the names float just above its head; a header like "⚑ GUIDES" a quarter
 * higher, as the Hub's were recorded); a few villagers, like Jerry (see {@link #villager}), and other
 * mobs (the Hub's Alchemist is a witch). A left or right click on any of them is its click (see
 * {@link Npcs}). They aren't saved with the world: the plugin spawns them each time it starts (an
 * island's when it loads).
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
     * Spawns one at {@code at} (its feet), facing the way it faces, under its name and "CLICK".
     *
     * @param id    what it's known as ("OPHELIA")
     * @param name  its name tag, in {@code &} colours
     * @param skin  its "textures" property; one without a signature shows too
     * @param click what a player's click on it does
     */
    public static Npc spawn(String id, Location at, String name, ProfileProperty skin, Consumer<Player> click) {
        return player(id, at, skin, List.of(name, "&e&lCLICK"), new double[]{0, -0.25}, Map.of(), click);
    }

    /**
     * A player-shaped NPC at {@code at} (its feet), facing the way it faces, in its skin, holding
     * {@code equipment}, its name tags at {@code heights} over its feet (top line first).
     *
     * @param id what it's known as; with where it stands, it makes its profile's id, so NPCs of the
     *           same name (the Hub's five candidates) keep their own skins
     */
    public static Npc player(String id, Location at, ProfileProperty skin, List<String> lines, double[] heights,
                             Map<EquipmentSlot, ItemStack> equipment, Consumer<Player> click) {
        String spot = "@" + at.getBlockX() + "," + at.getBlockY() + "," + at.getBlockZ();
        Mannequin body = at.getWorld().spawn(at, Mannequin.class, m -> {
            m.setProfile(ResolvableProfile.resolvableProfile()
                    .uuid(UUID.nameUUIDFromBytes(("npc:" + id + spot).getBytes()))
                    .addProperty(skin)
                    .build());
            m.setDescription(null);
            m.setImmovable(true);
            m.setInvulnerable(true);
            m.setGravity(false);
            m.setSilent(true);
            m.setPersistent(false);
            equipment.forEach((slot, item) -> m.getEquipment().setItem(slot, item));
        });
        return labelled(id, body, at, lines, heights, click);
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
            still(v);
        });
        return labelled(id, body, at, lines, heights, click);
    }

    /** An NPC of another kind of mob, standing as a villager NPC does. */
    public static Npc mob(String id, Location at, EntityType type, List<String> lines, double[] heights, Consumer<Player> click) {
        Class<? extends Entity> kind = type.getEntityClass();
        if (kind == null || !Mob.class.isAssignableFrom(kind)) throw new IllegalArgumentException(type + " isn't a mob");
        Entity body = at.getWorld().spawn(at, kind, e -> still((Mob) e));
        return labelled(id, body, at, lines, heights, click);
    }

    /** It doesn't move, turn, get pushed or hurt, make a sound, or stay with the world. */
    private static void still(Mob mob) {
        mob.setAI(false);
        mob.setCollidable(false);
        mob.setInvulnerable(true);
        mob.setGravity(false);
        mob.setSilent(true);
        mob.setPersistent(false);
    }

    /** The body and its name tags at {@code heights} over its feet, as one NPC. */
    private static Npc labelled(String id, Entity body, Location at, List<String> lines, double[] heights, Consumer<Player> click) {
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
