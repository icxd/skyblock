package net.icxd.dungeons.dungeons.generator.utils;

import java.util.List;

public class Random {
  public static <T> T random(List<T> ts) {
    return ts.get(new java.util.Random().nextInt(ts.size()));
  }
}
