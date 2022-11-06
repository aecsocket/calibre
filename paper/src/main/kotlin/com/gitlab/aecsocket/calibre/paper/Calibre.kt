package com.gitlab.aecsocket.calibre.paper

import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.BasePlugin
import com.gitlab.aecsocket.calibre.paper.component.*
import com.gitlab.aecsocket.sokol.core.componentType
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.bstats.bukkit.Metrics
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException

private const val BSTATS_ID = 10479

private lateinit var instance: Calibre
val CalibreAPI get() = instance

class Calibre : BasePlugin() {
    @ConfigSerializable
    data class Settings(
        val enableBstats: Boolean = true,
    )

    private data class Registration(
        val onInit: InitContext.() -> Unit,
        val onPostInit: PostInitContext.() -> Unit,
    )

    interface InitContext

    interface PostInitContext

    init {
        instance = this
    }

    lateinit var settings: Settings private set
    val players = CalibrePlayerFeature(this)

    private val registrations = ArrayList<Registration>()

    override fun onEnable() {
        super.onEnable()
        CalibreCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
            },
            onLoad = {
                addDefaultI18N()
            }
        )
        SokolAPI.registerConsumer(
            onInit = {
                engine
                    .systemFactory { CompleteSystem(it) }
                    .systemFactory { CompleteItemSystem(it) }
                    .systemFactory { LaunchOriginTarget }
                    .systemFactory { LaunchOriginPositionedInjectorSystem(it) }
                    .systemFactory { LaunchOriginItemInjectorSystem(it) }
                    .systemFactory { LauncherSystem(this@Calibre, it) }
                    .systemFactory { LaunchTransformSystem(it) }
                    .systemFactory { LaunchPhysicsRecoilSystem(it) }
                    .componentType<Complete>()
                    .componentType<RequiredSlot>()
                    .componentType<CompleteItem>()
                    .componentType<LaunchOrigin>()
                    .componentType<Launcher>()
                    .componentType<LaunchTransform>()
                    .componentType<LaunchPhysicsRecoil>()
                registerComponentType(RequiredSlot.Type)
                registerComponentType(CompleteItem.Type)
                registerComponentType(Launcher.Type)
                registerComponentType(LaunchTransform.Type)
                registerComponentType(LaunchPhysicsRecoil.Type)
            }
        )
    }

    override fun loadInternal(log: LogList, settings: ConfigurationNode): Boolean {
        if (super.loadInternal(log, settings)) {
            try {
                this.settings = settings.get { Settings() }
            } catch (ex: SerializationException) {
                log.line(LogLevel.Error, ex) { "Could not load settings file" }
                return false
            }

            if (this.settings.enableBstats) {
                Metrics(this, BSTATS_ID)
            }

            return true
        }
        return false
    }

    fun registerConsumer(
        onInit: InitContext.() -> Unit = {},
        onPostInit: PostInitContext.() -> Unit = {},
    ) {
        registrations.add(Registration(onInit, onPostInit))
    }
}
