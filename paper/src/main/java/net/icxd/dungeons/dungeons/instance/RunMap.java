package net.icxd.dungeons.dungeons.instance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.dungeons.paste.PastePlan;

/**
 * The Magical Map, drawn the way Hypixel's is (layout, colours and marks taken pixel for pixel
 * from recordings): a room shows once someone has walked into it, and the rooms behind its doors
 * as a grey cell with a question mark. Rooms are 18 pixels (16 on the 6-wide floors) with 4 between
 * them, centred on the map; doors are 7 pixels wide in the gap, in the colour of the room they lead
 * into (black for a shut wither door). Rooms with nothing to clear, like the fairy room, get their
 * green tick when found. Players are arrows: yours green, the others blue.
 */
final class RunMap extends MapRenderer {
    // Map palette colours, as Hypixel uses them.
    private static final byte NORMAL = 63;
    private static final byte ENTRANCE = 30;
    private static final byte BLOOD = 18;
    private static final byte FAIRY = (byte) 82;
    private static final byte PUZZLE = 66;
    private static final byte MINIBOSS = 74;
    private static final byte TRAP = 62;
    private static final byte UNKNOWN = (byte) 85;
    private static final byte WITHER = (byte) 119;
    private static final byte WHITE = 34;
    private static final byte GREEN = 30;
    private static final int GAP = 4;
    private static final int DOOR_FROM = 6;
    private static final int DOOR_WIDTH = 7;

    // Marks, as Hypixel draws them in an 18 pixel cell: {row, first column, last column}.
    private static final int[][] QUESTION = {{6, 7, 9}, {7, 6, 6}, {7, 10, 10}, {8, 10, 10}, {9, 9, 9}, {10, 8, 8}, {11, 8, 8}, {13, 8, 8}, {14, 8, 8}};
    private static final int[][] TICK = {{5, 12, 13}, {6, 11, 13}, {7, 10, 12}, {8, 9, 11}, {9, 5, 6}, {9, 8, 10}, {10, 5, 9}, {11, 5, 8}, {12, 6, 8}, {13, 6, 7}};

    private final RunLayout layout;
    private final DungeonRun run;
    private final int room;
    private final int startX;
    private final int startZ;
    private final Set<Integer> found = new HashSet<>();
    private final Set<Integer> ticked = new HashSet<>();
    private final byte[] pixels = new byte[128 * 128];
    private int version;
    private final Map<Player, Integer> drawn = new WeakHashMap<>();

    RunMap(DungeonRun run, RunLayout layout) {
        super(true);
        this.run = run;
        this.layout = layout;
        int w = layout.layout.getWidth();
        int h = layout.layout.getHeight();
        this.room = w >= 6 || h >= 6 ? 16 : 18;
        this.startX = (128 - (w * room + (w - 1) * GAP)) / 2;
        this.startZ = (128 - (h * room + (h - 1) * GAP)) / 2;
        redraw();
    }

    void show(MapView map) {
        for (MapRenderer renderer : map.getRenderers()) map.removeRenderer(renderer);
        map.addRenderer(this);
    }

    /** Someone walked into this room: it shows on the map from now on. */
    boolean find(PlacedRoom r) {
        if (!found.add(r.id())) return false;
        // Nothing to clear in the fairy room, so it's done as soon as it's found.
        if (r.type() == RoomType.FAIRY) ticked.add(r.id());
        redraw();
        return true;
    }

    /** A room is done (the Blood Room once the Watcher lets them pass): it gets its tick. */
    void complete(PlacedRoom r) {
        found.add(r.id());
        if (ticked.add(r.id())) redraw();
    }

    int foundRooms() {
        return found.size();
    }

    int completedRooms() {
        return ticked.size();
    }

    /** Doors changed (one opened). */
    void changed() {
        redraw();
    }

    private void redraw() {
        Arrays.fill(pixels, (byte) 0);
        for (int id : found) drawRoom(layout.room(id));
        for (Door door : layout.doors()) {
            boolean parent = found.contains(door.parent());
            boolean child = found.contains(door.child());
            if (!parent && !child) continue;
            PlacedRoom into = layout.room(door.child());
            if (!(parent && child)) {
                // The far side isn't found yet: its cell behind this door is a grey question mark.
                PlacedRoom unknown = layout.room(parent ? door.child() : door.parent());
                Position cell = unknown.cells().stream().filter(c -> door.edge().touches(c)).findFirst().orElse(unknown.cells().get(0));
                fillCell(cell, UNKNOWN);
                mark(cell, QUESTION, WITHER);
            }
            drawDoor(door, doorColor(door, parent && child, into));
        }
        for (int id : ticked) mark(RunLayout.firstCell(layout.room(id)), TICK, GREEN);
        version++;
    }

