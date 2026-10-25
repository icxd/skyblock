package net.icxd.dungeons.fairysouls;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.destroystokyo.paper.profile.ProfileProperty;

import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.npc.Lines;
import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * Tia the Fairy, by the Hub's Fairy Pond where the wiki and NEU have her (119.5 65 147.5; she was never recorded),
 * in NEU's skin, which has no signature, her name tag in her chat name's pink, facing the pond (UNKNOWN which way
 * she faces). The first time a profile talks to her she says the wiki's First Interaction lines, a second apart
 * (UNKNOWN: Elizabeth's were, as recorded), in a villager's voice (UNKNOWN, as most NPCs'); after that a click opens
 * her menu (see {@link TiaMenu}). Main thread.
 */
@OnlyOn(ServerType.LOBBY)
public final class Tia {
    public static final String ID = "TIA_THE_FAIRY";
    static final String NAME = "&dTia the Fairy";
    static final ProfileProperty SKIN = new ProfileProperty("textures",
            Utils.texture("73f072940a942b953a3f9d1391ff342b7d19076f7a1a3e944040d6420c7d4980"));
    static final double X = 119.5;
    static final double Y = 65;
    static final double Z = 147.5;
    /** North, at the pond (the captured map's open ground is that way). */
    static final float YAW = 180;
    /** The wiki's (Tia the Fairy's Dialogue), {@code %s} their name. */
    static final List<String> FIRST_TALK = List.of(
            "&e[NPC] &dTia the Fairy&f: Welcome to the &dFairy Pond&f, %s! I am Tia.",
            "&e[NPC] &dTia the Fairy&f: You may have noticed some strange orbs laying around the island.",
            "&e[NPC] &dTia the Fairy&f: They are the souls of my fallen sisters.",
            "&e[NPC] &dTia the Fairy&f: If you find any more during your travels, please bring them back to me!");
    static final long[] LINE_TICKS = {0, 20, 40, 60};

    private Tia() {
    }

    /** Spawns her in the Hub's main world, keeping her chunk loaded (she'd be gone with it). */
    public static Npc spawn(Plugin plugin, World world) {
        Location at = new Location(world, X, Y, Z, YAW, 0);
        at.getChunk().addPluginChunkTicket(plugin);
        return Npc.spawn(ID, at, NAME, SKIN, player -> click(plugin, player));
    }

    static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || Lines.talking(player)) return;
        if (Npc.firstTalk(user.profile(), ID)) {
            Lines.say(plugin, player, firstTalk(player.getName()), LINE_TICKS, Voice.VILLAGER, null);
            return;
        }
        TiaMenu.show(player);
    }

    static List<String> firstTalk(String name) {
        return FIRST_TALK.stream().map(line -> line.contains("%s") ? line.formatted(name) : line).toList();
    }
}
