package net.icxd.dungeons.npc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.destroystokyo.paper.profile.ProfileProperty;

import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.shop.Shop;
import net.icxd.dungeons.shop.ShopMenu;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Ophelia, the Dungeon Hub's shop: a click opens her menu (see {@link Shop#OPHELIA}), where players
 * also sell. The first time a profile talks to her she says her lines first (the wiki's "First
 * Interaction"; the time between them isn't known). Where she stands is config.yml's
 * {@code npcs.ophelia}, near the world spawn by default (Hypixel's hub has her at -49.5, 119, -9.5).
 * Her skin is NEU's, which has no signature; her name tag's red is her chat name's.
 */
@OnlyOn(ServerType.DUNGEON_HUB)
public final class Ophelia {
    public static final String ID = "OPHELIA";
    private static final String NAME = "&cOphelia";
    private static final ProfileProperty SKIN = new ProfileProperty("textures",
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGFhNGI4MjdhOWE4NWM0ZWZjMTQ3ZmM1OWQwZGE2ODhlYmU3ZTI2ZDI1OGI1NTNjMDQxZGEyZWQ2ZWM4YjZiOCJ9fX0=");
    static final List<String> FIRST_TALK = List.of(
            "&e[NPC] &cOphelia&f: These &cCatacombs&f hold many &eriches&f, powerful &aitems&f and great &bexperience.",
            "&e[NPC] &cOphelia&f: My trick? I make &6coins&f from the others' greed.",
            "&e[NPC] &cOphelia&f: Are you interested in buying anything?");
    private static final long BETWEEN_LINES = 30;
    /** On the profile: the NPCs it has talked to. */
    static final String TALKED = "talkedTo";

    private static final Set<UUID> talking = new HashSet<>();

    private Ophelia() {
    }

    /** Spawns her in the main world, keeping her chunk loaded (she'd be gone with it). */
    public static Npc spawn(Plugin plugin, World world, ConfigurationSection config) {
        Location at = where(world, config);
        at.getChunk().addPluginChunkTicket(plugin);
        return Npc.spawn(ID, at, NAME, SKIN, player -> talk(plugin, player));
    }

    /** Config's {@code at} (x, y, z) if it has one, else {@code from-spawn} blocks from the world spawn's corner; {@code yaw} either way. */
    static Location where(World world, ConfigurationSection config) {
        ConfigurationSection at = config == null ? null : config.getConfigurationSection("at");
        float yaw = config == null ? 90 : (float) config.getDouble("yaw", 90);
        if (at != null) return new Location(world, at.getDouble("x"), at.getDouble("y"), at.getDouble("z"), yaw, 0);
        ConfigurationSection offset = config == null ? null : config.getConfigurationSection("from-spawn");
        Location spawn = world.getSpawnLocation();
        return new Location(world, spawn.getBlockX() + (offset == null ? 4.5 : offset.getDouble("x")),
                spawn.getBlockY() + (offset == null ? 0 : offset.getDouble("y")),
                spawn.getBlockZ() + (offset == null ? 0.5 : offset.getDouble("z")), yaw, 0);
    }

    private static void talk(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || talking.contains(player.getUniqueId())) return;
        if (!firstTime(user.profile())) {
            new ShopMenu(Shop.OPHELIA, player).open(player);
            return;
        }
        talking.add(player.getUniqueId());
        for (int i = 0; i < FIRST_TALK.size(); i++) {
            String line = FIRST_TALK.get(i);
            boolean last = i == FIRST_TALK.size() - 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (last) talking.remove(player.getUniqueId());
                if (!player.isOnline()) return;
                player.sendMessage(Text.line(line));
                if (last) new ShopMenu(Shop.OPHELIA, player).open(player);
            }, i * BETWEEN_LINES);
        }
    }

    /** Whether this profile hasn't talked to her yet; it has from now on. */
    static boolean firstTime(Document profile) {
        List<Object> talked = profile.get(TALKED) instanceof List<?> list ? new ArrayList<>(list) : new ArrayList<>();
        if (talked.contains(ID)) return false;
        talked.add(ID);
        profile.put(TALKED, talked);
        return true;
    }
}
