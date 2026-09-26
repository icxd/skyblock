package net.icxd.dungeons.event;

import lombok.Getter;
import net.icxd.dungeons.gui.GUI;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.Inventory;

@Getter
public class GUIOpenEvent extends PlayerEvent implements Cancellable {
  private static final HandlerList handlers = new HandlerList();
  private final GUI opened;
  private final Inventory inventory;
  private boolean cancelled;

  public GUIOpenEvent(Player who, GUI opened, Inventory inventory) {
    super(who);
    this.opened = opened;
    this.inventory = inventory;
  }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
  @Override public HandlerList getHandlers() { return handlers; }
  public static HandlerList getHandlerList() { return handlers; }
}
