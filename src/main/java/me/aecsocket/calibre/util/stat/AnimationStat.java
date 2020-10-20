package me.aecsocket.calibre.util.stat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import me.aecsocket.calibre.item.ItemAnimation;
import me.aecsocket.unifiedframework.stat.impl.SimpleStat;

import java.lang.reflect.Type;
import java.util.function.Function;

public class AnimationStat extends SimpleStat<ItemAnimation> {
    public AnimationStat(ItemAnimation defaultValue) { super(defaultValue); }
    public AnimationStat() {}

    @Override public Type getValueType() { return ItemAnimation.class; }

    @Override
    public Function<ItemAnimation, ItemAnimation> getModFunction(JsonElement json, JsonDeserializationContext context) throws JsonParseException {
        ItemAnimation animation = context.deserialize(json, ItemAnimation.class);
        return b -> animation;
    }

    @Override
    public ItemAnimation copy(ItemAnimation value) { return value == null ? null : new ItemAnimation(value); }
}
