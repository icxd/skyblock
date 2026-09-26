package net.icxd.dungeons.utils;

/** Text shown in place of part of the action bar (defense or mana) until a moment passes. */
public record Replacement(String text, long until) {
    /** For the next {@code millis}. */
    public static Replacement forMillis(String text, long millis) {
        return new Replacement(text, System.currentTimeMillis() + millis);
    }

    public boolean expired() {
        return System.currentTimeMillis() >= until;
    }
}
