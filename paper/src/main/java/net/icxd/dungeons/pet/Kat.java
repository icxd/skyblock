package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.requirement.skill.SkillRequirement;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.npc.Abiphone;
import net.icxd.dungeons.npc.Lines;
import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.pet.PetData.KatUpgrade;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetData.Variant;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Kat the Pet Sitter, at the Hub's Pet Care (npcs/hub.json), as recorded (2026-10-25, 00:51.0 to 02:25.1) and as the
 * wiki's Kat has her. A click opens the Pet Sitter ({@link PetSitterMenu}), with a lever's click; the first time, after
 * her three lines (when they come, UNKNOWN: a second apart). Hired, she takes the pet and its cost (the pets data's
 * Kat/Costs: its time, its items, and its coins less 0.3% a level of the pet's), says she'll upgrade it, and when its
 * time is up she rings ({@link Abiphone}); then talking to her gives it back a rarity up, its XP as it was (so its
 * level is the new rarity's). One pet at a time; talked to before it's ready, she says when it will be. Answering
 * her call is talking to her, on the phone (UNKNOWN: never recorded; her Abiphone lines have "&b✆ &f" after her
 * name). What she won't take: a rarity the data says she doesn't upgrade (the wiki's Bingo, Grandma Wolf, Kuudra,
 * Montezuma and Wisps; the top rarity; a pet with no next rarity or no known cost); a pet holding a Tier Boost (the
 * wiki's 0.19.3); without the Taming level the new rarity takes (1, 5, 10, 20, 25 for Uncommon to Mythic). What the
 * Pet Sitter shows then, UNKNOWN (see PetSitterMenu). Its time doesn't drop with the pet's level (UNKNOWN whether
 * Hypixel's does). Kat Flowers and Bouquets aren't here. Main thread.
 */
public final class Kat {
    public static final String ID = "KAT";
    static final String NAME = "&bKat";
    static final String SAYS = "&e[NPC] " + NAME + "&f: &r";
    static final String ON_PHONE = "&e[NPC] " + NAME + "&f: &b✆ &f";
    static final Voice VOICE = Voice.villager(2);
    static final String FIELD = "kat";
    static final String PET = "pet";
    static final String TO = "to";
    static final String READY = "ready";
    static final String RANG = "rang";
    /** The Taming level each rarity takes (the wiki's Kat, Pet Sitting). */
    static final Map<Rarity, Integer> TAMING = Map.of(Rarity.UNCOMMON, 1, Rarity.RARE, 5, Rarity.EPIC, 10, Rarity.LEGENDARY, 20,
            Rarity.MYTHIC, 25);
    /** "Each level removes 0.3% of the upgrade cost." */
    static final double DISCOUNT = 0.003;
    /** The wiki's. */
    static final List<String> FIRST_TALK = List.of("Hey there, I'm Kat the Pet Sitter!",
            "While you're busy adventuring I can take care of your pets.", "I might even be able to give them new abilities!");
    static final long[] FIRST_TALK_TICKS = {0, 20, 40};
    /** 01:02.7 and 01:06.2. */
    static final long[] HIRED_TICKS = {0, 70};
    /** UNKNOWN: a second apart. */
    static final long[] CARING_TICKS = {0, 20, 40};
    /** 02:20.4, 02:21.7 and 02:25.1. */
    static final long[] PICKED_UP_TICKS = {0, 26, 94};
    static final long RING_CHECK_TICKS = 20;
    static final String NOT_ENOUGH_ITEMS = "&cYou don't have the required items!";
    /** UNKNOWN: what she says. */
    static final String NOT_ENOUGH_COINS = "&cYou don't have enough coins!";
    static final String TIER_BOOSTED = "&cKat won't upgrade a pet holding a Tier Boost!";

    private Kat() {
    }

    /** Rings anyone whose pet is ready, once, wherever they are. */
    public static void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> ringAll(plugin), RING_CHECK_TICKS, RING_CHECK_TICKS);
    }

    public static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || Lines.talking(player)) return;
        Document profile = user.profile();
        Sitting sitting = sitting(profile);
        if (sitting != null) {
            long left = sitting.ready() - System.currentTimeMillis();
            if (left > 0) Lines.say(plugin, player, caring(sitting, Pets.data(), left), CARING_TICKS, VOICE, null);
            else pickUp(plugin, player, false);
            return;
        }
        if (Npc.firstTalk(profile, ID)) {
            Lines.say(plugin, player, FIRST_TALK.stream().map(line -> SAYS + line).toList(), FIRST_TALK_TICKS, VOICE, () -> open(player));
            return;
        }
        open(player);
    }

    static void open(Player player) {
        new PetSitterMenu(player, null).open(player, MenuSound.CLICK);
    }

    // What she'd do

    /**
     * Kat's upgrade of a pet: to which rarity, how long it takes, what it costs (its coins with the pet's level's
     * discount) and the Taming level it takes.
     */
    record Offer(OwnedPet pet, Rarity to, long seconds, long coins, List<PetData.ItemCost> items, int taming) {
        Offer {
            items = List.copyOf(items);
        }
    }

    /** What she'd do with this pet; null if she doesn't upgrade it (at this rarity, or at all). */
    static Offer offer(OwnedPet pet, PetData data) {
        Pet kind = data.pet(pet.type());
        if (kind == null) return null;
        Variant variant = kind.variants().get(pet.tier());
        Rarity to = pet.tier().upgrade();
        if (variant == null || !variant.kat() || variant.katUpgrade() == null || to == pet.tier() || !kind.variants().containsKey(to)) return null;
        KatUpgrade upgrade = variant.katUpgrade();
        return new Offer(pet, to, upgrade.seconds(), cost(upgrade.coins(), data.level(kind, pet.tier(), pet.exp())), upgrade.items(),
                TAMING.getOrDefault(to, 0));
    }

    /** Its coins less 0.3% a level (the wiki's Kat: "base price x (1 - 0.003 x pet level)"; rounded, UNKNOWN how). */
    static long cost(long coins, int level) {
        return Math.round(coins * Math.max(0, 1 - DISCOUNT * level));
    }

    /** Why she won't take it as they are (the line Hire Kat ends with); null if she will. */
    static String refusal(Player player, User user, Offer offer) {
        if (HeldItems.effect(offer.pet().heldItem()).tiers() > 0) return TIER_BOOSTED;
        if (Skills.level(user.profile(), Skill.TAMING) < offer.taming()) return new SkillRequirement(Skill.TAMING, offer.taming()).lore().getFirst();
        for (PetData.ItemCost item : offer.items()) if (!new ItemCost(item.id(), item.amount()).canPay(player, user)) return NOT_ENOUGH_ITEMS;
        if (offer.coins() > 0 && !new CoinCost((int) Math.min(Integer.MAX_VALUE, offer.coins())).canPay(player, user)) return NOT_ENOUGH_COINS;
        return null;
    }

    // Her menus' items

    /** The Pet Sitter's button with no pet, as recorded. */
    static final Icon PET_SITTER = new Icon(Material.RED_TERRACOTTA, "&ePet Sitter", "&7Place a pet above for Kat to take", "&7care of!", "",
            "&7After some time, your pet &9Rarity &7will", "&7be upgraded!");
    /** UNKNOWN: what Hypixel's is (a barrier, NEU's Pet Sitter overlay says, for a pet with no next rarity). */
    static final Icon CANT_UPGRADE = new Icon(Material.BARRIER, "&cHire Kat", "&7Kat can't upgrade this pet!");

    /**
     * Hire Kat, as recorded: "&7Kat will take care of your &fBee &7for &91" / "&9minute&7 then its &9rarity&7 will be" /
     * "&7upgraded!", its cost, and "&eClick to hire Kat!" in green, or why not in red ("&cYou don't have the required
     * items!").
     */
    static Icon hire(Offer offer, PetData data, String refusal) {
        List<String> lore = new ArrayList<>(Text.wrap("&7Kat will take care of your " + PetText.colored(offer.pet(), data) + " &7for &9"
                + duration(offer.seconds()) + "&7 then its &9rarity&7 will be upgraded!", PetText.WIDTH));
        lore.add("");
        lore.add("&7Cost");
        lore.addAll(costLines(offer));
        lore.add("");
        lore.add(refusal == null ? "&eClick to hire Kat!" : refusal);
        return new Icon(refusal == null ? Material.GREEN_TERRACOTTA : Material.RED_TERRACOTTA, "&aHire Kat", lore);
    }

    /** "&6250,000 Coins", then each item ("&aSmall Honey Dipper", "&9Enchanted Mycelium &8x64"); UNKNOWN which comes first. */
    static List<String> costLines(Offer offer) {
        List<String> out = new ArrayList<>();
        if (offer.coins() > 0) out.add("&6" + Text.number(offer.coins()) + " Coins");
        for (PetData.ItemCost item : offer.items()) out.add(PetItemUse.name(item.id()) + (item.amount() > 1 ? " &8x" + item.amount() : ""));
        return out;
    }

    /** Confirm Hiring Kat's Confirm, as recorded. */
    static Icon confirm(Offer offer) {
        return new Icon(Material.GREEN_TERRACOTTA, "&aConfirm", Text.wrap("&cWARNING: You will not be able to retrieve your pet for "
                + duration(offer.seconds()) + " and its level will change as a result of the rarity upgrade.", PetText.WIDTH));
    }

    // Hiring her

    /** Confirmed: the pet and its cost are hers, and she says so (the menu closes). */
    static void hire(Plugin plugin, Player player, PlacedPet placed) {
        User user = User.ifLoaded(player.getUniqueId());
        PetData data = Pets.data();
        Offer offer = offer(placed.pet(), data);
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player) || offer == null || sitting(user.profile()) != null) {
            player.closeInventory();
            return;
        }
        String refusal = refusal(player, user, offer);
        if (refusal != null) {
            player.closeInventory();
            player.sendMessage(Text.line(refusal));
            return;
        }
        if (!placed.use()) {
            player.closeInventory();
            return;
        }
        for (PetData.ItemCost item : offer.items()) new ItemCost(item.id(), item.amount()).pay(player, user);
        if (offer.coins() > 0) new CoinCost((int) Math.min(Integer.MAX_VALUE, offer.coins())).pay(player, user);
        sit(user.profile(), new Sitting(offer.pet(), offer.to(), System.currentTimeMillis() + offer.seconds() * 1000, false));
        user.save();
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 1, 0.75f);
        Lines.say(plugin, player, hired(offer, data), HIRED_TICKS, VOICE, null);
    }

    /** "I'll get your &fBee &fupgraded to &a&lUNCOMMON &fin no time!" and "Come back in &e1 minute &fto pick it up!", as recorded. */
    static List<String> hired(Offer offer, PetData data) {
        return List.of(SAYS + "I'll get your " + PetText.colored(offer.pet(), data) + " &fupgraded to " + display(offer.to()) + " &fin no time!",
                SAYS + "Come back in &e" + duration(offer.seconds()) + " &fto pick it up!");
    }

    /** The wiki's, for a pet that isn't ready yet. */
    static List<String> caring(Sitting sitting, PetData data, long millisLeft) {
        return List.of(SAYS + "I'm currently taking care of your " + PetText.colored(sitting.pet(), data) + "&f!",
                SAYS + "You can pick it up in &e" + duration((millisLeft + 999) / 1000) + "&f.", SAYS + "Stop by once it's done, okay?");
    }

    // Picking it up

    /** It's ready: back to them a rarity up, as she says so (on the phone, answering her call). */
    static void pickUp(Plugin plugin, Player player, boolean phone) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        Sitting sitting = sitting(user.profile());
        if (sitting == null || sitting.ready() > System.currentTimeMillis()) return;
        clear(user.profile());
        ItemStash.give(player, PetItems.item(sitting.pet().withTier(sitting.to())));
        user.save();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 1);
        Lines.say(plugin, player, pickedUp(sitting, Pets.data(), phone), PICKED_UP_TICKS, VOICE, null);
    }

    /**
     * As recorded: "Oh, hey!", "I was able to upgrade your pet &fBee&f to &a&lUNCOMMON&f.", "If you have any other pets
     * you'd like to upgrade, you know where to find me!" (the pet's name in its old rarity's colour, UNKNOWN: the
     * recording's was Common, white either way).
     */
    static List<String> pickedUp(Sitting sitting, PetData data, boolean phone) {
        String says = phone ? ON_PHONE : SAYS;
        return List.of(says + "Oh, hey!", says + "I was able to upgrade your pet " + PetText.colored(sitting.pet(), data) + "&f to "
                + display(sitting.to()) + "&f.", says + "If you have any other pets you'd like to upgrade, you know where to find me!");
    }

    static void ringAll(Plugin plugin) {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = User.ifLoaded(player.getUniqueId());
            if (user == null || user.isReleased()) continue;
            Sitting sitting = sitting(user.profile());
            if (sitting == null || sitting.rang() || sitting.ready() > now) continue;
            sit(user.profile(), new Sitting(sitting.pet(), sitting.to(), sitting.ready(), true));
            Abiphone.ring(plugin, player, NAME, () -> pickUp(plugin, player, true));
        }
    }

    // The pet she has, kept with the profile

    /** The pet she has, the rarity it's getting, when it's ready (epoch millis) and whether she's rung about it. */
    record Sitting(OwnedPet pet, Rarity to, long ready, boolean rang) {
    }

    static Sitting sitting(Document profile) {
        Document pets = Pets.part(profile, false);
        Document kat = pets == null ? null : pets.get(FIELD, Document.class);
        if (kat == null) return null;
        OwnedPet pet = OwnedPet.from(kat.get(PET, Document.class));
        Rarity to = PetData.rarity(kat.getString(TO));
        if (pet == null || to == null) return null;
        return new Sitting(pet, to, kat.get(READY) instanceof Number n ? n.longValue() : 0, Boolean.TRUE.equals(kat.get(RANG)));
    }

    static void sit(Document profile, Sitting sitting) {
        Document kat = new Document(PET, sitting.pet().document()).append(TO, sitting.to().name()).append(READY, sitting.ready());
        if (sitting.rang()) kat.append(RANG, true);
        Pets.part(profile, true).put(FIELD, kat);
    }

    static void clear(Document profile) {
        Document pets = Pets.part(profile, false);
        if (pets != null) pets.remove(FIELD);
    }

    // Words

    /** "&a&lUNCOMMON". */
    static String display(Rarity rarity) {
        return "&" + rarity.getCode() + "&l" + rarity.name().replace('_', ' ');
    }

    /**
     * "1 minute", "2 days": the largest whole unit and, if there's more, the next ("1 day 4 hours"). The data's are all
     * one unit; UNKNOWN how Hypixel's says the rest of one.
     */
    static String duration(long seconds) {
        long[] sizes = {86_400, 3_600, 60, 1};
        String[] units = {"day", "hour", "minute", "second"};
        long left = Math.max(0, seconds);
        for (int i = 0; i < sizes.length; i++) {
            if (left < sizes[i] && i < sizes.length - 1) continue;
            String out = count(left / sizes[i], units[i]);
            long rest = i + 1 < sizes.length ? left % sizes[i] / sizes[i + 1] : 0;
            return rest > 0 ? out + " " + count(rest, units[i + 1]) : out;
        }
        return count(0, "second");
    }

    private static String count(long n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }
}
