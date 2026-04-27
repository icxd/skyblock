package net.icxd.dungeons.dwarven;

import lombok.Getter;
import net.icxd.dungeons.utils.Tree;

@Getter
public class HeartOfTheMountain {
  private final Tree<Perk> tree;

  public HeartOfTheMountain() {
    Tree.Node<Perk> rootLeaf = new Tree.Node<>(Perk.MINING_SPEED);
    populateLeaf(rootLeaf);
    this.tree = new Tree<>(rootLeaf);
  }

  private void populateLeaf(Tree.Node<Perk> leaf) {
    leaf.value().getNextPerks().forEach(perk -> {
      Tree.Node<Perk> perkLeaf = new Tree.Node<>(perk);
      populateLeaf(perkLeaf);
      leaf.addChild(perkLeaf);
    });
  }
}
