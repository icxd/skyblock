package net.icxd.dungeons.dungeons.generator.rooms;

import net.icxd.dungeons.dungeons.generator.utils.Coordinate;
import net.icxd.dungeons.dungeons.generator.utils.Random;

import java.util.Arrays;

public enum RoomSize {
  ONE_BY_ONE,

  TWO_BY_ONE, ONE_BY_TWO,
  TWO_BY_TWO,

  THREE_BY_ONE, ONE_BY_THREE,
  THREE_BY_TWO, TWO_BY_THREE,
  THREE_BY_THREE;
  
  public Coordinate getOffsetToCorner() {
    return switch (this) {
      case ONE_BY_ONE -> new Coordinate(1, 1);
      case TWO_BY_ONE -> new Coordinate(2, 1);
      case ONE_BY_TWO -> new Coordinate(1, 2);
      case TWO_BY_TWO -> new Coordinate(2, 2);
      case THREE_BY_ONE -> new Coordinate(3, 1);
      case ONE_BY_THREE -> new Coordinate(1, 3);
      case THREE_BY_TWO -> new Coordinate(3, 2);
      case TWO_BY_THREE -> new Coordinate(2, 3);
      case THREE_BY_THREE -> new Coordinate(3, 3);
    };
  }

  public static RoomSize getRandomRoomSize() {
    return Random.random(Arrays.asList(
        ONE_BY_ONE, TWO_BY_ONE, ONE_BY_TWO,
        TWO_BY_TWO, THREE_BY_ONE, ONE_BY_THREE,
        THREE_BY_TWO, TWO_BY_THREE, THREE_BY_THREE
    ));
  }
}
