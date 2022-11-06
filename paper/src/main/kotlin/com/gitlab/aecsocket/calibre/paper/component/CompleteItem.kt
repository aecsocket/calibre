package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ItemEvent
import com.gitlab.aecsocket.sokol.paper.component.HostedByItem
import com.gitlab.aecsocket.sokol.paper.util.ItemDescriptor
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class CompleteItem(val profile: Profile) : MarkerPersistentComponent {
    companion object {
        val Key = CalibreAPI.key("complete_item")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = CompleteItem::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val descriptor: ItemDescriptor
    ) : NonReadingComponentProfile {
        override fun readEmpty() = CompleteItem(this)
    }
}

@All(CompleteItem::class, Complete::class, HostedByItem::class)
@After(CompleteSystem::class)
class CompleteItemSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mCompleteItem = mappers.componentMapper<CompleteItem>()

    @Subscribe
    fun on(event: ItemEvent.CreateForm, entity: SokolEntity) {
        val completeItem = mCompleteItem.get(entity).profile
        completeItem.descriptor.applyTo(event.item, event.meta)
    }
}
