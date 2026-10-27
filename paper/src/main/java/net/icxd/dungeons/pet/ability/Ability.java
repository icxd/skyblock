package net.icxd.dungeons.pet.ability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.island.Islands;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.bonus.SetBonuses;
import net.icxd.dungeons.item.bonus.Worn;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.StatsMenu;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.pet.OwnedPet;
import net.icxd.dungeons.pet.PetData;
import net.icxd.dungeons.pet.Pets;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.SkyBlockTime;

/**
 * A pet's ability by its pet, its block's name and its text (see {@link PetAbility}), with what the abilities ask about
 * their owner and the world: where they are, what they hold and wear, which mobs are which.
 */
abstract class Ability implements PetAbility {
    /** The Stats menu's Combat Stats ("Increases all Combat stats by 10%"). */
    static final List<Stat> COMBAT = StatsMenu.Category.COMBAT.stats();
    /** Its Miscellaneous Stats. */
    static final List<Stat> MISC = StatsMenu.Category.MISC.stats();
    /** Its Fishing Stats. */
    static final List<Stat> FISHING = StatsMenu.Category.FISHING.stats();

    private final String pet;
    private final String name;
    private final String text;

    Ability(String pet, String name, String text) {
        this.pet = pet;
        this.name = name;
        this.text = text;
    }

