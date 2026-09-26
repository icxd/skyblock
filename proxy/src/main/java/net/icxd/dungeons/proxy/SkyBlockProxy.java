package net.icxd.dungeons.proxy;

import java.io.IOException;
import java.nio.file.Path;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

/**
 * The Velocity side of the network: parties, queues and sending players between the hub and
 * dungeon servers will live here.
 */
@Plugin(id = "skyblock", name = "SkyBlock", version = "1.0-SNAPSHOT", authors = {"icxd"})
public final class SkyBlockProxy {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private RemoteConsole console;

    @Inject
    public SkyBlockProxy(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("SkyBlock proxy plugin enabled with {} backend servers", proxy.getAllServers().size());
        try {
            console = RemoteConsole.load(proxy, logger, dataDirectory);
            if (console != null) console.start();
        } catch (IOException | RuntimeException e) {
            logger.error("Couldn't start the remote console", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (console != null) console.stop();
    }
}
