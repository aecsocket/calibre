package com.gitlab.aecsocket.calibre.paper.system.builtin;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.Formatter;
import com.gitlab.aecsocket.calibre.core.system.builtin.StatDisplaySystem;
import org.bukkit.map.MapFont;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@ConfigSerializable
public class PaperStatDisplaySystem extends StatDisplaySystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient final CalibrePlugin plugin;
    private static final Map<String, String> padding = new HashMap<>();

    /**
     * Used for registration.
     * @param plugin The plugin.
     * @param formatSupplier The function which generates formatters for specific stat types.
     */
    public PaperStatDisplaySystem(CalibrePlugin plugin, Function<Class<?>, Formatter<?>> formatSupplier) {
        super(formatSupplier);
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public PaperStatDisplaySystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperStatDisplaySystem(PaperStatDisplaySystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

    @Override protected int getWidth(String text) {
        MapFont font = plugin.font();
        if (!font.isValid(text))
            throw new IllegalArgumentException("Invalid characters in text: not added to font map?");
        return font.getWidth(text);
    }

    private String padding(String locale) {
        return padding.computeIfAbsent(locale, __ -> plugin.setting("symbol", "pad").getString(" "));
    }

    @Override
    protected String pad(String locale, int width) {
        String padding = padding(locale);
        MapFont font = plugin.font();
        if (font.isValid(padding)) {
            String pad = "";
            String nextPad = padding;
            while (font.getWidth(nextPad) < width) {
                pad = nextPad;
                nextPad += padding;
            }
            return pad;
        }
        return padding.repeat(width);
    }

    @Override public PaperStatDisplaySystem copy() { return new PaperStatDisplaySystem(this); }
}
