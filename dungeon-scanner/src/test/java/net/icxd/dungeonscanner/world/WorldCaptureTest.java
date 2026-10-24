package net.icxd.dungeonscanner.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** How {@link WorldCapture} reads the sidebar: as the client draws it, top to bottom. */
class WorldCaptureTest {
  @BeforeAll
  static void boot() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  private static void line(Scoreboard board, Objective objective, String holder, int value, String text) {
    board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(holder), objective).set(value);
    board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(holder), objective).display(Component.literal(text));
  }

  /** Hypixel's way: a team's prefix and suffix around a holder that's only a code, often one that isn't a real one. */
  private static void teamLine(Scoreboard board, Objective objective, String holder, int value, String prefix, String suffix) {
    PlayerTeam team = board.addPlayerTeam("line" + value);
    team.setPlayerPrefix(Component.literal(prefix));
    team.setPlayerSuffix(Component.literal(suffix));
    board.addPlayerToTeam(holder, team);
    board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(holder), objective).set(value);
  }

  @Test
  void theSidebarIsReadTopToBottomFromDisplayNamesAndTeams() {
    Scoreboard board = new Scoreboard();
    Objective sidebar = board.addObjective("sb", ObjectiveCriteria.DUMMY, Component.literal("SKYBLOCK"),
        ObjectiveCriteria.RenderType.INTEGER, false, null);
    board.setDisplayObjective(DisplaySlot.SIDEBAR, sidebar);
    // As on Hypixel: the server's id and the area each split around a code that isn't one.
    teamLine(board, sidebar, "§x", 6, "§710/24/26 §8m91", "§8AA");
    line(board, sidebar, "b", 5, " 8:00am ☀");
    teamLine(board, sidebar, "§t", 4, " §7\uE067 §aYour Isla", "§and");
    teamLine(board, sidebar, "§a", 3, "Purse: §6", "0");

    List<String> lines = WorldCapture.sidebar(board);
    assertEquals(List.of("10/24/26 m91AA", " 8:00am ☀", " \uE067 Your Island", "Purse: 0"), lines);
    assertEquals("Your Island", Sidebar.area(lines));
    assertEquals("m91AA", Sidebar.server(lines));
  }

  @Test
  void leavesAreSavedPersistentAndNothingElseChanges() {
    PalettedContainer<BlockState> states = new PalettedContainer<>(Blocks.AIR.defaultBlockState(), Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY));
    BlockState decaying = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 3);
    BlockState placed = Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    states.set(1, 2, 3, decaying);
    states.set(4, 5, 6, placed);
    states.set(7, 8, 9, Blocks.STONE.defaultBlockState());

    WorldCapture.persistentLeaves(states);
    assertEquals(decaying.setValue(LeavesBlock.PERSISTENT, true), states.get(1, 2, 3));
    assertEquals(placed, states.get(4, 5, 6));
    assertEquals(Blocks.STONE.defaultBlockState(), states.get(7, 8, 9));
    assertEquals(Blocks.AIR.defaultBlockState(), states.get(0, 0, 0));
  }
}
