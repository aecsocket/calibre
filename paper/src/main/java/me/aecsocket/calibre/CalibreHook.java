package me.aecsocket.calibre;

import me.aecsocket.calibre.util.CalibreRegistry;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

public interface CalibreHook {
    default void onEnable(CalibrePlugin plugin) {}

    default void postEnable() {}

    default void onDisable() {}

    default void registerSerializers(TypeSerializerCollection.Builder builder) {}

    default void onRegistryLoad(CalibreRegistry registry) {}
}
