package net.icxd.dungeons.proxy;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;

/**
 * Remote console for the proxy, speaking the Source RCON protocol like Paper's, so the server
 * manager can run proxy commands (and shut it down cleanly) the same way it does on Paper servers.
 * Configured by {@code remote-console.properties} in the plugin's folder ({@code port},
 * {@code password}); only accepts connections from this machine.
 */
final class RemoteConsole {
    private static final int AUTH = 3;
    private static final int COMMAND = 2;
    private static final int RESPONSE = 0;
    private static final int AUTH_RESPONSE = 2;

    private final ProxyServer proxy;
    private final Logger logger;
    private final int port;
    private final byte[] password;
    private ServerSocket socket;

    private RemoteConsole(ProxyServer proxy, Logger logger, int port, String password) {
        this.proxy = proxy;
        this.logger = logger;
        this.port = port;
        this.password = password.getBytes(StandardCharsets.UTF_8);
    }

    /** Null if there's no config file, or it has no password. */
    static RemoteConsole load(ProxyServer proxy, Logger logger, Path dataDirectory) throws IOException {
        Path file = dataDirectory.resolve("remote-console.properties");
        if (!Files.exists(file)) return null;
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        }
        String password = properties.getProperty("password", "");
        if (password.isEmpty()) return null;
        return new RemoteConsole(proxy, logger, Integer.parseInt(properties.getProperty("port", "25575").trim()), password);
    }

    void start() throws IOException {
        socket = new ServerSocket(port, 8, InetAddress.getLoopbackAddress());
        Thread accept = new Thread(this::accept, "skyblock-remote-console");
        accept.setDaemon(true);
        accept.start();
        logger.info("Remote console listening on 127.0.0.1:{}", port);
    }

    void stop() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    private void accept() {
        while (!socket.isClosed()) {
            try {
                Socket client = socket.accept();
                Thread thread = new Thread(() -> serve(client), "skyblock-remote-console-client");
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                if (!socket.isClosed()) logger.warn("Remote console: {}", e.toString());
            }
        }
    }

    private void serve(Socket client) {
        try (client; DataInputStream in = new DataInputStream(client.getInputStream()); OutputStream out = client.getOutputStream()) {
            client.setSoTimeout((int) TimeUnit.MINUTES.toMillis(10));
            boolean authed = false;
            while (true) {
                int length = Integer.reverseBytes(in.readInt());
                if (length < 10 || length > 4110) return;
                byte[] packet = new byte[length];
                in.readFully(packet);
                ByteBuffer buffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
                int id = buffer.getInt();
                int type = buffer.getInt();
                String body = new String(packet, 8, length - 10, StandardCharsets.UTF_8);
                if (type == AUTH) {
                    authed = MessageDigest.isEqual(body.getBytes(StandardCharsets.UTF_8), password);
                    write(out, authed ? id : -1, AUTH_RESPONSE, "");
                    if (!authed) return;
                } else if (type == COMMAND && authed) {
                    write(out, id, RESPONSE, run(body));
                } else {
                    return;
                }
            }
        } catch (EOFException ignored) {
            // Client went away.
        } catch (IOException e) {
            logger.debug("Remote console client: {}", e.toString());
        }
    }

    /** Runs a proxy command and returns what it printed. */
    private String run(String command) {
        String name = command.trim().toLowerCase(Locale.ROOT);
        if (name.equals("shutdown") || name.equals("end") || name.equals("stop")) {
            // Velocity only takes these from its own console.
            Thread shutdown = new Thread(proxy::shutdown, "skyblock-remote-shutdown");
            shutdown.start();
            return "Shutting down the proxy\n";
        }
        Output output = new Output();
        try {
            proxy.getCommandManager().executeAsync(output, command).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            output.text.append(e.getMessage() == null ? e.toString() : e.getMessage()).append('\n');
        }
        return output.text.toString();
    }

    private static void write(OutputStream out, int id, int type, String body) throws IOException {
        byte[] text = body.getBytes(StandardCharsets.UTF_8);
        // Long output is cut to one packet; the server manager doesn't need more.
        int size = Math.min(text.length, 4096);
        ByteBuffer buffer = ByteBuffer.allocate(14 + size).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(10 + size).putInt(id).putInt(type).put(text, 0, size).put((byte) 0).put((byte) 0);
        out.write(buffer.array());
        out.flush();
    }

    /** Collects what a command sends back. Has every permission, like the console. */
    private static final class Output implements CommandSource {
        private final StringBuilder text = new StringBuilder();

        @Override
        public void sendMessage(Component message) {
            synchronized (text) {
                // Velocity's own messages are translatable; render them like the console does.
                Component rendered = GlobalTranslator.render(message, Locale.ENGLISH);
                text.append(PlainTextComponentSerializer.plainText().serialize(rendered)).append('\n');
            }
        }

        @Override
        public Tristate getPermissionValue(String permission) {
            return Tristate.TRUE;
        }
    }
}
