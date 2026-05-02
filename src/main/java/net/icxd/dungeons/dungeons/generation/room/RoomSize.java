package net.icxd.dungeons.dungeons.generation.room;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.dungeons.generation.utils.Position;

@AllArgsConstructor
public enum RoomSize {
  ONE_BY_ONE(new Position[][]{
      {new Position(0, 0)}
  }),
  TWO_BY_ONE(new Position[][]{
      {new Position(0, 0), new Position(1, 0)},
      {new Position(0, 0), new Position(0, 1)},
      {new Position(-1, 0), new Position(0, 0)},
      {new Position(0, -1), new Position(0, 0)}
  }),
  THREE_BY_ONE(new Position[][]{
      {new Position(0, 0), new Position(1, 0), new Position(2, 0)},
      {new Position(0, 0), new Position(0, 1), new Position(0, 2)},
      {new Position(-2, 0), new Position(-1, 0), new Position(0, 0)},
      {new Position(0, -2), new Position(0, -1), new Position(0, 0)},
  }),
  FOUR_BY_ONE(new Position[][]{
      {new Position(0, 0), new Position(1, 0), new Position(2, 0), new Position(3, 0)},
      {new Position(0, 0), new Position(0, 1), new Position(0, 2), new Position(0, 3)},
      {new Position(-3, 0), new Position(-2, 0), new Position(-1, 0), new Position(0, 0)},
      {new Position(0, -3), new Position(0, -2), new Position(0, -1), new Position(0, 0)},
  }),
  TWO_BY_TWO(new Position[][]{
      {new Position(0, 0), new Position(1, 0), new Position(1, 1), new Position(0, 1)}
  }),
//  TODO:
//  L_SHAPE(new Position[][]{
//      {new Position(0, 0), new Position(1, 0), new Position(1, 1)}
//  }),
  ;

  @Getter
  private final Position[][] positions;
}
