package taboolib.module.database

/**
 * TabooLib
 * taboolib.module.database.Action
 *
 * @author sky
 * @since 2021/6/23 11:54 下午
 */
interface Action {

    val query: String

    val elements: List<Any>
}