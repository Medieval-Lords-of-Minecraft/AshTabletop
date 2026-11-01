package me.neoblade298.ashtabletop;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import me.neoblade298.neocore.bungee.BungeeCore;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.chat.MiniMessageManager;
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
		BungeeCore.loadFiles(new File(baseDir, "config.yml"), (cfg, file) -> {
			key = cfg.getString("key");
			name = cfg.getString("name");
			desc = Component.text(cfg.getString("description"), NamedTextColor.GRAY);
			minPlayers = cfg.getInt("min-players");
			maxPlayers = cfg.getInt("max-players");
		});
		BungeeCore.loadFiles(new File(baseDir, "manual.yml"), (cfg, file) -> {
			manual = new Component[cfg.getKeys().size()];
			int i = 0;
			for (String key : cfg.getKeys()) {
				manual[i++] = MiniMessageManager.parseFromYaml(cfg, key);
			}
		});
		
		GameManager.registerGame(this);
		AshTabletop.logger().log(Level.INFO, "Registering " + key + "...");
		
		header = Component.text("<< ", NamedTextColor.GRAY)
				.append(Component.text(name, NamedTextColor.GOLD))
				.append(Component.text(" >>", NamedTextColor.GRAY));
		
		String manualStr = "<dark_gray>[<gray><click:run_command:'/tt manual " +
				key +"'><hover:show_text:'Click here to read the manual!'>Click to read the manual for the game!</hover></click></gray>]";
		manualButton = BungeeCore.miniMessage().deserialize(manualStr);
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
	public void displayInfo(CommandSource p) {
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
