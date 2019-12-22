package bedwars;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Location;
import org.bukkit.Color;
import org.bukkit.ChatColor;

import java.util.ArrayList;

public final class Main extends JavaPlugin {

	/* GLOBAL VARIABLES */

	int gamephase = 0; // 0 = none, 1 = lobby, 2 = ingame
	FileConfiguration config;
	ArrayList<Team> teams;
	ArrayList<ResourceSpawner> resourcegens;
	Location playloclow, playlochigh, structureloclow, structurelochigh;


	/* COMMAND LISTENER */

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (gamephase == 1) {
			switch (label.toLowerCase()) {
				case "joingame":
					if (!(sender instanceof Player)) return false;
					for (Team team : teams) {
						if (team.name.equals(args[0])) {
							if (team.players.size() >= team.maxplayers) {
								sender.sendMessage(ChatColor.RED + "This team is full!");
							} else {
								team.players.add((Player) sender);
							}
							break;
						}
					}
					break;
				case "startgame":
					startGame();
					break;
			}
		}

		return true;
  }


  /* GAME MANAGER */

	@Override
	public void onEnable() {
		saveDefaultConfig();
		config = getConfig();
	}

	private void initGame() {
		teams = new ArrayList<Team>();
		resourcegens = new ArrayList<ResourceSpawner>();

		String warworld = config.getString("region.world");
		playloclow = getLocFromConfig(config, warworld, "playregion.low");
		playlochigh = getLocFromConfig(config, warworld, "playregion.high");
		structureloclow = getLocFromConfig(config, warworld, "structureregion.low");
		structurelochigh = getLocFromConfig(config, warworld, "structureregion.high");

		ConfigurationSection allteamconfigs = config.getConfigurationSection("teams");
		for (String teamname : allteamconfigs.getKeys(false)) {
			ConfigurationSection teamconfig = allteamconfigs.getConfigurationSection(teamname);
			// do something with color
			Location spawn = getLocFromConfig(teamconfig, warworld, "spawn");
			Location gen = getLocFromConfig(teamconfig, warworld, "generator");
			Location bed = getLocFromConfig(teamconfig, warworld, "bed");
			Team newteam = new Team(teamname, Color.AQUA, spawn, gen, bed, config.getInt("maxplayersperteam"));
			teams.add(newteam);
		}

		gamephase = 1;
	}

	private void startGame() {
		//clone world
		for (Team team : teams) {
			for (Player p : team.players) {
				p.teleport(team.spawn);
			}
			ItemStack is = new ItemStack(Material.IRON_INGOT);
			ResourceSpawner spawner = new ResourceSpawner(is, team.generator);
			resourcegen.add(spawner);
			spawner.runTaskTimer(this, 10, 10);
		}

		gamephase = 2;
	}

	private void endGame() {
		for (ResourceSpawner rs : resourcegens) {
			rs.cancel();
		}

		gamephase = 0;
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		for (Team team : teams) {
			for (int i=0;i<team.size();i++) {
				if (team.get(i).getUniqueId().equals(e.getPlayer().getUniqueId())) {
					team.remove(i);
					break;
				}
			}
		}
	}



	/* IN-GAME EVENT LISTENERS */

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		Material blocktype = e.getBlock().getType();
		if (blocktype == Material.RED_BED) {
			for (Team team : teams) {
				if (e.getBlock().getLocation().distance(team.bed) < 3) {
					team.hasbed = false;
					for (Player p : team.players) {
						p.sendTitle(ChatColor.RED + "BED DESTROYED", "");
					}
				}
			}
		} else {
		}
	}


	/* HELPER FUNCTIONS */

	Location getLocFromConfig(ConfigurationSection configsec, String world, String base) {
		return new Location(Bukkit.getWorld(world),
												configsec.getInt(base + ".x"),
												configsec.getInt(base + ".y"),
												configsec.getInt(base + ".z"));
	}

}

class ResourceSpanwner extends BukkitRunnable {

	ItemStack item;
	Location loc;

	public ResourceSpawner(ItemStack item, Location loc) {
		this.item = item;
		this.loc = loc;
	}

	@Override
	public void run() {
		loc.getWorld().dropItem(loc, item);
	}

}