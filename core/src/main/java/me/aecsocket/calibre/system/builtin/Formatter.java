package me.aecsocket.calibre.system.builtin;

import me.aecsocket.unifiedframework.parsing.math.MathExpressionNode;
import me.aecsocket.unifiedframework.parsing.math.MathParser;
import me.aecsocket.unifiedframework.stat.StatInstance;
import me.aecsocket.unifiedframework.util.data.Tuple3;
import me.aecsocket.unifiedframework.util.descriptor.Descriptor;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.Vector2DDescriptor;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;

import java.text.DecimalFormat;

public interface Formatter<T> {
    Component key(String locale, StatInstance<T> inst, StatDisplaySystem.Element element);
    Component format(String locale, StatInstance<T> inst, StatDisplaySystem.Element element);

    abstract class NumberDescriptorFormatter<N extends Number, T extends NumberDescriptor<N>> implements Formatter<T> {
        protected abstract Component bar(String locale, String key, double percent);
        protected abstract Component gen(String locale, String key, Object... args);

        @Override
        public Component key(String locale, StatInstance<T> inst, StatDisplaySystem.Element element) {
            NumberDescriptor<N> desc = inst.raw();
            ConfigurationNode config = element.config();

            config = merge(config, desc);

            return gen(locale, "stat.key." + config.node("key").getString(element.stat()));
        }

        @Override
        public Component format(String locale, StatInstance<T> inst, StatDisplaySystem.Element element) {
            NumberDescriptor<N> desc = inst.raw();

            Tuple3<String, Double, ConfigurationNode> generated = generate(desc.toDouble(desc.value()), desc, element.config());
            String text = generated.a();
            Double percent = generated.b();
            ConfigurationNode config = generated.c();

            Component result = Component.text(text);
            Component bar = percent == null ? null : bar(locale, "stat.bar", percent);
            String key = config.node("key").getString(element.stat());
            return gen(locale, "stat.value." + key,
                    "value", bar == null ? result : bar.append(result));
        }

        protected static ConfigurationNode merge(ConfigurationNode config, boolean absolute) {
            if (absolute)
                config = config.copy().mergeFrom(config.node("absolute"));
            else
                config = config.copy().mergeFrom(config.node("relative"));
            return config;
        }

        protected static ConfigurationNode merge(ConfigurationNode config, Descriptor<?> descriptor) {
            return merge(config, descriptor.operation() == Descriptor.Operation.SET);
        }

        public static Tuple3<String, Double, ConfigurationNode> generate(double value, Descriptor<?> desc, ConfigurationNode config) {
            boolean absolute = desc.operation() == Descriptor.Operation.SET;
            config = merge(config, absolute);

            // `function`
            String function = config.node("function").getString();
            if (function != null) {
                MathExpressionNode expr = MathParser.parses(function);
                expr.setVariable("n", value);
                value = expr.value();
            }

            // `min`, `max`
            ConfigurationNode min = config.node("min");
            ConfigurationNode max = config.node("max");
            Double percent;
            if (absolute && !min.virtual() && !max.virtual()) {
                double dMin = min.getDouble();
                double dMax = max.getDouble();
                percent = (value - dMin) / (dMax - dMin);
            } else
                percent = null;

            // `format`, create result
            String format = config.node("format").getString();

            return Tuple3.of(format == null ? desc.toString(value) : desc.toString(value, new DecimalFormat(format)), percent, config);
        }
    }

    abstract class Vector2DDescriptorFormatter implements Formatter<Vector2DDescriptor> {
        protected abstract Component bar(String locale, String key, double percent);
        protected abstract Component gen(String locale, String key, Object... args);

        @Override
        public Component key(String locale, StatInstance<Vector2DDescriptor> inst, StatDisplaySystem.Element element) {
            return null;
        }

        @Override
        public Component format(String locale, StatInstance<Vector2DDescriptor> inst, StatDisplaySystem.Element element) {
            ConfigurationNode config = element.config();

            Vector2DDescriptor desc = inst.raw();
            Vector2D value = desc.value();

            ConfigurationNode configX = config.copy().mergeFrom(config.node("x"));
            ConfigurationNode configY = config.copy().mergeFrom(config.node("y"));

            Tuple3<String, Double, ConfigurationNode> generatedX = NumberDescriptorFormatter.generate(value.x(), desc, configX);
            Tuple3<String, Double, ConfigurationNode> generatedY = NumberDescriptorFormatter.generate(value.y(), desc, configY);

            Component result = Double.compare(value.x(), value.y()) == 0
                    ? Component.text(generatedX.a())
                    : gen(locale, "stat.vector2",
                    "x", generatedX.a(),
                    "y", generatedY.a());

            Component barX = generatedX.b() == null ? null : bar(locale, "stat.bar", generatedX.b());
            Component barY = generatedY.b() == null ? null : bar(locale, "stat.bar", generatedY.b());
            String key = config.node("key").getString(element.stat());
            return gen(locale, "stat.value." + key,
                    "value", barX == null ? result : barX.append(barY).append(result));
        }
    }
}
