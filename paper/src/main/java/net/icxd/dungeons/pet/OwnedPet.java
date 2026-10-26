package net.icxd.dungeons.pet;

import java.util.UUID;

import org.bson.Document;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.icxd.dungeons.item.enums.Rarity;

/**
 * A pet someone has, in their Pets menu (kept with the profile as Hypixel's API keeps one, {@code pets_data.pets}) or
 * as an item (its {@code petInfo}): its own id, type ("BEE"), rarity, XP, held item (an item id, or null), the Pet
 * Candy it's had, its skin (an id, or null; skins aren't shown yet) and whether it's a favourite. Immutable.
 */
public record OwnedPet(String uuid, String type, Rarity tier, double exp, String heldItem, int candyUsed, String skin, boolean favorite) {
    static final String UUID_KEY = "uuid";
    static final String TYPE = "type";
    static final String TIER = "tier";
    static final String EXP = "exp";
    static final String HELD_ITEM = "heldItem";
    static final String CANDY_USED = "candyUsed";
    static final String SKIN = "skin";
    static final String FAVORITE = "favorite";

    public OwnedPet {
        type = type == null ? null : type.toUpperCase(java.util.Locale.ROOT);
        exp = Double.isFinite(exp) ? Math.max(0, exp) : 0;
        candyUsed = Math.max(0, candyUsed);
    }

    /** A new one: level 1, no XP, nothing held. */
    public static OwnedPet of(String type, Rarity tier) {
        return new OwnedPet(UUID.randomUUID().toString(), type, tier, 0, null, 0, null, false);
    }

    public OwnedPet withExp(double exp) {
        return new OwnedPet(uuid, type, tier, exp, heldItem, candyUsed, skin, favorite);
    }

    public OwnedPet withFavorite(boolean favorite) {
        return new OwnedPet(uuid, type, tier, exp, heldItem, candyUsed, skin, favorite);
    }

    /** Holding this item (by its id; null for none). */
    public OwnedPet withHeldItem(String heldItem) {
        return new OwnedPet(uuid, type, tier, exp, heldItem, candyUsed, skin, favorite);
    }

    // As kept with the profile

    public Document document() {
        Document out = new Document(UUID_KEY, uuid).append(TYPE, type).append(TIER, tier.name()).append(EXP, exp).append(CANDY_USED, candyUsed);
        if (heldItem != null) out.append(HELD_ITEM, heldItem);
        if (skin != null) out.append(SKIN, skin);
        if (favorite) out.append(FAVORITE, true);
        return out;
    }

    /** One kept with the profile; null if it isn't one (no type or rarity). */
    public static OwnedPet from(Document document) {
        if (document == null) return null;
        Rarity tier = PetData.rarity(document.getString(TIER));
        String type = document.getString(TYPE);
        if (tier == null || type == null || type.isBlank()) return null;
        String uuid = document.getString(UUID_KEY);
        return new OwnedPet(uuid == null || uuid.isBlank() ? UUID.randomUUID().toString() : uuid, type, tier,
                document.get(EXP) instanceof Number n ? n.doubleValue() : 0, blankless(document.getString(HELD_ITEM)),
                document.get(CANDY_USED) instanceof Number n ? n.intValue() : 0, blankless(document.getString(SKIN)),
                Boolean.TRUE.equals(document.get(FAVORITE)));
    }

    // As an item's petInfo

    /** Its {@code petInfo}, as Hypixel writes one on a pet item. */
    public String petInfo() {
        JsonObject info = new JsonObject();
        info.addProperty(TYPE, type);
        info.addProperty("active", false);
        info.addProperty(EXP, exp);
        info.addProperty(TIER, tier.name());
        info.addProperty("hideInfo", false);
        if (heldItem != null) info.addProperty(HELD_ITEM, heldItem);
        info.addProperty(CANDY_USED, candyUsed);
        if (skin != null) info.addProperty(SKIN, skin);
        info.addProperty(UUID_KEY, uuid);
        return info.toString();
    }

    /** The pet a {@code petInfo} describes (Hypixel's or ours); null if it doesn't. A missing uuid is {@code uuid}. */
    public static OwnedPet fromPetInfo(String petInfo, String uuid) {
        if (petInfo == null || petInfo.isBlank()) return null;
        try {
            JsonObject info = JsonParser.parseString(petInfo).getAsJsonObject();
            String type = string(info, TYPE);
            Rarity tier = PetData.rarity(string(info, TIER));
            if (type == null || tier == null) return null;
            String own = blankless(string(info, UUID_KEY));
            if (own == null) own = blankless(string(info, "uniqueId"));
            if (own == null) own = uuid != null && !uuid.isBlank() ? uuid : UUID.randomUUID().toString();
            return new OwnedPet(own, type, tier, number(info, EXP), blankless(string(info, HELD_ITEM)), (int) number(info, CANDY_USED),
                    blankless(string(info, SKIN)), false);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            return null;
        }
    }

    private static String string(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static double number(JsonObject json, String key) {
        JsonElement e = json.get(key);
        try {
            return e != null && e.isJsonPrimitive() ? e.getAsDouble() : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String blankless(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
