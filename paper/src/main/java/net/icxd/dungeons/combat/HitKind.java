package net.icxd.dungeons.combat;

/**
 * What dealt a player's damage to a mob (see {@link Combat#addHitListener} and the death event's killing
 * blow). The first four are hits: they tell the hit listeners. The last two are an effect's own damage
 * (a damage over time's tick, Cleave's share, a lightning strike), which tells none, so an effect can't
 * set itself off again (whether Hypixel's effects set off other on-hit effects is UNKNOWN: they don't).
 */
public enum HitKind {
    /** A melee hit, or a fist's. */
    MELEE,
    /** A player's arrow landing (a bow's, a shortbow's). */
    ARROW,
    /** One of Ferocity's extra strikes after a melee hit or an arrow. */
    FEROCITY,
    /** An ability's hit ({@code Hits.hurt}: magic damage, or a hit an ability works out as a melee one). */
    ABILITY,
    /** A damage over time's tick (see {@link MobDebuffs#dot}). */
    DOT,
    /** Any other effect's damage (see {@link MobHits#deal}). */
    OTHER;

    /** Whether it's a hit, which the hit listeners hear of. */
    public boolean isHit() {
        return this != DOT && this != OTHER;
    }
}
