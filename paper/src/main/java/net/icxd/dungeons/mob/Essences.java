package net.icxd.dungeons.mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;

/**
 * Essence from dungeon mobs: no item, straight onto the killer's profile ({@code dungeons.essence.<type>}),
 * with "+1 Undead Essence" on the action bar in Defense's place for about 1.5 seconds (3 packets), over a
 * Combat XP gain's (research critic.md 3.4). The amounts are the wiki's rows: the Crypt Undead's Undead
 * Essence 1 and 1 more half the time, the Lost Adventurer's Dragon Essence and the Angry Archaeologist's
 * Diamond Essence 3, 1 more at 70% and 1 more at 50%. The recordings only ever showed "+1" for all three:
 * each row here is a gain of its own and the bar shows the last (whether Hypixel's shows the last roll or
 * the wiki's amounts are off is UNKNOWN). Main thread.
 */
public final class Essences implements Listener {
    /** How long a gain shows: 3 action bar packets on Hypixel. */
    static final long SHOWN_MILLIS = 1_500;

    /** One row of a mob's essence: this much, this often (in percent). */
    record Roll(EssenceType type, int amount, double chance) {
    }

    private static final Map<String, List<Roll>> ROLLS = Map.of(
            MobKinds.CRYPT_UNDEAD.id(), List.of(new Roll(EssenceType.UNDEAD, 1, 100), new Roll(EssenceType.UNDEAD, 1, 50)),
            MobKinds.LOST_ADVENTURER.id(), List.of(new Roll(EssenceType.DRAGON, 3, 100), new Roll(EssenceType.DRAGON, 1, 70),
                    new Roll(EssenceType.DRAGON, 1, 50)),
            MobKinds.ANGRY_ARCHAEOLOGIST.id(), List.of(new Roll(EssenceType.DIAMOND, 3, 100), new Roll(EssenceType.DIAMOND, 1, 70),
                    new Roll(EssenceType.DIAMOND, 1, 50)));

    /** What a kind of mob's essence rows are (none for most). */
    static List<Roll> rolls(String kind) {
        return ROLLS.getOrDefault(kind, List.of());
    }

    /** The rows that came up, in order. */
    static List<Roll> roll(List<Roll> rolls, RandomGenerator random) {
        List<Roll> out = new ArrayList<>();
        for (Roll roll : rolls) {
            if (roll.chance() >= 100 || random.nextDouble() * 100 < roll.chance()) out.add(roll);
        }
        return out;
    }

    /** "&d+1 Undead Essence". */
    static String actionBar(EssenceType type, int amount) {
        return "&d+" + amount + " " + Utils.title(type.name()) + " Essence";
    }

    /** The profile's essence of a type (0 for none). */
    public static int essence(Document profile, EssenceType type) {
        Document dungeons = profile == null ? null : profile.get("dungeons", Document.class);
        Document essence = dungeons == null ? null : dungeons.get("essence", Document.class);
        return essence != null && essence.get(key(type)) instanceof Number n ? n.intValue() : 0;
    }

    /** Adds essence to the profile. */
    public static void add(Document profile, EssenceType type, int amount) {
        if (amount <= 0) return;
        Document dungeons = profile.get("dungeons", Document.class);
        if (dungeons == null) {
            dungeons = new Document();
            profile.put("dungeons", dungeons);
        }
        Document essence = dungeons.get("essence", Document.class);
        if (essence == null) {
            essence = new Document();
            dungeons.put("essence", essence);
        }
        essence.put(key(type), essence(profile, type) + amount);
    }

    private static String key(EssenceType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    /** After SkillGains, so the essence shows over the Combat XP. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer == null || !killer.isOnline()) return;
        List<Roll> gains = roll(rolls(event.kind().id()), java.util.concurrent.ThreadLocalRandom.current());
        if (gains.isEmpty()) return;
        User user = User.ifLoaded(killer.getUniqueId());
        if (user == null || user.isReleased()) return;
        for (Roll gain : gains) add(user.profile(), gain.type(), gain.amount());
        user.save();
        Roll last = gains.get(gains.size() - 1);
        PlayerSession session = PlayerSession.of(killer);
        Replacement shown = Replacement.forMillis(actionBar(last.type(), last.amount()), SHOWN_MILLIS);
        session.setDefenseReplacement(shown);
        StatsRunnable.sendActionBar(killer);
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (!killer.isOnline() || session.getDefenseReplacement() != shown) return;
            session.setDefenseReplacement(null);
            StatsRunnable.sendActionBar(killer);
        }, SHOWN_MILLIS / 50);
    }
}
