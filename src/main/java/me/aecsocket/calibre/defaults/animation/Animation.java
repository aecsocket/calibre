package me.aecsocket.calibre.defaults.animation;

import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Animation {
    public class Instance implements Tickable {
        private final Player player;
        private int index;
        private Frame frame;
        private long frameTime;

        public Instance(Player player, int index, Frame frame) {
            this.player = player;
            this.index = index;
            this.frame = frame;
        }

        public Instance(Player player) {
            this.player = player;
            updateFrame();
        }

        public Player getPlayer() { return player; }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public Frame getFrame() { return frame; }
        public void setFrame(Frame frame) { this.frame = frame; }
        public Frame updateFrame() { frame = isFinished() ? null : frames.get(index); return frame; }

        public long getFrameTime() { return frameTime; }
        public void setFrameTime(long frameTime) { this.frameTime = frameTime; }

        public boolean isFinished() { return index < 0 || index >= frames.size(); }

        @Override
        public void tick(TickContext tickContext) {
            if (frame == null) return;
            frameTime += tickContext.getPeriod();
            if (frameTime >= frame.duration)
                nextFrame();
        }

        public void apply() {
            if (frame != null) frame.apply();
        }

        public void nextFrame() {
            ++index;
            frameTime = 0;
            updateFrame();
            if (frame == null) return;
            frame.apply();
        }
    }

    public static class Frame {
        private long duration;

        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }

        public void apply() {

        }
    }

    private List<Frame> frames = new ArrayList<>();

    public Animation(List<Frame> frames) {
        this.frames = frames;
    }

    public Animation() {}

    public List<Frame> getFrames() { return frames; }
    public void setFrames(List<Frame> frames) { this.frames = frames; }

    public Instance start(Player player) {
        Instance instance = new Instance(player);
        instance.apply();
        return instance;
    }
}
