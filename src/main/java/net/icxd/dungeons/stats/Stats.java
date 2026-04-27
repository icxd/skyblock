package net.icxd.dungeons.stats;

import lombok.*;
import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.Enchantment;
import net.icxd.dungeons.item.enums.DungeonStar;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.reforge.ReforgeStat;
import net.icxd.dungeons.reforge.ReforgeStats;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Stats {
    public static final HashMap<UUID, Stats> STATS_CACHE = new HashMap<>();

    private double damage = 0;
    private double ferocity = 0;
    private double health = 0;
    private double defense = 0;
    private double strength = 0;
    private double intelligence = 0;
    private double miningFortune = 0;
    private double walkSpeed = 0;
    private double magicFind = 0;
    private double weaponAbilityDamage = 0;
    private double seaCreatureChance = 0;
    private double fishingSpeed = 0;
    private double criticalDamage = 0;
    private double trueDefense = 0;
    private double miningSpeed = 0;
    private double criticalChance = 0;
    private double attackSpeed = 0;
    private double breakingPower = 0;
    private double vitality = 0;
    private double mending = 0;
    private double abilityDamagePercent = 0;
    private double petLuck = 0;
    private double foragingWisdom = 0;
    private double combatWisdom = 0;
    private double riftTime = 0;
    private double riftDamage = 0;
    private double riftIntelligence = 0;
    private double fishingWisdom = 0;
    private double farmingWisdom = 0;
    private double healthRegeneration = 0;

    public Stats defaultStats() {
        return new Stats(
                0, 0, 100, 0, 0, 100, 0, 100,
                0, 0, 20, 0, 30, 0,
                0, 50, 0, 0, 100, 100, 0,
                0, 0, 0, 0, 0, 0, 0,
                0, 100);
    }

    public Stats set(Stats other) {
        this.damage = other.getDamage();
        this.ferocity = other.getFerocity();
        this.health = other.getHealth();
        this.defense = other.getDefense();
        this.strength = other.getStrength();
        this.intelligence = other.getIntelligence();
        this.miningFortune = other.getMiningFortune();
        this.walkSpeed = other.getWalkSpeed();
        this.magicFind = other.getMagicFind();
        this.weaponAbilityDamage = other.getWeaponAbilityDamage();
        this.seaCreatureChance = other.getSeaCreatureChance();
        this.fishingSpeed = other.getFishingSpeed();
        this.criticalDamage = other.getCriticalDamage();
        this.trueDefense = other.getTrueDefense();
        this.miningSpeed = other.getMiningSpeed();
        this.criticalChance = other.getCriticalChance();
        this.attackSpeed = other.getAttackSpeed();
        this.breakingPower = other.getBreakingPower();
        this.vitality = other.getVitality();
        this.mending = other.getMending();
        this.abilityDamagePercent = other.getAbilityDamagePercent();
        this.petLuck = other.getPetLuck();
        this.foragingWisdom = other.getForagingWisdom();
        this.combatWisdom = other.getCombatWisdom();
        this.riftTime = other.getRiftTime();
        this.riftDamage = other.getRiftDamage();
        this.riftIntelligence = other.getRiftIntelligence();
        this.fishingWisdom = other.getFishingWisdom();
        this.farmingWisdom = other.getFarmingWisdom();
        this.healthRegeneration = other.getHealthRegeneration();
        return this;
    }

    public Stats add(Stats other) {
        this.damage += other.getDamage();
        this.ferocity += other.getFerocity();
        this.health += other.getHealth();
        this.defense += other.getDefense();
        this.strength += other.getStrength();
        this.intelligence += other.getIntelligence();
        this.miningFortune += other.getMiningFortune();
        this.walkSpeed += other.getWalkSpeed();
        this.magicFind += other.getMagicFind();
        this.weaponAbilityDamage += other.getWeaponAbilityDamage();
        this.seaCreatureChance += other.getSeaCreatureChance();
        this.fishingSpeed += other.getFishingSpeed();
        this.criticalDamage += other.getCriticalDamage();
        this.trueDefense += other.getTrueDefense();
        this.miningSpeed += other.getMiningSpeed();
        this.criticalChance += other.getCriticalChance();
        this.attackSpeed += other.getAttackSpeed();
        this.breakingPower += other.getBreakingPower();
        this.vitality += other.getVitality();
        this.mending += other.getMending();
        this.abilityDamagePercent += other.getAbilityDamagePercent();
        this.petLuck += other.getPetLuck();
        this.foragingWisdom += other.getForagingWisdom();
        this.combatWisdom += other.getCombatWisdom();
        this.riftTime += other.getRiftTime();
        this.riftDamage += other.getRiftDamage();
        this.riftIntelligence += other.getRiftIntelligence();
        this.fishingWisdom += other.getFishingWisdom();
        this.farmingWisdom += other.getFarmingWisdom();
        this.healthRegeneration += other.getHealthRegeneration();
        return this;
    }

    public Stats multiply(double n) {
        this.damage *= n;
        this.ferocity *= n;
        this.health *= n;
        this.defense *= n;
        this.strength *= n;
        this.intelligence *= n;
        this.miningFortune *= n;
        this.walkSpeed *= n;
        this.magicFind *= n;
        this.weaponAbilityDamage *= n;
        this.seaCreatureChance *= n;
        this.fishingSpeed *= n;
        this.criticalDamage *= n;
        this.trueDefense *= n;
        this.miningSpeed *= n;
        this.criticalChance *= n;
        this.attackSpeed *= n;
        this.breakingPower *= n;
        this.vitality *= n;
        this.mending *= n;
        this.abilityDamagePercent *= n;
        this.petLuck *= n;
        this.foragingWisdom *= n;
        this.combatWisdom *= n;
        this.riftTime *= n;
        this.riftDamage *= n;
        this.riftIntelligence *= n;
        this.fishingWisdom *= n;
        this.farmingWisdom *= n;
        this.healthRegeneration *= n;
        return this;
    }

    public void addFromItemStack(ItemStack itemStack) {
        net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        if (nmsItem.getTag() != null) {
            SkyBlockItem item = ItemRegistry.get(nmsItem.getTag().getString("id"));
            if (item != null) {
                double boost = DungeonStar.valueOf(nmsItem.getTag().getString("dungeon_star")).getBoost();
                Rarity rarity = Rarity.valueOf(nmsItem.getTag().getString("rarity"));
                Reforge reforge = nmsItem.getTag().getString("reforge").equals("") ? null : Reforge.valueOf(nmsItem.getTag().getString("reforge"));
                ReforgeStats reforgeStats = null;
                if (reforge != null)
                    reforgeStats = reforge.getStats();
                this.add(item.stats());
                GenericItemType genericItemType = item.genericItemType();
                int hpb = nmsItem.getTag().getInt("hot_potato_books");
                boolean armor = genericItemType == GenericItemType.ARMOR;
                boolean weapon = genericItemType == GenericItemType.WEAPON;
                this.add(new Stats().setDamage((weapon ? hpb * 2 : 0)).setHealth((armor ? hpb * 4 : 0)).setDefense((armor ? hpb * 2 : 0)).setStrength((weapon ? hpb * 2 : 0)));

                if (reforgeStats != null)
                    this.addReforgeStat(this, rarity, reforgeStats);
                var enchants = nmsItem.getTag().getList("enchantments", 10);
                for (int i = 0; i < enchants.size(); i++) {
                    NBTTagCompound enchantment = enchants.get(i);
                    String name = enchantment.getString("name");
                    int level = enchantment.getInt("lvl");
                    Enchantment enchant = Enchantment.getByIdentifiable(name + "." + level);
                    switch (enchant.getType().getNamespace()) {
                        case "growth" -> this.add(new Stats().setHealth((level * 15)));
                        case "protection" -> this.add(new Stats().setDefense((level * 3)));
                    }
                }

                if (nmsItem.getTag().hasKey("attribute_1") && nmsItem.getTag().hasKey("attribute_2")) {
                    Attribute attribute1 = Attribute.valueOf(nmsItem.getTag().getString("attribute_1"));
                    int attribute1Level = nmsItem.getTag().getInt("attribute_1_level");
                    Attribute attribute2 = Attribute.valueOf(nmsItem.getTag().getString("attribute_2"));
                    int attribute2Level = nmsItem.getTag().getInt("attribute_2_level");
                    Stats stats = new Stats();
                    Player player = Bukkit.getPlayer(nmsItem.getTag().getString("owner"));
                    if (player != null && attribute1.requirement().test(player)) {
                        stats.add(attribute1.getStatsFunction().apply(attribute1Level));
                    }
                    if (player != null && attribute2.requirement().test(player)) {
                        stats.add(attribute2.getStatsFunction().apply(attribute2Level));
                    }
                    this.add(stats);
                }
            }
        }
    }

    public void addReforgeStat(Stats other, Rarity rarity, ReforgeStats stats) {
        double health = getStatFromRarity(rarity, stats.health());
        double defense = getStatFromRarity(rarity, stats.defense());
        double intelligence = getStatFromRarity(rarity, stats.intelligence());
        double speed = getStatFromRarity(rarity, stats.speed());
        double strength = getStatFromRarity(rarity, stats.strength());
        double critChance = getStatFromRarity(rarity, stats.critChance());
        double critDamage = getStatFromRarity(rarity, stats.critDamage());
        double attackSpeed = getStatFromRarity(rarity, stats.attackSpeed());
        double seaCreatureChance = getStatFromRarity(rarity, stats.seaCreatureChance());
        double magicFind = getStatFromRarity(rarity, stats.magicFind());
        double ferocity = getStatFromRarity(rarity, stats.ferocity());
        double miningSpeed = getStatFromRarity(rarity, stats.miningSpeed());
        double miningFortune = getStatFromRarity(rarity, stats.miningFortune());
        double farmingFortune = getStatFromRarity(rarity, stats.farmingFortune());
        double foragingFortune = getStatFromRarity(rarity, stats.foragingFortune());

        other.add(new Stats(
                0, ferocity, health, defense, strength,
                intelligence, miningFortune, speed, magicFind, 0, seaCreatureChance,
                0, critDamage, 0, miningSpeed, critChance, attackSpeed,
                0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0
        ));
    }

    public double getStatFromRarity(Rarity rarity, ReforgeStat stat) {
        return switch (rarity) {
            case COMMON -> stat.getCommon();
            case UNCOMMON -> stat.getUncommon();
            case RARE -> stat.getRare();
            case EPIC -> stat.getEpic();
            case LEGENDARY -> stat.getLegendary();
            case MYTHIC -> stat.getMythic();
            default -> 0;
        };
    }

    public Stats setDamage(double n) { this.damage = n; return this; }
    public Stats setFerocity(double n) { this.ferocity = n; return this; }
    public Stats setHealth(double n) { this.health = n; return this; }
    public Stats setDefense(double n) { this.defense = n; return this; }
    public Stats setStrength(double n) { this.strength = n; return this; }
    public Stats setIntelligence(double n) { this.intelligence = n; return this; }
    public Stats setMiningFortune(double n) { this.miningFortune = n; return this; }
    public Stats setWalkSpeed(double n) { this.walkSpeed = n; return this; }
    public Stats setMagicFind(double n) { this.magicFind = n; return this; }
    public Stats setWeaponAbilityDamage(double n) { this.weaponAbilityDamage = n; return this; }
    public Stats setSeaCreatureChance(double n) { this.seaCreatureChance = n; return this; }
    public Stats setFishingSpeed(double n) { this.fishingSpeed = n; return this; }
    public Stats setCriticalDamage(double n) { this.criticalDamage = n; return this; }
    public Stats setTrueDefense(double n) { this.trueDefense = n; return this; }
    public Stats setMiningSpeed(double n) { this.miningSpeed = n; return this; }
    public Stats setCriticalChance(double n) { this.criticalChance = n; return this; }
    public Stats setAttackSpeed(double n) { this.attackSpeed = n; return this; }
    public Stats setBreakingPower(double n) { this.breakingPower = n; return this; }
    public Stats setVitality(double n) { this.vitality = n; return this; }
    public Stats setMending(double n) { this.mending = n; return this; }
    public Stats setAbilityDamagePercent(double n) { this.abilityDamagePercent = n; return this; }
    public Stats setPetLuck(double n) { this.petLuck = n; return this; }
    public Stats setForagingWisdom(double n) { this.foragingWisdom = n; return this; }
    public Stats setCombatWisdom(double n) { this.combatWisdom = n; return this; }
    public Stats setRiftTime(double n) { this.riftTime = n; return this; }
    public Stats setRiftDamage(double n) { this.riftDamage = n; return this; }
    public Stats setRiftIntelligence(double n) { this.riftIntelligence = n; return this; }
    public Stats setFishingWisdom(double n) { this.fishingWisdom = n; return this; }
    public Stats setFarmingWisdom(double n) { this.farmingWisdom = n; return this; }
    public Stats setHealthRegeneration(double n) { this.healthRegeneration = n; return this; }
}
