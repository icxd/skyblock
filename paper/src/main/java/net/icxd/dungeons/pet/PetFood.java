package net.icxd.dungeons.pet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.leveling.SkyBlockLevels;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.pet.PetData.Pet;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Pet Food, as the wiki's Pet Candies has it: right-click the summoned pet with it ("&eRight-click on your summoned pet
 * to feed it!", looking at its head as a Pet Item is given), and after a confirmation (Hypixel's since 2023, never
 * recorded: {@link FeedPetMenu}) it gains the Pet EXP the food's lines say ("&7Grants &a200,000 &ePet EXP&7.", "a boost
 * of &a10,000 Exp"). Carrot Candy counts: ten in a pet's lifetime ("&a(3/10) Pet Candy Used"); Pet Cake, Chyme and Fish
 * Food don't. Gabagool is for Wisps (the wiki's table: 100 to 3,276,800), and is the only food Taming and an all-skills
 * Pet Item boost. A pet at its most level eats nothing (UNKNOWN). What it says, all UNKNOWN. The Caducous Feeder isn't
 * here. Main thread.
 */
public final class PetFood {
    static final int MOST_CANDY = 10;
    /** The wiki's Pet Candies. */
    static final Map<String, Double> GABAGOOL = Map.of("CRUDE_GABAGOOL", 100.0, "FUEL_GABAGOOL", 3_200.0, "VERY_CRUDE_GABAGOOL", 19_200.0,
            "HEAVY_GABAGOOL", 102_400.0, "HYPERGOLIC_GABAGOOL", 3_276_800.0);
    private static final Pattern GRANTS = Pattern.compile("(?:Grants|a boost of) ([\\d,]+) (?:Pet EXP|Exp)");
    private static final String CANDY = "Carrot Candy gives pets boosts of Pet EXP";
    private static final Pattern COLOR = Pattern.compile("[&§][0-9a-fk-or]");
    static final String TOO_MUCH_CANDY = "&cThis pet can't eat any more candy!";
    static final String MAXED = "&cThis pet is already at its max level!";
    static final String WISPS_ONLY = "&cOnly Wisp pets eat Gabagool!";

    /** A food: what it grants, whether it's Carrot Candy, whether it's Gabagool. */
    record Food(String id, String name, double xp, boolean candy, boolean gabagool) {
    }

    private static final Map<String, Food> FOODS = new ConcurrentHashMap<>();
    private static final Food NONE = new Food(null, null, 0, false, false);

    private PetFood() {
    }

    /** The food this item is; null if it isn't one. */
    static Food of(ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        String id = tag == null ? null : tag.getString("id");
        if (id == null) return null;
        Food food = FOODS.computeIfAbsent(id, k -> {
            SkyBlockItem item = ItemRegistry.get(k);
            Food made = item == null ? null : of(k, PetItemUse.name(k), item.lore());
            return made == null ? NONE : made;
        });
        return food == NONE ? null : food;
    }

    /** What these lines say it grants; null if they don't (Gabagool by its id). */
    static Food of(String id, String name, List<String> lore) {
        Double gabagool = GABAGOOL.get(id);
        if (gabagool != null) return new Food(id, name, gabagool, false, true);
        String plain = COLOR.matcher(String.join(" ", lore)).replaceAll("").replaceAll("\\s+", " ");
        Matcher m = GRANTS.matcher(plain);
        if (!m.find()) return null;
        return new Food(id, name, Double.parseDouble(m.group(1).replace(",", "")), plain.contains(CANDY), false);
    }

    /** Why the pet won't eat it; null if it will. */
    static String refusal(OwnedPet pet, Food food, PetData data) {
        Pet kind = data.pet(pet.type());
        if (kind == null) return MAXED;
        if (food.gabagool() && !wisp(kind)) return WISPS_ONLY;
        if (food.candy() && pet.candyUsed() >= MOST_CANDY) return TOO_MUCH_CANDY;
        if (Pets.level(pet, data) >= data.maxLevel(kind)) return MAXED;
        return null;
    }

    /** The Wisps: "Gabagool Pet, feed to gain XP". */
    static boolean wisp(Pet kind) {
        return kind.kind() != null && kind.kind().startsWith("Gabagool Pet");
    }

    /** The XP it gives this pet: its own, Gabagool's with Taming and an all-skills item's boost. */
    static double xp(Food food, OwnedPet pet, int taming) {
        if (!food.gabagool()) return food.xp();
        return food.xp() * (1 + taming / 100.0) * (1 + HeldItems.expBoost(pet.heldItem(), null));
    }

    /** Right-clicked on their summoned pet with it: asks first, if it'll eat it. */
    static void offer(Player player, Food food) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        OwnedPet pet = Pets.active(user.profile());
        if (pet == null) return;
        String refusal = refusal(pet, food, Pets.data());
        if (refusal != null) {
            player.sendMessage(Text.line(refusal));
            return;
        }
        new FeedPetMenu(player, food).open(player);
    }

    /** Confirmed: one from their hand to their summoned pet. */
    static void feed(Player player, Food food) {
        player.closeInventory();
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || InventorySyncListener.frozen(player)) return;
        Document profile = user.profile();
        PetData data = Pets.data();
        OwnedPet pet = Pets.active(profile);
        if (pet == null || refusal(pet, food, data) != null || !takeOne(player, food)) return;
        double xp = xp(food, pet, Skills.level(profile, Skill.TAMING));
        int before = Pets.level(pet, data);
        OwnedPet after = pet.withExp(pet.exp() + xp).withCandyUsed(pet.candyUsed() + (food.candy() ? 1 : 0));
        Pets.replace(profile, after);
        player.sendMessage(Text.line("&aYour " + PetText.colored(after, data) + " &agained &e" + Text.number(Math.floor(xp)) + " Pet EXP&a!"));
        int level = Pets.level(after, data);
        for (int l = before + 1; l <= level; l++) player.sendMessage(Text.line(PetXp.levelUp(after, data, l)));
        if (level > before) PetXp.chime(player);
        PetFollowers.refresh(player);
        PlayerSession.of(player).invalidateStats();
        if (Pets.raiseBest(profile, data)) SkyBlockLevels.changed(player);
        user.save();
    }

    private static boolean takeOne(Player player, Food food) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        Food held = of(hand);
        if (held == null || !held.id().equals(food.id())) return false;
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);
        return true;
    }
}
