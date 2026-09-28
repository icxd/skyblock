package net.icxd.dungeons.item.gemstone;

/**
 * A gemstone: which gem and how refined, the item "FINE_JASPER_GEM" (Hypixel's ids, {@code <QUALITY>_<GEM>_GEM}),
 * named "❁ Fine Jasper Gemstone" in the item data.
 */
public record Gem(GemstoneType type, GemstoneQuality quality) {
    public Gem {
        if (type == null || !type.gem() || quality == null) throw new IllegalArgumentException("not a gem: " + type + " " + quality);
    }

    /** Its item's id: "FINE_JASPER_GEM". */
    public String id() {
        return quality.name() + "_" + type.name() + "_GEM";
    }

    /** "&9❁ Fine Jasper Gemstone" without its colour: "❁ Fine Jasper Gemstone", as the item data names it. */
    public String name() {
        return type.getIcon() + " " + quality.getName() + " " + type.getName() + " Gemstone";
    }

    /** The gem an item id is ("FINE_JASPER_GEM"); null if it isn't one. */
    public static Gem of(String id) {
        if (id == null || !id.endsWith("_GEM")) return null;
        String[] parts = id.substring(0, id.length() - "_GEM".length()).split("_", 2);
        if (parts.length != 2) return null;
        GemstoneQuality quality = GemstoneQuality.of(parts[0]);
        GemstoneType type = GemstoneType.of(parts[1]);
        return quality == null || type == null || !type.gem() ? null : new Gem(type, quality);
    }
}
