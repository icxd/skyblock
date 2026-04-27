package net.icxd.dungeons.dungeons.generator.rooms;

import net.icxd.dungeons.dungeons.generator.utils.Random;
import org.bukkit.ChatColor;

import java.util.Arrays;

public enum RoomType {
  NONE, RED, GREEN, BLUE;

  public ChatColor color() {
    return switch (this) {
      case NONE -> ChatColor.BLACK;
      case RED -> ChatColor.RED;
      case GREEN -> ChatColor.GREEN;
      case BLUE -> ChatColor.BLUE;
    };
  }

  public static RoomType getRandomRoomType() {
    return Random.random(Arrays.asList(NONE, RED, GREEN, BLUE));
  }
}
