package taboolib.platform

import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandCallable
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.TranslatableText
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.text.format.TextStyles
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import taboolib.common.platform.*
import java.util.*

/**
 * TabooLib
 * taboolib.platform.SpongeCommand
 *
 * @author sky
 * @since 2021/7/4 2:45 下午
 */
@PlatformSide([Platform.SPONGE_API_7])
class Sponge7Command : PlatformCommand {

    override fun registerCommand(
        command: CommandStructure,
        executor: CommandExecutor,
        completer: CommandCompleter,
        commandBuilder: CommandBuilder.CommandBase.() -> Unit,
    ) {
        Sponge.getCommandManager().register(Sponge7Plugin.getInstance(), object : CommandCallable {

            override fun process(source: CommandSource, arguments: String): CommandResult {
                return if (executor.execute(adaptCommandSender(source), command, command.name, arguments.split(" ").toTypedArray())) {
                    CommandResult.success()
                } else {
                    CommandResult.empty()
                }
            }

            override fun getSuggestions(source: CommandSource, arguments: String, targetPosition: Location<World>?): MutableList<String> {
                return completer.execute(adaptCommandSender(source), command, command.name, arguments.split(" ").toTypedArray())?.toMutableList() ?: ArrayList()
            }

            override fun testPermission(source: CommandSource): Boolean {
                return source.hasPermission(command.permission)
            }

            override fun getShortDescription(source: CommandSource): Optional<Text> {
                return Optional.of(Text.of(command.description))
            }

            override fun getHelp(source: CommandSource): Optional<Text> {
                return Optional.of(Text.of(command.usage))
            }

            override fun getUsage(source: CommandSource): Text {
                return Text.of(command.usage)
            }
        }, command.name, *command.aliases.toTypedArray())
    }

    override fun unregisterCommand(command: String) {
        Sponge.getCommandManager().commands.filter { it.primaryAlias == command }.onEach {
            Sponge.getCommandManager().removeMapping(it)
        }
    }

    override fun unregisterCommands() {
        Sponge.getCommandManager().getOwnedBy(Sponge7Plugin.getInstance()).onEach {
            Sponge.getCommandManager().removeMapping(it)
        }
    }

    override fun unknownCommand(sender: ProxyCommandSender, command: String, state: Int) {
        when (state) {
            1 -> sender.cast<CommandSource>().sendMessage(TranslatableText.builder("command.unknown.command").color(TextColors.RED).build())
            2 -> sender.cast<CommandSource>().sendMessage(TranslatableText.builder("command.unknown.command").color(TextColors.RED).build())
            else -> return
        }
        val components = ArrayList<Text>()
        components += Text.of(command)
        components += TranslatableText.builder("command.context.here").color(TextColors.RED).style(TextStyles.ITALIC).build()
        sender.cast<CommandSource>().sendMessage(Text.join(components))
    }
}