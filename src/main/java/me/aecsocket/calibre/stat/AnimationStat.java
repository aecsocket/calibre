package me.aecsocket.calibre.stat;

import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.unifiedframework.stat.SimpleStat;

import java.lang.reflect.Type;

public class AnimationStat extends SimpleStat<Animation> {
    public AnimationStat(Animation defaultValue) { super(defaultValue); }
    public AnimationStat() {}
    @Override public Type getValueType() { return Animation.class; }

    @Override public Animation combine(Animation b, Animation v, String op) { return v; }
    @Override public Animation copy(Animation animation) { return animation == null ? null : new Animation(animation); }
}