    @Override
    public String pet() {
        return pet;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String text() {
        return text;
    }

    /** A source name for what this ability gives someone (a buff, a debuff): its pet's and its own, and theirs. */
    String source(Player player) {
        return "pet:" + pet + ":" + name + ":" + player.getUniqueId();
    }

    // ---------- where ----------

    private static ServerType server() {
        return Dungeons.getSkyBlockServer() == null ? ServerType.NONE : Dungeons.getSkyBlockServer().getServerType();
    }

    /** On the Crimson Isle. */
    static boolean onCrimsonIsle() {
        return server() == ServerType.CRIMSON_ISLE;
    }

    /** On a Mining Island: the Dwarven Mines (the Gold Mine and the Deep Caverns aren't islands of their own here). */
    static boolean onMiningIsland() {
        return server() == ServerType.DWARVEN_MINES;
    }

    /** In a dungeon run. */
    static boolean inDungeon(Player player) {
        return RunManager.inRun(player);
    }

    /** On their own Private Island. */
    static boolean onOwnIsland(Player player) {
        return Islands.isOwnIsland(player, player.getWorld());
    }

    /** On a public island: anywhere but a Private Island (the Hub, the Dungeon Hub, the Crimson Isle, the Mines, a dungeon). */
    static boolean onPublicIsland() {
        return server() != ServerType.ISLAND;
    }

    /** SkyBlock's night: from 7pm to 6am. */
    static boolean night() {
        return !SkyBlockTime.now().isDay();
    }

    // ---------- what they hold and wear ----------

    /** The SkyBlock id of what they hold (upper case); null for nothing, or anything else. */
    static String held(Player player) {
        NBTTagCompound tag = ItemNBT.read(player.getInventory().getItemInMainHand());
        return tag == null ? null : tag.getString("id").toUpperCase(Locale.ROOT);
    }

    /** The SkyBlock item they hold; null for nothing, or anything else. */
    static SkyBlockItem heldItem(Player player) {
        String id = held(player);
        return id == null ? null : ItemRegistry.get(id);
    }

    /** Whether what they hold is a weapon (a sword, a longsword, a wand, a bow, a fishing weapon). */
    static boolean holdsWeapon(Player player) {
        SkyBlockItem item = heldItem(player);
        return item != null && item.genericItemType() == GenericItemType.WEAPON;
    }

    /** What they wear: their armor, helmet first, then their equipment. */
    static List<Worn.Piece> worn(Player player) {
        return SetBonuses.worn(player).pieces();
    }

    /** The stats the worn pieces whose ids pass {@code ids} give, as each piece's own (what "the stats of X Armor" are). */
    static Stats wornStats(Player player, Predicate<String> ids) {
        Stats stats = new Stats();
        for (Worn.Piece piece : worn(player)) if (ids.test(piece.id())) stats.add(ItemStats.of(piece.stack(), player));
        return stats;
    }

    /** Whether the item's text says it's of this kind ("This item is Arachnal Ж!", "This item is Undead ༕!"). */
    static boolean marked(SkyBlockItem item, String kind) {
        if (item == null) return false;
        String says = "This item is " + kind;
        for (String line : item.lore()) if (line.replaceAll("[&§][0-9a-fk-or]", "").contains(says)) return true;
        return false;
    }

    /** The stack's SkyBlock item; null for nothing, or anything else. */
    static SkyBlockItem item(ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag == null ? null : ItemRegistry.get(tag.getString("id"));
    }

    // ---------- stats ----------

    /** These stats {@code percent} percent more. */
    static void scale(Stats stats, List<Stat> which, double percent) {
        for (Stat stat : which) stats.set(stat, stats.get(stat) * (1 + percent / 100));
    }

    /** This stat {@code percent} percent more. */
    static void scale(Stats stats, Stat stat, double percent) {
        stats.set(stat, stats.get(stat) * (1 + percent / 100));
    }

    /** These stats, {@code percent} percent of them, added to {@code to}. */
    static void addShare(Stats to, Stats from, double percent) {
        for (Stat stat : Stat.values()) if (from.has(stat)) to.add(stat, from.get(stat) * percent / 100);
    }

    /**
     * Their health now as a share of their max ({@code stats}' Health): for stats, which mustn't ask for the player's
     * stats themselves (full until their health is known).
     */
    static double healthShare(Player player, Stats stats) {
        double health = PlayerSession.of(player).getHealth();
        double max = Math.max(1, stats.get(Stat.HEALTH));
        return health < 0 ? 1 : Math.min(1, health / max);
    }

    // ---------- mobs and players ----------

    /** One of SkyBlock's mobs (or what one shot) whose entity is one of these types (zombies, wolves). */
    static boolean is(Entity entity, EntityType... types) {
        Entity mob = PlayerDamage.attacker(entity);
        if (mob == null) return false;
        for (EntityType type : types) if (mob.getType() == type) return true;
        return false;
    }

    /** Zombies, as Hypixel's "zombies" are (a zombie villager, a husk and a drowned are). */
    static boolean zombie(Entity entity) {
        return is(entity, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED);
    }

    /** The mob types of what hit them or what they hit (a projectile's shooter's). */
    static Set<MobType> types(Entity entity) {
        return SetBonuses.types(entity);
    }

    /** The level a mob's name shows; -1 for none. */
    static int level(Entity entity) {
        return SetBonuses.shownLevel(entity);
    }

    /** The other players within {@code range} of them in play (not the dead or dungeon ghosts), nearest first. */
    static List<Player> near(Player player, double range) {
        List<Player> near = new ArrayList<>();
        for (Player other : player.getWorld().getPlayers()) {
            if (other == player || !PetAbilities.inPlay(other)) continue;
            if (other.getLocation().distanceSquared(player.getLocation()) <= range * range) near.add(other);
        }
        near.sort((a, b) -> Double.compare(a.getLocation().distanceSquared(player.getLocation()),
                b.getLocation().distanceSquared(player.getLocation())));
        return near;
    }

    /**
     * How many unique pets of this kind ("Combat", "Foraging") they own at their most level, but {@code except}'s type
     * (Symbiosis: "for every other unique maxed Combat Pet that you own").
     */
    static int maxedPets(Player player, String except, String kind) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased()) return 0;
        PetData data = Pets.data();
        Set<String> maxed = new HashSet<>();
        for (OwnedPet pet : Pets.pets(user.profile())) {
            PetData.Pet of = data.pet(pet.type());
            if (of != null && !pet.type().equals(except) && of.kind().startsWith(kind) && Pets.maxed(pet, data)) maxed.add(pet.type());
        }
        return maxed.size();
    }

    /** Whether one of SkyBlock's mobs is alive and can be hurt (see {@link net.icxd.dungeons.combat.MobHits#hittable}). */
    static boolean mob(LivingEntity entity) {
        return net.icxd.dungeons.combat.MobHits.hittable(entity);
    }
}
