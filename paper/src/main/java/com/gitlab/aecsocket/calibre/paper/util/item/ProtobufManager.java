package com.gitlab.aecsocket.calibre.paper.util.item;

import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.paper.proto.Tree;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.paper.component.PaperSlot;
import com.gitlab.aecsocket.unifiedframework.core.util.log.LogLevel;
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
        InvalidDataHandling handling = plugin.setting(n -> n.get(InvalidDataHandling.class, InvalidDataHandling.CONTINUE), "invalid_data_handling");
        return read(msg, handling);
    }

    public PaperComponent read(Tree.Component msg, InvalidDataHandling handling) {
        PaperComponent component = plugin.registry().get(msg.getId(), PaperComponent.class);
        if (component == null) {
            switch (handling) {
                case WARN:
                    plugin.log(LogLevel.WARN, "Component [%s] does not exist", msg.getId());
                case CONTINUE:
                case REMOVE:
                    break;
            }
            return null;
        }
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

            PaperComponent child = read(value, handling);
            if (!slot.isCompatible(child)) {
                switch (handling) {
                    case WARN:
                        plugin.log(LogLevel.WARN, "On component [%s] the component [%s] is not compatible with slot [%s]", component.id(), child.id(), key);
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
