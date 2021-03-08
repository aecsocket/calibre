package com.gitlab.aecsocket.calibre.paper;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.netty.WirePacket;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.rule.Rule;
import com.gitlab.aecsocket.calibre.core.rule.RuledStatCollectionList;
import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.*;
import com.gitlab.aecsocket.calibre.core.system.builtin.Formatter;
import com.gitlab.aecsocket.calibre.core.system.gun.*;
import com.gitlab.aecsocket.calibre.paper.blueprint.PaperBlueprint;
import com.gitlab.aecsocket.calibre.paper.system.gun.projectile.BulletSystem;
import com.gitlab.aecsocket.calibre.paper.system.gun.projectile.ExplosiveProjectileSystem;
import com.gitlab.aecsocket.calibre.paper.system.gun.projectile.IncendiaryProjectileSystem;
import com.gitlab.aecsocket.calibre.paper.system.gun.projectile.PaperProjectileSystem;
import com.gitlab.aecsocket.calibre.paper.system.gun.reload.external.PaperRemoveReloadSystem;
import com.gitlab.aecsocket.calibre.paper.system.gun.reload.external.PaperSingleChamberReloadSystem;
import com.gitlab.aecsocket.calibre.paper.system.gun.reload.internal.PaperInsertReloadSystem;
import com.gitlab.aecsocket.calibre.core.util.*;
import com.gitlab.aecsocket.calibre.paper.util.*;
import com.gitlab.aecsocket.calibre.paper.util.item.ItemManager;
import com.gitlab.aecsocket.calibre.paper.system.builtin.*;
import com.gitlab.aecsocket.calibre.paper.system.gun.*;
import com.gitlab.aecsocket.unifiedframework.core.util.result.LoggingEntry;
import com.gitlab.aecsocket.unifiedframework.paper.serialization.configurate.*;
import com.gitlab.aecsocket.unifiedframework.paper.util.VelocityTracker;
import com.gitlab.aecsocket.unifiedframework.paper.util.plugin.BasePlugin;
import com.gitlab.aecsocket.unifiedframework.paper.util.plugin.PlayerDataManager;
import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.unifiedframework.paper.gui.GUIManager;
import com.gitlab.aecsocket.unifiedframework.paper.gui.GUIVector;
import com.gitlab.aecsocket.unifiedframework.core.loop.ThreadLoop;
import com.gitlab.aecsocket.unifiedframework.core.registry.Ref;
import com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.*;
import com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.descriptor.NumberDescriptorSerializer;
import com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.descriptor.Vector2DDescriptorSerializer;
import com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.descriptor.Vector3DDescriptorSerializer;
import com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.vector.Vector2DSerializer;
import com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.vector.Vector2ISerializer;
import com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.vector.Vector3DSerializer;
import com.gitlab.aecsocket.unifiedframework.core.serialization.configurate.vector.Vector3ISerializer;
import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import com.gitlab.aecsocket.unifiedframework.core.util.*;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.ParticleData;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.Vector2DDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.Vector3DDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.log.LabelledLogger;
import com.gitlab.aecsocket.unifiedframework.core.util.log.LogLevel;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2D;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2I;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3I;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingSchemes;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Core plugin class for Paper servers.
 */
public class CalibrePlugin extends BasePlugin<CalibreIdentifiable> {
    public static final int THREAD_LOOP_PERIOD = 10;
    public static final int BSTATS_PLUGIN_ID = 10479; // TODO
    public static final List<String> DEFAULT_RESOURCES = Collections.unmodifiableList(ListInit.of(new ArrayList<String>())
            .init(BasePlugin.DEFAULT_RESOURCES)
            .init("README.txt")
            .get());
    public static final Map<Path, Type> REGISTRY_TYPES = MapInit.of(new HashMap<Path, Type>())
            .init(Path.of("component"), PaperComponent.class)
            .init(Path.of("blueprint"), PaperBlueprint.class)
            .get();

