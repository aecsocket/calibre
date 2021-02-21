package com.gitlab.aecsocket.calibre.paper.system;

import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import net.kyori.adventure.text.Component;

public interface PaperSystem extends CalibreSystem {
    CalibrePlugin plugin();

    @Override
    default Component gen(String locale, String key, Object... args) {
        return plugin().gen(locale, key, args);
    }

    default <M extends Message> M unpack(Any raw, Class<M> type) {
        try {
            return raw.unpack(type);
        } catch (InvalidProtocolBufferException e) {
            throw new InvalidMessageTypeException(type);
        }
    }
    default Any writeProtobuf() { return Any.getDefaultInstance(); }
    default PaperSystem readProtobuf(Any message) { return (PaperSystem) copy(); }
}
