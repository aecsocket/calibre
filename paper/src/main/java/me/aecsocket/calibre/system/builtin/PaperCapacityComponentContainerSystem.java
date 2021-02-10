package me.aecsocket.calibre.system.builtin;

import com.google.protobuf.Any;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.proto.system.SystemsBuiltin;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.ItemEvents;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.unifiedframework.util.Quantifier;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class PaperCapacityComponentContainerSystem extends CapacityComponentContainerSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperCapacityComponentContainerSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public PaperCapacityComponentContainerSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperCapacityComponentContainerSystem(PaperCapacityComponentContainerSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override
    protected <I extends Item> void remove(ItemEvents.Click<I> event, Quantifier<CalibreComponent<I>> last) {
        super.remove(event, last);
        if (event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) event.user())::location, last.get().tree().stat("remove_sound"));
    }

    @Override
    protected <I extends Item> void insert(ItemEvents.Click<I> event, int amount, I rawCursor, CalibreComponent<I> cursor) {
        super.insert(event, amount, rawCursor, cursor);
        if (event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) event.user())::location, cursor.tree().stat("insert_sound"));
    }

    @Override public PaperCapacityComponentContainerSystem partialCopy() { return new PaperCapacityComponentContainerSystem(this); }

    @Override
    public Any writeProtobuf() {
        var builder = SystemsBuiltin.ComponentContainerSystem.newBuilder();
        components.forEach(quant ->
                builder.addComponents(SystemsBuiltin.ComponentQuantifier.newBuilder()
                        .setComponent(plugin.itemManager().protobuf().write((PaperComponent) quant.get().buildTree()))
                        .setAmount(quant.getAmount())
                ));
        return Any.pack(builder.build());
    }

    @Override
    public PaperCapacityComponentContainerSystem readProtobuf(Any raw) {
        SystemsBuiltin.ComponentContainerSystem msg = unpack(raw, SystemsBuiltin.ComponentContainerSystem.class);
        PaperCapacityComponentContainerSystem sys = new PaperCapacityComponentContainerSystem(this);
        msg.getComponentsList().forEach(quant ->
                sys.components.add(new Quantifier<>(plugin.itemManager().protobuf().read(quant.getComponent()).buildTree(), quant.getAmount())));
        return sys;
    }
}
