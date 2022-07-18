package com.gitlab.aecsocket.calibre.paper.feature

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.Stat
import com.gitlab.aecsocket.sokol.paper.PaperFeature
import com.gitlab.aecsocket.sokol.paper.PaperFeatureContext
import com.gitlab.aecsocket.sokol.paper.PaperNodeEvent
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

object CalibreTestFeature : Keyed {
    override val id get() = "calibre_test"

    class Type : PaperFeature {
        override val id get() = CalibreTestFeature.id

        override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
        override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()

        override fun createProfile(node: ConfigurationNode) = Profile()

        inner class Profile : PaperFeature.Profile {
            override val type get() = this@Type

            override fun createData() = Data()

            override fun createData(node: ConfigurationNode) = Data()

            override fun createData(tag: CompoundBinaryTag) = Data()

            inner class Data : PaperFeature.Data {
                override val profile get() = this@Profile
                override val type get() = this@Type

                override fun createState() = State()

                override fun serialize(node: ConfigurationNode) {}

                override fun serialize(tag: CompoundBinaryTag.Mutable) {}

                override fun copy() = Data()
            }

            inner class State : PaperFeature.State {
                override val profile get() = this@Profile
                override val type get() = this@Type

                override fun asData() = Data()

                override fun onEvent(event: NodeEvent, ctx: PaperFeatureContext) {
                    when (event) {
                        is PaperNodeEvent.OnInput -> {
                            event.player.sendMessage("Calibre test - input = ${event.input}")
                        }
                    }
                }

                override fun serialize(tag: CompoundBinaryTag.Mutable) {}
            }
        }
    }
}
