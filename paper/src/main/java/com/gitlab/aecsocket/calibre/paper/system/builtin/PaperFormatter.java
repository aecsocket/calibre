package com.gitlab.aecsocket.calibre.paper.system.builtin;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.builtin.Formatter;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Locale;

public final class PaperFormatter {
    private PaperFormatter() {}

    public static class NumberDescriptorFormatter<N extends Number, T extends NumberDescriptor<N>> extends Formatter.NumberDescriptorFormatter<N, T> {
        private final CalibrePlugin plugin;

        public NumberDescriptorFormatter(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin plugin() { return plugin; }

        @Override
        protected Component bar(Locale locale, String key, double percent) {
            return plugin.bar(locale, key, percent, 0, plugin.setting(ConfigurationNode::getInt, "stat_formatter", "bar_widths", 0));
        }

        @Override
        protected Component gen(Locale locale, String key, Object... args) {
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
        protected Component bar(Locale locale, String key, double percent) {
            return plugin.bar(locale, key, percent, 0, plugin.setting(ConfigurationNode::getInt, "stat_formatter", "bar_widths", 1));
        }

        @Override
        protected Component gen(Locale locale, String key, Object... args) {
            return plugin.gen(locale, key, args);
        }
    }
}
