package taboolib.platform

import cn.nukkit.Server
import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.command.data.CommandData
import taboolib.common.platform.*
import taboolib.common.reflect.Reflex.Companion.getProperty

/**
 * TabooLib
 * taboolib.platform.NukkitCommand
 *
 * @author sky
 * @since 2021/7/3 1:15 上午
 */
@PlatformSide([Platform.NUKKIT])
class NukkitCommand : PlatformCommand {

    val knownCommands = ArrayList<CommandStructure>()

    val registeredCommands by lazy {
        Server.getInstance().commandRegistry.getProperty<MutableMap<String, Command>>("registeredCommands")!!
    }

    override fun registerCommand(
        command: CommandStructure,
        executor: CommandExecutor,
        completer: CommandCompleter,
        commandBuilder: CommandBuilder.CommandBase.() -> Unit,
    ) {
        // TODO: 2021/7/15 Not Support Suggestions
        val registerCommand = object : Command(command.name, CommandData.builder(command.name)
            .setDescription(command.description)
            .setUsageMessage(command.usage)
            .addPermission(command.permission)
            .setPermissionMessage(command.permissionMessage)
            .build()) {

            override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
                return executor.execute(adaptCommandSender(sender), command, commandLabel, args)
            }
        }
        knownCommands += command
        registeredCommands[command.name] = registerCommand
        command.aliases.forEach { registeredCommands[it] = registerCommand }
    }

    override fun unregisterCommand(command: String) {
        registeredCommands.remove(command)
    }

    override fun unregisterCommands() {
        knownCommands.forEach { registeredCommands.remove(it.name) }
    }

    override fun unknownCommand(sender: ProxyCommandSender, command: String, state: Int) {
        when (state) {
            1 -> sender.cast<CommandSender>().sendMessage("§cUnknown or incomplete command, see below for error")
            2 -> sender.cast<CommandSender>().sendMessage("§cIncorrect argument for command")
            else -> return
        }
        sender.cast<CommandSender>().sendMessage("$command§r§c§o<--[HERE]")
    }
}