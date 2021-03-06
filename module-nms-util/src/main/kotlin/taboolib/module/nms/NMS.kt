package taboolib.module.nms

import com.google.gson.JsonObject
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.submit
import taboolib.common.reflect.Reflex.Companion.reflex
import taboolib.common.reflect.Reflex.Companion.reflexInvoke
import taboolib.common.reflect.Reflex.Companion.staticInvoke
import taboolib.module.nms.i18n.I18n
import taboolib.module.nms.type.LightType
import taboolib.module.nms.type.Toast
import taboolib.module.nms.type.ToastBackground
import taboolib.module.nms.type.ToastFrame
import taboolib.platform.BukkitPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

private val classJsonElement = Class.forName("com.google.gson.JsonElement")

private val scoreboardMap = ConcurrentHashMap<String, PacketScoreboard>()

private val toastMap = ConcurrentHashMap<Toast, NamespacedKey>()

val nmsGeneric = nmsProxy(NMSGeneric::class.java)

val nmsScoreboard = nmsProxy(NMSScoreboard::class.java)

/**
 * 获取物品NBT数据
 */
fun ItemStack.getItemTag(): ItemTag {
    return nmsGeneric.getItemTag(this)
}

/**
 * 写入物品NBT数据
 */
fun ItemStack.setItemTag(itemTag: ItemTag): ItemStack {
    return nmsGeneric.setItemTag(this, itemTag)
}

/**
 * 获取物品内部名称
 */
fun ItemStack.getInternalKey(): String {
    return nmsGeneric.getKey(this)
}

/**
 * 获取物品内部名称
 */
fun ItemStack.getInternalName(): String {
    return nmsGeneric.getName(this)
}

fun ItemStack.getI18nName(player: Player? = null): String {
    return I18n.get().getName(player, this)
}

/**
 * 获取实体内部名称
 */
fun Entity.getInternalName(): String {
    return nmsGeneric.getName(this)
}

fun Entity.getI18nName(player: Player? = null): String {
    return I18n.get().getName(player, this)
}

/**
 * 获取附魔内部名称
 */
fun Enchantment.getInternalName(): String {
    return nmsGeneric.getEnchantmentKey(this)
}

fun Enchantment.getI18nName(player: Player? = null): String {
    return I18n.get().getName(player, this)
}

/**
 * 获取药水效果内部名称
 */
fun PotionEffectType.getInternalName(): String {
    return nmsGeneric.getPotionEffectTypeKey(this)
}

fun PotionEffectType.getI18nName(player: Player? = null): String {
    return I18n.get().getName(player, this)
}

/**
 * 生成实体并在生成之前执行特定行为
 */
fun <T : Entity> Location.spawnEntity(entity: Class<T>, func: Consumer<T>) {
    nmsGeneric.spawnEntity(this, entity, func)
}

/**
 * 创建光源
 */
fun Block.createLight(lightLevel: Int, lightType: LightType = LightType.ALL, update: Boolean = true): Boolean {
    if (nmsGeneric.getRawLightLevel(this, lightType) > lightLevel) {
        nmsGeneric.deleteLight(this, lightType)
    }
    val result = nmsGeneric.createLight(this, lightType, lightLevel)
    if (update) {
        // 更新邻边区块 (为了防止光只在一个区块的尴尬局面)
        (-1..1).forEach { x ->
            (-1..1).forEach { z ->
                nmsGeneric.updateLight(world.getChunkAt(chunk.x + x, chunk.z + z))
            }
        }
    }
    return result
}

/**
 * 删除光源
 */
fun Block.deleteLight(lightType: LightType = LightType.ALL, update: Boolean = true): Boolean {
    val result = nmsGeneric.deleteLight(this, lightType)
    if (update) {
        (-1..1).forEach { x ->
            (-1..1).forEach { z ->
                nmsGeneric.updateLight(world.getChunkAt(chunk.x + x, chunk.z + z))
            }
        }
    }
    return result
}

/**
 * 发送记分板数据包
 * @param content 记分板内容（设置为空时注销记分板）
 */
fun Player.sendScoreboard(vararg content: String) {
    if (scoreboardMap.containsKey(name)) {
        scoreboardMap[name]?.run {
            sendTitle(this@sendScoreboard, content.firstOrNull().toString())
            sendContent(this@sendScoreboard, content.filterIndexed { index, _ -> index > 0 })
        }
    } else {
        scoreboardMap[name] = PacketScoreboard(this, content.firstOrNull().toString(), content.filterIndexed { index, _ -> index > 0 })
    }
}

/**
 * 发送虚拟 Toast 成就信息
 * @param icon 图标
 * @param message 信息
 * @param frame 成就框架
 * @param background 成就背景图片
 */
