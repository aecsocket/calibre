package me.aecsocket.calibre.item.animation;

import com.google.gson.*;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.unifiedframework.util.json.JsonAdapter;
import org.bukkit.inventory.EquipmentSlot;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AnimationAdapter implements JsonDeserializer<Animation>, JsonAdapter {
    private final CalibrePlugin plugin;

    public AnimationAdapter(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    @Override
    public Animation deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonArray array = assertArray(json);
        List<Animation.Frame> frames = new ArrayList<>();
        for (JsonElement elem : array) {
            if (elem.isJsonArray()) {
                // Single frame
                JsonArray frame = elem.getAsJsonArray();
                if (frame.size() < 2) continue;
                if (frame.get(1).isJsonObject()) {
                    // [ delay, item descriptor, equipment slot ]
                    // ItemDescriptor
                    frames.add(new Animation.Frame(
                            plugin,
                            at(frame, 0, JsonElement::getAsLong, 0L),
                            (EquipmentSlot) at(frame, 2, context, EquipmentSlot.class),
                            at(frame, 1, context, ItemDescriptor.class)
                    ));
                } else {
                    // [ delay, custom model data, damage, equipment slot ]
                    // Model data + damage
                    frames.add(new Animation.Frame(
                            plugin,
                            at(frame, 0, JsonElement::getAsLong, 0L),
                            at(frame, 3, context, EquipmentSlot.class),
                            at(frame, 1, JsonElement::getAsInt),
                            at(frame, 2, JsonElement::getAsInt)
                    ));
                }
            } else {
                /*
                {
                  "from": model data/damage to start at,
                  "range": amount of frames to do
                  "duration": frame delays in ms,
                  "slot": equipment slot or null,
                  "model_data": default model data,
                  "damage": default damage
                }
                 */
                // Range of frames
                JsonObject object = assertObject(elem);
                int from = get(object, "from").getAsInt();
                long duration = get(object, "duration").getAsLong();
                EquipmentSlot slot = object.has("slot") ? context.deserialize(object.get("slot"), EquipmentSlot.class) : null;
                Integer modelData = object.has("model_data") ? object.get("model_data").getAsInt() : null;
                Integer damage = object.has("damage") ? object.get("damage").getAsInt() : null;
                for (int i = 0; i < get(object, "range").getAsInt(); i++) {
                    frames.add(new Animation.Frame(
                            plugin,
                            duration,
                            slot,
                            modelData == null ? from + i : modelData,
                            damage == null ? from + i : damage
                    ));
                }
            }
        }
        return new Animation(frames);
    }
}
