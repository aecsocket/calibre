package com.gitlab.aecsocket.calibre.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.calibre.paper.CalibreAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.PersistentComponent
import com.gitlab.aecsocket.sokol.paper.PersistentComponentFactory
import com.gitlab.aecsocket.sokol.paper.PersistentComponentType
import com.gitlab.aecsocket.sokol.paper.component.HostedByItem
import com.gitlab.aecsocket.sokol.paper.component.ItemHolder
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.text
import org.bukkit.inventory.EquipmentSlot
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

data class CalibreTest(
    var num: Int = 0
) : PersistentComponent {
    companion object {
        val Key = CalibreAPI.key("calibre_test")
    }

    override val componentType get() = CalibreTest::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .set("num") { makeInt(num) }

    override fun write(node: ConfigurationNode) {
        node.node("num").set(num)
    }

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = tag.asCompound().run { CalibreTest(
            get("num") { asInt() }
        ) }

        override fun read(node: ConfigurationNode) = CalibreTest(
            node.node("num").get { 0 }
        )

        override fun readFactory(node: ConfigurationNode) = PersistentComponentFactory { CalibreTest() }
    }
}

@All(CalibreTest::class, HostedByItem::class, ItemHolder::class)
class CalibreTestSystem(engine: SokolEngine) : SokolSystem {
    private val mCalibreTest = engine.componentMapper<CalibreTest>()
    private val mItem = engine.componentMapper<HostedByItem>()
    private val mItemHolder = engine.componentMapper<ItemHolder>()

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntityAccess) {
        val calibreTest = mCalibreTest.map(entity)
        val item = mItem.map(entity)
        val itemHolder = mItemHolder.map(entity)

        calibreTest.num += 1
        item.writeMeta { meta ->
            meta.displayName(text("CalibreTest: ${calibreTest.num}"))
        }

        when (itemHolder) {
            is ItemHolder.InEquipment -> {
                if (itemHolder.slot == EquipmentSlot.HAND) {
                    itemHolder.mob.sendActionBar(text("Calibre Test says: ${calibreTest.num}"))
                }
            }
            else -> {}
        }
    }
}
