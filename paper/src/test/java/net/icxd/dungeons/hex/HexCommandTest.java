package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.commands.user.HexCommand;
import net.icxd.dungeons.common.Rank;

/** {@code /hex}: everyone's, not a Sandbox tool (a Sandbox profile's is free in the Hex itself). */
class HexCommandTest {
    @Test
    void everyonesAndNotASandboxTool() {
        CommandParameters params = HexCommand.class.getAnnotation(CommandParameters.class);
        assertEquals(Rank.DEFAULT, params.permission());
        assertFalse(params.sandbox());
        assertEquals("hecks,thehex", params.aliases());
    }
}
