package net.icxd.dungeons.command.exceptions;

public class CommandFailException extends RuntimeException {
    public CommandFailException(String message) {
        super(message);
    }
}