    private static CalibrePlugin instance;
    public static CalibrePlugin instance() { return instance; }

    private final List<CalibreHook> hooks = new ArrayList<>();
    private final PlayerDataManager<CalibrePlayerData> playerData = new PlayerDataManager<>() {
        @Override protected CalibrePlayerData createData(Player player) { return new CalibrePlayerData(CalibrePlugin.this, player); }
    };
    private final ItemManager itemManager = new ItemManager(this);
    private final SchedulerSystem.Scheduler systemScheduler = new SchedulerSystem.Scheduler(10000, 100);
    private final CasingManager casingManager = new CasingManager(this);
    private final LocationalDamageManager locationalDamageManager = new LocationalDamageManager(this);
    private final VelocityTracker velocityTracker = new VelocityTracker();
    private final Map<Class<?>, com.gitlab.aecsocket.calibre.core.system.builtin.Formatter<?>> statFormatters = new HashMap<>();
    private final ThreadLoop threadLoop = new ThreadLoop(THREAD_LOOP_PERIOD);
    private PaperCommandManager commandManager;
    private GUIManager guiManager;
    private ProtocolManager protocol;
    private MapFont font;
    private StatMapSerializer statMapSerializer;
    private Rule.Serializer ruleSerializer;

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        guiManager = new GUIManager(this);
        protocol = ProtocolLibrary.getProtocolManager();
        font = new MinecraftFont();

        createCommandManager();

        Bukkit.getPluginManager().registerEvents(new CalibreListener(this), this);
        playerData.setup(this);
        protocol.addPacketListener(new CalibrePacketAdapter(this));

        schedulerLoop.register(playerData);
        schedulerLoop.register(systemScheduler);
        schedulerLoop.register(casingManager);
        schedulerLoop.register(velocityTracker);

