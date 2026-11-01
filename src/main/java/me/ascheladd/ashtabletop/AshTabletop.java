package me.ascheladd.ashtabletop;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import java.util.Locale;
import java.util.logging.Level;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import me.neoblade298.neocore.bukkit.commands.SubcommandManager;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.ascheladd.ashtabletop.commands.*;
import me.ascheladd.ashtabletop.thecrew.commands.*;
import net.kyori.adventure.text.format.NamedTextColor;

public class AshTabletop extends JavaPlugin {
	private static AshTabletop inst;
	private static Logger logger;
	private static File folder;

	@Override
    public void onEnable() {
		inst = this;
		logger = getLogger();
		folder = getDataFolder();
		for (String game : new String[] {"The Crew", "Monopoly", "Uno"}) {
			String resourceDirectory = game.equals("The Crew") ? "thecrew" : game.toLowerCase(Locale.ROOT);
			String[] files = game.equals("The Crew")
					? new String[] {"config.yml", "manual.yml", "tasks.yml"}
					: new String[] {"config.yml", "manual.yml"};
			for (String file : files) {
				File destination = new File(new File(folder, game), file);
				if (!destination.exists()) {
					try (InputStream resource = Objects.requireNonNull(getResource("games/" + resourceDirectory + "/" + file))) {
						Files.createDirectories(destination.getParentFile().toPath());
						Files.copy(resource, destination.toPath());
					}
					catch (IOException e) {
						logger.log(Level.SEVERE, "Unable to install game configuration: " + destination, e);
						getServer().getPluginManager().disablePlugin(this);
						return;
					}
				}
			}
		}
		try {
			getServer().getPluginManager().registerEvents(new GameManager(), this);
		}
		catch (IllegalArgumentException e) {
			logger.log(Level.SEVERE, "Invalid game configuration. Correct the game files before enabling AshTabletop.", e);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		initCommands();
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		GameManager.clear();
	}
	
	private void initCommands() {
		SubcommandManager tt = new SubcommandManager("tt", "tabletop.use", NamedTextColor.RED, this);
		tt.registerCommandList("");
		tt.register(new CmdTabletopCreate("create", "Creates a lobby for a game", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopInvite("invite", "Invites players to your lobby", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopInfo("info", "View info of the player's current session", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopLeave("leave", "Leave your current session", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopLobbies("lobbies", "View a list of active lobbies", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopGames("games", "View a list of all playable games", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopInstances("instances", "View a list of all active games", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopJoin("join", "Join a lobby", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopKick("kick", "Kick a player from your session", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopKicklist("kicklist", "View moderation list", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopPublic("public", "Set your lobby to public", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopPrivate("private", "Set your lobby to private", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopStart("start", "Starts the game", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopSpectate("spectate", "Spectate an active game", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopSetParameter("set", "Sets a game parameter", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopSetHost("sethost", "Sets the game host", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopViewGame("viewgame", "Gets additional info for a game", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopManual("manual", "View a game's manual", null, SubcommandRunner.PLAYER_ONLY));
		tt.register(new CmdTabletopReturn("return", "Return from a game to a lobby", null, SubcommandRunner.PLAYER_ONLY));
		
		SubcommandManager tta = new SubcommandManager("tta", "tabletop.admin", NamedTextColor.DARK_RED, this);
		tta.registerCommandList("");
		tta.register(new CmdTabletopAdminKick("kick", "Force kicks a player from a session", null, SubcommandRunner.BOTH));
		tta.register(new CmdTabletopAdminSetHost("sethost", "Force sets a host for a session", null, SubcommandRunner.BOTH));
		tta.register(new CmdTabletopAdminDisband("end", "Force sets a host for a session", null, SubcommandRunner.BOTH));
		tta.register(new CmdTabletopAdminDebug("debug", "Show debug for an instance", null, SubcommandRunner.PLAYER_ONLY));

		SubcommandManager thecrew = new SubcommandManager("thecrew", "tabletop.use", NamedTextColor.RED, this);
		thecrew.register(new CmdTheCrewPlay("play", "Plays a card in your hand", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewAcceptTasks("accepttasks", "Accepts rolled tasks", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewAcceptTask("accepttask", "Accepts a task", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewPassTask("passtask", "Passes on accepting a task", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewRerollTasks("rerolltasks", "Rerolls tasks", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewViewTasks("viewtasks", "Views assigned tasks", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewViewHand("viewhand", "Views a player's hand", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewMod("mod", "Views moderation list for The Crew", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewRestartRound("restartround", "Restarts the round", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewRestartGame("restartgame", "Restarts the game", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewUseSonars("usesonars", "Uses a sonars", null, SubcommandRunner.PLAYER_ONLY));
		thecrew.register(new CmdTheCrewSonar("sonar", "Brings up sonar menu", null, SubcommandRunner.PLAYER_ONLY));
		Objects.requireNonNull(getCommand("mono")).setExecutor(new ChatGameCommand("monopoly"));
		Objects.requireNonNull(getCommand("uno")).setExecutor(new ChatGameCommand("uno"));
	}
	
	public static AshTabletop inst() {
		return inst;
	}
	
	public static BukkitScheduler scheduler() {
		return Bukkit.getScheduler();
	}
	
	public static Optional<Player> findPlayer(String name) {
		return Optional.ofNullable(Bukkit.getPlayerExact(name));
	}

	public static Optional<Player> findPlayer(UUID uuid) {
		return Optional.ofNullable(Bukkit.getPlayer(uuid));
	}
	
	public static Logger logger() {
		return logger;
	}
	
	public static File folder() {
		return folder;
	}
}
