package me.aecsocket.calibre.defaults;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

public class ActionSystem implements CalibreSystem<ActionSystem> {
    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private long nextAvailable;

    public ActionSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "action"; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) { this.parent = parent; }

    public long getNextAvailable() { return nextAvailable; }
    public void setNextAvailable(long nextAvailable) { this.nextAvailable = nextAvailable; }
    public long availableIn(long delay) { nextAvailable = System.currentTimeMillis() + delay; return nextAvailable; }
    public boolean isAvailable() { return System.currentTimeMillis() >= nextAvailable; }

    public void startAction(Long delay, Location location, SoundData[] sound, ParticleData[] particles, Object particleData, Player player, EquipmentSlot slot, Animation animation) {
        SoundData.play(location, sound);
        if (particleData == null)
            ParticleData.spawn(location, particles);
        else
            ParticleData.spawn(location, particleData, particles);
        if (animation != null) plugin.getPlayerData(player).startAnimation(animation, slot);
        if (delay != null) availableIn(delay);
    }

    public void startAction(Long delay, Location location, SoundData[] sound, ParticleData[] particles, Player player, EquipmentSlot slot, Animation animation) {
        startAction(delay, location, sound, particles, null, player, slot, animation);
    }

    public void startAction(Long delay, Location location, SoundData[] sound, ParticleData[] particles, Object particleData) {
        startAction(delay, location, sound, particles, particleData, null, null, null);
    }

    public void startAction(Long delay, Location location, SoundData[] sound, ParticleData[] particles) {
        startAction(delay, location, sound, particles, null);
    }

    public void startAction(Long delay) {
        startAction(delay, null, null, null);
    }

    @Override public TypeToken<ActionSystem> getDescriptorType() { return new TypeToken<>(){}; }

    @Override
    public void acceptDescriptor(ActionSystem descriptor) {
        nextAvailable = descriptor.nextAvailable;
    }

    @Override public ActionSystem createDescriptor() { return this; }

    @Override public ActionSystem clone() { try { return (ActionSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public ActionSystem copy() { return clone(); }
}
