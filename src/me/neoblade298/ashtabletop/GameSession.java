package me.neoblade298.ashtabletop;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import me.neoblade298.neocore.bungee.BungeeCore;
import me.neoblade298.neocore.bungee.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public abstract class GameSession<T extends GamePlayer> {
	protected String name;
	protected Game game;
	protected UUID host;
	protected boolean isPublic;
	protected HashMap<String, GameParameter> params = new HashMap<String, GameParameter>();
	public GameSession(String name, Game game, UUID host, boolean isPublic) {
		this.name = name;
		this.game = game;
		this.host = host;
		this.isPublic = isPublic;
	}
	public GameSession(String name, Game game, UUID host, boolean isPublic, HashMap<String, GameParameter> params) {
		this(name, game, host, isPublic);
		this.params = params;
	}
	public abstract void broadcast(String msg);
	public String getName() {
		return name;
	}
	public Game getGame() {
		return game;
	}
	public UUID getHost() {
		return host;
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	public boolean isPublic() {
		return isPublic;
	}
	public void setHost(CommandSource cmdUser, String username) {
		Player cmdr = (Player) cmdUser;
		Optional<Player> p = BungeeCore.proxy().getPlayer(username);
		if (p.isEmpty()) {
			Util.msgRaw(cmdUser, Component.text("That player isn't online!", NamedTextColor.RED));
			return;
		}
		
		host = p.get().getUniqueId();
		broadcast("<yellow>" + cmdr.getUsername() + " <gray>set the game's host to </gray>" + p.get().getUsername());
	}
	public HashMap<String, GameParameter> getParameters() {
		return params;
	}
	public abstract void leavePlayer(Player p);
	public abstract void kickPlayer(Player s, String username);
	public abstract void adminKickPlayer(CommandSource s, String username);
	public abstract void displayInfo(Player viewer, Player viewed);
}
