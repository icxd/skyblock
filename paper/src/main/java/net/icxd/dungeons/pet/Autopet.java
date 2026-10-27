package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.SkyBlockServer;
import net.icxd.dungeons.command.Callbacks;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.pet.PetText.Where;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Autopet, as the wiki's Autopet has it: rules that equip a pet when something happens, unless one of up to five
 * exceptions holds, the first that matches (in order) equipping, two ticks at least between two. Rule slots come two
 * at a time (the wiki's Autopet Rules 2-Pack, 21,000 Bits; here bought in the Autopet menu, UNKNOWN), to 28. What it
 * says, as SkyHanni's tests have Hypixel's: "&cAutopet &eequipped your &7[Lvl 100] &6Mosquito&e! &a&lVIEW RULE", VIEW
 * RULE opening the rule (its hover: "Equip: [Lvl 100] Mosquito Held Item: Quick Claw", SkyHanni's). Triggers here
 * are those the plugin has: on login (each join, UNKNOWN), entering an island (each server's), gaining a skill's XP,
 * entering combat (hitting or being hurt after 10 seconds of neither, UNKNOWN), killing a mob of a vanilla type,
 * choosing a dungeon class (or a run starting with it), entering a Catacombs floor, and throwing a fishing hook.
 * Exceptions: in an island, not in one, already having a pet equipped. Its menus are UNKNOWN (never recorded): see
 * {@link AutopetMenu}. Kept with the profile's pets. Main thread.
 */
public final class Autopet {
    static final String FIELD = "autopet";
    static final String SLOTS = "slots";
    static final String RULES = "rules";
    static final int PACK = 2;
    static final int MOST = 28;
    static final int PACK_BITS = 21_000;
    static final int MOST_EXCEPTIONS = 5;
    static final long COOLDOWN_MILLIS = 100;
    static final long COMBAT_MILLIS = 10_000;

    /** What a rule's trigger or exception names: skills, islands, mobs, classes, floors, pet types. */
    enum Arg {
        SKILL, ISLAND, MOB, CLASS, FLOOR, PET;

        /** The choices, as stored. */
        List<String> choices(PetData data) {
            return switch (this) {
                case SKILL -> Arrays.stream(Skill.values()).filter(s -> !s.cosmetic()).map(Enum::name).toList();
                case ISLAND -> Arrays.stream(ServerType.values()).filter(t -> t != ServerType.NONE).map(Enum::name).toList();
                case MOB -> MOBS.stream().map(Enum::name).toList();
                case CLASS -> Arrays.stream(DungeonClass.values()).map(Enum::name).toList();
                case FLOOR -> List.of("1", "2", "3", "4", "5", "6", "7");
                case PET -> data.pets().stream().map(PetData.Pet::id).sorted().toList();
            };
        }

        /** How a choice reads ("Farming", "Dwarven Mines", "Iron Golem", "Floor VII"). */
        String shown(String choice, PetData data) {
            if (choice == null) return "?";
            return switch (this) {
                case SKILL -> {
                    try {
                        yield Skill.valueOf(choice).getName();
                    } catch (IllegalArgumentException e) {
                        yield choice;
                    }
                }
                case ISLAND -> {
                    try {
                        yield ServerType.valueOf(choice).getDisplayName();
                    } catch (IllegalArgumentException e) {
                        yield choice;
                    }
                }
                case CLASS -> {
                    DungeonClass c = DungeonClass.parse(choice);
                    yield c == null ? choice : c.getDisplayName();
                }
                case FLOOR -> "Floor " + net.icxd.dungeons.utils.Utils.getRomanNumeral(Integer.parseInt(choice));
                case PET -> {
                    PetData.Pet pet = data.pet(choice);
                    yield pet == null ? choice : pet.name();
                }
                case MOB -> George.titled(choice);
            };
        }
    }

    /** The vanilla mobs "On kill a [Mob]" offers here (UNKNOWN: Hypixel's list). */
    static final List<EntityType> MOBS = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER,
            EntityType.ENDERMAN, EntityType.CREEPER, EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.BLAZE, EntityType.WITHER_SKELETON,
            EntityType.WOLF, EntityType.IRON_GOLEM, EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.GHAST, EntityType.ZOMBIFIED_PIGLIN,
            EntityType.WITCH, EntityType.GUARDIAN, EntityType.SQUID, EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN,
            EntityType.RABBIT, EntityType.BAT, EntityType.WITHER, EntityType.ENDER_DRAGON);

    /** A rule's "when", as the wiki words it. */
    enum Trigger {
        LOGIN("On login", null),
        ENTER_ISLAND("On enter %s", Arg.ISLAND),
        SKILL_XP("On gain %s XP", Arg.SKILL),
        ENTER_COMBAT("On enter combat", null),
        KILL_MOB("On kill a %s", Arg.MOB),
        SELECT_CLASS("On select dungeon %s", Arg.CLASS),
        ENTER_FLOOR("When you enter Catacombs %s", Arg.FLOOR),
        FISHING_HOOK("On throw fishing hook", null);

        final String text;
        final Arg arg;

        Trigger(String text, Arg arg) {
            this.text = text;
            this.arg = arg;
        }
    }

    /** A rule's "except if", as the wiki words it. */
    enum Unless {
        IN_ISLAND("Except if in %s", Arg.ISLAND),
        NOT_IN_ISLAND("Except if NOT in %s", Arg.ISLAND),
        HAS_PET("Except if already has %s equipped", Arg.PET);

        final String text;
        final Arg arg;

        Unless(String text, Arg arg) {
            this.text = text;
            this.arg = arg;
        }
    }

    record Except(Unless type, String arg) {
        String text(PetData data) {
            return type.text.formatted(type.arg.shown(arg, data));
        }
    }

    /** A rule: the pet it equips (its uuid), when, with what (null for none), and its exceptions. */
    record Rule(String pet, Trigger trigger, String arg, List<Except> exceptions) {
        Rule {
            exceptions = List.copyOf(exceptions);
        }

        String when(PetData data) {
            return trigger.arg == null ? trigger.text : trigger.text.formatted(trigger.arg.shown(arg, data));
        }
    }

    private static final Map<UUID, Long> LAST_SWAP = new HashMap<>();
    private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();

    private Autopet() {
    }

    /** Hears what triggers rules, beyond its listener's events (once, as the plugin starts). */
    public static void enable() {
        SkillGains.addGainListener((player, skill, base) -> fire(player, Trigger.SKILL_XP, skill.name()));
        DungeonProfile.addClassListener((user, dungeonClass) -> {
            Player player = user.getPlayer();
            if (player != null) fire(player, Trigger.SELECT_CLASS, dungeonClass.name());
        });
        DungeonRun.addStartListener((player, floor) -> {
            fire(player, Trigger.ENTER_FLOOR, String.valueOf(floor));
            User user = User.ifLoaded(player.getUniqueId());
            DungeonClass selected = user == null ? null : DungeonProfile.selectedClass(user);
            if (selected != null) fire(player, Trigger.SELECT_CLASS, selected.name());
        });
    }

    // Firing

    /** Something that triggers rules happened to them: the first rule it matches, and no exception stops, equips its pet. */
    static void fire(Player player, Trigger trigger, String arg) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        Document profile = user.profile();
        List<Rule> rules = rules(profile);
        if (rules.isEmpty()) return;
        OwnedPet active = Pets.active(profile);
        ServerType here = here();
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            if (rule.trigger() != trigger || (trigger.arg != null && !matches(rule.arg(), arg))) continue;
            if (stopped(rule, here, active)) continue;
            OwnedPet pet = Pets.find(profile, rule.pet());
            // A rule whose pet has left the menu does nothing; the next may.
            if (pet == null) continue;
            if (active == null || !active.uuid().equals(pet.uuid())) equip(player, user, pet, i);
            return;
        }
    }

    private static boolean matches(String wanted, String got) {
        return wanted != null && wanted.equalsIgnoreCase(got);
    }

    /** Whether an exception stops the rule: any of them (two "NOT in"s, as the wiki says, then never let it). */
    static boolean stopped(Rule rule, ServerType here, OwnedPet active) {
        for (Except e : rule.exceptions()) {
            boolean in = here != null && here.name().equalsIgnoreCase(e.arg());
            switch (e.type()) {
                case IN_ISLAND -> {
                    if (in) return true;
                }
                case NOT_IN_ISLAND -> {
                    if (!in) return true;
                }
                case HAS_PET -> {
                    if (active != null && active.type().equalsIgnoreCase(e.arg())) return true;
                }
            }
        }
        return false;
    }

    /** The pet out, as summoning it from the menu does, and the line saying so. */
    private static void equip(Player player, User user, OwnedPet pet, int rule) {
        long now = System.currentTimeMillis();
        Long last = LAST_SWAP.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MILLIS) return;
        LAST_SWAP.put(player.getUniqueId(), now);
        Pets.activate(user.profile(), pet.uuid());
        PetFollowers.refresh(player);
        PlayerSession.of(player).invalidateStats();
        user.save();
        PetData data = Pets.data();
        UUID view = Callbacks.add(player, () -> AutopetRuleMenu.edit(player, rule));
        Component line = Text.line("&cAutopet &eequipped your " + PetText.name(pet, data, Where.ITEM) + "&e! ")
                .append(Text.line("&a&lVIEW RULE").clickEvent(ClickEvent.runCommand("/cb " + view))
                        .hoverEvent(HoverEvent.showText(Text.line(hover(pet, data)))));
        player.sendMessage(line);
    }

    /** "Equip: [Lvl 100] Mosquito Held Item: Quick Claw" (SkyHanni's test line), coloured as the line's are (UNKNOWN). */
    static String hover(OwnedPet pet, PetData data) {
        SkyBlockItem held = pet.heldItem() == null ? null : ItemRegistry.get(pet.heldItem());
        return "&7Equip: " + PetText.name(pet, data, Where.ITEM) + (held == null ? "" : "\n&7Held Item: " + PetItemUse.name(pet.heldItem()));
    }

    /** They hit something or were hurt: "On enter combat", if they weren't in combat for 10 seconds. */
    static void combat(Player player) {
        long now = System.currentTimeMillis();
        Long last = LAST_COMBAT.put(player.getUniqueId(), now);
        if (last == null || now - last >= COMBAT_MILLIS) fire(player, Trigger.ENTER_COMBAT, null);
    }

    static void forget(UUID player) {
        LAST_SWAP.remove(player);
        LAST_COMBAT.remove(player);
    }

    static ServerType here() {
        SkyBlockServer server = Dungeons.getSkyBlockServer();
        return server == null ? null : server.getServerType();
    }

    /** Fired a moment after joining: "On login", then "On enter" this island. */
    static void joined(Player player) {
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (!player.isOnline()) return;
            fire(player, Trigger.LOGIN, null);
            ServerType here = here();
            if (here != null) fire(player, Trigger.ENTER_ISLAND, here.name());
        }, 20);
    }

    // Kept with the profile

    /** How many rules they may have (two a pack). */
    static int slots(Document profile) {
        Document autopet = part(profile, false);
        return autopet != null && autopet.get(SLOTS) instanceof Number n ? Math.min(MOST, n.intValue()) : 0;
    }

    static void addSlots(Document profile) {
        part(profile, true).put(SLOTS, Math.min(MOST, slots(profile) + PACK));
    }

    static List<Rule> rules(Document profile) {
        Document autopet = part(profile, false);
        List<Rule> out = new ArrayList<>();
        if (autopet == null || !(autopet.get(RULES) instanceof List<?> list)) return out;
        for (Object o : list) {
            if (!(o instanceof Document d)) continue;
            Trigger trigger = parse(Trigger.class, d.getString("when"));
            if (trigger == null || d.getString("pet") == null) continue;
            List<Except> exceptions = new ArrayList<>();
            if (d.get("unless") instanceof List<?> unless) {
                for (Object u : unless) {
                    if (!(u instanceof Document e)) continue;
                    Unless type = parse(Unless.class, e.getString("type"));
                    if (type != null) exceptions.add(new Except(type, e.getString("arg")));
                }
            }
            out.add(new Rule(d.getString("pet"), trigger, d.getString("arg"), exceptions));
        }
        return out;
    }

    static void put(Document profile, List<Rule> rules) {
        List<Document> out = new ArrayList<>();
        for (Rule rule : rules) {
            Document d = new Document("pet", rule.pet()).append("when", rule.trigger().name());
            if (rule.arg() != null) d.append("arg", rule.arg());
            List<Document> unless = new ArrayList<>();
            for (Except e : rule.exceptions()) unless.add(new Document("type", e.type().name()).append("arg", e.arg()));
            if (!unless.isEmpty()) d.append("unless", unless);
            out.add(d);
        }
        part(profile, true).put(RULES, out);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name) {
        if (name == null) return null;
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Document part(Document profile, boolean make) {
        Document pets = Pets.part(profile, make);
        Document autopet = pets == null ? null : pets.get(FIELD, Document.class);
        if (autopet == null && make) {
            autopet = new Document();
            pets.put(FIELD, autopet);
        }
        return autopet;
    }
}
