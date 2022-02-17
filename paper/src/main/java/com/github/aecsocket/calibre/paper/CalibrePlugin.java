package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.Ticks;
import com.github.aecsocket.minecommons.core.scheduler.Task;
import com.github.aecsocket.minecommons.paper.effect.PaperEffectors;
import com.github.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.github.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CalibrePlugin extends BasePlugin<CalibrePlugin> {
    private static final Map<Locale, DecimalFormat> DECIMAL_FORMATS = new HashMap<>();

    private final PaperScheduler scheduler = new PaperScheduler(this);
    private final PaperEffectors effectors = new PaperEffectors(this);
    private final Map<Player, PlayerData> playerData = new HashMap<>();
    private final Explosions explosions = new Explosions(this);
    private final Penetration penetration = new Penetration(this);
    private PaperRaycast.Builder raycastBuilder;

    public PaperScheduler scheduler() { return scheduler; }
    public PaperEffectors effectors() { return effectors; }
    public Explosions explosions() { return explosions; }
    public Penetration penetration() { return penetration; }
    public PaperRaycast.Builder raycastBuilder() { return raycastBuilder; }

    public PlayerData playerData(Player player) {
        return playerData.computeIfAbsent(player, k -> new PlayerData(this, k));
    }

    @Override
    public void onEnable() {
        super.onEnable();

        scheduler.run(Task.repeating(ctx -> {
            var iter = playerData.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                if (entry.getKey().isValid()) {
                    ctx.run(Task.single(entry.getValue()::tick));
                } else
                    iter.remove();
            }
        }, Ticks.MSPT));
    }

    public void damage(LivingEntity entity, double damage, @Nullable LivingEntity source) {
        if (entity == source)
            entity.damage(damage);
        else
            entity.damage(damage, source);
    }

    @Override
    public void load() {
        super.load();
        explosions.load();
        penetration.load();
        raycastBuilder = PaperRaycast.builder(); // TODO raycast options
        for (var data : playerData.values()) {
            data.load();
        }
    }

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }

    public static String format2(Locale locale, double value) {
        DecimalFormat format = DECIMAL_FORMATS.computeIfAbsent(locale, k -> new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(k)));
        return format.format(value);
    }

    public static String format2Raw(Locale locale, double value) {
        return String.format(locale, "%.2f", value);
    }
}
