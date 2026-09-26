package net.icxd.dungeons.anticheat.check;

import net.icxd.dungeons.anticheat.check.combat.ReachCheck;

import java.util.List;

/** The checks there are; {@link CheckListener} runs them on each hit. */
public final class CheckHandler {
    public static final List<Check> checks = List.of(new ReachCheck());

    private CheckHandler() {
    }
}
