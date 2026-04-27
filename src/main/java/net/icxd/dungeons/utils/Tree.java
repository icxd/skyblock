package net.icxd.dungeons.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Tree<T>(Node<T> root) {

  public static final class Node<T> {
    private final T value;
    private final List<Node<T>> children;
    private final GrowthDirection direction;

    public Node(T value, List<Node<T>> children, GrowthDirection direction) {
      this.value = value;
      this.children = children;
      this.direction = direction;
    }

    public Node(T value, List<Node<T>> children) {
      this.value = value;
      this.children = children;
      this.direction = GrowthDirection.UP;
    }

    public Node(T value) {
      this.value = value;
      this.children = new ArrayList<>();
      this.direction = GrowthDirection.UP;
    }

    public T value() { return value; }
    public List<Node<T>> children() { return children; }
    public GrowthDirection direction() { return direction; }

    public void addChild(Node<T> value) { this.children.add(value); }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      Node<T> that = (Node<T>) obj;
      return Objects.equals(this.value, that.value) &&
          Objects.equals(this.children, that.children) &&
          Objects.equals(this.direction, that.direction);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, children, direction);
    }

    @Override
    public String toString() {
      return "Leaf[" +
          "value=" + value + ", " +
          "children=" + children + ", " +
          "direction=" + direction + ']';
    }
  }

  public enum GrowthDirection { UP, DOWN, LEFT, RIGHT }
}
