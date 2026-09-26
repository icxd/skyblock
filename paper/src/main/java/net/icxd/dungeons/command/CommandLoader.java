package net.icxd.dungeons.command;

import java.util.ArrayList;
import java.util.List;

public class CommandLoader {
    private final List<SCommand> commands = new ArrayList<SCommand>();

    public void register(SCommand command) {
        this.commands.add(command);
        command.register();
    }

    public int getCommandAmount() {
        return this.commands.size();
    }
}

