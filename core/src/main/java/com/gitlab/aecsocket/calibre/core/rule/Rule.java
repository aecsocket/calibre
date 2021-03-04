package com.gitlab.aecsocket.calibre.core.rule;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.rule.visitor.Visitor;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public interface Rule {
    Map<String, Class<? extends Rule>> DEFAULT_RULE_TYPES = MapInit.of(new HashMap<String, Class<? extends Rule>>())
            .init(ConstantRule.TYPE, ConstantRule.class)

            .init(LogicRule.Not.TYPE, LogicRule.Not.class)
            .init(LogicRule.And.TYPE, LogicRule.And.class)
            .init(LogicRule.Or.TYPE, LogicRule.Or.class)

            .init(ComponentRule.Complete.TYPE, ComponentRule.Complete.class)
            .init(ComponentRule.SlotFilled.TYPE, ComponentRule.SlotFilled.class)
            .init(ComponentRule.HasCategory.TYPE, ComponentRule.HasCategory.class)
            .init(ComponentRule.HasSystem.TYPE, ComponentRule.HasSystem.class)

            .init(NavigationRule.As.TYPE, NavigationRule.As.class)
            .init(NavigationRule.AsRoot.TYPE, NavigationRule.AsRoot.class)
            .init(NavigationRule.IsRoot.TYPE, NavigationRule.IsRoot.class)

            .init(SlotRule.AsChild.TYPE, SlotRule.AsChild.class)
            .init(SlotRule.AsParent.TYPE, SlotRule.AsParent.class)
            .get();

    final class Serializer implements TypeSerializer<Rule> {
        private Map<String, Class<? extends Rule>> ruleTypes;

        public Serializer(Map<String, Class<? extends Rule>> ruleTypes) {
            this.ruleTypes = ruleTypes;
        }

        public Serializer() {}

        public Map<String, Class<? extends Rule>> ruleTypes() { return ruleTypes; }
        public void ruleTypes(Map<String, Class<? extends Rule>> ruleTypes) { this.ruleTypes = ruleTypes; }

        @Override
        public void serialize(Type type, @Nullable Rule obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Rule deserialize(Type type, ConfigurationNode node) throws SerializationException {
            Object raw = node.raw();
            if (raw instanceof Boolean) {
                return new ConstantRule((Boolean) raw);
            }

            Map<String, Class<? extends Rule>> ruleTypes = this.ruleTypes;
            if (ruleTypes == null)
                ruleTypes = DEFAULT_RULE_TYPES;
            String sRuleType = node.node("type").getString();
            Class<? extends Rule> ruleType = ruleTypes.get(sRuleType);
            if (ruleType == null)
                throw new SerializationException(node, type, "Invalid rule type [" + sRuleType + "]");
            return node.get(ruleType);
        }
    }

    String type();
    boolean applies(CalibreComponent<?> component);
    void visit(Visitor visitor);
}
