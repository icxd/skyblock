package net.icxd.dungeons.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.Rank;

/** Which commands are Sandbox tools is the owner's list: the item browser and the commands that edit the held item. */
class SandboxToolsTest {
    private static final Set<String> TOOLS = Set.of("AddEnchantmentCommand", "DataCommand", "ItemCommand", "NBTCommand",
            "RecombobulateCommand", "UnlockCommand", "UpgradeCommand");
    /** They reach other players or the shared world. */
    private static final Set<String> STAFF_ONLY = Set.of("PlayerDataCommand", "DungeonCommand", "SpawnEntityCommand", "SpawnRewardChestCommand");

    @Test
    void theSandboxToolsAreTheItemBrowserAndTheItemEdits() throws Exception {
        Map<String, CommandParameters> commands = commands();
        Set<String> sandbox = new TreeSet<>();
        commands.forEach((name, params) -> {
            if (params.sandbox()) sandbox.add(name);
        });
        assertEquals(new TreeSet<>(TOOLS), sandbox);
        // Staff may use them on any profile.
        for (String tool : TOOLS) assertEquals(Rank.STAFF, commands.get(tool).permission(), tool);
        for (String command : STAFF_ONLY) {
            assertNotNull(commands.get(command), command);
            assertEquals(Rank.STAFF, commands.get(command).permission(), command);
            assertFalse(commands.get(command).sandbox(), command);
        }
    }

    /** Every command there is (each class under command.commands), by class name. */
    private static Map<String, CommandParameters> commands() throws Exception {
        // Where SCommand was loaded from (the tests have a command.commands of their own).
        Path classes = Path.of(SCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String pkg = SCommand.class.getPackageName() + ".commands.";
        Path dir = classes.resolve(pkg.replace('.', File.separatorChar));
        Map<String, CommandParameters> out = new TreeMap<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                String relative = dir.relativize(file).toString();
                String name = pkg + relative.substring(0, relative.length() - ".class".length()).replace(File.separatorChar, '.');
                Class<?> type = Class.forName(name, false, SCommand.class.getClassLoader());
                if (!SCommand.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) continue;
                CommandParameters params = type.getAnnotation(CommandParameters.class);
                assertNotNull(params, type.getSimpleName() + " has no @CommandParameters");
                out.put(type.getSimpleName(), params);
            }
        }
        return out;
    }
}
