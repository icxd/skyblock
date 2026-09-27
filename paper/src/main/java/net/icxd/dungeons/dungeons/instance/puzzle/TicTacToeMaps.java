package net.icxd.dungeons.dungeons.instance.puzzle;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * The X and O on the Tic Tac Toe board: maps in item frames, as on Hypixel (a filled map each, drawn
 * red on light grey). The drawings are this plugin's own; the colours are the recorded ones, so the
 * middle pixel is red for an X and grey for an O, which is how solver mods tell them apart
 * (Skyblocker reads {@code colors[8256]}: 114 is X, 33 is O). Two maps for the whole server, made
 * when first needed.
 */
final class TicTacToeMaps {
    /** Map colours: light grey (snow, shade 1) and red (red, shade 2). */
    static final byte GREY = 33;
    static final byte RED = 114;
    /** Where the marks are drawn, and how thick. */
    private static final int FROM = 22;
    private static final int TO = 105;
    private static final double STROKE = 7;
    private static final double RING_OUTER = 42;
    private static final double RING_INNER = 29;

    private static MapView x;
    private static MapView o;

    private TicTacToeMaps() {
    }

    /** A filled map showing that mark. */
    static ItemStack item(TicTacToeGame.Mark mark) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) item.getItemMeta();
        meta.setMapView(view(mark));
        item.setItemMeta(meta);
        return item;
    }

    private static synchronized MapView view(TicTacToeGame.Mark mark) {
        if (mark == TicTacToeGame.Mark.X) {
            if (x == null) x = create(draw(mark));
            return x;
        }
        if (o == null) o = create(draw(mark));
        return o;
    }

    private static MapView create(byte[] pixels) {
        MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
        for (MapRenderer renderer : view.getRenderers()) view.removeRenderer(renderer);
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);
        view.addRenderer(new MapRenderer(false) {
            private boolean drawn;

            @Override
            public void render(MapView map, MapCanvas canvas, Player player) {
                if (drawn) return;
                for (int j = 0; j < 128; j++) {
                    for (int i = 0; i < 128; i++) canvas.setPixel(i, j, pixels[j * 128 + i]);
                }
                drawn = true;
            }
        });
        return view;
    }

    /** The 128x128 picture of a mark, row by row. */
    static byte[] draw(TicTacToeGame.Mark mark) {
        byte[] pixels = new byte[128 * 128];
        double middle = 63.5;
        for (int j = 0; j < 128; j++) {
            for (int i = 0; i < 128; i++) {
                boolean ink;
                if (mark == TicTacToeGame.Mark.X) {
                    boolean inside = i >= FROM && i <= TO && j >= FROM && j <= TO;
                    // Distance to either diagonal through the middle.
                    double down = Math.abs((i - middle) - (j - middle)) / Math.sqrt(2);
                    double up = Math.abs((i - middle) + (j - middle)) / Math.sqrt(2);
                    ink = inside && Math.min(down, up) <= STROKE;
                } else {
                    double r = Math.hypot(i - middle, j - middle);
                    ink = r <= RING_OUTER && r >= RING_INNER;
                }
                pixels[j * 128 + i] = ink ? RED : GREY;
            }
        }
        return pixels;
    }
}
