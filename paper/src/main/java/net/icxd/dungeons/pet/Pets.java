package net.icxd.dungeons.pet;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.pet.PetData.Formula;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetData.Reward;
import net.icxd.dungeons.pet.PetData.Variant;
import net.icxd.dungeons.pet.ability.PetAbilities;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * A profile's pets: its Pets menu's ({@code pets.list}, each as {@link OwnedPet#document}), the one summoned
 * ({@code pets.active}), the menu's settings, how many it can hold, and the highest Pet Score it has had (what its
 * SkyBlock XP counts: removing a pet lowers the score the menu shows, not the XP, recorded 2026-10-25). The data
 * ({@link PetData}) is read off the main thread when the plugin starts. What's here needs no server. Main thread.
 */
public final class Pets {
    static final String FIELD = "pets";
    static final String LIST = "list";
    static final String ACTIVE = "active";
    static final String SORT = "sort";
    static final String VISIBILITY = "visibility";
    static final String NAMETAGS = "nametags";
    static final String UPGRADES = "upgrades";
    static final String BEST_SCORE = "bestScore";
    /** "Current Max Pets: 112", and "+28 Pet Slots" (5,000 Bits) twice more (the wiki's Pets). */
    public static final int SLOTS = 112;
    /** The pets a Tier Boost does nothing for (the wiki's Pet Items; SkyCrypt's {@code ignoresTierBoost}). */
    static final Set<String> NO_TIER_BOOST = Set.of("GRIFFIN", "DROPLET_WISP", "FROST_WISP", "GLACIAL_WISP", "SUBZERO_WISP");
    public static final int SLOTS_PER_UPGRADE = 28;
    public static final int MOST_UPGRADES = 2;
    public static final int UPGRADE_BITS = 5_000;

    private static volatile PetData data = PetData.empty();

    private Pets() {
    }

    /**
     * Reads the data off the main thread, and starts what pets do: the PET item shown as its pet, skill XP's share for
     * the summoned one, and summoned pets following their owners. Once, when the plugin starts.
     */
    public static void start(JavaPlugin plugin) {
        ItemBehaviours.register(PetItems.ID, new PetItems());
        SkillGains.addGainListener(PetXp::gained);
        PetFollowers.start(plugin);
        Kat.start(plugin);
        Fann.start(plugin);
        George.enable();
        Logger log = plugin.getLogger();
        Path folder = plugin.getDataFolder().toPath();
        CompletableFuture.supplyAsync(() -> PetData.load(folder)).thenAccept(loaded -> Bukkit.getScheduler().runTask(plugin, () -> {
            loaded.problems().forEach(p -> log.warning("Pets: " + p));
            data = loaded;
            if (loaded.count() > 0) {
                log.info("Pets: " + loaded.count() + " pets, " + loaded.scoreRewards().size() + " Pet Score rewards");
                PetAbilities.problems(loaded).forEach(p -> log.warning("Pet abilities: " + p));
                PetFollowers.refreshAll();
            }
        }));
    }

    public static PetData data() {
        return data;
    }

    /** For tests: the data they count with. */
    static void data(PetData loaded) {
        data = loaded;
    }

    // The Pets menu's

    /** The menu's sorts, as recorded ("&b▶ Rarity", then A to Z, Z to A, Pet Exp, Skill). */
    public enum Sort {
        RARITY("Rarity"), A_TO_Z("A to Z"), Z_TO_A("Z to A"), PET_EXP("Pet Exp"), SKILL("Skill");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** The next (a left click), or the one before (a right click, "Right-Click to go backwards!"), round. */
        public Sort step(boolean back) {
            Sort[] all = values();
            return all[(ordinal() + (back ? all.length - 1 : 1)) % all.length];
        }
    }

    /** Whose pets they see, as the menu lists them; a left click takes the next (recorded: Hide all, Only your pet, Show all). */
    public enum Visibility {
        SHOW_ALL("Show all pets"), HIDE_ALL("Hide all pets"), ONLY_YOURS("Only your pet");

        private final String label;

        Visibility(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public Visibility next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    static Document part(Document profile, boolean make) {
        Document pets = profile == null ? null : profile.get(FIELD, Document.class);
        if (pets == null && make) {
            pets = new Document();
            profile.put(FIELD, pets);
        }
        return pets;
    }

    /** The profile's pets, as kept (the menu sorts them). */
    public static List<OwnedPet> pets(Document profile) {
        Document part = part(profile, false);
        List<OwnedPet> out = new ArrayList<>();
        if (part == null || !(part.get(LIST) instanceof List<?> list)) return out;
        for (Object o : list) {
            OwnedPet pet = o instanceof Document d ? OwnedPet.from(d) : null;
            if (pet != null) out.add(pet);
        }
        return out;
    }

    /** Replaces the profile's pets (yoinking a real profile's). The one summoned stays so if it's among them. */
    public static void set(Document profile, List<OwnedPet> pets) {
        List<Document> stored = new ArrayList<>(pets.size());
        for (OwnedPet pet : pets) stored.add(pet.document());
        Document part = part(profile, true);
        part.put(LIST, stored);
        String active = part.getString(ACTIVE);
        if (active != null && pets.stream().noneMatch(p -> p.uuid().equals(active))) part.remove(ACTIVE);
    }

    /** A real profile's pets in place of these (see Yoink), the one out with them, and its highest Pet Score raised to theirs. */
    public static void yoinked(Document profile, List<OwnedPet> pets, String active) {
        set(profile, pets);
        activate(profile, active);
        raiseBest(profile, data());
    }

    public static OwnedPet find(Document profile, String uuid) {
        for (OwnedPet pet : pets(profile)) if (pet.uuid().equals(uuid)) return pet;
        return null;
    }

    /** The one summoned; null for none. */
    public static OwnedPet active(Document profile) {
        Document part = part(profile, false);
        String uuid = part == null ? null : part.getString(ACTIVE);
        return uuid == null ? null : find(profile, uuid);
    }

    /** Summons this one (null: none). */
    static void activate(Document profile, String uuid) {
        Document part = part(profile, true);
        if (uuid == null) part.remove(ACTIVE);
        else part.put(ACTIVE, uuid);
    }

    /** How many the menu holds: 112, and 28 more for each upgrade bought. */
    public static int slots(Document profile) {
        Document part = part(profile, false);
        int upgrades = part != null && part.get(UPGRADES) instanceof Number n ? n.intValue() : 0;
        return SLOTS + SLOTS_PER_UPGRADE * Math.max(0, Math.min(MOST_UPGRADES, upgrades));
    }

    static int upgrades(Document profile) {
        return (slots(profile) - SLOTS) / SLOTS_PER_UPGRADE;
    }

    static void upgrade(Document profile) {
        part(profile, true).put(UPGRADES, Math.min(MOST_UPGRADES, upgrades(profile) + 1));
    }

    /** Adds one to the menu; false if it's full (it isn't added). */
    public static boolean add(Document profile, OwnedPet pet) {
        List<OwnedPet> pets = pets(profile);
        if (pets.size() >= slots(profile)) return false;
        pets.removeIf(p -> p.uuid().equals(pet.uuid()));
        pets.add(pet);
        set(profile, pets);
        return true;
    }

    /** Takes one out of the menu (despawned, if it was out, and out of its Exp Share slot); null if it isn't there. */
    public static OwnedPet remove(Document profile, String uuid) {
        List<OwnedPet> pets = pets(profile);
        OwnedPet found = null;
        for (OwnedPet pet : pets) if (pet.uuid().equals(uuid)) found = pet;
        if (found == null) return null;
        pets.remove(found);
        set(profile, pets);
        ExpSharing.forget(profile, uuid);
        return found;
    }

    /** Puts this one in place of the one with its uuid (its XP changed, it's a favourite now). */
    static void replace(Document profile, OwnedPet pet) {
        List<OwnedPet> pets = pets(profile);
        for (int i = 0; i < pets.size(); i++) if (pets.get(i).uuid().equals(pet.uuid())) pets.set(i, pet);
        set(profile, pets);
    }

    public static Sort sort(Document profile) {
        Document part = part(profile, false);
        try {
            return part == null || part.getString(SORT) == null ? Sort.RARITY : Sort.valueOf(part.getString(SORT));
        } catch (IllegalArgumentException e) {
            return Sort.RARITY;
        }
    }

    static void sort(Document profile, Sort sort) {
        part(profile, true).put(SORT, sort.name());
    }

    /** Everyone's pets, by default, as the wiki's menu shows it. */
    public static Visibility visibility(Document profile) {
        Document part = part(profile, false);
        try {
            return part == null || part.getString(VISIBILITY) == null ? Visibility.SHOW_ALL : Visibility.valueOf(part.getString(VISIBILITY));
        } catch (IllegalArgumentException e) {
            return Visibility.SHOW_ALL;
        }
    }

    static void visibility(Document profile, Visibility visibility) {
        part(profile, true).put(VISIBILITY, visibility.name());
    }

    /** Whether they see pets' names over them (on by default, as recorded). */
    public static boolean nametags(Document profile) {
        Document part = part(profile, false);
        return part == null || !Boolean.FALSE.equals(part.get(NAMETAGS));
    }

    static void nametags(Document profile, boolean on) {
        part(profile, true).put(NAMETAGS, on);
    }

    /** In the menu's order: favourites first (recorded), then by the sort; Rarity's is the best first, as recorded. */
    public static List<OwnedPet> sorted(List<OwnedPet> pets, Sort sort, PetData data) {
        Comparator<OwnedPet> byName = Comparator.comparing(p -> name(p, data));
        Comparator<OwnedPet> order = switch (sort) {
            case RARITY -> Comparator.<OwnedPet, Rarity>comparing(OwnedPet::tier).reversed()
                    .thenComparing(Comparator.comparingDouble(OwnedPet::exp).reversed()).thenComparing(byName);
            case A_TO_Z -> byName.thenComparing(Comparator.<OwnedPet, Rarity>comparing(OwnedPet::tier).reversed())
                    .thenComparing(Comparator.comparingDouble(OwnedPet::exp).reversed());
            case Z_TO_A -> byName.reversed().thenComparing(Comparator.<OwnedPet, Rarity>comparing(OwnedPet::tier).reversed())
                    .thenComparing(Comparator.comparingDouble(OwnedPet::exp).reversed());
            case PET_EXP -> Comparator.comparingDouble(OwnedPet::exp).reversed().thenComparing(byName);
            case SKILL -> Comparator.<OwnedPet, String>comparing(p -> skill(p, data)).thenComparing(Comparator.<OwnedPet, Rarity>comparing(OwnedPet::tier).reversed())
                    .thenComparing(Comparator.comparingDouble(OwnedPet::exp).reversed());
        };
        List<OwnedPet> out = new ArrayList<>(pets);
        out.sort(Comparator.comparing((OwnedPet p) -> !p.favorite()).thenComparing(order));
        return out;
    }

    private static String name(OwnedPet pet, PetData data) {
        Pet kind = data.pet(pet.type());
        return kind == null ? pet.type() : kind.name(tier(pet, data));
    }

    private static String skill(OwnedPet pet, PetData data) {
        Pet kind = data.pet(pet.type());
        return kind == null || kind.kind() == null ? "~" : kind.kind();
    }

    // Rarity and levels

    /**
     * The rarity a pet is as: its own, or as many more as its held item raises it (the Tier Boost's 1, "until the Pet Item
     * held by the Pet is changed", the wiki's Pets), up to Legendary ("It cannot increase a Rarity to Mythic"), to a
     * rarity its type has, and none for the Griffin and the Wisps ("Does not work on the Griffin Pet or the Wisp Pet").
     * Everything goes by it but its item's {@code petInfo} and Kat, which keep its own.
     */
    public static Rarity tier(OwnedPet pet, PetData data) {
        int tiers = HeldItems.effect(pet.heldItem()).tiers();
        if (tiers <= 0 || NO_TIER_BOOST.contains(pet.type())) return pet.tier();
        Pet kind = data.pet(pet.type());
        Rarity tier = pet.tier();
        for (int i = 0; i < tiers && tier.compareTo(Rarity.LEGENDARY) < 0; i++) {
            Rarity next = tier.upgrade();
            if (kind == null || kind.variant(next) == null) break;
            tier = next;
        }
        return tier;
    }

    /** Its level: its XP on the table of the rarity it's as (see {@link #tier}). */
    public static int level(OwnedPet pet, PetData data) {
        return data.level(data.pet(pet.type()), tier(pet, data), pet.exp());
    }

    public static boolean maxed(OwnedPet pet, PetData data) {
        Pet kind = data.pet(pet.type());
        return kind != null && level(pet, data) >= data.maxLevel(kind);
    }

    // The Pet Score

    /**
     * The menu's Pet Score: each pet type counts once, its best (the highest rarity, then the highest level), by its
     * rarity ("+1→6"), one more at its most level; Special and Very Special count none (the wiki's Pets).
     */
    public static int score(List<OwnedPet> pets, PetData data) {
        Map<String, OwnedPet> best = new HashMap<>();
        for (OwnedPet pet : pets) {
            if (data.pet(pet.type()) == null || data.scoreOf(tier(pet, data)) == 0) continue;
            best.merge(pet.type(), pet, (a, b) -> better(a, b, data));
        }
        int score = 0;
        for (OwnedPet pet : best.values()) score += data.scoreOf(tier(pet, data)) + (maxed(pet, data) ? data.scoreMaxLevel() : 0);
        return score;
    }

    /** The better of two pets of a type: the higher rarity, then the higher level (the first, if they're the same). */
    public static OwnedPet better(OwnedPet a, OwnedPet b, PetData data) {
        Rarity at = tier(a, data);
        Rarity bt = tier(b, data);
        if (at != bt) return at.compareTo(bt) > 0 ? a : b;
        return level(a, data) >= level(b, data) ? a : b;
    }

    public static int score(Document profile, PetData data) {
        return score(pets(profile), data);
    }

    /** The highest Pet Score the profile has had: what the Pet Score task's SkyBlock XP counts. */
    public static int bestScore(Document profile) {
        Document part = part(profile, false);
        return part != null && part.get(BEST_SCORE) instanceof Number n ? n.intValue() : 0;
    }

    /** Keeps the score if it's the highest yet; whether it was. */
    static boolean raiseBest(Document profile, PetData data) {
        int score = score(profile, data);
        if (score <= bestScore(profile)) return false;
        part(profile, true).put(BEST_SCORE, score);
        return true;
    }

    /** The Magic Find the Pet Score gives: the highest threshold it reaches's. */
    public static int magicFind(int score, PetData data) {
        int magicFind = 0;
        for (Reward reward : data.scoreRewards()) if (score >= reward.score()) magicFind = reward.magicFind();
        return magicFind;
    }

    // Stats

    /** A pet's own stats at its level (none before its stats start: the dragons' egg). */
    public static Stats stats(OwnedPet pet, PetData data) {
        Stats stats = new Stats();
        Pet kind = pet == null ? null : data.pet(pet.type());
        Variant variant = kind == null ? null : kind.variant(tier(pet, data));
        if (variant == null) return stats;
        int level = level(pet, data);
        if (level < kind.statsFrom()) return stats;
        for (Map.Entry<Stat, Formula> e : variant.stats().entrySet()) stats.add(e.getKey(), e.getValue().at(level));
        return stats;
    }

    /**
     * A pet's stats with its held item's (see {@link HeldItems#granted}), as its lore shows them: the Dwarf Turtle Shelmet's
     * +10 True Defense is in its True Defense line (recorded), a Minos Relic's third more in each (UNKNOWN whether the
     * lore shows those).
     */
    public static Stats withItem(OwnedPet pet, PetData data) {
        Stats base = stats(pet, data);
        return base.copy().add(HeldItems.granted(pet.heldItem(), base, level(pet, data)));
    }

    /**
     * A pet's own stats for its owner (null for none known): at its level, twice that with what they wear that doubles
     * them (the Bee Mask's "Double the base pet stats of your active Bee pet", see PetCopies).
     */
    static Stats base(Player owner, OwnedPet pet, PetData data) {
        Stats base = stats(pet, data);
        Pet kind = data.pet(pet.type());
        if (owner == null || kind == null || !PetCopies.doubled(owner, kind.name(tier(pet, data)))) return base;
        for (Stat stat : Stat.values()) if (base.has(stat)) base.set(stat, base.get(stat) * 2);
        return base;
    }

    /**
     * What copies of a pet's stats copy (Chimera, the Manticore Claw, the Daedalus Blades, see PetCopies): its own (see
     * {@link #base}) and its held item's, not what its abilities give it ("It does not copy stats granted by pet
     * abilities, but it does copy stats added to the pet's base stats by Pet Items", the wiki's Chimera).
     */
    public static Stats copyable(Player owner, OwnedPet pet, PetData data) {
        Stats base = base(owner, pet, data);
        return base.copy().add(HeldItems.granted(pet.heldItem(), base, level(pet, data)));
    }

    /**
     * A pet's stats as they count for its owner (null for none known): its own (see {@link #base}), its held item's, and
     * what its abilities give it ("to your pet", see PetAbility#petStats).
     */
    public static Stats petStats(Player owner, OwnedPet pet, PetData data) {
        Stats base = base(owner, pet, data);
        Stats stats = base.copy().add(HeldItems.granted(pet.heldItem(), base, level(pet, data)));
        if (owner != null) PetAbilities.petStats(owner, pet, base, stats);
        return stats;
    }

    /** What the profile's pets give its stats, with no player to ask (see {@link #stats(Player, Document)}). */
    public static Stats stats(Document profile) {
        return stats(null, profile);
    }

    /** What the profile's pets give a player's stats: the summoned one's (see {@link #petStats}), and the Pet Score's Magic Find. */
    public static Stats stats(Player player, Document profile) {
        PetData data = data();
        Stats stats = new Stats();
        if (profile == null || data.count() == 0) return stats;
        OwnedPet active = active(profile);
        if (active != null) stats.add(petStats(player, active, data));
        int magicFind = magicFind(score(profile, data), data);
        if (magicFind > 0) stats.add(Stat.MAGIC_FIND, magicFind);
        return stats;
    }
}
