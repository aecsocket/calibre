package me.aecsocket.calibre.util;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.contexts.ContextResolver;
import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.Registry;

public class RegistryCommandContext<T extends Identifiable> implements ContextResolver<T, BukkitCommandExecutionContext> {
    private final Class<T> type;
    private Registry registry;

    public RegistryCommandContext(Class<T> type, Registry registry) {
        this.type = type;
        this.registry = registry;
    }

    public Class<T> getType() { return type; }

    public Registry getRegistry() { return registry; }
    public void setRegistry(Registry registry) { this.registry = registry; }

    @Override
    public T getContext(BukkitCommandExecutionContext context) throws InvalidCommandArgument {
        String id = context.popFirstArg();
        Identifiable result = registry.getRaw(id);
        if (result == null) throw new InvalidCommandArgument("No item with ID " + id + " found");
        if (!type.isAssignableFrom(result.getClass())) throw new InvalidCommandArgument("Item " + id + " is not a " + type.getSimpleName());
        return type.cast(result);
    }
}
