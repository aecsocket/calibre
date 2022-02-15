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

import java.util.HashMap;
import java.util.Map;

public final class CalibrePlugin extends BasePlugin<CalibrePlugin> {
    private final PaperScheduler scheduler = new PaperScheduler(this);
    private final PaperEffectors effectors = new PaperEffectors(this);
    private final Map<Player, PlayerData> playerData = new HashMap<>();
    private Explosions explosions;
    private Penetration penetration;
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
        explosions = setting(Explosions.DEFAULT, (n, d) -> n.get(Explosions.class, d), "explosions");
        penetration = setting(Penetration.DEFAULT, (n, d) -> n.get(Penetration.class, d), "penetration");
        raycastBuilder = PaperRaycast.builder(); // TODO raycast options
    }

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }
}
