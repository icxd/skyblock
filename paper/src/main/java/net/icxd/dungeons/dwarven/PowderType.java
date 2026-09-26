package net.icxd.dungeons.dwarven;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;

@AllArgsConstructor
@Getter
public enum PowderType {
  MITHRIL(ChatColor.DARK_GREEN), GEMSTONE(ChatColor.LIGHT_PURPLE), GLACITE(ChatColor.AQUA);

  private final ChatColor color;
}
