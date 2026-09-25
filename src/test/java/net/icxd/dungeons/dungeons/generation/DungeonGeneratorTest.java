package net.icxd.dungeons.dungeons.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.icxd.dungeons.dungeons.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.DoorSlot;
import net.icxd.dungeons.dungeons.generation.room.HypixelRooms;
import net.icxd.dungeons.dungeons.generation.room.Room;
import net.icxd.dungeons.dungeons.generation.room.RoomShape;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

class DungeonGeneratorTest {

  @ParameterizedTest
  @EnumSource(DungeonFloor.class)
  void everyLayoutIsValid(DungeonFloor floor) {
    DungeonGenerator generator = new DungeonGenerator(DungeonConfig.forFloor(floor), HypixelRooms.pool());
    for (long seed = 0; seed < 300; seed++) {
      DungeonLayout layout = generator.generate(seed);
      List<String> problems = LayoutValidator.validate(layout);
      assertTrue(problems.isEmpty(), floor + " seed " + seed + ": " + problems + "\n" + layout.render());
    }
  }

  @Test
  void sameSeedSameDungeon() {
    DungeonGenerator generator = new DungeonGenerator(DungeonConfig.forFloor(DungeonFloor.FLOOR_7), HypixelRooms.pool());
    assertEquals(generator.generate(1234).render(), generator.generate(1234).render());
  }

  @Test
  void rareRoomsAreRare() {
    DungeonGenerator generator = new DungeonGenerator(DungeonConfig.forFloor(DungeonFloor.FLOOR_7), HypixelRooms.pool());
    int rare = 0;
    int runs = 500;
    for (long seed = 0; seed < runs; seed++) rare += generator.generate(seed).roomsOfType(RoomType.RARE).size();
    assertTrue(rare < runs / 5, rare + " rare rooms in " + runs + " dungeons");
  }

  @Test
  void specialColumnHoldsPuzzlesTrapAndMiniboss() {
    DungeonGenerator generator = new DungeonGenerator(DungeonConfig.forFloor(DungeonFloor.FLOOR_4), HypixelRooms.pool());
    for (long seed = 0; seed < 100; seed++) {
      DungeonLayout layout = generator.generate(seed);
      for (PlacedRoom r : layout.getRooms()) {
        if (r.type() == RoomType.PUZZLE || r.type() == RoomType.TRAP || r.type() == RoomType.MINIBOSS) {
          assertEquals(layout.getWidth() - 1, r.cells().get(0).x(), "seed " + seed + "\n" + layout.render());
        }
      }
    }
  }

  /** Templates with restricted walls: the generator must rotate/place them so every door fits. */
  @Test
  void respectsCustomDoorRules() {
    List<Room> templates = new ArrayList<>(HypixelRooms.ALL);
    templates.removeIf(r -> r.getType() == RoomType.REGULAR && r.getShape() != RoomShape.ONE_BY_ONE);
    // Long rooms that only open at their two ends, and a 2x2 that can't have doors on its north side.
    for (int i = 0; i < 8; i++) {
      templates.add(Room.builder().id("corridor_" + i).type(RoomType.REGULAR).shape(RoomShape.ONE_BY_TWO)
          .doorSlots(Set.of(DoorSlot.of(0, 0, Direction.WEST), DoorSlot.of(1, 0, Direction.EAST))).build());
      templates.add(Room.builder().id("hall_" + i).type(RoomType.REGULAR).shape(RoomShape.TWO_BY_TWO)
          .doorSlots(Set.of(DoorSlot.of(0, 1, Direction.WEST), DoorSlot.of(1, 1, Direction.EAST),
              DoorSlot.of(0, 1, Direction.SOUTH), DoorSlot.of(1, 1, Direction.SOUTH))).build());
    }
    RoomPool pool = new RoomPool(templates, true);
    DungeonGenerator generator = new DungeonGenerator(DungeonConfig.forFloor(DungeonFloor.FLOOR_7), pool);
    for (long seed = 0; seed < 50; seed++) {
      DungeonLayout layout = generator.generate(seed);
      List<String> problems = LayoutValidator.validate(layout);
      assertTrue(problems.isEmpty(), "seed " + seed + ": " + problems + "\n" + layout.render());
    }
  }

  @Test
  void rotatingATemplateRotatesItsDoors() {
    Room corner = Room.builder().id("corner").type(RoomType.REGULAR).shape(RoomShape.ONE_BY_ONE)
        .doorSlots(Set.of(DoorSlot.of(0, 0, Direction.NORTH), DoorSlot.of(0, 0, Direction.EAST))).exactDoors(true).build();
    Position c = new Position(2, 2);
    List<Placement> placements = Placement.enumerate(corner, List.of(c), 5, 5);
    assertEquals(4, placements.size());
    Placement quarter = placements.stream().filter(p -> p.rotation() == 1).findFirst().orElseThrow();
    assertEquals(Set.of(Edge.of(c, Direction.EAST), Edge.of(c, Direction.SOUTH)), quarter.requiredDoors());

    // An L's missing corner fixes its rotation; a 1x4 only has two orientations.
    assertEquals(4, RoomShape.L_SHAPE.orientations().size());
    assertEquals(2, RoomShape.ONE_BY_FOUR.orientations().size());
    assertEquals(1, RoomShape.TWO_BY_TWO.orientations().size());

    // Door on the far end of a 1x2 follows the room when it's turned upright.
    Room end = Room.builder().id("end").type(RoomType.REGULAR).shape(RoomShape.ONE_BY_TWO)
        .doorSlots(Set.of(DoorSlot.of(1, 0, Direction.EAST))).build();
    List<Position> upright = List.of(new Position(1, 1), new Position(1, 2));
    Placement turned = Placement.enumerate(end, upright, 5, 5).stream().filter(p -> p.rotation() == 1).findFirst().orElseThrow();
    assertEquals(Set.of(Edge.of(new Position(1, 2), Direction.SOUTH)), turned.allowedDoors());
  }
}
