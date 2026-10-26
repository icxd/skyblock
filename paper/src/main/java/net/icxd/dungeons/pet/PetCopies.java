package net.icxd.dungeons.pet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enchanting.armor.EnchantNumbers;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.StatsMenu;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;

/**
 * What copies the summoned pet's stats onto its owner, as their texts say: Chimera on what they hold ("Copies 20% of
 * your active pet's stats.", its book's, to 100% at V), the Manticore Claw's Chimeric Core worn ("Copies 10% of your
 * active pet's Combat stats."), and the Daedalus Blades held ("Gains +4❁ Damage per Taming level and copies the base
 * Combat stats of your active pet."). They copy the pet's stats with its item's, not its abilities' ("It does not copy
 * stats granted by pet abilities, but it does copy stats added to the pet's base stats by Pet Items", the wiki's
 * Chimera; see {@link Pets#copyable}). And what doubles a pet's own stats while worn (the Bee Mask's "Double the base pet
 * stats of your active Bee pet."). The Daedalus Blades' Mythological Bestiary bonus waits on Diana's mobs. Main thread.
 */
public final class PetCopies {
    static final String CHIMERA = "chimera";
    /** The Stats menu's Combat stats, as what "Combat stats" are. */
    static final List<Stat> COMBAT = StatsMenu.Category.COMBAT.stats();
    private static final Pattern COMBAT_SHARE = Pattern.compile("Copies (\\d+(?:\\.\\d+)?)% of your active pet's Combat stats");
    private static final Pattern PER_TAMING = Pattern.compile("Gains \\+(\\d+(?:\\.\\d+)?)\\S* Damage per Taming level");
    private static final Pattern DOUBLES = Pattern.compile("Double the base pet stats of your active (.+?) pet");
    private static final String BASE_COMBAT = "copies the base Combat stats of your active pet";
    private static final Pattern COLOR = Pattern.compile("[&§][0-9a-fk-or]");

    /** What an item's text says it copies (none of it for what it doesn't say), read once. */
    record Copier(double combatShare, double perTaming, boolean baseCombat, String doubles) {
        static final Copier NONE = new Copier(0, 0, false, null);
    }

    private static final Map<String, Copier> COPIERS = new ConcurrentHashMap<>();

    private PetCopies() {
    }

    /** Registers the stats, once, as the plugin starts. */
    public static void enable() {
        PlayerStats.addModifier(PetCopies::stats);
    }

    static void stats(Player player, Stats stats) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return;
        PetData data = Pets.data();
        OwnedPet pet = Pets.active(user.profile());
        if (pet == null || data.pet(pet.type()) == null) return;
        Stats copyable = Pets.copyable(player, pet, data);
        Integer chimera = Combat.heldEnchantments(player).get(CHIMERA);
        if (chimera != null && chimera > 0) share(stats, copyable, null, EnchantNumbers.get(CHIMERA, chimera, 0));
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) {
            Copier copier = copier(piece.item());
            if (copier.combatShare() > 0) share(stats, copyable, COMBAT, copier.combatShare());
        }
        NBTTagCompound tag = ItemNBT.read(player.getInventory().getItemInMainHand());
        Copier held = tag == null ? Copier.NONE : copier(ItemRegistry.get(tag.getString("id")));
        if (held.perTaming() > 0) stats.add(Stat.DAMAGE, held.perTaming() * Skills.level(user.profile(), Skill.TAMING));
        if (held.baseCombat()) share(stats, copyable, COMBAT, 100);
    }

    /** Whether something they wear doubles this pet's own stats (the Bee Mask on a Bee). */
    static boolean doubled(Player player, String petName) {
        for (Worn.Piece piece : SetBonuses.worn(player).pieces()) {
            String doubles = copier(piece.item()).doubles();
            if (doubles != null && doubles.equalsIgnoreCase(petName)) return true;
        }
        return false;
    }

    /** {@code percent} of these stats ({@code which} of them; null: all) added to {@code to}. */
    static void share(Stats to, Stats from, List<Stat> which, double percent) {
        for (Stat stat : which == null ? List.of(Stat.values()) : which) if (from.has(stat)) to.add(stat, from.get(stat) * percent / 100);
    }

    static Copier copier(SkyBlockItem item) {
        if (item == null) return Copier.NONE;
        return COPIERS.computeIfAbsent(item.id(), id -> {
            StringBuilder text = new StringBuilder(String.join(" ", item.lore()));
            for (ItemBlock block : item.blocks()) text.append(' ').append(String.join(" ", block.text()));
            return copier(text.toString());
        });
    }

    /** What these lines say it copies. */
    static Copier copier(String text) {
        String plain = COLOR.matcher(text).replaceAll("").replaceAll("\\s+", " ");
        Matcher share = COMBAT_SHARE.matcher(plain);
        Matcher perTaming = PER_TAMING.matcher(plain);
        Matcher doubles = DOUBLES.matcher(plain);
        return new Copier(share.find() ? Double.parseDouble(share.group(1)) : 0, perTaming.find() ? Double.parseDouble(perTaming.group(1)) : 0,
                plain.contains(BASE_COMBAT), doubles.find() ? doubles.group(1) : null);
    }
}
