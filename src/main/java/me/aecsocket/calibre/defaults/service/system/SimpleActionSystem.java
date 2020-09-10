package me.aecsocket.calibre.defaults.service.system;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * A simple implementation of an {@link ActionSystem}.
 */
public class SimpleActionSystem implements CalibreSystem<SimpleActionSystem>, ActionSystem {
    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private long nextAvailable;

    public SimpleActionSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "action"; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) { this.parent = parent; }

    public long getNextAvailable() { return nextAvailable; }
    public void setNextAvailable(long nextAvailable) { this.nextAvailable = nextAvailable; }
    @Override public void availableIn(long delay) { nextAvailable = System.currentTimeMillis() + delay; }
    @Override public boolean isAvailable() { return System.currentTimeMillis() >= nextAvailable; }

    @Override
    public @Nullable Collection<Class<?>> getServiceTypes() {
        return Arrays.asList(
                ActionSystem.class
        );
    }

    @Override
    public void startAction(Long delay,
                            Location location, SoundData[] sound, ParticleData[] particles, Object particleData,
                            LivingEntity entity, EquipmentSlot slot, Animation animation) {
        SoundData.play(location, sound);
        if (particleData == null)
            ParticleData.spawn(location, particles);
        else
            ParticleData.spawn(location, particleData, particles);
        if (entity instanceof Player)
            if (animation != null) plugin.getPlayerData((Player) entity).startAnimation(animation, slot);
        if (delay != null) availableIn(delay);
    }

    @Override public TypeToken<SimpleActionSystem> getDescriptorType() { return new TypeToken<>(){}; }

    @Override
    public void acceptDescriptor(SimpleActionSystem descriptor) {
        nextAvailable = descriptor.nextAvailable;
    }

    @Override public SimpleActionSystem createDescriptor() { return this; }

    @Override public SimpleActionSystem clone() { try { return (SimpleActionSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public SimpleActionSystem copy() { return clone(); }
}
