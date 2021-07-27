package taboolib.module.ui.receptacle

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.submit
import taboolib.module.nms.nmsProxy

/**
 * @author Arasple
 * @date 2020/11/29 10:38
 */
open class Receptacle(var type: ReceptacleType, title: String = type.toBukkitType().defaultTitle) {

    private var viewer: Player? = null

    //自动更新时间 tick
    private var refreshTime: Long = 0L

    private var onOpen: ((player: Player, receptacle: Receptacle) -> Unit) = { _, _ -> }

    private var onClose: ((player: Player, receptacle: Receptacle) -> Unit) = { _, _ -> }

    private var onClick: ((player: Player, event: ReceptacleInteractEvent) -> Unit) = { _, _ -> }

    private val contents = arrayOfNulls<ItemStack?>(type.totalSize)

    private var hidePlayerInventory = false

    private var stateId = 1
        get() {
            return field++
        }

    var title = title
        set(value) {
            field = value
            submit(delay = 3, async = true) {
                initializationPackets()
            }
        }

    fun hidePlayerInventory(hidePlayerInventory: Boolean) {
        this.hidePlayerInventory = hidePlayerInventory
    }

    fun getItem(slot: Int): ItemStack? {
        setupPlayerInventorySlots()
        return contents.getOrNull(slot)
    }

    fun hasItem(slot: Int): Boolean {
        return getItem(slot) != null
    }

    fun setItem(itemStack: ItemStack? = null, slots: Collection<Int>, display: Boolean = true) {
        setItem(itemStack, *slots.toIntArray(), display = display)
    }

    fun setItem(itemStack: ItemStack? = null, vararg slots: Int, display: Boolean = true) {
        slots.forEach { contents[it] = itemStack }
        if (display && viewer != null) {
            slots.forEach { PacketWindowSetSlot(it, itemStack, stateId = stateId).send(viewer!!) }
        }
    }

    fun clear(display: Boolean = true) {
        contents.indices.forEach { contents[it] = null }
        if (display) {
            refresh()
        }
    }

    fun refresh(slot: Int = -1) {
        if (viewer != null) {
            setupPlayerInventorySlots()
            if (slot >= 0) {
                PacketWindowSetSlot(slot, contents[slot], stateId = stateId).send(viewer!!)
            } else {
                PacketWindowItems(contents).send(viewer!!)
            }
        }
    }

    fun onOpen(handler: (player: Player, receptacle: Receptacle) -> Unit) {
        this.onOpen = handler
    }

    fun onClose(handler: (player: Player, receptacle: Receptacle) -> Unit) {
        this.onClose = handler
    }

    fun onClick(clickEvent: (player: Player, event: ReceptacleInteractEvent) -> Unit) {
        this.onClick = clickEvent
    }

    fun open(player: Player) {
        viewer = player
        initializationPackets()
        player.setViewingReceptacle(this)
        onOpen(player, this)
        autoRefresh()
    }

    fun close(sendPacket: Boolean = true) {
        if (viewer != null) {
            if (sendPacket) {
                PacketWindowClose().send(viewer!!)
            }
            onClose(viewer!!, this)
            viewer!!.setViewingReceptacle(null)
        }
    }

    internal fun callEventClick(event: ReceptacleInteractEvent) {
        if (viewer != null) {
            onClick(viewer!!, event)
        }
    }

    internal fun initializationPackets() {
        if (viewer != null) {
            nmsProxy<NMS>().sendInventoryPacket(viewer!!, PacketWindowOpen(type, title))
            refresh()
        }
    }

    internal fun setupPlayerInventorySlots() {
        if (hidePlayerInventory || viewer == null) {
            return
        }
        viewer!!.inventory.contents.forEachIndexed { index, itemStack ->
            if (itemStack != null) {
                val slot = when (index) {
                    in 0..8 -> type.hotBarSlots[index]
                    in 9..35 -> type.mainInvSlots[index - 9]
                    else -> -1
                }
                if (slot > 0) {
                    contents[slot] = itemStack
                }
            }
        }
    }

    private fun autoRefresh() {
        if (refreshTime <= 0) {
            return
        }
        submit(delay = refreshTime, async = true) {
            autoRefresh()
            if (viewer != null) {
                setupPlayerInventorySlots()
            }
            PacketWindowItems(contents).send(viewer!!)
        }
    }

}
