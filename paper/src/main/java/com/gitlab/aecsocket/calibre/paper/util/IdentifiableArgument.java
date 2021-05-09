package com.gitlab.aecsocket.calibre.paper.util;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.captions.Caption;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import com.gitlab.aecsocket.calibre.core.util.CalibreIdentifiable;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class IdentifiableArgument<C> extends CommandArgument<C, CalibreIdentifiable> {
    public static final Caption ARGUMENT_PARSE_FAILURE_IDENTIFIABLE = Caption.of("argument.parse.failure.identifiable");

    public static final class Exception extends ParserException {
        public Exception(String input, CommandContext<?> ctx, Class<? extends CalibreIdentifiable> targetType) {
            super(IdentifiableArgument.class, ctx, ARGUMENT_PARSE_FAILURE_IDENTIFIABLE,
                    CaptionVariable.of("input", input),
                    CaptionVariable.of("type", targetType == null ? "<null>" : targetType.getSimpleName()));
        }
    }

    private final Class<? extends CalibreIdentifiable> targetType;

    private IdentifiableArgument(
            final CalibrePlugin plugin,
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>,
                    @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            final Class<? extends CalibreIdentifiable> targetType
    ) {
        super(required, name, new IdentifiableParser<>(plugin, targetType), defaultValue, CalibreIdentifiable.class, suggestionsProvider, defaultDescription);
        this.targetType = targetType;
    }

    public Class<? extends CalibreIdentifiable> targetType() { return targetType; }

    public static <C> @NonNull Builder<C> newBuilder(final @NonNull String name, final CalibrePlugin plugin) {
        return new Builder<>(name, plugin);
    }

    public static <C> @NonNull CommandArgument<C, CalibreIdentifiable> of(final @NonNull String name, final CalibrePlugin plugin) {
        return IdentifiableArgument.<C>newBuilder(name, plugin).asRequired().build();
    }

    public static <C> @NonNull CommandArgument<C, CalibreIdentifiable> optional(final @NonNull String name, final CalibrePlugin plugin) {
        return IdentifiableArgument.<C>newBuilder(name, plugin).asOptional().build();
    }

    public static <C> @NonNull CommandArgument<C, CalibreIdentifiable> optional(
            final @NonNull String name,
            final @NonNull CalibreIdentifiable defaultValue,
            final CalibrePlugin plugin
    ) {
        return IdentifiableArgument.<C>newBuilder(name, plugin).asOptionalWithDefault(defaultValue.toString()).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, CalibreIdentifiable> {
        private final CalibrePlugin plugin;
        private Class<? extends CalibreIdentifiable> targetType;

        private Builder(final @NonNull String name, final CalibrePlugin plugin) {
            super(CalibreIdentifiable.class, name);
            this.plugin = plugin;
        }

        public CalibrePlugin plugin() { return plugin; }

        public @NonNull Builder<C> withTargetType(final Class<? extends CalibreIdentifiable> targetType) {
            this.targetType = targetType;
            return this;
        }

        /**
         * Builder a new example component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull IdentifiableArgument<C> build() {
            return new IdentifiableArgument<>(
                    plugin,
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription(),
                    targetType
            );
        }

    }

    public static final class IdentifiableParser<C> implements ArgumentParser<C, CalibreIdentifiable> {
        private final CalibrePlugin plugin;
        private final Class<? extends CalibreIdentifiable> targetType;

        public IdentifiableParser(CalibrePlugin plugin, Class<? extends CalibreIdentifiable> targetType) {
            this.plugin = plugin;
            this.targetType = targetType;
        }

        public CalibrePlugin plugin() { return plugin; }
        public Class<? extends CalibreIdentifiable> targetType() { return targetType; }

        @Override
        public @NonNull ArgumentParseResult<@NonNull CalibreIdentifiable> parse(@NonNull CommandContext<@NonNull C> ctx, @NonNull Queue<@NonNull String> input) {
            String arg = input.peek();
            if (arg == null)
                return ArgumentParseResult.failure(new NoInputProvidedException(IdentifiableArgument.class, ctx));

            CalibreIdentifiable object = plugin.registry().get(arg);
            if (object == null || (targetType != null && !targetType.isInstance(object)))
                return ArgumentParseResult.failure(new Exception(arg, ctx, targetType));
            input.remove();
            return ArgumentParseResult.success(object);
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<C> ctx, @NonNull String input) {
            return plugin.registry().getRegistry().entrySet().stream()
                    .filter(e -> targetType == null || targetType.isInstance(e.getValue().get()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
    }
}
