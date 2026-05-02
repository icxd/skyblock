package net.icxd.dungeons.dungeons.generation.room;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import net.icxd.dungeons.dungeons.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.secrets.Secret;
import net.icxd.dungeons.dungeons.generation.secrets.SecretType;
import net.icxd.dungeons.utils.Utils;

@Builder
@Getter
public class Room {
  // Never generated
  public static final Room EMPTY = new RoomBuilder()
      .id("empty").type(RoomType.EMPTY)
      .required(false).canGenerateNaturally(false).build();

  // Required rooms
  public static final Room START = new RoomBuilder()
      .id("start").type(RoomType.START).size(RoomSize.ONE_BY_ONE)
      .required(true).canGenerateNaturally(false).build();
  public static final Room FAIRY = new RoomBuilder()
      .id("fairy").type(RoomType.FAIRY).size(RoomSize.ONE_BY_ONE)
      .required(true).canGenerateNaturally(false).build();
  public static final Room BLOOD = new RoomBuilder()
      .id("blood").type(RoomType.BLOOD).size(RoomSize.ONE_BY_ONE)
      .required(true).canGenerateNaturally(false).build();

  // 1x1 Rooms
  public static final Room ADMIN = new RoomBuilder().id("admin").type(RoomType.REGULAR).size(RoomSize.ONE_BY_ONE).build();
  public static final Room HALL = new RoomBuilder().id("hall").type(RoomType.REGULAR).size(RoomSize.ONE_BY_ONE).build();
  public static final Room YOMIOKA = new RoomBuilder().id("yomioka").type(RoomType.REGULAR).size(RoomSize.ONE_BY_ONE).build();
  public static final Room LEAVES = new RoomBuilder().id("leaves").type(RoomType.REGULAR).size(RoomSize.ONE_BY_ONE).build();
  public static final Room[] ONE_BY_ONE_ROOMS = {ADMIN, HALL, YOMIOKA, LEAVES};

  // 1x2 Rooms
  public static final Room SKULL = new RoomBuilder().id("skull").type(RoomType.REGULAR).size(RoomSize.TWO_BY_ONE).build();
  public static final Room BALCONY = new RoomBuilder().id("balcony").type(RoomType.REGULAR).size(RoomSize.TWO_BY_ONE).build();
  public static final Room GRAND_LIBRARY = new RoomBuilder().id("grand_library").type(RoomType.REGULAR).size(RoomSize.TWO_BY_ONE).build();
  public static final Room CRYPT = new RoomBuilder().id("crypt").type(RoomType.REGULAR).size(RoomSize.TWO_BY_ONE).build();
  public static final Room PURPLE_FLAGS = new RoomBuilder().id("purple_flags").type(RoomType.REGULAR).size(RoomSize.TWO_BY_ONE).build();
  public static final Room[] ONE_BY_TWO_ROOMS = {SKULL, BALCONY, GRAND_LIBRARY, CRYPT, PURPLE_FLAGS};

  // 1x3 Rooms
  public static final Room DIAGONAL = new RoomBuilder().id("diagonal").type(RoomType.REGULAR).size(RoomSize.THREE_BY_ONE).build();
  public static final Room RED_BLUE = new RoomBuilder().id("red_blue").type(RoomType.REGULAR).size(RoomSize.THREE_BY_ONE).build();
  public static final Room CATWALK = new RoomBuilder().id("catwalk").type(RoomType.REGULAR).size(RoomSize.THREE_BY_ONE).build();
  public static final Room DEATHMITE = new RoomBuilder().id("deathmite").type(RoomType.REGULAR).size(RoomSize.THREE_BY_ONE).build();
  public static final Room GRAVEL = new RoomBuilder().id("gravel").type(RoomType.REGULAR).size(RoomSize.THREE_BY_ONE).build();
  public static final Room[] ONE_BY_THREE_ROOMS = {DIAGONAL, RED_BLUE, CATWALK, DEATHMITE, GRAVEL};

  // 1x4 Rooms
  public static final Room HALLWAY = new RoomBuilder().id("hallway").type(RoomType.REGULAR).size(RoomSize.FOUR_BY_ONE).build();
  public static final Room MOSSY = new RoomBuilder().id("mossy").type(RoomType.REGULAR).size(RoomSize.FOUR_BY_ONE).build();
  public static final Room PIT = new RoomBuilder().id("pit").type(RoomType.REGULAR).size(RoomSize.FOUR_BY_ONE).build();
  public static final Room QUARTZ_KNIGHT = new RoomBuilder().id("quartz_knight").type(RoomType.REGULAR).size(RoomSize.FOUR_BY_ONE).build();
  public static final Room SEWER = new RoomBuilder().id("sewer").type(RoomType.REGULAR).size(RoomSize.FOUR_BY_ONE).build();
  public static final Room[] ONE_BY_FOUR_ROOMS = {HALLWAY, MOSSY, PIT, QUARTZ_KNIGHT, SEWER};

  // 2x2 Rooms
  public static final Room STAIRS = new RoomBuilder().id("stairs").type(RoomType.REGULAR).size(RoomSize.TWO_BY_TWO).build();
  public static final Room MUSEUM = new RoomBuilder().id("museum").type(RoomType.REGULAR).size(RoomSize.TWO_BY_TWO).build();
  public static final Room SUPER_TALL = new RoomBuilder().id("super_tall").type(RoomType.REGULAR).size(RoomSize.TWO_BY_TWO).build();
  public static final Room FLAGS = new RoomBuilder().id("flags").type(RoomType.REGULAR).size(RoomSize.TWO_BY_TWO).build();
  public static final Room CATHEDRAL = new RoomBuilder().id("cathedral").type(RoomType.REGULAR).size(RoomSize.TWO_BY_TWO).build();
  public static final Room RAIL_TRACK = new RoomBuilder().id("rail_track").type(RoomType.REGULAR).size(RoomSize.TWO_BY_TWO).build();
  public static final Room[] TWO_BY_TWO_ROOMS = {STAIRS, MUSEUM, SUPER_TALL, FLAGS, CATHEDRAL, RAIL_TRACK};

  public static final Room[] ALL_ROOMS = Stream.of(
      ONE_BY_ONE_ROOMS,
      ONE_BY_TWO_ROOMS,
      ONE_BY_THREE_ROOMS,
      ONE_BY_FOUR_ROOMS,
      TWO_BY_TWO_ROOMS
  ).flatMap(Arrays::stream).toArray(Room[]::new);

  private final String id;
  private final RoomType type;
  private final RoomSize size;
  @Builder.Default
  private DungeonFloor minimumFloor = DungeonFloor.ENTRANCE;
  @Builder.Default
  private boolean required = false, canGenerateNaturally = true;
  @Builder.Default
  private List<Secret> secrets = new ArrayList<>();

  public static Room getRandomRoom() {
    return ALL_ROOMS[Utils.random(0, ALL_ROOMS.length - 1)];
  }
}
