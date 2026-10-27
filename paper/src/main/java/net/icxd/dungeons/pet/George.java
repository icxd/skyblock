package net.icxd.dungeons.pet;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.npc.Lines;
import net.icxd.dungeons.npc.Npc;
import net.icxd.dungeons.npc.Voice;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.pet.PetData.Variant;
import net.icxd.dungeons.pet.PetText.Where;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.ItemStash;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * George the Pet Collector, at the Hub's Pet Care (npcs/hub.json), as recorded (2026-10-25, 04:04.3 to 04:08.5) and
 * as the wiki's George has him. A click opens Offer Pets ({@link OfferPetsMenu}), with a lever's click; the first
 * time, after his three lines (when they come, UNKNOWN: a second apart). He buys a pet for what the pets data says
 * he pays for its rarity (NEU's George prices, the wiki's), "&aYou sold your &7[Lvl 4] &aBee to &eGeorge &afor
 * &6250 Coins&a!" with a pling, to 50,000,000 coins a day (the wiki's; what he says past it, UNKNOWN). And the pets
 * he collects (+1 Taming Level Cap): each one given raises the profile's Taming cap by one ({@link Skills#cap}), to
 * +10, kept as the API keeps them ({@code pets_data.pet_care.pet_types_sacrificed}), and counts for SkyBlock
 * Leveling's Taming Cap Increases (+10 SkyBlock XP); he says the wiki's lines for it, and when it's the last, the
 * wiki's six more. How one is given, UNKNOWN (never recorded): with it in the slot, the cap's item says "&eClick to
 * donate your pet!", and a click asks first, as a sale does. What a sold or given pet held comes back to them
 * (UNKNOWN). Main thread.
 */
public final class George {
    public static final String ID = "GEORGE";
    static final String SAYS = "&e[NPC] George&f: &r";
    /** UNKNOWN: a villager's, as most of the Hub's speak. */
    static final Voice VOICE = Voice.VILLAGER;
    static final String FIELD = "george";
    /** The pet types given him, as the API has them. */
    static final String SACRIFICED = "sacrificed";
    static final String DAY = "day";
    static final String SOLD = "sold";
    /** "There is a limit of 50m that can be earned from selling pets to George per day." */
    static final long DAILY_LIMIT = 50_000_000;
    /** UNKNOWN: what he says. */
    static final String LIMIT_REACHED = "&cGeorge can't buy any more pets today!";
    static final String CONFIRM_SALE = "Confirm Sale";
    /** UNKNOWN: the sale's menu, for a pet given. */
    static final String CONFIRM_DONATION = "Confirm Donation";
    /** The wiki's. */
    static final List<String> FIRST_TALK = List.of("Hey there, I'm George the Pet Collector!",
            "I travel the world searching far and wide for the rarest pets I can find!",
            "If you have any pets which need a new home I'd be happy to buy them off you!");
    /** The wiki's, after the last one he wants. */
    static final List<String> ALL_GIVEN = List.of("You've done it!",
            "You've brought me all the pets I've asked for, and in doing so, have shown exceptional dedication and skill.",
            "Your Taming level cap can now reach the maximum level of 60, thanks to your efforts.",
            "You've not only helped me complete my collection but also demonstrated the true spirit of a master tamer.",
            "Remember, the world of taming is vast and ever-changing.",
            "May your adventures continue to be as fruitful and exciting as this quest has been!");
    static final long LINE_TICKS = 20;
    static final String INCREASED = "Your Taming level cap has been increased!";

    /** A pet he collects: its type, the least rarity he takes, and what he says for it (the wiki's). */
    record Wanted(String type, Rarity rarity, List<String> lines) {
        Wanted {
            lines = List.copyOf(lines);
        }
    }

