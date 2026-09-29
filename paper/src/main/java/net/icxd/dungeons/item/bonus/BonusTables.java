package net.icxd.dungeons.item.bonus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Hypixel's tables that bonuses' text doesn't have, kept with the item data out of this repository
 * ({@link #FILE}, next to items.json: servermgr links that folder into every server): the kill steps of the
 * Bulwarks ("Kill Zombies to accumulate defense against them", whose text shows only the next step: the
 * wiki's Revenant, Tarantula and Final Destination Armor have the rest). Without the file a Bulwark has only
 * the step its text shows (see {@link CountedSets}). Read once, as the plugin starts.
 */
public final class BonusTables {
    /** Where it is in the plugin's data folder. */
    public static final String FILE = "items/bonus_tables.json";

    /** Reaching {@code at} (kills) gives {@code value}. */
    record Step(double at, double value) {
    }

    private static volatile BonusTables current = new BonusTables(Map.of());

    private final Map<String, List<Step>> killSteps;

    BonusTables(Map<String, List<Step>> killSteps) {
        this.killSteps = Map.copyOf(killSteps);
    }

    static BonusTables get() {
        return current;
    }

    static void set(BonusTables tables) {
        current = tables;
    }

    /** A bonus's kill steps, fewest kills first; none if the tables don't have it. */
    List<Step> killSteps(String bonus) {
        return killSteps.getOrDefault(bonus, List.of());
    }

    /** Reads the file; without it (or broken), no tables, which the log says. */
    static BonusTables load(Path file, Logger log) {
        if (!Files.exists(file)) {
            if (log != null) log.warning("There's no " + file + ", so the Bulwarks go no further than the step their text shows");
            return new BonusTables(Map.of());
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            if (log != null) log.warning("Couldn't read " + file + ": " + e.getMessage());
            return new BonusTables(Map.of());
        }
    }

    /** The tables in the file's JSON: {@code "kill_steps": {"Zombie Bulwark": [[50, 20], [300, 50], ...]}}. */
    static BonusTables parse(JsonObject json) {
        Map<String, List<Step>> steps = new HashMap<>();
        JsonObject kills = json.getAsJsonObject("kill_steps");
        if (kills != null) {
            for (Map.Entry<String, JsonElement> e : kills.entrySet()) {
                List<Step> list = new ArrayList<>();
                for (JsonElement step : e.getValue().getAsJsonArray()) {
                    JsonArray pair = step.getAsJsonArray();
                    list.add(new Step(pair.get(0).getAsDouble(), pair.get(1).getAsDouble()));
                }
                list.sort((a, b) -> Double.compare(a.at(), b.at()));
                steps.put(e.getKey(), List.copyOf(list));
            }
        }
        return new BonusTables(steps);
    }

    /** What the steps give at {@code count}: the last one reached's; 0 before the first. */
    static double reached(List<Step> steps, double count) {
        double value = 0;
        for (Step step : steps) {
            if (count < step.at()) break;
            value = step.value();
        }
        return value;
    }

    /** The first step not reached yet at {@code count}; null once they all are. */
    static Step next(List<Step> steps, double count) {
        for (Step step : steps) if (count < step.at()) return step;
        return null;
    }
}
