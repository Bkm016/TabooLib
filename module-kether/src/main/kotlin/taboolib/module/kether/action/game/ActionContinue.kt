package taboolib.module.kether.action.game

import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture


/**
 * @author IzzelAliz
 */
class ActionContinue : ScriptAction<Void>() {

    override fun run(frame: ScriptFrame): CompletableFuture<Void> {
        val s = frame.script()
        s.listener?.complete(null)
        s.listener = null
        s.event = null
        return CompletableFuture.completedFuture(null)
    }

    internal object Parser {

        @KetherParser(["continue"])
        fun parser() = scriptParser {
            ActionContinue()
        }
    }
}