fun Player.sendToast(icon: Material, message: String, frame: ToastFrame = ToastFrame.TASK, background: ToastBackground = ToastBackground.ADVENTURE) {
    submit {
        val cache = Toast(icon, message, frame)
        val namespaceKey = toastMap.computeIfAbsent(cache) {
            inject(
                NamespacedKey(BukkitPlugin.getInstance(), "toast_${UUID.randomUUID()}"),
                toJsonToast(icon.reflexInvoke<Any>("getKey").toString(), message, frame, background)
            )
        }
        // 注册成就
        grant(this@sendToast, namespaceKey)
        // 延迟注销，否则会出问题
        submit(delay = 20) {
            revoke(this@sendToast, namespaceKey)
        }
    }
}

private fun grant(player: Player, key: NamespacedKey) {
    val advancement = Bukkit.getAdvancement(key)!!
    if (!player.getAdvancementProgress(advancement).isDone) {
        player.getAdvancementProgress(advancement).remainingCriteria.forEach {
            player.getAdvancementProgress(advancement).awardCriteria(it)
        }
    }
}

private fun revoke(player: Player, key: NamespacedKey) {
    val advancement = Bukkit.getAdvancement(key)
    if (advancement != null && player.getAdvancementProgress(advancement).isDone) {
        player.getAdvancementProgress(advancement).awardedCriteria.forEach {
            player.getAdvancementProgress(advancement).revokeCriteria(it)
        }
    }
}

private fun inject(key: NamespacedKey, toast: JsonObject): NamespacedKey {
    if (Bukkit.getAdvancement(key) == null) {
        val localMinecraftKey = obcClass("util.CraftNamespacedKey").staticInvoke<Any>("toMinecraft", key)
        val localMinecraftServer = nmsClass("MinecraftServer").staticInvoke<Any>("getServer")!!
        val localLootPredicateManager = localMinecraftServer.reflexInvoke<Any>("getLootPredicateManager")
        val localSerializedAdvancement = nmsClass("Advancement\$SerializedAdvancement").staticInvoke<Any>(
            "a",
            toast,
            nmsClass("LootDeserializationContext")
                .getDeclaredConstructor(localMinecraftKey!!.javaClass, localLootPredicateManager!!.javaClass)
                .newInstance(localMinecraftKey, localLootPredicateManager)
        )
        if (localSerializedAdvancement != null) {
            localMinecraftServer.reflexInvoke<Any>("getAdvancementData")!!.reflex<Any>("REGISTRY")!!
                .reflexInvoke<Any>("a", HashMap(Collections.singletonMap(localMinecraftKey, localSerializedAdvancement)))
        }
    }
    return key
}

private fun eject(key: NamespacedKey): NamespacedKey {
    try {
        Bukkit.getUnsafe().removeAdvancement(key)
        val console = Bukkit.getServer().reflex<Any>("console")!!
        val advancements = console.reflexInvoke<Any>("getAdvancementData")!!.reflex<MutableMap<Any, Any>>("REGISTRY/advancements")!!
        for ((k, v) in advancements) {
            if (v.reflex<Any>("name/key") == key.key) {
                advancements.remove(k)
                break
            }
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }
    return key
}

private fun toJsonToast(icon: String, title: String, frame: ToastFrame, background: ToastBackground): JsonObject {
    val json = JsonObject()
    json.add("display", JsonObject().run {
        this.add("icon", JsonObject().run {
            this.addProperty("item", icon)
            this
        })
        this.addProperty("title", title)
        this.addProperty("description", "")
        this.addProperty("background", background.url)
        this.addProperty("frame", frame.name.lowercase(Locale.getDefault()))
        this.addProperty("announce_to_chat", false)
        this.addProperty("show_toast", true)
        this.addProperty("hidden", true)
        this
    })
    json.add("criteria", JsonObject().run {
        this.add("IMPOSSIBLE", JsonObject().run {
            this.addProperty("trigger", "minecraft:impossible")
            this
        })
        this
    })
    return json
}

/**
 * 记分板缓存
 */
private class PacketScoreboard(player: Player, title: String, context: List<String>) {

    private var currentTitle = ""
    private val currentContent = HashMap<Int, String>()

    init {
        nmsScoreboard.setupScoreboard(player, false)
        nmsScoreboard.display(player)
        sendTitle(player, title)
        sendContent(player, context)
    }

    fun sendTitle(player: Player, title: String) {
        if (currentTitle != title) {
            currentTitle = title
            nmsScoreboard.setDisplayName(player, title)
        }
    }

    fun sendContent(player: Player, lines: List<String>) {
        nmsScoreboard.changeContent(player, lines, currentContent)
        currentContent.clear()
        currentContent.putAll(lines.mapIndexed { index, s -> index to s }.toMap())
    }
}