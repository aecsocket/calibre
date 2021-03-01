package com.gitlab.aecsocket.calibre.paper;

import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.function.Consumer;

public interface CalibreHook {
    default void serverLoad() {}

    default void calibreDisable() {}

    default void registerSerializers(TypeSerializerCollection.Builder builder) {}

    default void preLoad() {}

    default void postLoad() {}
}
