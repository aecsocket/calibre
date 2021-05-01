package com.gitlab.aecsocket.calibre.paper.system.builtin;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.Formatter;
import com.gitlab.aecsocket.calibre.core.system.builtin.StatDisplaySystem;
import io.papermc.paper.text.PaperComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.map.MapFont;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@ConfigSerializable
public final class PaperStatDisplaySystem extends StatDisplaySystem implements PaperSystem {
    @FromMaster(fromDefault = true)
    private transient final CalibrePlugin plugin;
    private static final Map<Locale, String> padding = new HashMap<>();

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
    private PaperStatDisplaySystem() {
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

    @Override public CalibrePlugin calibre() { return plugin; }

    @Override protected int getWidth(Component component) {
        String text = PlainComponentSerializer.plain().serialize(component);
        MapFont font = plugin.font();
        if (!font.isValid(text))
            throw new IllegalArgumentException("Invalid characters in text: not added to font map?");
        return font.getWidth(text);
    }

    private String padding(Locale locale) {
        return padding.computeIfAbsent(locale, __ -> plugin.setting(n -> n.getString(" "), "symbol", "pad"));
    }

    @Override
    protected String pad(Locale locale, int width) {
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
