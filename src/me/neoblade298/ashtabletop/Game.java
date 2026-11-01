package me.neoblade298.ashtabletop;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public abstract class Game {
	protected String key, name;
	protected Component desc, manualButton;
	protected int minPlayers = 1, maxPlayers = 4;
	protected Component[] manual;
	
	private static final Component footer = Component.text("=====", NamedTextColor.GRAY);
	private final Component header;
	
	public Game(File baseDir) {
		YamlConfiguration cfg = load(new File(baseDir, "config.yml"));
		key = cfg.getString("key");
		name = cfg.getString("name");
		String description = cfg.getString("description");
		if (key == null || !key.matches("[a-z0-9_-]+") || name == null || name.isBlank()
				|| description == null || !cfg.isInt("min-players") || !cfg.isInt("max-players")) {
			throw new IllegalArgumentException("Invalid game metadata in " + baseDir);
		}
		desc = Component.text(description, NamedTextColor.GRAY);
		minPlayers = cfg.getInt("min-players");
		maxPlayers = cfg.getInt("max-players");
		if (minPlayers < 1 || maxPlayers < minPlayers) {
			throw new IllegalArgumentException("Invalid player limits in " + baseDir);
		}
		YamlConfiguration instructions = load(new File(baseDir, "manual.yml"));
			manual = new Component[instructions.getKeys(false).size()];
			int i = 0;
			for (String page : instructions.getKeys(false)) {
				if (!instructions.isString(page) && !instructions.isList(page)) {
					throw new IllegalArgumentException("Invalid manual page " + page + " in " + baseDir);
				}
				String text = instructions.isString(page)
						? instructions.getString(page) : String.join("", instructions.getStringList(page));
				manual[i++] = NeoCore.miniMessage().deserialize(text);
			}
		if (manual.length == 0) throw new IllegalArgumentException("Empty manual in " + baseDir);
		
		GameManager.registerGame(this);
		AshTabletop.logger().log(Level.INFO, "Registering " + key + "...");
		
		header = Component.text("<< ", NamedTextColor.GRAY)
				.append(Component.text(name, NamedTextColor.GOLD))
				.append(Component.text(" >>", NamedTextColor.GRAY));
		
		String manualStr = "<dark_gray>[<gray><click:run_command:'/tt manual " +
				key +"'><hover:show_text:'Click here to read the manual!'>Click to read the manual for the game!</hover></click></gray>]";
		manualButton = NeoCore.miniMessage().deserialize(manualStr);
	}

	private static YamlConfiguration load(File file) {
		YamlConfiguration cfg = new YamlConfiguration();
		try {
			cfg.load(file);
		}
		catch (IOException | InvalidConfigurationException e) {
			throw new IllegalArgumentException("Unable to load " + file, e);
		}
		return cfg;
	}
	
	public String getKey() {
		return key;
	}
	public String getName() {
		return name;
	}
	public Component getDescription() {
		return desc;
	}
	public Component[] getManual() {
		return manual;
	}
	public int getMinPlayers() {
		return minPlayers;
	}
	public int getMaxPlayers() {
		return maxPlayers;
	}
	public void displayInfo(CommandSender p) {
		Util.msgRaw(p, header);
		Util.msgRaw(p, desc);
		Util.msgRaw(p, footer);
		p.sendMessage(manualButton);
	}
	public void displayManual(Player p) {
		displayManual(p, 1);
	}
	public void displayManual(Player p, int page) {
		page--;
		if (page < 0 || page + 1 > manual.length) {
			Util.displayError(p, "That page in the manual doesn't exist!");
			return;
		}
		p.sendMessage(manual[page]);
	}
	public abstract GameLobby<? extends GamePlayer> createLobby(String name, UUID uuid, boolean isPublic);
}
