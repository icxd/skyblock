package net.icxd.dungeonscanner.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

class LegacyMapperTest {
  static LegacyMapper mapper;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    mapper = new LegacyMapper();
  }

  private static BlockState block(String id) {
    return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id)).defaultBlockState();
  }

  private static String legacy(BlockState state) {
    return mapper.toLegacy(state).map(LegacyBlock::toString).orElse("none");
  }

  @Test
  void plainBlocks() {
    assertEquals("0:0", legacy(Blocks.AIR.defaultBlockState()));
    assertEquals("1:0", legacy(Blocks.STONE.defaultBlockState()));
    assertEquals("2:0", legacy(Blocks.GRASS_BLOCK.defaultBlockState()));
    assertEquals("31:1", legacy(Blocks.SHORT_GRASS.defaultBlockState()));
    assertEquals("98:0", legacy(Blocks.STONE_BRICKS.defaultBlockState()));
    assertEquals("98:1", legacy(Blocks.MOSSY_STONE_BRICKS.defaultBlockState()));
    assertEquals("98:2", legacy(Blocks.CRACKED_STONE_BRICKS.defaultBlockState()));
    assertEquals("98:3", legacy(Blocks.CHISELED_STONE_BRICKS.defaultBlockState()));
    assertEquals("173:0", legacy(Blocks.COAL_BLOCK.defaultBlockState()));
    assertEquals("159:11", legacy(block("blue_terracotta")));
    assertEquals("159:14", legacy(block("red_terracotta")));
    assertEquals("97:5", legacy(Blocks.INFESTED_CHISELED_STONE_BRICKS.defaultBlockState()));
    assertEquals("35:14", legacy(block("red_wool")));
    assertEquals("166:0", legacy(Blocks.BARRIER.defaultBlockState()));
    assertEquals("5:5", legacy(Blocks.DARK_OAK_PLANKS.defaultBlockState()));
  }

  @Test
  void stairsIgnoreComputedShape() {
    // 1.8 stairs: 0 east, 1 west, 2 south, 3 north, +4 upside down. Shape is computed, not stored.
    BlockState east = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
    assertEquals("53:0", legacy(east));
    assertEquals("53:0", legacy(east.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT)));
    assertEquals("109:7", legacy(Blocks.STONE_BRICK_STAIRS.defaultBlockState()
        .setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.TOP)));
  }

  @Test
  void slabsAndLogs() {
    assertEquals("44:5", legacy(Blocks.STONE_BRICK_SLAB.defaultBlockState()));
    assertEquals("44:13", legacy(Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP)));
    assertEquals("126:1", legacy(Blocks.SPRUCE_SLAB.defaultBlockState()));
    assertEquals("17:4", legacy(Blocks.OAK_LOG.defaultBlockState().setValue(
        net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X)));
  }

  @Test
  void doorHalves() {
    BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
    // 1.8 lower half: facing (0 east, 1 south, 2 west, 3 north) + 4 if open.
    assertEquals("64:3", legacy(lower));
    assertEquals("64:7", legacy(lower.setValue(DoorBlock.OPEN, true)));
    // Upper half: 8 + hinge right (1) + powered (2).
    BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER).setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT);
    assertEquals("64:9", legacy(upper));
  }

  @Test
  void blocksThatUsedTileEntities() {
    assertEquals("144:1", legacy(Blocks.PLAYER_HEAD.defaultBlockState()));
    assertEquals("144:5", legacy(Blocks.PLAYER_WALL_HEAD.defaultBlockState()
        .setValue(net.minecraft.world.level.block.WallSkullBlock.FACING, Direction.EAST)));
    assertEquals("140:0", legacy(Blocks.POTTED_POPPY.defaultBlockState()));
    assertEquals("140:0", legacy(Blocks.FLOWER_POT.defaultBlockState()));
    assertEquals("175:8", legacy(Blocks.TALL_GRASS.defaultBlockState()
        .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)));
    assertEquals("175:2", legacy(Blocks.TALL_GRASS.defaultBlockState()));
    // Standing banner data is its rotation (26.2's default rotation is 8).
    assertEquals("176:8", legacy(block("red_banner")));
    assertEquals("176:0", legacy(block("red_banner").setValue(
        net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 0)));
  }

  @Test
  void everyOneEightBlockMapsBack() {
    // Round trip: nothing that existed in 1.8 should be missing from the table.
    assertTrue(mapper.size() > 300, "only " + mapper.size() + " blocks mapped");
  }
}
