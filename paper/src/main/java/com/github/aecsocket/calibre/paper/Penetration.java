package com.github.aecsocket.calibre.paper;

import org.bukkit.NamespacedKey;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Collections;
import java.util.Map;

@ConfigSerializable
public record Penetration(
    Map<NamespacedKey, Double> hardness
) {
    public static final Penetration DEFAULT = new Penetration(Collections.emptyMap());

    public Instance instance() {
        return new Instance(this);
    }

    public static final class Instance {
        private final Penetration options;

        public Instance(Penetration options) {
            this.options = options;
        }


    }
}
