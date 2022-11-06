package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.transform
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.HostedByItem
import com.gitlab.aecsocket.sokol.paper.component.ItemHolder
import com.gitlab.aecsocket.sokol.paper.component.PositionRead
import com.gitlab.aecsocket.sokol.paper.component.PositionTarget
import org.bukkit.World

interface LaunchOrigin : SokolComponent {
    override val componentType get() = LaunchOrigin::class

    val world: World
    val transform: Transform
}

object LaunchOriginTarget : SokolSystem

@All(PositionRead::class)
@Before(LaunchOriginTarget::class)
@After(PositionTarget::class)
class LaunchOriginPositionedInjectorSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mLaunchOrigin = mappers.componentMapper<LaunchOrigin>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val positionRead = mPositionRead.get(entity)
        mLaunchOrigin.set(entity, object : LaunchOrigin {
            override val world get() = positionRead.world
            override val transform get() = positionRead.transform
        })
    }
}

@All(ItemHolder::class)
@Before(LaunchOriginTarget::class)
class LaunchOriginItemInjectorSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mItemHolder = mappers.componentMapper<ItemHolder>()
    private val mLaunchOrigin = mappers.componentMapper<LaunchOrigin>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val itemHolder = mItemHolder.get(entity)
        if (itemHolder !is ItemHolder.InEquipment || !itemHolder.slot.isHand) return

        val mob = itemHolder.mob
        mLaunchOrigin.set(entity, object : LaunchOrigin {
            override val world get() = mob.world
            override val transform get() = mob.eyeLocation.transform()
        })
    }
}
