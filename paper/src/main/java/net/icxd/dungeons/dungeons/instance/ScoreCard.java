package net.icxd.dungeons.dungeons.instance;

import java.awt.Color;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import net.icxd.dungeons.common.DungeonFloor;

/**
 * What a run's map shows: nothing yet while it runs (the dungeon map comes with clearing), and the
 * score card once it's over, like Hypixel's "Your Score Summary": skill, exploration, speed and
 * bonus on the left, the grade and total on the right.
 */
final class ScoreCard {
    private static final int SIZE = 128;
    private static final Color BACKGROUND = new Color(24, 24, 24);
    private static final Color PANEL = new Color(64, 64, 64);
    private static final Color BORDER = new Color(170, 0, 0);
    private static final Color TEXT = Color.WHITE;
    /** Map colours are a fixed palette; this red is in it (chat's lighter red comes out orange). */
    private static final Color RED = new Color(255, 0, 0);
    private static final Color TITLE = RED;
    private static final Color TIER = new Color(255, 255, 85);
    private static final String[] LABELS = {"SKILL", "EXPLOR", "SPEED", "BONUS"};

    private ScoreCard() {
    }

    /** A blank map (transparent, so it shows as an empty sheet). */
    static void blank(MapView map) {
        show(map, new Color[SIZE * SIZE]);
    }

    static void draw(MapView map, DungeonFloor floor, Score score) {
        Color[] pixels = new Color[SIZE * SIZE];
        fill(pixels, 0, 0, SIZE, SIZE, BORDER);
        fill(pixels, 2, 2, SIZE - 4, SIZE - 4, BACKGROUND);

        String title = floor.getDungeonName().replace("Master Mode ", "MM ");
        text(pixels, (SIZE - width(title, 1)) / 2, 5, title, 1, TITLE);
        text(pixels, (SIZE - width(floor.getTierName(), 1)) / 2, 15, floor.getTierName(), 1, TIER);

        int[] values = {score.skill(), score.explore(), score.speed(), score.bonus()};
        for (int i = 0; i < 4; i++) {
            int top = 28 + i * 24;
            fill(pixels, 5, top, 74, 20, PANEL);
            text(pixels, 8, top + 6, LABELS[i], 1, TEXT);
            String value = String.valueOf(values[i]);
            text(pixels, 76 - width(value, 1), top + 6, value, 1, TEXT);
        }

        String grade = score.grade();
        Color gradeColor = color(DungeonRun.gradeColor(grade));
        int scale = grade.length() > 1 ? 3 : 4;
        text(pixels, 104 - width(grade, scale) / 2, 42, grade, scale, gradeColor);
        String total = String.valueOf(score.total());
        text(pixels, 104 - width(total, 2) / 2, 90, total, 2, TIER);
        show(map, pixels);
    }

    private static void show(MapView map, Color[] pixels) {
        for (MapRenderer renderer : map.getRenderers()) map.removeRenderer(renderer);
        map.addRenderer(new MapRenderer() {
            private boolean drawn;

            @Override
            public void render(MapView view, MapCanvas canvas, Player player) {
                if (drawn) return;
                drawn = true;
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        Color c = pixels[y * SIZE + x];
                        if (c == null) canvas.setPixel(x, y, (byte) 0);
                        else canvas.setPixelColor(x, y, c);
                    }
                }
            }
        });
    }

    private static void fill(Color[] pixels, int x, int y, int w, int h, Color color) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) pixels[j * SIZE + i] = color;
        }
    }

    private static int width(String text, int scale) {
        int w = 0;
        for (char c : text.toCharArray()) {
            MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar(c);
            if (sprite != null) w += (sprite.getWidth() + 1) * scale;
        }
        return w - scale;
    }

    /** The map font, each pixel {@code scale} times as big. */
    private static void text(Color[] pixels, int x, int y, String text, int scale, Color color) {
        for (char c : text.toCharArray()) {
            MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar(c);
            if (sprite == null) continue;
            for (int row = 0; row < sprite.getHeight(); row++) {
                for (int col = 0; col < sprite.getWidth(); col++) {
                    if (!sprite.get(row, col)) continue;
                    fill(pixels, x + col * scale, y + row * scale, scale, scale, color);
                }
            }
            x += (sprite.getWidth() + 1) * scale;
        }
    }

    /** The colour of a chat colour code ("&6"). */
    private static Color color(String code) {
        return switch (code.charAt(1)) {
            case '6' -> new Color(255, 170, 0);
            case 'e' -> new Color(255, 255, 85);
            case '5' -> new Color(170, 0, 170);
            case 'a' -> new Color(85, 255, 85);
            case '9' -> new Color(85, 85, 255);
            default -> RED;
        };
    }
}
