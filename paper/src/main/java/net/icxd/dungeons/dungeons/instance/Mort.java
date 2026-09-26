package net.icxd.dungeons.dungeons.instance;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;

import com.destroystokyo.paper.profile.ProfileProperty;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.icxd.dungeons.utils.Utils;

/**
 * Mort, who stands in the entrance room: clicking him opens the Ready Up menu before the run
 * starts, and he hands out the map when it does. As on Hypixel he's a player-shaped NPC with two
 * invisible armor stands carrying his name and "CLICK" (at his feet and a quarter block lower, so
 * the names float just above his head).
 */
final class Mort {
    /** Hypixel's skin for him (Mojang-signed). */
    private static final String TEXTURE = "eyJ0aW1lc3RhbXAiOjE1Nzg0MDk0MTMxNjksInByb2ZpbGVJZCI6IjQxZDNhYmMyZDc0OTQwMGM5MDkwZDU0MzRkMDM4MzFiIiwicHJvZmlsZU5hbWUiOiJNZWdha2xvb24iLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzliNTY4OTViOTY1OTg5NmFkNjQ3ZjU4NTk5MjM4YWY1MzJkNDZkYjljMWIwMzg5YjhiYmViNzA5OTlkYWIzM2QiLCJtZXRhZGF0YSI6eyJtb2RlbCI6InNsaW0ifX19fQ==";
    private static final String SIGNATURE = "VEyyNaHlOvqAGUk+r9tWnKp09JSL6jrCdphYgxbldPga6YCvWv8w8Lluebx6gX8R+TLRpg2EWpSUPRBgwEMKBolUxzV9gFQBxQD1IaUDdbVTucNCqp8tPnBEVazM9HOcje4XTwm47yucZbjnEQFndHwyPyFBORCd5vbO6JfzopR0ZD00A0lZn07JGwJz/2WTGjqM8CtP8Yi7RHykaJaso2xfYKcIBaLLE1iMm5G4ZIQZEtfgstCQ58/W9R+FBegqGfgDccwqXP/zOTDl110BE77cRTufeAjjCXZfmSjegF6ctbcA+SxJYgXpQdHlFaWO8fQhJmuauhCMBcKMzL3nP/EMlDFvnFTYlnQBTz7dUqolLqY8fX6jbr1F9eCH8Rb8CBrVsMx4kAX/G5QGKeyzoWRQtDJxAJuHp5U8Gw9c1zDW4Yapse1Gp/0Gj4r1XfWoPjEGDv7FQgdBRMlggBKxtrctbFvDCfHHExZh6y/PQfP6U9BdIy2TRbPCD2tb9r86mvYSmFd1PXV8POElLLaIJTvqqYNewe2pwgdVt4aj2JLBGrP7my8ditkR5q7bWHFTzl+8DbhF4ect5g6I3MhRjq/61InzNwky6lObsRYVHsDAj4FYc6eT5CLfLZnePzeM/RBTCf+K3Jx6eCF7XWgO1OZGr5W4y5/daWTZpKXz/yw=";

    private final List<Entity> entities;

    private Mort(List<Entity> entities) {
        this.entities = entities;
    }

    /** At {@code at} (his feet), facing the way it faces. */
    static Mort spawn(Location at) {
        Mannequin npc = at.getWorld().spawn(at, Mannequin.class, m -> {
            m.setProfile(ResolvableProfile.resolvableProfile()
                    .uuid(UUID.nameUUIDFromBytes("Mort".getBytes()))
                    .addProperty(new ProfileProperty("textures", TEXTURE, SIGNATURE))
                    .build());
            m.setDescription(null);
            m.setImmovable(true);
            m.setInvulnerable(true);
            m.setGravity(false);
            m.setSilent(true);
            m.setPersistent(false);
        });
        return new Mort(List.of(npc, label(at, "&bMort"), label(at.clone().subtract(0, 0.25, 0), "&e&lCLICK")));
    }

    private static ArmorStand label(Location at, String name) {
        return at.getWorld().spawn(at, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            stand.setCustomName(Utils.color(name));
            stand.setCustomNameVisible(true);
        });
    }

    /** Him or one of his name tags (whose boxes are in the way of clicking him). */
    boolean is(Entity entity) {
        return entities.contains(entity);
    }

    void remove() {
        entities.forEach(Entity::remove);
    }
}
