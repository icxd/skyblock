package net.icxd.dungeons.yoink;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One of a player's real SkyBlock profiles, from Hypixel's {@code skyblock/profiles} (API v2): its name ("Strawberry"),
 * its mode (Ironman, Stranded, Bingo; none for a normal one), whether it's the one they play on, and their own part of it
 * (a co-op's other members have their own). The bank is the profile's, shared by a co-op; null when its API setting is off.
 */
public record HypixelProfile(String id, String name, String mode, boolean selected, int members, JsonObject member, Double bank) {
    /** Their profiles in a {@code skyblock/profiles} answer, the one they play on first; none they've left. */
    public static List<HypixelProfile> of(JsonObject answer, String playerId) {
        List<HypixelProfile> out = new ArrayList<>();
        if (answer == null || !(answer.get("profiles") instanceof JsonElement profiles) || !profiles.isJsonArray()) return out;
        String id = playerId.replace("-", "").toLowerCase();
        for (JsonElement element : profiles.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject profile = element.getAsJsonObject();
            JsonObject members = object(profile, "members");
            JsonObject member = members == null ? null : object(members, id);
            // Left or kicked: what's left of them is a deletion notice.
            if (member == null || object(object(member, "profile"), "deletion_notice") != null) continue;
            JsonObject banking = object(profile, "banking");
            out.add(new HypixelProfile(text(profile, "profile_id"), text(profile, "cute_name"), text(profile, "game_mode"),
                    profile.has("selected") && profile.get("selected").getAsBoolean(), members.size(), member,
                    banking != null && banking.has("balance") ? banking.get("balance").getAsDouble() : null));
        }
        out.sort(Comparator.comparing((HypixelProfile p) -> !p.selected()));
        return out;
    }

    /** Its mode as the profile menu says it: "Ironman", "Stranded", "Bingo"; null for a normal profile. */
    public String modeName() {
        if (mode == null || mode.isBlank()) return null;
        return switch (mode) {
            case "ironman" -> "Ironman";
            case "island" -> "Stranded";
            case "bingo" -> "Bingo";
            default -> Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
        };
    }

    /** Their purse; 0 if the API doesn't say. */
    public double purse() {
        JsonObject currencies = object(member, "currencies");
        return currencies != null && currencies.has("coin_purse") ? currencies.get("coin_purse").getAsDouble() : 0;
    }

    /** Their SkyBlock level (100 XP each), as far as the API says. */
    public int level() {
        JsonObject leveling = object(member, "leveling");
        return leveling != null && leveling.has("experience") ? leveling.get("experience").getAsInt() / 100 : 0;
    }

    /** What's at this dotted path in their part of the profile ("inventory.inv_contents.data"); null if it isn't there. */
    public JsonElement at(String path) {
        JsonElement at = member;
        for (String part : path.split("\\.")) {
            if (at == null || !at.isJsonObject()) return null;
            at = at.getAsJsonObject().get(part);
        }
        return at == null || at.isJsonNull() ? null : at;
    }

    static JsonObject object(JsonObject in, String key) {
        return in != null && in.get(key) instanceof JsonElement e && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static String text(JsonObject in, String key) {
        return in.get(key) instanceof JsonElement e && e.isJsonPrimitive() ? e.getAsString() : null;
    }
}
