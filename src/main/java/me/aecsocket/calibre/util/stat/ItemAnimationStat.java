package me.aecsocket.calibre.util.stat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import me.aecsocket.calibre.item.ItemAnimation;
import me.aecsocket.unifiedframework.stat.impl.OperationStat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ItemAnimationStat extends OperationStat<ItemAnimation> {
    public ItemAnimationStat(ItemAnimation defaultValue) { super(defaultValue); }
    public ItemAnimationStat() {}

    @Override protected boolean allowSquash() { return false; }
    @Override
    protected ItemAnimation toT(JsonElement json, JsonDeserializationContext context) throws IllegalArgumentException {
        return context.deserialize(json, ItemAnimation.class);
    }
    @Override
    protected ItemAnimation modify(ItemAnimation b, ItemAnimation v, String op) {
        switch (op) {
            default: return v;
            case "+":
                if (b == null) return v;
                if (v == null) return b;
                List<ItemAnimation.Frame> frames = new ArrayList<>(b.getFrames());
                frames.addAll(v.getFrames());
                return new ItemAnimation(
                        b.getPlugin(),
                        frames
                );
            case "+b":
                if (b == null) return v;
                if (v == null) return b;
                frames = new ArrayList<>(v.getFrames());
                frames.addAll(b.getFrames());
                return new ItemAnimation(
                        b.getPlugin(),
                        frames
                );
        }
    }

    @Override public Type getValueType() { return ItemAnimation.class; }
    @Override public ItemAnimation copy(ItemAnimation value) { return value == null ? null : new ItemAnimation(value); }
}