    /** In the recorded order. */
    static final List<Wanted> WANTED = List.of(
            new Wanted("ENDERMAN", Rarity.MYTHIC, List.of("Thank you for bringing me the &dMythic Enderman&f.",
                    "Their enigmatic presence and otherworldly aura have always captivated me.")),
            new Wanted("BLACK_CAT", Rarity.LEGENDARY, List.of("Shhhh. Don't disturb the &6Legendary Black Cat&f.",
                    "I've always been fascinated by their mysterious charm.")),
            new Wanted("RIFT_FERRET", Rarity.EPIC, List.of("Ah, the Rift is a place of mystery, one I've longed to explore but alas, have "
                    + "never had the chance.", "Thank you for bringing me this pet!")),
            new Wanted("SLUG", Rarity.EPIC, List.of("Oh yes, an &5Epic Slug&f.", "My garden seems to miss having one around!")),
            new Wanted("SPIRIT", Rarity.EPIC, List.of("Is that a &5Epic Spirit&f?", "Their mysterious aura has always intrigued me.")),
            new Wanted("GIRAFFE", Rarity.EPIC, List.of("I've always wanted a &5Epic Giraffe&f?",
                    "I'm curious to see if they really are as tall as the legends say.")),
            new Wanted("JELLYFISH", Rarity.EPIC, List.of("Do you think the &5Epic Jellyfish&f can dance?",
                    "I've always been mesmerized by their underwater ballet.")),
            new Wanted("BAL", Rarity.EPIC, List.of("Thank you for getting the &5Epic Bal&f.", "It's a bit too hot for me down there!")),
            new Wanted("BABY_YETI", Rarity.EPIC, List.of("I'm glad you could fetch me a &5Epic Baby Yeti&f?", "I cannot stand the cold!")),
            new Wanted("FROST_WISP", Rarity.RARE, List.of("Now, I am be delighted to see a &9Rare Frost Wisp&f.",
                    "Their icy presence is said to be quite refreshing.")));

    private George() {
    }

    private static boolean enabled;

    /** Each pet type given raises the Taming cap by one. As the plugin starts (again does nothing). */
    public static void enable() {
        if (enabled) return;
        enabled = true;
        Skills.addCapRaise((profile, skill) -> skill == Skill.TAMING ? sacrificed(profile).size() : 0);
    }

