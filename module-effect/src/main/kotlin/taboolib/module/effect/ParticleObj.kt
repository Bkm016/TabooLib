package taboolib.module.effect

import taboolib.common.platform.PlatformExecutor.PlatformTask
import taboolib.common.platform.submit
import taboolib.common.util.Location

/**
 * 表示一个特效对象
 *
 * @author Zoyn
 */
abstract class ParticleObj(var spawner: ParticleSpawner) {

    open lateinit var origin: Location
    open var period = 0L

    var showType = ShowType.NONE

    private var running = false
    private var matrix: Matrix? = null
    private var task: PlatformTask? = null

    fun addMatrix(matrix: Matrix) {
        this.matrix = matrix.multiply(this.matrix)
    }

    fun setMatrix(matrix: Matrix?) {
        this.matrix = matrix
    }

    fun removeMatrix() {
        matrix = null
    }

    fun hasMatrix(): Boolean {
        return matrix != null
    }

    abstract fun show()

    open fun alwaysShow() {
        turnOffTask()
        // 此处的延迟 2tick 是为了防止turnOffTask还没把特效给关闭时的缓冲
        submit(delay = 2) {
            running = true
            submit(period = period) {
                if (running) {
                    show()
                }
            }
            showType = ShowType.ALWAYS_SHOW
        }
    }

    open fun alwaysShowAsync() {
        turnOffTask()
        // 此处的延迟 2tick 是为了防止turnOffTask还没把特效给关闭时的缓冲
        submit(delay = 2) {
            running = true
            submit(period = period, async = true) {
                if (running) {
                    show()
                }
            }
            showType = ShowType.ALWAYS_SHOW_ASYNC
        }
    }

    open fun turnOffTask() {
        if (task != null) {
            running = false
            task!!.cancel()
            showType = ShowType.NONE
        }
    }

    /**
     * 通过给定一个坐标就可以使用已经指定的参数来播放粒子
     * @param location 坐标
     */
    fun spawnParticle(location: Location) {
        var showLocation = location
        if (hasMatrix()) {
            val vector = location.clone().subtract(origin).toVector()
            val changed = matrix!!.applyVector(vector)
            showLocation = origin.clone().add(changed)
        }
        spawner.spawn(showLocation)
    }
}