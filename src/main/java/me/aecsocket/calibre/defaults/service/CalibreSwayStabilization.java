package me.aecsocket.calibre.defaults.service;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.calibre.util.CalibrePlayer;
import me.aecsocket.calibre.util.CalibreProtocol;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import me.aecsocket.unifiedframework.util.Vector2;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface CalibreSwayStabilization extends CalibreInbuilt {
    boolean stabilize(ItemUser user, Vector2 sway, TickContext tickContext);

    class Provider implements CalibreSwayStabilization, Tickable {
        private final CalibrePlugin plugin;

        public Provider(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin getPlugin() { return plugin; }

        @Override
        public void enable() {
            plugin.getSchedulerLoop().registerTickable(this);
        }

        @Override
        public void disable() {
            plugin.getSchedulerLoop().unregisterTickable(this);
        }

        @Override
        public boolean stabilize(ItemUser user, Vector2 sway, TickContext tickContext) {
            if (!(user instanceof PlayerItemUser)) return false;
            Player player = ((PlayerItemUser) user).getEntity();
            if (!player.isSneaking()) return false;
            CalibrePlayer data = plugin.getPlayerData(player);
            if (data.getStamina() <= 0 || !data.canUseStamina()) return false;
            data.setStamina(Math.max(0,
                    data.getStamina() - (int) (tickContext.getPeriod() * plugin.setting("service.sway_stabilization.loss_rate_multiplier", double.class, 1d))
            ));
            if (data.getStamina() == 0)
                data.setCanUseStamina(false);
            data.setLastStaminaDrain(System.currentTimeMillis());
            return true;
        }

        @Override
        public void tick(TickContext tickContext) {
            for (CalibrePlayer data : plugin.getPlayers().values()) {
                Player player = data.getPlayer();

                if (data.getStamina() == -1)
                    data.setStamina(plugin.setting("service.sway_stabilization.stamina", int.class, 4000));
                if (data.getMaxStamina() == -1)
                    data.setMaxStamina(plugin.setting("service.sway_stabilization.max_stamina", int.class, 4000));

                if (!data.canUseStamina() && !player.isSneaking())
                    data.setCanUseStamina(true);

                if (
                        data.getStamina() < data.getMaxStamina()
                        && !player.isSwimming()
                        && player.getEyeLocation().getBlock().getType() != Material.WATER
                ) {
                    CalibreProtocol.sendAir(player, (double) data.getStamina() / data.getMaxStamina());
                    if (data.getLastStaminaDrain() + plugin.setting("service.sway_stabilization.regen_start_time", long.class, 1000L) < System.currentTimeMillis())
                        data.setStamina(Math.min(data.getMaxStamina(),
                                data.getStamina() + (int) (tickContext.getPeriod() * plugin.setting("service.sway_stabilization.regen_rate_multiplier", double.class, 1.5d))
                        ));
                }
            }
        }
    }
}
