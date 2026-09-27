package net.icxd.dungeons.dungeons.instance.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.junit.jupiter.api.Test;

/** What {@link PuzzleEvents} counts on from Paper. */
class PuzzleEventsTest {
    /**
     * The server fires a PlayerInteractAtEntityEvent for every right click on an entity, and it only
     * reaches the PlayerInteractEntityEvent listener while it shares that event's handler list. Should
     * it get one of its own, clicks on the Weirdos would go unheard.
     */
    @Test
    void anEntityClickReachesTheEntityClickListener() throws NoSuchMethodException {
        assertEquals(PlayerInteractEntityEvent.class,
                PlayerInteractAtEntityEvent.class.getMethod("getHandlerList").getDeclaringClass());
    }
}
