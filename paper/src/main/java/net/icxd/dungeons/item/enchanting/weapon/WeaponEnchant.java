package net.icxd.dungeons.item.enchanting.weapon;

import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;

import java.util.HashMap;
import java.util.Map;

/**
 * The weapon enchantments that do something in code here (ENCHANTS_WEAPONS.md), by what items store them as (an
 * ultimate's id without Hypixel's "ultimate_", as {@link EnchantmentData#id} has it: Duplex is "reiterate", Drain
 * "syphon"), and an item's levels of them, read straight from its data's list (no map a hit). No Bukkit.
 */
public enum WeaponEnchant {
    CLEAVE("cleave"),
    LIFE_STEAL("life_steal"),
    MANA_STEAL("mana_steal"),
    DRAIN("syphon"),
    THUNDERLORD("thunderlord"),
    THUNDERBOLT("thunderbolt"),
    LETHALITY("lethality"),
    VENOMOUS("venomous"),
    FIRE_ASPECT("fire_aspect"),
    KNOCKBACK("knockback"),
    VAMPIRISM("vampirism"),
    TABASCO("tabasco"),
    CHAMPION("champion"),
    FLAME("flame"),
    PUNCH("punch"),
    PIERCING("piercing"),
    INFINITE_QUIVER("infinite_quiver"),
    TOXOPHILITE("toxophilite"),
    INFERNO("inferno"),
    FATAL_TEMPO("fatal_tempo"),
    COMBO("combo"),
    SOUL_EATER("soul_eater"),
    SWARM("swarm"),
    REND("rend"),
    DUPLEX("reiterate"),
    WISE("wise"),
    JERRY("jerry");

    private static final WeaponEnchant[] ALL = values();
    private static final Map<String, WeaponEnchant> BY_ID = new HashMap<>();
    /** What an item with none of them has (shared: never written to). */
    private static final Levels NONE = new Levels(new int[ALL.length]);

    static {
        for (WeaponEnchant enchant : ALL) BY_ID.put(enchant.id, enchant);
    }

    private final String id;

    WeaponEnchant(String id) {
        this.id = id;
    }

    /** What items store it as. */
    public String id() {
        return id;
    }

    /** The one items store as this id (Hypixel's too: "ultimate_soul_eater"); null if it isn't one of these. */
    public static WeaponEnchant of(String stored) {
        return stored == null ? null : BY_ID.get(EnchantmentData.id(stored));
    }

    /** An item's levels of these (0 for one it doesn't have). */
    public static final class Levels {
        private final int[] levels;

        private Levels(int[] levels) {
            this.levels = levels;
        }

        public int of(WeaponEnchant enchant) {
            return levels[enchant.ordinal()];
        }

        public boolean has(WeaponEnchant enchant) {
            return levels[enchant.ordinal()] > 0;
        }

        /** Whether it has none of them. */
        public boolean none() {
            return this == NONE;
        }
    }

    /** The levels of these on an item with this data (null: none). */
    public static Levels levels(NBTTagCompound tag) {
        if (tag == null) return NONE;
        NBTTagList list = tag.getList("enchantments", 10);
        int[] levels = null;
        for (int i = 0; i < list.size(); i++) {
            NBTTagCompound entry = list.get(i);
            WeaponEnchant enchant = of(entry.getString("name"));
            if (enchant == null) continue;
            if (levels == null) levels = new int[ALL.length];
            levels[enchant.ordinal()] = Math.max(0, entry.getInt("lvl"));
        }
        return levels == null ? NONE : new Levels(levels);
    }

    /** The level of this one on an item with this data; 0 without it. */
    public int on(NBTTagCompound tag) {
        if (tag == null) return 0;
        NBTTagList list = tag.getList("enchantments", 10);
        for (int i = 0; i < list.size(); i++) {
            NBTTagCompound entry = list.get(i);
            if (of(entry.getString("name")) == this) return Math.max(0, entry.getInt("lvl"));
        }
        return 0;
    }

    /** Sets its level on an item's data (the one it has; nothing if it hasn't it), as Hypixel stores levels: an int. */
    public boolean setOn(NBTTagCompound tag, int level) {
        NBTTagList list = tag.getList("enchantments", 10);
        for (int i = 0; i < list.size(); i++) {
            NBTTagCompound entry = list.get(i);
            if (of(entry.getString("name")) != this) continue;
            entry.setInt("lvl", level);
            return true;
        }
        return false;
    }
}
