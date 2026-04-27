package net.icxd.dungeons.entity.enums;

import lombok.Getter;
import org.bukkit.ChatColor;

@Getter
public enum EntityDropType {
    GUARANTEED(ChatColor.GREEN),
    COMMON(ChatColor.GREEN),
    OCCASIONAL(ChatColor.BLUE),
    RARE(ChatColor.BLUE),
    VERY_RARE(ChatColor.AQUA),
    EXTRAORDINARILY_RARE(ChatColor.DARK_PURPLE),
    CRAZY_RARE(ChatColor.LIGHT_PURPLE),
    RNGESUS_INCARNATE(ChatColor.RED);

    private final ChatColor color;

    EntityDropType(ChatColor color) {
        this.color = color;
    }

    public String getDisplay() {
        return "" + color + ChatColor.BOLD + name().replaceAll("_", " ");
    }
}
