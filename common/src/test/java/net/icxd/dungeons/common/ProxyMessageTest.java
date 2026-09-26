package net.icxd.dungeons.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ProxyMessageTest {
    @Test
    void roundTrips() {
        UUID player = UUID.randomUUID();
        for (ProxyMessage.Kind kind : ProxyMessage.Kind.values()) {
            ProxyMessage message = new ProxyMessage(kind, player, "server:dungeon01");
            assertEquals(message, ProxyMessage.decode(message.encode()));
        }
        assertEquals("", ProxyMessage.decode(new ProxyMessage(ProxyMessage.Kind.RECLAIM, player, null).encode()).argument());
    }

    @Test
    void ignoresWhatItDoesntUnderstand() {
        assertNull(ProxyMessage.decode(new byte[0]));
        assertNull(ProxyMessage.decode(new byte[] {9, 0, 0}));
        byte[] data = new ProxyMessage(ProxyMessage.Kind.SEND, UUID.randomUUID(), "LOBBY").encode();
        data[3] = 'X'; // the kind's name no longer matches one
        assertNull(ProxyMessage.decode(data));
    }
}
