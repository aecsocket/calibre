package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.CalibrePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.map.MapFont;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public class PaperStatDisplaySystem extends StatDisplaySystem {
    private transient final CalibrePlugin plugin;
    private static final Map<String, String> padding = new HashMap<>();

    public PaperStatDisplaySystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public PaperStatDisplaySystem() { this(CalibrePlugin.getInstance()); }

    public PaperStatDisplaySystem(StatDisplaySystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public PaperStatDisplaySystem(PaperStatDisplaySystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override
    protected int listenerPriority() {
        return plugin.setting("system", ID, "listener_priority").getInt(1000);
    }

    @Override protected int getWidth(String text) {
        MapFont font = plugin.getFont();
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
        MapFont font = plugin.getFont();
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
