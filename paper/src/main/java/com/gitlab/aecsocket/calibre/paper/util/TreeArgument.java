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
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.util.CalibreIdentifiable;
import com.gitlab.aecsocket.calibre.core.util.ItemSupplier;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.unifiedframework.core.util.TextUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreeArgument<C> extends CommandArgument<C, ComponentTree> {
    public static final Caption ARGUMENT_PARSE_FAILURE_TREE = Caption.of("argument.parse.failure.tree");

    public static final class Exception extends ParserException {
        public Exception(String input, CommandContext<?> ctx, String error) {
            super(TreeArgument.class, ctx, ARGUMENT_PARSE_FAILURE_TREE,
                    CaptionVariable.of("input", input),
                    CaptionVariable.of("error", error));
        }
    }

    private TreeArgument(
            final CalibrePlugin plugin,
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>,
                    @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription
    ) {
        super(required, name, new TreeParser<>(plugin), defaultValue, ComponentTree.class, suggestionsProvider, defaultDescription);
    }

    public static <C> @NonNull Builder<C> newBuilder(final @NonNull String name, final CalibrePlugin plugin) {
        return new Builder<>(name, plugin);
    }

    public static <C> @NonNull CommandArgument<C, ComponentTree> of(final @NonNull String name, final CalibrePlugin plugin) {
        return TreeArgument.<C>newBuilder(name, plugin).asRequired().build();
    }

    public static <C> @NonNull CommandArgument<C, ComponentTree> optional(final @NonNull String name, final CalibrePlugin plugin) {
        return TreeArgument.<C>newBuilder(name, plugin).asOptional().build();
    }

    public static <C> @NonNull CommandArgument<C, ComponentTree> optional(
            final @NonNull String name,
            final @NonNull CalibreIdentifiable defaultValue,
            final CalibrePlugin plugin
    ) {
        return TreeArgument.<C>newBuilder(name, plugin).asOptionalWithDefault(defaultValue.toString()).build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, ComponentTree> {
        private final CalibrePlugin plugin;

        private Builder(final @NonNull String name, final CalibrePlugin plugin) {
            super(ComponentTree.class, name);
            this.plugin = plugin;
        }

        public CalibrePlugin plugin() { return plugin; }

        /**
         * Builder a new example component
         *
         * @return Constructed component
         */
        @Override
        public @NonNull TreeArgument<C> build() {
            return new TreeArgument<>(
                    plugin,
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription()
            );
        }

    }

    public static final class TreeParser<C> implements ArgumentParser<C, ComponentTree> {
        private final CalibrePlugin plugin;

        public TreeParser(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin plugin() { return plugin; }

        @Override
        public @NonNull ArgumentParseResult<@NonNull ComponentTree> parse(@NonNull CommandContext<@NonNull C> ctx, @NonNull Queue<@NonNull String> input) {
            String arg = input.peek();
            if (arg == null)
                return ArgumentParseResult.failure(new NoInputProvidedException(TreeArgument.class, ctx));

            ComponentTree tree;
            try {
                HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                        .source(() -> new BufferedReader(new StringReader(arg)))
                        .build();
                tree = loader.load(plugin.configOptions()).get(ComponentTree.class);
            } catch (ConfigurateException e) {
                return ArgumentParseResult.failure(new Exception(arg, ctx, TextUtils.combineMessages(e)));
            }

            if (tree == null) {
                return ArgumentParseResult.failure(new Exception(arg, ctx, "null tree"));
            }
            input.remove();
            return ArgumentParseResult.success(tree);
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<C> ctx, @NonNull String input) {
            return plugin.registry().getRegistry().entrySet().stream()
                    .filter(e -> e.getValue().get() instanceof ItemSupplier)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
    }
}
