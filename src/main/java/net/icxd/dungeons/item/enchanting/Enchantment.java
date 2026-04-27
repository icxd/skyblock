package net.icxd.dungeons.item.enchanting;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Enchantment implements ConfigurationSerializable {
    private final EnchantmentType type;
    private final int level;

    public Enchantment(EnchantmentType type, int level) {
        this.type = type;
        this.level = level;
    }

    @Override
    public String toString() { return type.getName() + " " + level; }

    public String getDisplayName() {
        String color = "" + ChatColor.BLUE;
        return (!type.isUltimate() ? color : "" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD) + this;
    }

    public String toIdentifiableString() { return type.getNamespace() + "." + level; }

    public String getDescription() {
        return switch (type.getNamespace()) {
            case "growth" -> type.getDescription(level * 15);
            case "protection" -> type.getDescription(level * 3);
            default -> type.getDescription(level);
        };
    }

    public static Enchantment getByIdentifiable(String identifiable) {
        String[] spl = identifiable.split("\\.");  // split on period
        return new Enchantment(EnchantmentType.getByNamespace(spl[0]), Integer.parseInt(spl[1]));
    }

    public boolean equalsType(Enchantment enchantment) {
        return enchantment.type == type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Enchantment)) return false;
        Enchantment enchantment = (Enchantment) o;
        return enchantment.level == level && enchantment.type == type;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.getNamespace());
        map.put("level", level);
        return map;
    }
}
