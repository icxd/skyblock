package net.icxd.dungeonscanner.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mojang.serialization.Codec;

import net.minecraft.SharedConstants;
import net.minecraft.core.IdMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

/** What {@link ChunkNbt} writes, read back with Minecraft's own codecs and region files. */
class ChunkNbtTest {
  private static final BlockState AIR = Blocks.AIR.defaultBlockState();
  /** Stands in for the biome registry, which only a world has. */
  private static final List<String> BIOMES = List.of("minecraft:plains", "hypixel:wilderness", "minecraft:the_void", "hypixel:graveyard");
  private static Strategy<BlockState> blocks;
  private static Strategy<String> biomes;

  @BeforeAll
  static void boot() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    blocks = Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY);
    biomes = Strategy.createForBiomes(new IdMap<>() {
      @Override
      public int getId(String biome) {
        return BIOMES.indexOf(biome);
      }

      @Override
      public String byId(int id) {
        return id >= 0 && id < BIOMES.size() ? BIOMES.get(id) : null;
      }

      @Override
      public int size() {
        return BIOMES.size();
      }

      @Override
      public Iterator<String> iterator() {
        return BIOMES.iterator();
      }
    });
  }

  /** A section holding {@code kinds} different block states, each somewhere. */
  private static PalettedContainer<BlockState> section(int kinds) {
    List<BlockState> states = new ArrayList<>();
    for (Block block : BuiltInRegistries.BLOCK) {
      states.addAll(block.getStateDefinition().getPossibleStates());
      if (states.size() >= kinds) break;
    }
    PalettedContainer<BlockState> section = new PalettedContainer<>(AIR, blocks);
    int i = 0;
    for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) section.set(x, y, z, states.get(i++ % kinds));
    return section;
  }

  /** Biomes laid out by {@code pick}, packed as the client packs them, by name. */
  private static ChunkSnapshot.Section withBiomes(int y, PalettedContainer<BlockState> states, IntUnaryOperator pick) {
    PalettedContainer<String> container = new PalettedContainer<>("minecraft:plains", biomes);
    int i = 0;
    for (int by = 0; by < 4; by++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) container.set(x, by, z, BIOMES.get(pick.applyAsInt(i++)));
    PalettedContainerRO.PackedData<String> packed = container.pack(biomes);
    return new ChunkSnapshot.Section(y, states, packed.paletteEntries(), packed.storage().map(LongStream::toArray).orElse(null));
  }

  private static CompoundTag section(CompoundTag chunk, int index) {
    return chunk.getListOrEmpty("sections").getCompoundOrEmpty(index);
  }

  @Test
  void blocksReadBackWithMinecraftsCodec() {
    // One state (no data), up to 16 (4 bits), a hash map palette (5 bits) and past 256 (as many bits as needed).
    for (int kinds : new int[]{1, 2, 16, 17, 300}) {
      PalettedContainer<BlockState> written = section(kinds);
      CompoundTag chunk = ChunkNbt.chunk(new ChunkSnapshot(0, 0, List.of(withBiomes(-4, written, i -> 0)), List.of()), blocks);
      CompoundTag states = section(chunk, 0).getCompoundOrEmpty("block_states");
      assertEquals(kinds == 1, !states.contains("data"), kinds + " kinds");
      PalettedContainer<BlockState> read = PalettedContainer.codecRW(BlockState.CODEC, blocks, AIR).parse(NbtOps.INSTANCE, states).getOrThrow();
      for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
        assertEquals(written.get(x, y, z), read.get(x, y, z), kinds + " kinds at " + x + "," + y + "," + z);
      }
    }
  }

  @Test
  void biomesKeepTheirNamesAndReadBack() {
    ChunkSnapshot.Section s = withBiomes(0, section(1), i -> i % 7 == 0 ? 1 : i % 5 == 0 ? 3 : 0);
    CompoundTag chunk = ChunkNbt.chunk(new ChunkSnapshot(0, 0, List.of(s), List.of()), blocks);
    CompoundTag written = section(chunk, 0).getCompoundOrEmpty("biomes");
    PalettedContainerRO<String> read = PalettedContainer.codecRO(Codec.STRING, biomes, "minecraft:plains").parse(NbtOps.INSTANCE, written).getOrThrow();
    int i = 0;
    for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++, i++) {
      assertEquals(i % 7 == 0 ? "hypixel:wilderness" : i % 5 == 0 ? "hypixel:graveyard" : "minecraft:plains", read.get(x, y, z));
    }
    // One biome everywhere: just the palette.
    CompoundTag single = section(ChunkNbt.chunk(new ChunkSnapshot(0, 0, List.of(withBiomes(0, section(1), k -> 2)), List.of()), blocks), 0)
        .getCompoundOrEmpty("biomes");
    assertEquals("minecraft:the_void", single.getListOrEmpty("palette").getStringOr(0, ""));
    assertFalse(single.contains("data"));
  }

  @Test
  void aChunkIsMarkedFullWithItsBlockEntitiesAndLightToBeWorkedOut() {
    CompoundTag sign = new CompoundTag();
    sign.putString("id", "minecraft:sign");
    sign.putInt("x", 35);
    sign.putInt("y", 70);
    sign.putInt("z", -12);
    List<ChunkSnapshot.Section> sections = List.of(withBiomes(-4, section(1), i -> 0), withBiomes(-3, section(2), i -> 0));
    CompoundTag chunk = ChunkNbt.chunk(new ChunkSnapshot(2, -1, sections, List.of(sign)), blocks);
    assertEquals(SharedConstants.WORLD_VERSION, chunk.getIntOr("DataVersion", 0));
    assertEquals(2, chunk.getIntOr("xPos", 0));
    assertEquals(-1, chunk.getIntOr("zPos", 0));
    assertEquals(-4, chunk.getIntOr("yPos", 0));
    assertEquals("minecraft:full", chunk.getStringOr("Status", ""));
    assertFalse(chunk.getBooleanOr("isLightOn", true));
    assertEquals(List.of(-4, -3), List.of((int) section(chunk, 0).getByteOr("Y", (byte) 0), (int) section(chunk, 1).getByteOr("Y", (byte) 0)));
    ListTag blockEntities = chunk.getListOrEmpty("block_entities");
    assertEquals(1, blockEntities.size());
    assertEquals(sign, blockEntities.getCompoundOrEmpty(0));
  }

  @Test
  void decorationsAreSavedByChunkWithTheirPosition() {
    CompoundTag frame = new CompoundTag();
    frame.putString("id", "minecraft:item_frame");
    CompoundTag tag = ChunkNbt.entities(3, -2, List.of(frame));
    assertEquals(SharedConstants.WORLD_VERSION, tag.getIntOr("DataVersion", 0));
    assertEquals(List.of(3, -2), Arrays.stream(tag.getIntArray("Position").orElseThrow()).boxed().toList());
    assertEquals(frame, tag.getListOrEmpty("Entities").getCompoundOrEmpty(0));
  }

  @Test
  void regionFilesHoldTheChunksWritten(@TempDir Path dir) throws Exception {
    CompoundTag a = ChunkNbt.chunk(new ChunkSnapshot(0, 0, List.of(withBiomes(0, section(17), i -> 1)), List.of()), blocks);
    CompoundTag b = ChunkNbt.chunk(new ChunkSnapshot(-1, 5, List.of(withBiomes(0, section(2), i -> 0)), List.of()), blocks);
    try (RegionFileStorage storage = new RegionFileStorage(new RegionStorageInfo("test", Level.OVERWORLD, "chunk"), dir, false)) {
      storage.write(new ChunkPos(0, 0), a);
      storage.write(new ChunkPos(-1, 5), b);
    }
    assertTrue(Files.exists(dir.resolve("r.0.0.mca")));
    assertTrue(Files.exists(dir.resolve("r.-1.0.mca")));
    try (RegionFileStorage storage = new RegionFileStorage(new RegionStorageInfo("test", Level.OVERWORLD, "chunk"), dir, false)) {
      assertEquals(a, storage.read(new ChunkPos(0, 0)));
      assertEquals(b, storage.read(new ChunkPos(-1, 5)));
    }
  }

  @Test
  void theSidebarSaysWhereYouAre() {
    List<String> island = List.of("10/24/26 m35DV", "", "Early Spring 1st", "8:00am ☀", "\uE067 Your Island", "", "Purse: 0");
    assertEquals("Your Island", Sidebar.area(island));
    assertEquals("m35DV", Sidebar.server(island));
    assertEquals("Village", Sidebar.area(List.of(" 10:30pm ☽", " ⏣ Village")));
    assertEquals("The Catacombs (E)", Sidebar.area(List.of("10:50am", "The Catacombs (E)")));
    // Another line that reads like a time: the area is the line after a time that has the icon.
    assertEquals("Test Island", Sidebar.area(List.of("\"8:00am\"", " 8:00am ☀", " \uE067 Test Island", "Purse: 0")));
    assertNull(Sidebar.area(List.of("Games in this lobby are", "under heavy development!")));
    assertNull(Sidebar.server(List.of("Purse: 10")));
  }
}
