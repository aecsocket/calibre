package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.Composite
import com.gitlab.aecsocket.sokol.paper.component.CompositeSystem
import com.gitlab.aecsocket.sokol.paper.component.ENTITY_SLOT_CHILD_KEY
import com.gitlab.aecsocket.sokol.paper.component.forAllEntities

object Complete : SokolComponent {
    override val componentType get() = Complete::class
}

object RequiredSlot : MarkerPersistentComponent {
    override val componentType get() = RequiredSlot::class
    override val key = CalibreAPI.key("required_slot")
    val Type = ComponentType.singletonComponent(key, this)
}

@All(Composite::class)
class CompleteSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mRequiredSlot = mappers.componentMapper<RequiredSlot>()
    private val mComplete = mappers.componentMapper<Complete>()
    private val mComposite = mappers.componentMapper<Composite>()

    private fun apply(entity: SokolEntity) {
        var valid = true
        mComposite.forAllEntities(entity) { target ->
            if (!valid) return@forAllEntities
            if (!mRequiredSlot.has(target)) return@forAllEntities
            val childComposite = mComposite.getOr(target) ?: return@forAllEntities
            if (!childComposite.contains(ENTITY_SLOT_CHILD_KEY)) {
                valid = false
            }
        }

        if (valid) mComplete.set(entity, Complete)
        else mComplete.remove(entity)
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        apply(entity)
    }

    @Subscribe
    fun on(event: CompositeSystem.TreeMutate, entity: SokolEntity) {
        apply(entity)
    }
}
