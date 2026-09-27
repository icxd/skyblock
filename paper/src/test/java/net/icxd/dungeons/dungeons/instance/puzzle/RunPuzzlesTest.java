package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

/** The puzzles' bookkeeping, with a made-up run (no world). */
class RunPuzzlesTest {
    /** A run that keeps what the puzzles do to it; tasks run when {@link #run} says. */
    static final class FakeHost implements PuzzleHost {
        final List<String> log = new ArrayList<>();
        final List<String> chat = new ArrayList<>();
        final List<Integer> solved = new ArrayList<>();
        final List<Integer> failed = new ArrayList<>();
        final List<long[]> when = new ArrayList<>();
        final List<Runnable> tasks = new ArrayList<>();

        @Override
        public World world() {
            return null;
        }

        @Override
        public List<Player> players() {
            return List.of();
        }

        @Override
        public void tell(String message) {
            chat.add(message);
        }

        @Override
        public void later(long ticks, Runnable task) {
            when.add(new long[]{ticks});
            tasks.add(task);
        }

        @Override
        public void solved(int room) {
            solved.add(room);
        }

        @Override
        public void failed(int room) {
            failed.add(room);
        }

        @Override
        public void log(String message) {
            log.add(message);
        }

        @Override
        public void blessing(Player finder, String blessing, int level) {
        }

        /** Runs the tasks waiting (in order) and says how long each waited. */
        List<Long> run() {
            List<Long> delays = new ArrayList<>();
            for (long[] w : when) delays.add(w[0]);
            List<Runnable> now = List.copyOf(tasks);
            tasks.clear();
            when.clear();
            now.forEach(Runnable::run);
            return delays;
        }
    }

    private static final PuzzleFrame FRAME = new PuzzleFrame(0, 0, 31, 31, 0);

    /** A player called that (only their name is asked for). */
    static Player named(String name) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> {
            if (method.getName().equals("getName")) return name;
            throw new UnsupportedOperationException(method.getName());
        });
    }

    @Test
    void puzzlesItCantDo() {
        FakeHost host = new FakeHost();
        RunPuzzles puzzles = new RunPuzzles(host, List.of(new RunPuzzles.Room(3, "tic_tac_toe", FRAME), new RunPuzzles.Room(5, "blaze", FRAME),
                new RunPuzzles.Room(6, "mystery", FRAME), new RunPuzzles.Room(7, "water_board", null)), PuzzleData.NONE);
        assertEquals(4, puzzles.count());
        assertEquals(4, puzzles.notDone());
        assertEquals(List.of(" ???: &7[&6&l✦&7]", " ???: &7[&6&l✦&7]", " ???: &7[&6&l✦&7]", " ???: &7[&6&l✦&7]"), puzzles.tabRows());
        assertEquals(4, host.log.size(), host.log.toString());
        assertTrue(host.log.get(0).contains("no data"), host.log.get(0));
        assertTrue(host.log.get(2).contains("not a known puzzle"), host.log.get(2));
        assertTrue(host.log.get(3).contains("wasn't pasted"), host.log.get(3));
    }

    /** A puzzle that does nothing by itself. */
    private static final class Plain extends Puzzle {
        int started;

        Plain(PuzzleHost host) {
            super(host, 9, FRAME, "Water Board");
        }

        @Override
        void start() {
            started++;
        }
    }

    @Test
    void foundThenSolved() {
        FakeHost host = new FakeHost();
        Plain puzzle = new Plain(host);
        puzzle.discover();
        puzzle.discover();
        assertEquals(1, puzzle.started);
        assertEquals(" ???: &7[&6&l✦&7]", puzzle.tabRow(), "the tab shows it a moment later");
        assertEquals(List.of(PuzzleTab.FOUND), host.run());
        assertEquals(" Water Board: &7[&6&l✦&7] ", puzzle.tabRow());
        puzzle.solve(PuzzleTab.AFTER_CHEST);
        assertEquals(List.of(9), host.solved);
        assertTrue(puzzle.isDone());
        assertEquals(List.of(PuzzleTab.AFTER_CHEST), host.run());
        assertEquals(" Water Board: &7[&a&l✔&7] ", puzzle.tabRow());
        // Over is over.
        puzzle.fail(named("Steve"), PuzzleTab.AFTER_LINE);
        assertEquals(List.of(), host.failed);
    }

    @Test
    void failed() {
        FakeHost host = new FakeHost();
        Plain puzzle = new Plain(host);
        puzzle.discover();
        host.run();
        puzzle.fail(named("Steve"), PuzzleTab.AFTER_LINE);
        assertEquals(List.of(9), host.failed);
        assertEquals(List.of(PuzzleTab.AFTER_LINE), host.run());
        assertEquals(" Water Board: &7[&c&l✖&7] &f(&bSteve&f)", puzzle.tabRow());
        assertTrue(puzzle.isOver());
        assertEquals(false, puzzle.isDone());
    }
}
