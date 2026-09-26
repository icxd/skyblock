package net.icxd.dungeons.dungeons.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.icxd.dungeons.dungeons.generation.room.Room;

/**
 * Stage 3: picks the final template + rotation for every room out of the options the connector
 * left it. Templates are unique per dungeon where possible (you never get the same room twice in
 * one run): that's a bipartite matching between rooms and templates, solved with Kuhn's algorithm
 * in random order so the result is still random. Rooms that can't get a unique template (the pool
 * is too small) fall back to a repeat.
 */
final class TemplateAssigner {
  private final List<RoomNode> rooms;
  private final Random random;

  private final Map<Room, Integer> owner = new HashMap<>();
  private final List<List<Room>> choices = new ArrayList<>();
  private boolean[] seen;

  TemplateAssigner(List<RoomNode> rooms, Random random) {
    this.rooms = rooms;
    this.random = random;
  }

  /** @return chosen placement per room id; {@code repeats[0]} receives the number of duplicates. */
  Placement[] assign(int[] repeats) {
    for (RoomNode room : rooms) {
      Set<Room> distinct = new LinkedHashSet<>();
      for (Placement p : room.options) if (p.fits(room.doors)) distinct.add(p.template());
      List<Room> list = new ArrayList<>(distinct);
      Collections.shuffle(list, random);
      choices.add(list);
    }

    // Most constrained rooms first, so a picky room doesn't lose its only template to a flexible one.
    List<Integer> order = new ArrayList<>();
    for (int i = 0; i < rooms.size(); i++) order.add(i);
    Collections.shuffle(order, random);
    order.sort((a, b) -> Integer.compare(choices.get(a).size(), choices.get(b).size()));

    for (int id : order) {
      seen = new boolean[rooms.size()];
      augment(id);
    }

    Map<Integer, Room> byRoom = new HashMap<>();
    owner.forEach((template, roomId) -> byRoom.put(roomId, template));

    Placement[] result = new Placement[rooms.size()];
    for (RoomNode room : rooms) {
      Room template = byRoom.get(room.id);
      if (template == null) {
        template = choices.get(room.id).get(0);
        repeats[0]++;
      }
      List<Placement> rotations = new ArrayList<>();
      for (Placement p : room.options) {
        if (p.template() == template && p.fits(room.doors)) rotations.add(p);
      }
      result[room.id] = rotations.get(random.nextInt(rotations.size()));
    }
    return result;
  }

  private boolean augment(int roomId) {
    for (Room template : choices.get(roomId)) {
      Integer current = owner.get(template);
      if (current != null && seen[current]) continue;
      if (current == null) {
        owner.put(template, roomId);
        return true;
      }
      seen[current] = true;
      if (augment(current)) {
        owner.put(template, roomId);
        return true;
      }
    }
    return false;
  }
}
