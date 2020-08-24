package me.aecsocket.calibre.item;

import me.aecsocket.unifiedframework.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

public final class ItemEvents {
    private ItemEvents() {}

    public static class Hold implements Event<Hold.Listener> {
        public interface Listener { void onHold(Player player, EquipmentSlot hand); }

        private final Player player;
        private final EquipmentSlot hand;

        public Hold(Player player, EquipmentSlot hand) {
            this.player = player;
            this.hand = hand;
        }

        public Player getPlayer() { return player; }
        public EquipmentSlot getHand() { return hand; }

        @Override public void send(Listener listener) { listener.onHold(player, hand); }
    }
}