    private byte doorColor(Door door, boolean bothFound, PlacedRoom into) {
        if (door.type() == DoorType.BLOOD) return BLOOD;
        if (door.type() == DoorType.WITHER && run.isShut(door)) return WITHER;
        if (!bothFound) return door.type() == DoorType.FAIRY ? WITHER : UNKNOWN;
        return color(into.type());
    }

    private static byte color(RoomType type) {
        return switch (type) {
            case START -> ENTRANCE;
            case BLOOD -> BLOOD;
            case FAIRY -> FAIRY;
            case PUZZLE -> PUZZLE;
            case MINIBOSS -> MINIBOSS;
            case TRAP -> TRAP;
            default -> NORMAL;
        };
    }

    private int cellX(Position cell) {
        return startX + cell.x() * (room + GAP);
    }

    private int cellZ(Position cell) {
        return startZ + cell.y() * (room + GAP);
    }

    private void drawRoom(PlacedRoom r) {
        byte c = color(r.type());
        Set<Position> cells = new HashSet<>(r.cells());
        for (Position cell : r.cells()) {
            fillCell(cell, c);
            // Cells of one room are joined across the gap between them.
            if (cells.contains(new Position(cell.x() + 1, cell.y()))) fill(cellX(cell) + room, cellZ(cell), GAP, room, c);
            if (cells.contains(new Position(cell.x(), cell.y() + 1))) fill(cellX(cell), cellZ(cell) + room, room, GAP, c);
            if (cells.contains(new Position(cell.x() + 1, cell.y())) && cells.contains(new Position(cell.x(), cell.y() + 1))
                    && cells.contains(new Position(cell.x() + 1, cell.y() + 1))) {
                fill(cellX(cell) + room, cellZ(cell) + room, GAP, GAP, c);
            }
        }
    }

    private void fillCell(Position cell, byte c) {
        fill(cellX(cell), cellZ(cell), room, room, c);
    }

    private void drawDoor(Door door, byte c) {
        Position a = door.edge().a();
        Position b = door.edge().b();
        int from = DOOR_FROM - (18 - room) / 2;
        if (b.x() > a.x()) fill(cellX(a) + room, cellZ(a) + from, GAP, DOOR_WIDTH, c);
        else fill(cellX(a) + from, cellZ(a) + room, DOOR_WIDTH, GAP, c);
    }

    private void mark(Position cell, int[][] mark, byte c) {
        int shift = (18 - room) / 2;
        for (int[] row : mark) fill(cellX(cell) + row[1] - shift, cellZ(cell) + row[0] - shift, row[2] - row[1] + 1, 1, c);
    }

    private void fill(int x, int z, int w, int h, byte c) {
        for (int j = z; j < z + h; j++) {
            for (int i = x; i < x + w; i++) {
                if (i >= 0 && i < 128 && j >= 0 && j < 128) pixels[j * 128 + i] = c;
            }
        }
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        Integer seen = drawn.get(player);
        if (seen == null || seen != version) {
            for (int j = 0; j < 128; j++) {
                for (int i = 0; i < 128; i++) canvas.setPixel(i, j, pixels[j * 128 + i]);
            }
            drawn.put(player, version);
        }
        MapCursorCollection cursors = new MapCursorCollection();
        for (Player member : run.players()) {
            if (!member.getWorld().equals(player.getWorld())) continue;
            double px = (member.getLocation().getX() - PastePlan.HYPIXEL_BASE) * (room + GAP) / PastePlan.PITCH + startX;
            double pz = (member.getLocation().getZ() - PastePlan.HYPIXEL_BASE) * (room + GAP) / PastePlan.PITCH + startZ;
            byte x = (byte) Math.clamp(Math.round(px * 2 - 128), -128, 127);
            byte z = (byte) Math.clamp(Math.round(pz * 2 - 128), -128, 127);
            float yaw = member.getLocation().getYaw();
            byte direction = (byte) (((int) Math.floor((yaw + 11.25) / 22.5) % 16 + 16) % 16);
            MapCursor.Type type = member.equals(player) ? MapCursor.Type.FRAME : MapCursor.Type.BLUE_MARKER;
            cursors.addCursor(new MapCursor(x, z, direction, type, true));
        }
        canvas.setCursors(cursors);
    }
}
