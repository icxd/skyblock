package net.icxd.dungeonscanner.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
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

  @Test
  void theSidebarIsReadTopToBottomFromDisplayNamesAndTeams() {
    Scoreboard board = new Scoreboard();
    Objective sidebar = board.addObjective("sb", ObjectiveCriteria.DUMMY, Component.literal("SKYBLOCK"),
        ObjectiveCriteria.RenderType.INTEGER, false, null);
    board.setDisplayObjective(DisplaySlot.SIDEBAR, sidebar);
    line(board, sidebar, "a", 6, "10/24/26 m35DV");
    line(board, sidebar, "b", 5, " 8:00am ☀");
    // Hypixel's way: a team's prefix and suffix around a holder that's only a colour code.
    PlayerTeam team = board.addPlayerTeam("area");
    team.setPlayerPrefix(Component.literal(" \uE067 Your Isl"));
    team.setPlayerSuffix(Component.literal("and"));
    board.addPlayerToTeam("§a", team);
    board.getOrCreatePlayerScore(ScoreHolder.forNameOnly("§a"), sidebar).set(4);
    line(board, sidebar, "d", 3, "Purse: 0");

    List<String> lines = WorldCapture.sidebar(board);
    assertEquals(List.of("10/24/26 m35DV", " 8:00am ☀", " \uE067 Your Island", "Purse: 0"), lines);
    assertEquals("Your Island", Sidebar.area(lines));
    assertEquals("m35DV", Sidebar.server(lines));
  }
}
