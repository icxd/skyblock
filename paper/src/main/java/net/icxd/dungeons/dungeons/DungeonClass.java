package net.icxd.dungeons.dungeons;

import java.util.List;
import java.util.Locale;

import org.bukkit.Material;

/**
 * The five dungeon classes, picked in the Ready Up menu. Names of their passives and abilities are
 * Hypixel's (from the Ready Up menu); what they do comes with the class system.
 */
public enum DungeonClass {
    HEALER("Healer", Material.SPLASH_POTION,
            List.of("Renew", "Healing Aura", "Revive", "Orbies", "Soul Tether", "Overheal"),
            List.of("Healing Circle", "Wish"),
            List.of("Healing Potion", "Revive Self")),
    MAGE("Mage", Material.BLAZE_ROD,
            List.of("Mage Staff", "Efficient Spells"),
            List.of("Guided Sheep", "Thunderstorm"),
            List.of("Pop-up Wall", "Fireball")),
    BERSERK("Berserk", Material.IRON_SWORD,
            List.of("Bloodlust", "Lust for Blood", "Indomitable", "Weapon Master"),
            List.of("Throwing Axe", "Ragnarok"),
            List.of("Strength Potion", "Ghost Axe")),
    ARCHER("Archer", Material.BOW,
            List.of("Doubleshot", "Bone Plating", "Bouncy Arrows"),
            List.of("Explosive Shot", "Machine Gun Bow"),
            List.of("Stun Bow", "Healing Bow")),
    TANK("Tank", Material.LEATHER_CHESTPLATE,
            List.of("Protective Barrier", "Taunt", "Diversion", "Defensive Stance"),
            List.of("Seismic Wave", "Castle of Stone"),
            List.of("Stun Potion", "Absorption Potion"));

    private final String displayName;
    private final Material icon;
    private final List<String> passives;
    private final List<String> orbAbilities;
    private final List<String> ghostAbilities;

    DungeonClass(String displayName, Material icon, List<String> passives, List<String> orbAbilities, List<String> ghostAbilities) {
        this.displayName = displayName;
        this.icon = icon;
        this.passives = passives;
        this.orbAbilities = orbAbilities;
        this.ghostAbilities = ghostAbilities;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** "B" for Berserk, as in the sidebar's "[B] Name". */
    public String getLetter() {
        return displayName.substring(0, 1);
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getPassives() {
        return passives;
    }

    public List<String> getOrbAbilities() {
        return orbAbilities;
    }

    public List<String> getGhostAbilities() {
        return ghostAbilities;
    }

    /** From a user document's {@code dungeons.selectedClass}; Healer (the default) if it's missing or unknown. */
    public static DungeonClass parse(String name) {
        if (name != null) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Falls through to the default.
            }
        }
        return HEALER;
    }
}
