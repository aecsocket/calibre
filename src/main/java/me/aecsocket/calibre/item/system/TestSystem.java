package me.aecsocket.calibre.item.system;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.BooleanStat;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

// TODO remove from prod
public class TestSystem implements CalibreSystem<TestSystem>, ItemEvents.Hold.Listener, ItemEvents.Interact.Listener {
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("number_stat", new NumberStat.Int(3))
            .init("bool_stat", new BooleanStat(false))
            .get();

    private transient CalibreComponent parent;
    private ArrayList<Integer> field;

    @Override public String getId() { return "test"; }
    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    @Override public void setParent(CalibreComponent parent) { this.parent = parent; }
    public CalibreComponent getParent() { return parent; }

    @Override
    public void registerListeners(EventDispatcher dispatcher) {
        dispatcher.registerListener(ItemEvents.Hold.class, this, 0);
        dispatcher.registerListener(ItemEvents.Interact.class, this, 0);
    }

    @Override
    public void onHold(ItemStack itemStack, Player player, EquipmentSlot hand) {
        player.sendActionBar("Field = " + field + " | Root? " + parent.isRoot());
    }

    @Override
    public void onInteract(ItemStack itemStack, Player player, EquipmentSlot hand, BlockFace clickedFace, Block clickedBlock) {
        player.sendMessage("Interact w/ " + hand);
    }

    @Override public TypeToken<TestSystem> getDescriptorType() { return new TypeToken<>(){}; }
    @Override public void acceptDescriptor(TestSystem descriptor) { this.field = descriptor.field; }
    @Override public TestSystem createDescriptor() { return this; }

    @Override public TestSystem clone() { try { return (TestSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public TestSystem copy() {
        TestSystem copy = clone();
        copy.field = field == null ? null : new ArrayList<>(field);
        return copy;
    }
}
