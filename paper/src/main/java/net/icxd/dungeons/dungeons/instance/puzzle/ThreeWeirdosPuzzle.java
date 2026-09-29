package net.icxd.dungeons.dungeons.instance.puzzle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.icxd.dungeons.utils.Text;

/**
 * Three Weirdos (never recorded; the wiki and the solver mods). Three NPCs stand by three chests;
 * clicking one gets their statement in chat ("[NPC] Name: ..."); one chest has the reward. The right
 * one: "PUZZLE SOLVED! ... wasn't fooled by Name! Good job!" (SkyHanni's pattern) and a Tier V
 * blessing from the chest. A wrong one fails it.
 *
 * <p>As the mods expect: each NPC has an armor stand named after them at their block
 * (Skyblocker, Odin), and their chest is next to them (the data's offset).
 */
final class ThreeWeirdosPuzzle extends Puzzle {
    private final PuzzleData.Weirdos data;
    private final Random random = new Random();
    private List<WeirdosRiddle.Weirdo> weirdos = List.of();
    private final List<List<Entity>> npcs = new ArrayList<>();
    private final List<PuzzleChest> chests = new ArrayList<>();

    ThreeWeirdosPuzzle(PuzzleHost host, int room, PuzzleFrame frame, PuzzleData.Weirdos data) {
        super(host, room, frame, "Three Weirdos");
        this.data = data;
    }

    @Override
    void start() {
        weirdos = WeirdosRiddle.roll(random);
        BlockFace facing = frame.face(data.facing());
        for (int i = 0; i < 3; i++) {
            WeirdosRiddle.Weirdo weirdo = weirdos.get(i);
            int[] at = data.npcs().get(i);
            Location feet = feet(at);
            feet.setYaw(yaw(facing));
            npcs.add(spawn(feet, weirdo.name()));
            int[] off = data.chestOffset();
            chests.add(PuzzleChest.place(block(at[0] + off[0], at[1] + off[1], at[2] + off[2]), facing));
        }
    }

    /**
     * The NPC and the armor stand with their name, the way Mort is made. Their skins are UNKNOWN, so
     * they have the default ones.
     */
    private static List<Entity> spawn(Location feet, String name) {
        Mannequin npc = feet.getWorld().spawn(feet, Mannequin.class, m -> {
            // A whole profile (id and name), so nothing looks it up.
            m.setProfile(ResolvableProfile.resolvableProfile().name(name)
                    .uuid(UUID.nameUUIDFromBytes(("Weirdo " + name).getBytes(StandardCharsets.UTF_8))).build());
            m.setDescription(null);
            m.setImmovable(true);
            m.setInvulnerable(true);
            m.setGravity(false);
            m.setSilent(true);
            m.setPersistent(false);
        });
        ArmorStand label = feet.getWorld().spawn(feet, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setPersistent(false);
            s.customName(Text.line("&c" + name));
            s.setCustomNameVisible(true);
        });
        return List.of(npc, label);
    }

    /** Minecraft's yaw for a direction: south 0, west 90, north 180, east -90. */
    private static float yaw(BlockFace face) {
        return switch (face) {
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> -90;
            default -> 0;
        };
    }

    @Override
    boolean click(Player player, Entity entity) {
        for (int i = 0; i < npcs.size(); i++) {
            if (npcs.get(i).contains(entity)) {
                player.sendMessage(Text.line(WeirdosRiddle.line(weirdos.get(i))));
                return true;
            }
        }
        return false;
    }

    @Override
    boolean click(Player player, Block block, boolean right) {
        for (int i = 0; i < chests.size(); i++) {
            PuzzleChest chest = chests.get(i);
            if (!chest.is(block)) continue;
            if (!right) return true;
            if (chest.isOpened()) {
                chest.open(host, player, null, 0);
                return true;
            }
            if (isOver()) return true;
            WeirdosRiddle.Weirdo weirdo = weirdos.get(i);
            if (weirdo.reward()) {
                host.tell("&a&lPUZZLE SOLVED! " + named(player) + " &ewasn't fooled by &c" + weirdo.name() + "&e! &4G&co&6o&ed&a &2j&bo&3b&5!");
                chest.open(host, player, randomBlessing(), BLESSING_LEVEL);
                solve(PuzzleTab.AFTER_CHEST);
            } else {
                // Never recorded: worded like Tic Tac Toe's fail and the solve line (the RustClear
                // recreation has the same). UNVERIFIED.
                host.tell("&c&lPUZZLE FAIL! " + named(player) + " &ewas fooled by &c" + weirdo.name() + "&e! &4Y&ci&6k&ee&as&2!");
                fail(player, PuzzleTab.AFTER_LINE);
            }
            return true;
        }
        return false;
    }

    @Override
    boolean owns(Entity entity) {
        for (List<Entity> npc : npcs) if (npc.contains(entity)) return true;
        return false;
    }

    @Override
    void dispose() {
        for (List<Entity> npc : npcs) npc.forEach(Entity::remove);
        chests.forEach(PuzzleChest::dispose);
    }

    /** New weirdos with a new riddle, and their chests shut again. */
    @Override
    boolean restart() {
        dispose();
        npcs.clear();
        chests.clear();
        start();
        return true;
    }
}
