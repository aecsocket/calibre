package me.aecsocket.calibre.stat;

import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.unifiedframework.stat.SimpleStat;

import java.lang.reflect.Type;
import java.util.stream.Collectors;

/**
 * A stat which holds an animation. This supports the following operations:
 * <ul>
 *     <li>= (default)</li>
 *     <li>+</li>
 * </ul>
 */
public class AnimationStat extends SimpleStat<Animation> {
    public AnimationStat(Animation defaultValue) { super(defaultValue); }
    public AnimationStat() {}
    @Override public Type getValueType() { return Animation.class; }

    @Override public Animation combine(Animation b, Animation v, String op) {
        if (op == null) op = "";
        switch (op) {
            case "+":
                b.copy().getFrames().addAll(v.getFrames().stream().map(Animation.Frame::clone).collect(Collectors.toList()));
                return b;
            default:
                return v;
        }
    }
    @Override public Animation copy(Animation animation) { return animation == null ? null : new Animation(animation); }
}
