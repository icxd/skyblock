package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.npc.Abiphone;
import net.icxd.dungeons.npc.Lines;
import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.pet.PetData.Formula;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetData.Variant;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Fann the pet trainer, at the Hub's Pet Care (npcs/hub.json), as the wiki's Fann has her (never recorded): with
 * Taming 10 she trains a pet from your inventory for so many days, or until a level, and it gains that much Pet XP
 * a day by the training's type (Free, its XP by the Taming level; Light to Turbo!, coins a day and Turbo!'s Bits, each
 * a Taming level more: the Fann page's table, where the wiki's pet calculator has other costs). An Owl pet put in the
 * session adds its Efficient Trainer's XP (+0.1% a level), and the coins spent at her take off its Training Refunds
 * (1% a million, to 5%: the Owl's lore has the first, UNKNOWN past it). When it's done she rings ({@link Abiphone};
 * answering, the wiki's lines for it), and the pet waits in its slot (Pet Training, {@link PetTrainingMenu}) to be
 * picked up, its Owl with it. A session can be stopped at any time: the pet comes back with the XP so far, and half
 * the coins of the time left (the wiki's "only refunds half the remaining training time", UNKNOWN how). One slot:
 * #2 and #3 open with Great and Ultimate Carrot Candies fed to Hootie, who isn't here. She also sells the Owl pet
 * (2,200,000 coins). Main thread.
 */
public final class Fann {
    public static final String ID = "FANN";
    static final String SAYS = "&e[NPC] Fann&f: &r";
    static final String ON_PHONE = "&e[NPC] Fann&f: &b✆ &f";
    /** UNKNOWN: a villager's, high, as Kat's and Bea's. */
    static final Voice VOICE = Voice.villager(2);
    static final int TAMING = 10;
    static final String FIELD = "fann";
    static final String SESSIONS = "sessions";
    static final String SPENT = "spent";
    static final int SLOTS = 3;
    /** #1 alone (the others need Hootie). */
    static final int OPEN_SLOTS = 1;
    static final String OWL = "OWL";
    static final long OWL_PRICE = 2_200_000;
    static final double MOST_REFUND = 5;
    static final double REFUND_STEP = 1_000_000;
    static final long DAY = 86_400_000;
    static final long RING_CHECK_TICKS = 20;
    static final long LINE_TICKS = 20;
    /** The wiki's. */
    static final List<String> TOO_LOW = List.of("My services aren't for everyone young traveler!", "Come back to me when you have &aTaming Skill 10");
    static final List<String> FIRST_TALK = List.of("Hey there, adventurer!", "Meet my lovable owl, Hootie!", "We offer a special service here.",
            "For a small fee, we'll train your pets and help them while you're off doing heroic things.", "Hootie approves, trust me!",
            "So, interested in leaving your pet with us for a bit?");

    /** A training's type, as the wiki's Fann: its name and colour (SkyHanni's "&a▶ Light"), Taming level, XP, coins and Bits a day. */
    enum Training {
        FREE("Free", 'f', 10, 0, 0, 0),
        LIGHT("Light", 'a', 20, 250_000, 200_000, 0),
        MODERATE("Moderate", '9', 30, 500_000, 1_000_000, 0),
        EXPERT("Expert", '5', 40, 750_000, 2_000_000, 0),
        ULTRA("Ultra", '6', 50, 1_500_000, 2_750_000, 0),
        TURBO("Turbo!", 'd', 60, 2_500_000, 3_000_000, 500);

        final String label;
        final char color;
        final int taming;
        final double xp;
        final double coins;
        final double bits;

        Training(String label, char color, int taming, double xp, double coins, double bits) {
            this.label = label;
            this.color = color;
            this.taming = taming;
            this.xp = xp;
            this.coins = coins;
            this.bits = bits;
        }

        /** "&aLight". */
        String colored() {
            return "&" + color + label;
        }

        /** Pet XP a day at this Taming level, before an Owl's. */
        double xpPerDay(int tamingLevel) {
            return this == FREE ? freeXp(tamingLevel) : xp;
        }
    }

    /**
     * Free training's XP a day: 20,000 at Taming 10 ("Extra Free Training", the wiki's UI), 500 more a level to 50 and
     * 1,000 more past it, 50,000 at 60 (the wiki's pet calculator).
     */
    static double freeXp(int taming) {
        int level = Math.max(TAMING, taming);
        return level < 50 ? 20_000 + (level - TAMING) * 500 : 40_000 + (level - 50) * 1_000;
    }

    /** A session: the pet, its Owl (null for none), the type, when it started and ends, and the XP it gives in all. */
    record Session(OwnedPet pet, OwnedPet owl, Training type, long start, long end, double xp, boolean rang) {
        boolean done(long now) {
            return now >= end;
        }

        /** Its XP so far. */
        double xpBy(long now) {
            if (end <= start || now >= end) return xp;
            return xp * Math.max(0, now - start) / (end - start);
        }
    }

    private Fann() {
    }

    /** Rings anyone whose session is done, once for each, wherever they are. */
    public static void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> ringAll(plugin), RING_CHECK_TICKS, RING_CHECK_TICKS);
    }

    public static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || Lines.talking(player)) return;
        Document profile = user.profile();
        if (Skills.level(profile, Skill.TAMING) < TAMING) {
            Lines.say(plugin, player, said(TOO_LOW), ticks(TOO_LOW.size()), VOICE, null);
            return;
        }
        if (Npc.firstTalk(profile, ID)) {
            Lines.say(plugin, player, said(FIRST_TALK), ticks(FIRST_TALK.size()), VOICE, () -> PetTrainingMenu.show(player, MenuSound.CLICK));
            return;
        }
        PetTrainingMenu.show(player, MenuSound.CLICK);
    }

    // What a session gives and costs

    /** An Owl's Efficient Trainer, in percent: its number at its level (+0.1% a level in the data). */
    static double owlBonus(OwnedPet owl, PetData data) {
        if (owl == null || !OWL.equals(owl.type())) return 0;
        Pet kind = data.pet(OWL);
        Variant variant = kind == null ? null : kind.variant(owl.tier());
        Formula efficient = variant == null || variant.numbers().isEmpty() ? null : variant.numbers().getFirst();
        int level = Pets.level(owl, data);
        return efficient == null ? 0.1 * level : efficient.at(level);
    }

    /** The Owl's Training Refunds for these coins spent at Fann, in percent. */
    static double refund(double spent) {
        return Math.min(MOST_REFUND, Math.floor(spent / REFUND_STEP));
    }

    /** What a session would be: its days, XP, coins (less the refund) and Bits. */
    record Plan(double days, double xp, double coins, double bits) {
    }

    /** For so many days. */
    static Plan forDays(Training type, int taming, double owlBonus, double refund, double days) {
        double xp = type.xpPerDay(taming) * (1 + owlBonus / 100) * days;
        return new Plan(days, xp, type.coins * days * (1 - refund / 100), type.bits * days);
    }

    /** Until it has {@code xpNeeded} more. */
    static Plan forXp(Training type, int taming, double owlBonus, double refund, double xpNeeded) {
        double perDay = type.xpPerDay(taming) * (1 + owlBonus / 100);
        return forDays(type, taming, owlBonus, refund, Math.max(0, xpNeeded) / perDay);
    }

    /** The most a type their Taming level allows. */
    static List<Training> allowed(int taming) {
        return Arrays.stream(Training.values()).filter(t -> taming >= t.taming).toList();
    }

    // Starting, stopping and picking up

    /** Starts a session in slot {@code slot}; why not, or null if it started (the coins and Bits are paid). */
    static String begin(Player player, User user, int slot, PlacedPet pet, PlacedPet owl, Training type, Plan plan) {
        Document profile = user.profile();
        if (slot >= OPEN_SLOTS || sessions(profile).get(slot) != null) return "&cThis slot is in use!";
        if (Skills.level(profile, Skill.TAMING) < type.taming) return "&cYou need &aTaming " + type.taming + " &cfor this training!";
        if (!(plan.days() > 0)) return "&cThis pet doesn't need any more training!";
        double coins = Math.ceil(plan.coins());
        int bits = (int) Math.ceil(plan.bits());
        if (!Purse.has(user, coins)) return "&cYou don't have enough coins!";
        if (user.getBits() < bits) return "&cYou don't have enough Bits!";
        if (!pet.use()) return "&cThat pet isn't here any more!";
        OwnedPet owlPet = owl != null && owl.use() ? owl.pet() : null;
        Purse.take(user, coins);
        if (bits > 0) profile.put("bits", user.getBits() - bits);
        Document fann = part(profile, true);
        fann.put(SPENT, spent(profile) + coins);
        long now = System.currentTimeMillis();
        List<Session> sessions = sessions(profile);
        sessions.set(slot, new Session(pet.pet(), owlPet, type, now, now + Math.round(plan.days() * DAY), plan.xp(), false));
        put(profile, sessions);
        user.save();
        return null;
    }

    /** Its pet back (with the XP so far, or all of it once done) and its Owl; stopped early, half the time left's coins. */
    static void pickUp(Player player, int slot) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        Document profile = user.profile();
        List<Session> sessions = sessions(profile);
        Session session = slot < sessions.size() ? sessions.get(slot) : null;
        if (session == null) return;
        long now = System.currentTimeMillis();
        sessions.set(slot, null);
        put(profile, sessions);
        if (!session.done(now)) {
            double left = (double) (session.end() - now) / DAY;
            double refund = Math.floor(session.type().coins * left * (1 - refund(spent(profile)) / 100) / 2);
            if (refund > 0) Purse.add(user, refund);
        }
        ItemStash.give(player, PetItems.item(session.pet().withExp(session.pet().exp() + session.xpBy(now))));
        if (session.owl() != null) ItemStash.give(player, PetItems.item(session.owl()));
        user.save();
    }

    /** Buys an Owl pet. */
    static void buyOwl(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        OwnedPet owl = PetItems.newPet(OWL);
        if (owl == null) return;
        if (!Purse.take(user, OWL_PRICE)) {
            player.sendMessage(Text.line("&cYou don't have enough coins!"));
            return;
        }
        ItemStash.give(player, PetItems.item(owl));
        user.save();
        player.sendMessage(Text.line("&aYou bought " + PetItems.shopName(owl) + " &afor &6" + Text.number(OWL_PRICE) + " Coins&a!"));
    }

    static void ringAll(Plugin plugin) {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = User.ifLoaded(player.getUniqueId());
            if (user == null || user.isReleased()) continue;
            Document profile = user.profile();
            List<Session> sessions = sessions(profile);
            List<String> concluded = new ArrayList<>();
            for (int i = 0; i < sessions.size(); i++) {
                Session session = sessions.get(i);
                if (session == null || session.rang() || !session.done(now)) continue;
                sessions.set(i, new Session(session.pet(), session.owl(), session.type(), session.start(), session.end(), session.xp(), true));
                concluded.add(concluded(i, session, Pets.data()));
            }
            if (concluded.isEmpty()) continue;
            put(profile, sessions);
            List<String> lines = new ArrayList<>();
            lines.add(ON_PHONE + "Hey &b" + player.getName() + "&f!");
            for (String line : concluded) lines.add(ON_PHONE + line);
            Abiphone.ring(plugin, player, "Fann", () -> Lines.say(plugin, player, lines, ticks(lines.size()), VOICE, null));
        }
    }

    /** The wiki's "&e[#1] &f[Training Type] [Pet] has concluded and is now &7[Lvl #]". */
    static String concluded(int slot, Session session, PetData data) {
        OwnedPet trained = session.pet().withExp(session.pet().exp() + session.xp());
        return "&e[#" + (slot + 1) + "] &f" + session.type().label + " " + PetText.colored(session.pet(), data) + " &fhas concluded and is now &7[Lvl "
                + Pets.level(trained, data) + "]";
    }

    // Kept with the profile

    static List<Session> sessions(Document profile) {
        Document fann = part(profile, false);
        List<Session> out = new ArrayList<>(Arrays.asList(new Session[SLOTS]));
        if (fann == null || !(fann.get(SESSIONS) instanceof List<?> list)) return out;
        for (int i = 0; i < SLOTS && i < list.size(); i++) if (list.get(i) instanceof Document d) out.set(i, session(d));
        return out;
    }

    private static Session session(Document d) {
        OwnedPet pet = OwnedPet.from(d.get("pet", Document.class));
        if (pet == null) return null;
        Training type;
        try {
            type = Training.valueOf(d.getString("type"));
        } catch (IllegalArgumentException | NullPointerException e) {
            type = Training.FREE;
        }
        return new Session(pet, OwnedPet.from(d.get("owl", Document.class)), type, number(d, "start"), number(d, "end"),
                d.get("xp") instanceof Number n ? n.doubleValue() : 0, Boolean.TRUE.equals(d.get("rang")));
    }

    static void put(Document profile, List<Session> sessions) {
        List<Document> out = new ArrayList<>();
        for (Session s : sessions) {
            if (s == null) {
                out.add(null);
                continue;
            }
            Document d = new Document("pet", s.pet().document()).append("type", s.type().name()).append("start", s.start())
                    .append("end", s.end()).append("xp", s.xp());
            if (s.owl() != null) d.append("owl", s.owl().document());
            if (s.rang()) d.append("rang", true);
            out.add(d);
        }
        part(profile, true).put(SESSIONS, out);
    }

    /** Coins spent at Fann, for the Owl's Training Refunds. */
    static double spent(Document profile) {
        Document fann = part(profile, false);
        return fann != null && fann.get(SPENT) instanceof Number n ? n.doubleValue() : 0;
    }

    private static long number(Document d, String key) {
        return d.get(key) instanceof Number n ? n.longValue() : 0;
    }

    private static Document part(Document profile, boolean make) {
        Document pets = Pets.part(profile, make);
        Document fann = pets == null ? null : pets.get(FIELD, Document.class);
        if (fann == null && make) {
            fann = new Document();
            pets.put(FIELD, fann);
        }
        return fann;
    }

    // Words

    private static List<String> said(List<String> lines) {
        return lines.stream().map(line -> SAYS + line).toList();
    }

    /** A second apart (UNKNOWN). */
    static long[] ticks(int lines) {
        long[] out = new long[lines];
        for (int i = 0; i < lines; i++) out[i] = i * LINE_TICKS;
        return out;
    }

    /** "0d 0h 10m 29s", as SkyHanni reads her "Will take:". */
    static String duration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        return seconds / 86_400 + "d " + seconds % 86_400 / 3_600 + "h " + seconds % 3_600 / 60 + "m " + seconds % 60 + "s";
    }

    /** "22,795.5": coins and Bits with a decimal, as SkyHanni reads hers. */
    static String decimal(double value) {
        return Text.number(Math.round(value * 10) / 10.0);
    }

    /** Whether it can be trained: not an Owl (it goes in the Owl's place), not at its most level. */
    static boolean trainable(OwnedPet pet, PetData data) {
        Pet kind = data.pet(pet.type());
        return kind != null && !OWL.equals(pet.type()) && Pets.level(pet, data) < data.maxLevel(kind);
    }

    /** The XP it needs to reach {@code level} (0 if it has it). */
    static double xpTo(OwnedPet pet, PetData data, int level) {
        Pet kind = data.pet(pet.type());
        if (kind == null) return 0;
        Rarity tier = Pets.tier(pet, data);
        return Math.max(0, data.totalXp(kind, tier, level) - pet.exp());
    }
}
