package net.icxd.dungeons.shop;

import net.icxd.dungeons.item.cost.coins.CoinCost;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Shops' coin costs less a player's discount (the Shady Ring's line). */
class ShopDiscountTest {
    @Test
    void discounted() {
        assertEquals(99_000, Shop.discounted(100_000, 1));
        assertEquals(97_000, Shop.discounted(100_000, 3));
        // Rounded up to a whole coin.
        assertEquals(98, Shop.discounted(101, 3));
        assertEquals(0, Shop.discounted(100, 100));
    }

    /** Nobody to discount for: the ware's own costs. */
    @Test
    void noViewer() {
        Shop.Ware ware = new Shop.Ware("TEST", 1, 0, false, List.of(new CoinCost(500)));
        assertSame(ware.costs(), Shop.costs(ware, null));
    }
}