        threadLoop.register(playerData);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hooks.forEach(CalibreHook::calibreDisable);
        if (threadLoop.running())
            threadLoop.stop();
    }

    @Override
    @EventHandler(priority = EventPriority.MONITOR)
    public void serverLoad(ServerLoadEvent event) {
        hooks.forEach(CalibreHook::serverLoad);
        createConfigOptions();
        super.serverLoad(event);
        threadLoop.start();
    }

    public List<CalibreHook> hooks() { return hooks; }
    public LabelledLogger pluginLogger() { return logger; }
    public ItemManager itemManager() { return itemManager; }
    public SchedulerSystem.Scheduler systemScheduler() { return systemScheduler; }
    public CasingManager casingManager() { return casingManager; }
    public LocationalDamageManager locationalDamageManager() { return locationalDamageManager; }
    public VelocityTracker velocityTracker() { return velocityTracker; }
    public Map<Class<?>, com.gitlab.aecsocket.calibre.core.system.builtin.Formatter<?>> statFormatters() { return statFormatters; }
    public ThreadLoop threadLoop() { return threadLoop; }
    public ConfigurationOptions configOptions() { return configOptions; }
    public PaperCommandManager commandManager() { return commandManager; }
    public GUIManager guiManager() { return guiManager; }
    public ProtocolManager protocol() { return protocol; }
    public MapFont font() { return font; }
    public StatMapSerializer statMapSerializer() { return statMapSerializer; }
    public Rule.Serializer ruleSerializer() { return ruleSerializer; }

    public void addHook(CalibreHook hook) { hooks.add(hook); }

    @SuppressWarnings("unchecked")
    public <T> Formatter<T> statFormatter(Class<T> type) { return (Formatter<T>) statFormatters.get(type); }
    public <T> CalibrePlugin statFormatter(Class<T> type, Formatter<T> formatter) { statFormatters.put(type, formatter); return this; }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void createConfigOptions() {
        configOptions = configOptions.serializers(builder -> {
            hooks.forEach(hook -> hook.registerSerializers(builder));
            statMapSerializer = new StatMapSerializer();
            ruleSerializer = new Rule.Serializer();
            ObjectMapper.Factory mapper = ObjectMapper.factoryBuilder()
                    .defaultNamingScheme(NamingSchemes.SNAKE_CASE)
                    .build();
            TypeSerializer systemSerializer = new CalibreSystem.Serializer(Utils.delegate(CalibreSystem.class, builder, mapper), NamingSchemes.SNAKE_CASE);
            builder
                    .register(Color.class, ColorSerializer.INSTANCE)
                    .register(BlockData.class, BlockDataSerializer.INSTANCE)
                    .register(Particle.DustOptions.class, DustOptionsSerializer.INSTANCE)
                    .register(ItemStack.class, ItemStackSerializer.INSTANCE)
                    .register(Location.class, LocationSerializer.INSTANCE)
                    .register(NamespacedKey.class, NamespacedKeySerializer.INSTANCE)

                    .register(NumberDescriptor.Byte.class, NumberDescriptorSerializer.Byte.INSTANCE)
                    .register(NumberDescriptor.Short.class, NumberDescriptorSerializer.Short.INSTANCE)
                    .register(NumberDescriptor.Integer.class, NumberDescriptorSerializer.Integer.INSTANCE)
                    .register(NumberDescriptor.Long.class, NumberDescriptorSerializer.Long.INSTANCE)
                    .register(NumberDescriptor.Float.class, NumberDescriptorSerializer.Float.INSTANCE)
                    .register(NumberDescriptor.Double.class, NumberDescriptorSerializer.Double.INSTANCE)
                    .register(Vector2DDescriptor.class, Vector2DDescriptorSerializer.INSTANCE)
                    .register(Vector3DDescriptor.class, Vector3DDescriptorSerializer.INSTANCE)

                    .register(Vector.class, VectorSerializer.INSTANCE)
                    .register(GUIVector.class, GUIVectorSerializer.INSTANCE)
                    .register(Vector3D.class, Vector3DSerializer.INSTANCE)
                    .register(Vector3I.class, Vector3ISerializer.INSTANCE)
                    .register(Vector2D.class, Vector2DSerializer.INSTANCE)
                    .register(Vector2I.class, Vector2ISerializer.INSTANCE)

                    .register(ParticleData.class, ParticleDataSerializer.INSTANCE)
                    .register(SoundData.class, new SoundDataSerializer(this))

                    .register(StatMap.class, statMapSerializer)
                    .registerExact(Rule.class, ruleSerializer)
                    .register(StatCollection.class, StatCollection.Serializer.INSTANCE)
                    .register(RuledStatCollectionList.class, RuledStatCollectionList.Serializer.INSTANCE)
                    .register(CasingManager.Category.class, new CasingManager.Category.Serializer(this, Utils.delegate(CasingManager.Category.class, builder, mapper)))

                    .register(ComponentContainerSystem.class, new ComponentContainerSystem.Serializer((TypeSerializer<ComponentContainerSystem>) systemSerializer))
                    .register(SchedulerSystem.class, new SchedulerSystem.Serializer((TypeSerializer<SchedulerSystem>) systemSerializer))
                    .register(CalibreSystem.class, systemSerializer)

                    .register(new TypeToken<Quantifier<ComponentTree>>(){}, new QuantifierSerializer<>())
                    .register(ItemAnimation.class, new ItemAnimation.Serializer(this))
                    .register(SightPath.class, SightPath.Serializer.INSTANCE)
                    .register(FireModePath.class, FireModePath.Serializer.INSTANCE)
                    .register(PaperComponent.class, new PaperComponent.Serializer(Utils.delegate(PaperComponent.class, builder, mapper)))
                    .register(ComponentTree.class, new ComponentTree.AbstractSerializer() {
                        @Override protected <T extends CalibreIdentifiable> T byId(String id, Class<T> type) { return registry.get(id, type); }
                    })
                    .registerAnnotatedObjects(mapper);
        });
    }

    @Override
    protected void registerDefaults() {
        statFormatters.put(NumberDescriptor.Byte.class, new PaperFormatter.NumberDescriptorFormatter<Byte, NumberDescriptor.Byte>(this));
        statFormatters.put(NumberDescriptor.Short.class, new PaperFormatter.NumberDescriptorFormatter<Short, NumberDescriptor.Short>(this));
        statFormatters.put(NumberDescriptor.Integer.class, new PaperFormatter.NumberDescriptorFormatter<Integer, NumberDescriptor.Integer>(this));
        statFormatters.put(NumberDescriptor.Long.class, new PaperFormatter.NumberDescriptorFormatter<Long, NumberDescriptor.Long>(this));
        statFormatters.put(NumberDescriptor.Float.class, new PaperFormatter.NumberDescriptorFormatter<Float, NumberDescriptor.Float>(this));
        statFormatters.put(NumberDescriptor.Double.class, new PaperFormatter.NumberDescriptorFormatter<Double, NumberDescriptor.Double>(this));
        statFormatters.put(Vector2DDescriptor.class, new PaperFormatter.Vector2DDescriptorFormatter(this));

        PaperStatDisplaySystem statDisplay = new PaperStatDisplaySystem(this, this::statFormatter);
        registry.register(statDisplay);
        registry.register(new PaperSlotDisplaySystem(this));
        registry.register(new PaperComponentContainerSystem(this));
        registry.register(new PaperCapacityComponentContainerSystem(this));
        registry.register(new PaperNameFromChildSystem(this));
        registry.register(new PaperNameOverrideSystem(this));
        registry.register(new PaperSchedulerSystem(this, systemScheduler));

        registry.register(new GenericStatsSystem(this));
        registry.register(new InventoryComponentAccessorSystem(this));
        registry.register(new PaperGunSystem(this));
        registry.register(new PaperGunInfoSystem(this));
        registry.register(new PaperSwayStabilizationSystem(this));
        registry.register(new PaperFireModeSystem(this));
        registry.register(new PaperSightSystem(this));
        registry.register(new PaperChamberSystem(this));
        registry.register(new PaperSingleChamberReloadSystem(this));
        registry.register(new PaperInsertReloadSystem(this));
        registry.register(new PaperRemoveReloadSystem(this));
        registry.register(new PaperProjectileSystem(this));
        registry.register(new BulletSystem(this));
        registry.register(new IncendiaryProjectileSystem(this));
        registry.register(new ExplosiveProjectileSystem(this));
    }

    protected void createCommandManager() {
        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.getCommandCompletions().registerCompletion("registry", ctx -> {
            String sType = ctx.getConfig("type");
            Class<?> type = null;
            if (sType != null) {
                try {
                    type = Class.forName(sType);
                } catch (ClassNotFoundException | ClassCastException ignore) {}
            }

            final Class<?> fType = type;
            return registry.getRegistry().entrySet().stream()
                    .filter(entry -> fType == null || fType.isInstance(entry.getValue().get()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        });
        commandManager.getCommandContexts().registerContext(CalibreIdentifiable.class, ctx -> {
            String sType = ctx.getFlagValue("type", (String) null);
            Class<?> type = null;
            if (sType != null) {
                try {
                    type = Class.forName(sType);
                } catch (ClassNotFoundException | ClassCastException ignore) {}
            }

            CommandSender sender = ctx.getSender();
            String id = ctx.popFirstArg();
            Ref<? extends CalibreIdentifiable> result = registry.getRef(id);
            if (result == null) {
                sendMessage(sender, "command.error.no_object",
                        "id", id);
                throw new InvalidCommandArgument(false);
            }
            CalibreIdentifiable obj = result.get();
            if (type != null && !type.isInstance(result.get())) {
                sendMessage(sender, "command.error.not_type",
                        "id", id,
                        "found", obj.getClass().getSimpleName(),
                        "expected", type.getSimpleName());
                throw new InvalidCommandArgument(false);
            }
            return obj;
        });
        commandManager.getCommandContexts().registerContext(ComponentTree.class, ctx -> {
            CommandSender sender = ctx.getSender();
            Locale locale = locale(sender);
            String input = ctx.popFirstArg();

            ComponentTree tree;
            try {
                HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                        .source(() -> new BufferedReader(new StringReader(input)))
                        .build();
                tree = loader.load().get(ComponentTree.class);
            } catch (ConfigurateException e) {
                sendMessage(sender, "command.error.parse_tree",
                        "error", TextUtils.combineMessages(e));
                throw new InvalidCommandArgument(false);
            }

            if (tree == null) {
                sendMessage(sender, "command.error.parse_tree",
                        "error", "null");
                throw new InvalidCommandArgument(false);
            }

            return tree;
        });
        commandManager.registerCommand(new CalibreCommand(this));
    }

    @Override
    protected void loadSettings(List<LoggingEntry> result) {
        super.loadSettings(result);
        if (settings.root() != null) {
            setting(ConfigurationNode::childrenMap, "font_map").forEach((key, node) ->
                    font.setChar(key.toString().charAt(0), new MapFont.CharacterSprite(node.getInt(), 0, new boolean[0])));
            systemScheduler.cleanDelay(setting(ConfigurationNode::getLong, "scheduler", "clean_delay"));
            systemScheduler.cleanThreshold(setting(ConfigurationNode::getLong, "scheduler", "clean_threshold"));

            casingManager.load();
            locationalDamageManager.load();

            if (setting(n -> n.getBoolean(true), "enable_bstats")) {
                Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
                // TODO add some cool charts
            }
        }
    }

    @Override protected List<String> defaultResources() { return DEFAULT_RESOURCES; }
    @Override protected Map<Path, Type> registryTypes() { return REGISTRY_TYPES; }
    //endregion

    //region Utils
    public CalibrePlayerData playerData(Player player) { return playerData.get(player); }

    public Component bar(Locale locale, String key, double fullPercent, double partPercent, int width) {
        String full = setting(n -> n.getString("="), "symbol", "full_bar");
        String part = setting(n -> n.getString("~"), "symbol", "part_bar");
        String empty = setting(n -> n.getString("-"), "symbol", "empty_bar");

        int fullWidth = (int) (Utils.clamp01(fullPercent) * width);
        int partWidth = (int) ((Utils.clamp01(partPercent + fullPercent) * width) - fullWidth);
        int emptyWidth = Math.max(0, width - partWidth - fullWidth);

        return gen(locale, key,
                "full", full.repeat(fullWidth),
                "part", part.repeat(partWidth),
                "empty", empty.repeat(emptyWidth));
    }

    public void sendMessage(CommandSender sender, String key, Object... args) {
        sender.sendMessage(Identity.nil(), gen(locale(sender), key, args), MessageType.SYSTEM);
    }

    public void sendPacket(PacketContainer packet, Player target, boolean wire) {
        try {
            if (wire) {
                WirePacket wirePacket = WirePacket.fromPacket(packet);
                protocol.sendWirePacket(target, wirePacket);
            } else
                protocol.sendServerPacket(target, packet);
        } catch (InvocationTargetException | RuntimeException e) {
            log(LogLevel.WARN, e, "Could not send packet to %s (%s)", target.getName(), target.getUniqueId());
        }
    }

    public void sendPacket(PacketContainer packet, Player target) {
        sendPacket(packet, target, false);
    }

    public void sendPacket(Player target, PacketType type, boolean wire, Consumer<PacketContainer> builder) {
        PacketContainer packet = new PacketContainer(type);
        builder.accept(packet);
        sendPacket(packet, target, wire);
    }

    public void sendPacket(Player target, PacketType type, Consumer<PacketContainer> builder) {
        sendPacket(target, type, false, builder);
    }

    public double hardness(Block block) {
        Material material = block.getType();
        return setting(n -> n.getDouble(material.getBlastResistance()), "hardness", material.getKey().getKey());
    }
    //endregion
}
