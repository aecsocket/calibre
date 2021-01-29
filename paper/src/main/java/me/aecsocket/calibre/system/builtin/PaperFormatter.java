package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import net.kyori.adventure.text.Component;

public final class PaperFormatter {
    private PaperFormatter() {}

    public static class NumberDescriptorFormatter<N extends Number, T extends NumberDescriptor<N>> extends Formatter.NumberDescriptorFormatter<N, T> {
        private final CalibrePlugin plugin;

        public NumberDescriptorFormatter(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin plugin() { return plugin; }

        @Override
        protected Component bar(String locale, String key, double percent) {
            return plugin.bar(locale, key, percent, 0, plugin.setting("system", StatDisplaySystem.ID, "bar_widths").node(0).getInt());
        }

        @Override
        protected Component gen(String locale, String key, Object... args) {
            return plugin.gen(locale, key, args);
        }
    }

    public static class Vector2DDescriptorFormatter extends Formatter.Vector2DDescriptorFormatter {
        private final CalibrePlugin plugin;

        public Vector2DDescriptorFormatter(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin plugin() { return plugin; }

        @Override
        protected Component bar(String locale, String key, double percent) {
            return plugin.bar(locale, key, percent, 0, plugin.setting("system", StatDisplaySystem.ID, "bar_widths").node(1).getInt());
        }

        @Override
        protected Component gen(String locale, String key, Object... args) {
            return plugin.gen(locale, key, args);
        }
    }
}
