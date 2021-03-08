package com.gitlab.aecsocket.calibre.paper.system.builtin;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.system.builtin.ComponentContainerSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.google.protobuf.Any;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.paper.proto.system.SystemsBuiltin;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.BukkitItemUser;
import com.gitlab.aecsocket.unifiedframework.core.util.Quantifier;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public final class PaperComponentContainerSystem extends ComponentContainerSystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperComponentContainerSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    private PaperComponentContainerSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperComponentContainerSystem(PaperComponentContainerSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin calibre() { return plugin; }

    @Override
    protected <I extends Item> void remove(ItemEvents.ItemClick<I> event, Quantifier<CalibreComponent<I>> last) {
        super.remove(event, last);
        if (event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) event.user())::location, last.get().tree().stat("remove_sound"));
    }

    @Override
    protected <I extends Item> void insert(ItemEvents.ItemClick<I> event, int amount, I rawCursor, CalibreComponent<I> cursor) {
        super.insert(event, amount, rawCursor, cursor);
        if (event.user() instanceof BukkitItemUser)
            SoundData.play(((BukkitItemUser) event.user())::location, cursor.tree().stat("insert_sound"));
    }

    @Override public PaperComponentContainerSystem partialCopy() { return new PaperComponentContainerSystem(this); }

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
    public PaperComponentContainerSystem readProtobuf(Any raw) {
        SystemsBuiltin.ComponentContainerSystem msg = unpack(raw, SystemsBuiltin.ComponentContainerSystem.class);
        PaperComponentContainerSystem sys = new PaperComponentContainerSystem(this);
        msg.getComponentsList().forEach(quant ->
                sys.components.add(new Quantifier<>(plugin.itemManager().protobuf().read(quant.getComponent()).buildTree(), quant.getAmount())));
        return sys;
    }
}
