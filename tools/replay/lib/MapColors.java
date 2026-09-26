import net.minecraft.world.level.material.MapColor;

/** Prints the ARGB colour of every packed map colour id (0-255) as a JSON array; used by `replay.js setup`. */
public class MapColors {
    public static void main(String[] args) {
        StringBuilder out = new StringBuilder("[");
        for (int id = 0; id < 256; id++) {
            int argb;
            try {
                argb = MapColor.getColorFromPackedId(id);
            } catch (RuntimeException e) {
                argb = 0;
            }
            out.append(id == 0 ? "" : ",").append(argb);
        }
        System.out.println(out.append("]"));
    }
}
