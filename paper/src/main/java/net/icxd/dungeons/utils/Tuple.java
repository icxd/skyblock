package net.icxd.dungeons.utils;

import lombok.Getter;

public record Tuple<T, U>(T first, U second) {

    @Override
    public String toString() {
        return "Tuple{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
