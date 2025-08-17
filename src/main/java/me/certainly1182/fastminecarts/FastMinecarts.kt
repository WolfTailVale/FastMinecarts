package me.certainly1182.fastminecarts

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.plugin.java.JavaPlugin

class FastMinecarts : JavaPlugin(), Listener {
    private val VANILLA_MAX_SPEED = 0.4
    private var _blockMaxSpeeds = mutableMapOf<Material, Double>()
    private val railTypes = listOf(
        Material.RAIL, Material.POWERED_RAIL,
        Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL
    )
    override fun onEnable() {
        saveDefaultConfig()
        loadConfig()
        Bukkit.getPluginManager().registerEvents(this, this)
        logger.info("FastMinecarts v${description.version} has been enabled!")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("fastminecarts", ignoreCase = true)) {
            if (args.isEmpty()) {
                sender.sendMessage("${ChatColor.YELLOW}FastMinecarts v${description.version}")
                sender.sendMessage("${ChatColor.YELLOW}Use /fastminecarts reload to reload the configuration.")
                return true
            }

            if (args[0].equals("reload", ignoreCase = true)) {
                if (!sender.hasPermission("fastminecarts.reload")) {
                    sender.sendMessage("${ChatColor.RED}You don't have permission to reload the configuration.")
                    return true
                }

                reloadConfig()
                loadConfig()
                sender.sendMessage("${ChatColor.GREEN}FastMinecarts configuration reloaded successfully!")
                logger.info("${sender.name} reloaded the FastMinecarts configuration.")
                return true
            }

            sender.sendMessage("${ChatColor.RED}Unknown subcommand. Use /fastminecarts reload")
            return true
        }
        return false
    }
    private fun loadConfig() {
        val blockConfig = config.getConfigurationSection("blocks") ?: return
        _blockMaxSpeeds.clear()
        for (key in blockConfig.getKeys(false)) {
            val material = Material.getMaterial(key)
            if (material != null) {
                _blockMaxSpeeds[material] = blockConfig.getDouble(key)
                logger.info("Loaded speed multiplier for $key: ${blockConfig.getDouble(key)}")
            } else {
                logger.warning("Unknown material in config: $key")
            }
        }
        logger.info("Loaded ${_blockMaxSpeeds.size} block speed configurations.")
    }
    @EventHandler(ignoreCancelled = true)
    fun onVehicleMove(event: VehicleMoveEvent) {
        if (event.vehicle !is Minecart) return

        val minecart = event.vehicle as Minecart
        if (minecart.isEmpty) return
        if (minecart.passengers.first() !is Player) return

        val railBlock = event.vehicle.location.block
        if (railBlock.type !in railTypes) return

        val blockBelow = railBlock.getRelative(0, -1, 0)
        val blockMultiplier = _blockMaxSpeeds[blockBelow.type] ?: VANILLA_MAX_SPEED
        minecart.maxSpeed = blockMultiplier
    }

    @EventHandler(ignoreCancelled = true)
    fun onVehicleExit(event: VehicleExitEvent) {
        if (event.vehicle !is Minecart) return
        if (event.exited !is Player) return

        val minecart = event.vehicle as Minecart
        if (minecart.maxSpeed > VANILLA_MAX_SPEED) {
            minecart.maxSpeed = VANILLA_MAX_SPEED
        }
    }
}