    public static void click(Plugin plugin, Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || Lines.talking(player)) return;
        if (Npc.firstTalk(user.profile(), ID)) {
            Lines.say(plugin, player, FIRST_TALK.stream().map(line -> SAYS + line).toList(), ticks(FIRST_TALK.size()), VOICE,
                    () -> open(player));
            return;
        }
        open(player);
    }

    static void open(Player player) {
        new OfferPetsMenu(player, null).open(player, MenuSound.CLICK);
    }

    // Selling

    /** What he pays for it: his price for its rarity (0: he doesn't buy it). */
    static long price(OwnedPet pet, PetData data) {
        Pet kind = data.pet(pet.type());
        Variant variant = kind == null ? null : kind.variants().get(pet.tier());
        return variant == null ? 0 : variant.george();
    }

    /** Offer Pets' button with no pet, as recorded. */
    static final Icon OFFER_A_PET = new Icon(Material.RED_TERRACOTTA, "&eOffer a Pet", "&7Place a pet above and George will",
            "&7tell you what he's willing to pay for it!");
    /** UNKNOWN: what Hypixel's is. */
    static final Icon WONT_BUY = new Icon(Material.BARRIER, "&cAccept Offer", "&7George won't buy this pet!");

    /** Accept Offer, as recorded. */
    static Icon accept(long price) {
        return new Icon(Material.GREEN_TERRACOTTA, "&aAccept Offer", "&7George is willing to make an offer on", "&7your pet!", "", "&9Offer:",
                coins(price), "", "&cWARNING: This will permanently", "&cremove your pet.", "", "&eClick to accept offer!");
    }

    /** Confirm Sale's Confirm, as recorded: "&7Giving: &aBee", "&7Receiving: &6250 Coins". */
    static Icon confirmSale(OwnedPet pet, PetData data, long price) {
        return new Icon(Material.GREEN_TERRACOTTA, "&aConfirm", "&7Giving: " + PetText.colored(pet, data), "&7Receiving: " + coins(price));
    }

    /** "&aYou sold your &7[Lvl 4] &aBee to &eGeorge &afor &6250 Coins&a!", as recorded. */
    static String sold(OwnedPet pet, PetData data, long price) {
        return "&aYou sold your " + PetText.name(pet, data, Where.ITEM) + " to &eGeorge &afor " + coins(price) + "&a!";
    }

    /** Confirmed: the pet is his, and its coins theirs (the menu closes). */
    static void sell(Player player, PlacedPet placed) {
        User user = User.ifLoaded(player.getUniqueId());
        PetData data = Pets.data();
        long price = price(placed.pet(), data);
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player) || price <= 0) {
            player.closeInventory();
            return;
        }
        Document profile = user.profile();
        long now = System.currentTimeMillis();
        if (soldToday(profile, now) + price > DAILY_LIMIT) {
            player.closeInventory();
            player.sendMessage(Text.line(LIMIT_REACHED));
            return;
        }
        if (!placed.use()) {
            player.closeInventory();
            return;
        }
        refundHeldItem(player, placed.pet());
        Purse.add(user, price);
        recordSale(profile, price, now);
        user.save();
        player.closeInventory();
        player.sendMessage(Text.line(sold(placed.pet(), data, price)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 1, 0.75f);
    }

    /** Coins he's paid them today (UTC). */
    static long soldToday(Document profile, long now) {
        Document george = part(profile, false);
        return george != null && day(now).equals(george.getString(DAY)) && george.get(SOLD) instanceof Number n ? n.longValue() : 0;
    }

    static void recordSale(Document profile, long price, long now) {
        Document george = part(profile, true);
        long today = soldToday(profile, now) + price;
        george.put(DAY, day(now));
        george.put(SOLD, today);
    }

    private static String day(long now) {
        return LocalDate.ofInstant(Instant.ofEpochMilli(now), ZoneOffset.UTC).toString();
    }

    // The Taming cap

    /** The pet types given him, as the API keeps them ("ENDERMAN"). */
    public static List<String> sacrificed(Document profile) {
        Document george = part(profile, false);
        List<String> out = new ArrayList<>();
        if (george != null && george.get(SACRIFICED) instanceof List<?> list) {
            for (Object type : list) if (type instanceof String s && !out.contains(s.toUpperCase(Locale.ROOT))) out.add(s.toUpperCase(Locale.ROOT));
        }
        return out;
    }

    /** /yoinkprofile's: the API's {@code pet_types_sacrificed}, in place of theirs. */
    public static void yoinked(Document profile, List<String> types) {
        part(profile, true).put(SACRIFICED, new ArrayList<>(types.stream().map(t -> t.toUpperCase(Locale.ROOT)).distinct().toList()));
    }

    /** The one he still wants that this pet is (its type, at least its rarity); null for none. */
    static Wanted wanted(Document profile, OwnedPet pet) {
        List<String> given = sacrificed(profile);
        for (Wanted wanted : WANTED) {
            if (wanted.type().equals(pet.type()) && pet.tier().compareTo(wanted.rarity()) >= 0 && !given.contains(wanted.type())) return wanted;
        }
        return null;
    }

    /**
     * +1 Taming Level Cap, as recorded: "&7Your cap: &dTaming L", the pets he still wants ("  &dMythic Enderman", each
     * at its least rarity), "&8+&b10 SkyBlock XP". With one of them in the slot, "&eClick to donate your pet!" (UNKNOWN);
     * with none left, "  &aNone" (UNKNOWN).
     */
    static Icon cap(Document profile, PetData data, boolean donatable) {
        List<String> lore = new ArrayList<>(List.of("&7Raise your Taming Skill level cap by", "&7one, up to &f+10&7.", "",
                "&7Your cap: &dTaming " + Utils.getRomanNumeral(Skills.cap(profile, Skill.TAMING)), "", "&7Pets left to donate:"));
        List<String> given = sacrificed(profile);
        int left = 0;
        for (Wanted wanted : WANTED) {
            if (given.contains(wanted.type())) continue;
            lore.add("  " + wantedName(wanted, data));
            left++;
        }
        if (left == 0) lore.add("  &aNone");
        lore.addAll(List.of("", "&8+&b10 SkyBlock XP", "", "&7Donate any of the pets above to", "&7increase your cap by one."));
        if (donatable) lore.addAll(List.of("", "&eClick to donate your pet!"));
        return new Icon(Material.POLAR_BEAR_SPAWN_EGG, "&d+1 Taming Level Cap", lore);
    }

    /** "&dMythic Enderman". */
    static String wantedName(Wanted wanted, PetData data) {
        Pet kind = data.pet(wanted.type());
        String name = kind == null ? titled(wanted.type()) : kind.name(wanted.rarity());
        return "&" + wanted.rarity().getCode() + titled(wanted.rarity().name()) + " " + name;
    }

    /** UNKNOWN: Confirm Sale's, for the cap. */
    static Icon confirmDonation(OwnedPet pet, PetData data) {
        return new Icon(Material.GREEN_TERRACOTTA, "&aConfirm", "&7Giving: " + PetText.colored(pet, data), "&7Receiving: &d+1 Taming Level Cap");
    }

    /** Confirmed: the pet is his, their cap one higher, and he says so (the menu closes). */
    static void donate(Plugin plugin, Player player, PlacedPet placed) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) {
            player.closeInventory();
            return;
        }
        Document profile = user.profile();
        Wanted wanted = wanted(profile, placed.pet());
        if (wanted == null || !placed.use()) {
            player.closeInventory();
            return;
        }
        refundHeldItem(player, placed.pet());
        List<String> given = sacrificed(profile);
        given.add(wanted.type());
        part(profile, true).put(SACRIFICED, given);
        user.save();
        player.closeInventory();
        List<String> lines = new ArrayList<>();
        for (String line : wanted.lines()) lines.add(SAYS + line);
        lines.add(SAYS + INCREASED);
        if (given.containsAll(WANTED.stream().map(Wanted::type).toList())) for (String line : ALL_GIVEN) lines.add(SAYS + line);
        Lines.say(plugin, player, lines, ticks(lines.size()), VOICE, null);
        // Taming XP past the old cap counts now: its level, and what its levels give.
        PlayerSession.of(player).invalidateStats();
        SkyBlockLevels.changed(player);
    }

    /** What the pet held, back to them (UNKNOWN whether Hypixel's gives it back). */
    private static void refundHeldItem(Player player, OwnedPet pet) {
        SkyBlockItem held = pet.heldItem() == null ? null : ItemRegistry.get(pet.heldItem());
        if (held != null) ItemStash.give(player, ItemBuilder.build(held));
    }

    // Words

    /** "&6250 Coins". */
    static String coins(long coins) {
        return "&6" + Text.number(coins) + " Coins";
    }

    /** "Very Special" for VERY_SPECIAL. */
    static String titled(String name) {
        StringBuilder out = new StringBuilder();
        for (String word : name.toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** A second apart (UNKNOWN). */
    static long[] ticks(int lines) {
        long[] out = new long[lines];
        for (int i = 0; i < lines; i++) out[i] = i * LINE_TICKS;
        return out;
    }

    private static Document part(Document profile, boolean make) {
        Document pets = Pets.part(profile, make);
        Document george = pets == null ? null : pets.get(FIELD, Document.class);
        if (george == null && make) {
            george = new Document();
            pets.put(FIELD, george);
        }
        return george;
    }
}
