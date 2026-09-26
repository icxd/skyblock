package net.icxd.dungeons.dungeons.instance;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.papermc.paper.datacomponent.item.ResolvableProfile;

/** Skins and head textures for dungeon things, from {@code dungeons/textures.json} (recorded on Hypixel). */
final class DungeonTextures {
    /** For undeads with no skin of their own yet. */
    static final String UNDEAD = "Undead";
    private static final Map<String, ProfileProperty> TEXTURES = new HashMap<>();

    static {
        try (Reader in = new InputStreamReader(DungeonTextures.class.getResourceAsStream("/dungeons/textures.json"), StandardCharsets.UTF_8)) {
            JsonObject all = JsonParser.parseReader(in).getAsJsonObject().getAsJsonObject("textures");
            for (String name : all.keySet()) {
                JsonObject t = all.getAsJsonObject(name);
                TEXTURES.put(name, new ProfileProperty("textures", t.get("value").getAsString(), t.get("signature").getAsString()));
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Couldn't read dungeons/textures.json: " + e);
        }
    }

    private DungeonTextures() {
    }

    static ProfileProperty get(String name) {
        ProfileProperty texture = TEXTURES.get(name);
        return texture != null ? texture : TEXTURES.get(UNDEAD);
    }

    /** For a Mannequin: the named skin, or the generic undead one. */
    static ResolvableProfile profile(String name) {
        ResolvableProfile.Builder builder = ResolvableProfile.resolvableProfile().uuid(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
        ProfileProperty texture = get(name);
        if (texture != null) builder.addProperty(texture);
        return builder.build();
    }

    /** A player head with that texture. */
    static ItemStack head(String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
        ProfileProperty texture = get(name);
        if (texture != null) profile.setProperty(texture);
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }
}
