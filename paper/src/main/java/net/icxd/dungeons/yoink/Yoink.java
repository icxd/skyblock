package net.icxd.dungeons.yoink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.bson.Document;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.icxd.dungeons.bank.BankAccount;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Copies the picked parts of a real profile onto the one a player plays on, each replacing that part of theirs
 * (see {@link YoinkPart}), then saves and says what came over and what couldn't (items this plugin doesn't have).
 * The profile's data is written as plain values, the items through {@link HypixelItems}. Main thread.
 */
public final class Yoink {
    private final Player player;
    private final User user;
    private final HypixelProfile from;
    /** What each part brought, for the summary ("Skills: 12"). */
    private final List<String> done = new ArrayList<>();
    /** Items that didn't come over, by id, and how many of each. */
    private final Map<String, Integer> skipped = new TreeMap<>();

    private Yoink(Player player, User user, HypixelProfile from) {
        this.player = player;
        this.user = user;
        this.from = from;
    }

    /** Copies these parts of {@code from} (a profile of {@code owner}'s) onto the player's, and tells them how it went. */
    public static void run(Player player, String owner, HypixelProfile from, Collection<YoinkPart> parts) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Yoink yoink = new Yoink(player, user, from);
        for (YoinkPart part : YoinkPart.values()) {
            if (!parts.contains(part) || !part.available(from)) continue;
            try {
                yoink.apply(part);
            } catch (RuntimeException e) {
                yoink.done.add("&c" + part.title() + ": failed (" + e.getMessage() + ")");
            }
        }
        user.save();
        player.sendMessage(Text.line("&aYoinked &b" + from.name() + " &afrom " + owner + " onto &e" + user.profileName() + "&a:"));
        for (String line : yoink.done) player.sendMessage(Text.line("&8 - &7" + line));
        if (!yoink.skipped.isEmpty()) {
            player.sendMessage(Text.line("&eNot brought over (no such item here): &7" + String.join("&8, &7", yoink.skipped.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())).limit(12)
                    .map(e -> e.getKey() + (e.getValue() > 1 ? " x" + e.getValue() : "")).toList())
                    + (yoink.skipped.size() > 12 ? " &8and " + (yoink.skipped.size() - 12) + " more" : "")));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
    }

    private void apply(YoinkPart part) {
        Document profile = user.profile();
        switch (part) {
            case COINS -> coins(profile);
            case SKILLS -> skills(profile);
            case COLLECTIONS -> collections(profile);
            case DUNGEONS -> dungeons(profile);
            case MINIONS -> minions(profile);
            default -> items(part, profile);
        }
    }

    // Numbers

    private void coins(Document profile) {
        double purse = Math.max(0, from.purse());
        profile.put(Purse.FIELD, purse);
        String line = "Coins: &6" + Coins.format(purse) + " &7in the purse";
        if (from.bank() != null) {
            BankAccount.setBalance(profile, Math.max(0, from.bank()));
            line += ", &6" + Coins.format(from.bank()) + " &7in the bank";
        }
        done.add(line);
    }

    private void skills(Document profile) {
        JsonObject experience = HypixelProfile.object(HypixelProfile.object(from.member(), "player_data"), "experience");
        Document skills = profile.get("skills") instanceof Document d ? d : new Document();
        int count = 0;
        for (Skill skill : Skill.values()) {
            JsonElement xp = experience == null ? null : experience.get("SKILL_" + skill.key().toUpperCase(Locale.ROOT));
            skills.put(skill.key(), xp != null && xp.isJsonPrimitive() ? Math.max(0, xp.getAsDouble()) : 0.0);
            if (xp != null) count++;
        }
        profile.put("skills", skills);
        done.add("Skills: " + count + " of " + Skill.values().length);
    }

    private void collections(Document profile) {
        JsonObject counts = HypixelProfile.object(from.member(), "collection");
        profile.remove(Collections.COUNTS);
        int known = 0;
        int unknown = 0;
        for (Map.Entry<String, JsonElement> entry : counts == null ? List.<Map.Entry<String, JsonElement>>of() : counts.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) continue;
            if (Collections.set(profile, entry.getKey(), entry.getValue().getAsLong()) != null) known++;
            else unknown++;
        }
        done.add("Collections: " + known + (unknown > 0 ? " &8(" + unknown + " this plugin doesn't have)" : ""));
    }

    private void dungeons(Document profile) {
        JsonObject dungeons = HypixelProfile.object(from.member(), "dungeons");
        JsonObject types = HypixelProfile.object(dungeons, "dungeon_types");
        JsonObject catacombs = HypixelProfile.object(types, "catacombs");
        JsonObject master = HypixelProfile.object(types, "master_catacombs");
        JsonObject classes = HypixelProfile.object(dungeons, "player_classes");
        Document ours = profile.get("dungeons") instanceof Document d ? d : new Document();
        ours.put("catacombsExp", number(catacombs, "experience"));
        Document floors = ours.get("floors") instanceof Document d ? d : new Document();
        floors.put("highest", (int) Math.max(0, number(catacombs, "highest_tier_completed")));
        floors.put("masterHighest", (int) Math.max(0, number(master, "highest_tier_completed")));
        ours.put("floors", floors);
        Document classExp = new Document();
        for (DungeonClass dungeonClass : DungeonClass.values()) {
            classExp.put(dungeonClass.name(), number(HypixelProfile.object(classes, dungeonClass.name().toLowerCase(Locale.ROOT)), "experience"));
        }
        ours.put("classExp", classExp);
        if (dungeons != null && dungeons.has("selected_dungeon_class") && dungeons.get("selected_dungeon_class").isJsonPrimitive()) {
            String selected = dungeons.get("selected_dungeon_class").getAsString().toUpperCase(Locale.ROOT);
            for (DungeonClass dungeonClass : DungeonClass.values()) if (dungeonClass.name().equals(selected)) ours.put("selectedClass", selected);
        }
        JsonObject essences = HypixelProfile.object(HypixelProfile.object(from.member(), "currencies"), "essence");
        Document essence = new Document();
        for (EssenceType type : EssenceType.values()) {
            essence.put(type.name().toLowerCase(Locale.ROOT), (int) number(HypixelProfile.object(essences, type.name()), "current"));
        }
        ours.put("essence", essence);
        profile.put("dungeons", ours);
        done.add("Dungeons: Catacombs XP &b" + Text.number(number(catacombs, "experience")) + "&7, floor " + floors.get("highest")
                + ", master floor " + floors.get("masterHighest"));
    }

    private void minions(Document profile) {
        JsonElement crafted = from.at("player_data.crafted_generators");
        List<String> tiers = new ArrayList<>();
        if (crafted != null && crafted.isJsonArray()) for (JsonElement tier : crafted.getAsJsonArray()) tiers.add(tier.getAsString());
        Document minions = profile.get("minions") instanceof Document d ? d : new Document();
        minions.put("craftedMinions", tiers);
        profile.put("minions", minions);
        done.add("Crafted Minions: " + tiers.size());
    }

    private static double number(JsonObject in, String key) {
        return in != null && in.get(key) instanceof JsonElement e && e.isJsonPrimitive() ? e.getAsDouble() : 0;
    }

    // Items

    private void items(YoinkPart part, Document profile) {
        done.add(part.title() + ": not yet");
    }
}
