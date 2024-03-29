package taboolib.module.configuration

import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependency
import taboolib.common.inject.Injector
import taboolib.common.platform.Awake
import taboolib.common.platform.releaseResourceFile
import taboolib.common.reflect.Ref
import taboolib.common5.FileWatcher
import java.io.File
import java.lang.reflect.Field
import java.util.concurrent.CopyOnWriteArraySet

@RuntimeDependency("!org.yaml:snakeyaml:1.28", test = "!org.yaml.snakeyaml.Yaml")
@Awake
object ConfigLoader : Injector.Fields {

    val files = HashMap<String, ConfigFile>()

    override fun inject(field: Field, clazz: Class<*>, instance: Any) {
        if (field.isAnnotationPresent(Config::class.java)) {
            val file = releaseResourceFile(field.getAnnotation(Config::class.java).value)
            if (field.getAnnotation(Config::class.java).migrate) {
                val resourceAsStream = clazz.classLoader.getResourceAsStream(file.name)
                if (resourceAsStream != null) {
                    val bytes = resourceAsStream.migrateTo(file.inputStream())
                    if (bytes != null) {
                        file.writeBytes(bytes)
                    }
                }
            }
            val conf = SecuredFile.loadConfiguration(file)
            val configFile = ConfigFile(conf, file)
            try {
                // ClassCastException
                Ref.put(instance, field, conf)
            } catch (ex: Throwable) {
                ex.printStackTrace()
                return
            }
            if (isFileWatcherHook) {
                FileWatcher.INSTANCE.addSimpleListener(file) {
                    if (file.exists()) {
                        conf.load(file)
                    }
                }
            }
            conf.onReload {
                configFile.nodes.forEach { NodeLoader.inject(it, clazz, instance) }
            }
            files[file.name] = configFile
        }
    }

    override val priority: Byte
        get() = 0

    override val lifeCycle: LifeCycle
        get() = LifeCycle.INIT

    @Awake
    object NodeLoader : Injector.Fields {

        override fun inject(field: Field, clazz: Class<*>, instance: Any) {
            if (field.isAnnotationPresent(ConfigNode::class.java)) {
                val node = field.getAnnotation(ConfigNode::class.java)
                val file = files[node.bind] ?: return
                file.nodes += field
                Ref.put(instance, field, file.conf.get(node.value.ifEmpty { field.name }))
            }
        }

        override val priority: Byte
            get() = 1

        override val lifeCycle: LifeCycle
            get() = LifeCycle.INIT
    }

    class ConfigFile(val conf: SecuredFile, val file: File) {

        val nodes = CopyOnWriteArraySet<Field>()
    }

    val isFileWatcherHook by lazy {
        try {
            FileWatcher.INSTANCE
            true
        } catch (ex: NoClassDefFoundError) {
            false
        }
    }
}