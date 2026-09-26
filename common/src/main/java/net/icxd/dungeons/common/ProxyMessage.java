package net.icxd.dungeons.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * A message between the proxy and a Paper server, sent as a plugin message on {@link #CHANNEL}
 * through a player's connection. The proxy drops anything a client sends on the channel, so a
 * server can trust what arrives on it.
 *
 * @param player   who it's about
 * @param argument depends on the kind; empty if unused
 */
public record ProxyMessage(Kind kind, UUID player, String argument) {
    public static final String CHANNEL = "skyblock:main";
    private static final int VERSION = 1;

    public enum Kind {
        /** Proxy to server: the player is about to move; save and release their data (argument: request id). */
        HANDOFF,
        /** Server to proxy: done (argument: the request id). */
        HANDED_OFF,
        /** Proxy to server: the move failed and the player is staying; take their data back. */
        RECLAIM,
        /**
         * Server to proxy: send the player somewhere (argument: a server type, like {@code LOBBY}, or
         * {@code server:NAME}).
         */
        SEND,
    }

    public ProxyMessage {
        if (argument == null) argument = "";
    }

    public byte[] encode() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(VERSION);
            out.writeUTF(kind.name());
            out.writeLong(player.getMostSignificantBits());
            out.writeLong(player.getLeastSignificantBits());
            out.writeUTF(argument);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** Null if it isn't a message this version understands. */
    public static ProxyMessage decode(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            if (in.readByte() != VERSION) return null;
            Kind kind = Kind.valueOf(in.readUTF());
            UUID player = new UUID(in.readLong(), in.readLong());
            return new ProxyMessage(kind, player, in.readUTF());
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }
}
