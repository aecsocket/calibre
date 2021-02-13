package me.aecsocket.calibre.util.item;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.component.PaperSlot;
import me.aecsocket.calibre.proto.Tree;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.unifiedframework.util.log.LogLevel;
import org.spongepowered.configurate.serialize.SerializationException;

public class ProtobufManager {
    public enum InvalidDataHandling {
        CONTINUE,
        REMOVE,
        WARN
    }

    private final CalibrePlugin plugin;

    public ProtobufManager(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin plugin() { return plugin; }

    public PaperComponent read(byte[] bytes) throws ComponentCreationException {
        try {
            return read(Tree.Component.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            throw new ComponentCreationException(e, null);
        }
    }

    public PaperComponent read(Tree.Component msg) {
        InvalidDataHandling handling = InvalidDataHandling.CONTINUE;
        try {
            handling = plugin.setting("invalid_data_handling").get(InvalidDataHandling.class, handling);
        } catch (SerializationException e) {
            plugin.log(LogLevel.WARN, e, "Invalid `invalid_data_handling` key specified, defaulting to %s", handling.name());
        }

        return read(msg, handling);
    }

    public PaperComponent read(Tree.Component msg, InvalidDataHandling handling) {
        PaperComponent component = plugin.registry().get(msg.getId(), PaperComponent.class);
        if (component == null)
            throw new ComponentCreationException("Component [" + msg.getId() + "] does not exist", msg);
        component = component.copy();

        for (var entry : msg.getSlotsMap().entrySet()) {
            String key = entry.getKey();
            Tree.Component value = entry.getValue();

            PaperSlot slot = component.slot(key);
            if (slot == null) {
                switch (handling) {
                    case WARN:
                        plugin.log(LogLevel.WARN, "Component [%s] does not have slot [%s]", component.id(), key);
                    case CONTINUE:
                        component.invalidSlots().put(key, value);
                        break;
                    case REMOVE:
                        break;
                }
                continue;
            }

            slot.set(read(value, handling));
        }

        // TODO systems
        for (var entry : msg.getSystemsMap().entrySet()) {
            String key = entry.getKey();
            Any value = entry.getValue();

            PaperSystem system = component.system(key);
            if (system == null) {
                switch (handling) {
                    case WARN:
                        plugin.log(LogLevel.WARN, "Component [%s] does not have system [%s]", component.id(), key);
                    case CONTINUE:
                        component.invalidSystems().put(key, value);
                        break;
                    case REMOVE:
                        break;
                }
                continue;
            }

            component.system(system.readProtobuf(value));
        }
        return component;
    }

    public Tree.Component write(PaperComponent component) {
        Tree.Component.Builder msg = Tree.Component.newBuilder();
        msg.setId(component.id());

        msg.putAllSlots(component.invalidSlots());
        for (var entry : component.<CalibreSlot>slots().entrySet()) {
            CalibreSlot slot = entry.getValue();
            if (slot.get() != null) {
                msg.putSlots(entry.getKey(), write(slot.get()));
            }
        }

        // TODO systems
        msg.putAllSystems(component.invalidSystems());
        for (var entry : component.<PaperSystem>systems().entrySet()) {
            msg.putSystems(entry.getKey(), entry.getValue().writeProtobuf());
        }

        return msg.build();
    }
}
