package net.icxd.dungeons.item.enchanting;

import lombok.Getter;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Enchantment implements ConfigurationSerializable {
    private final EnchantmentType type;
    private final int level;

    public Enchantment(EnchantmentType type, int level) {
        this.type = type;
        this.level = level;
    }

    /** "Sharpness V". */
    @Override
    public String toString() {
        return type.getName() + " " + Utils.getRomanNumeral(level);
    }

    /** "&9Sharpness V", or "&d&lUltimate Wise V" for an ultimate enchantment. */
    public String getDisplayName() {
        return (type.isUltimate() ? "&d&l" : "&9") + this;
    }

    /** Hypixel's description, wrapped as item lore; empty if there's none for this level. */
    public List<String> getDescription() {
        String text = type.getDescription(level);
        return text == null ? List.of() : Text.wrap(text, Text.LORE_WIDTH);
    }

    public static Enchantment getByIdentifiable(String identifiable) {
        String[] spl = identifiable.split("\\.");  // split on period
        return new Enchantment(EnchantmentType.getByNamespace(spl[0]), Integer.parseInt(spl[1]));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Enchantment)) return false;
        Enchantment enchantment = (Enchantment) o;
        return enchantment.level == level && enchantment.type == type;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, level);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.getNamespace());
        map.put("level", level);
        return map;
    }
}
