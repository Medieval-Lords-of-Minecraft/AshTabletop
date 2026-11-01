package me.ascheladd.ashtabletop;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.util.Util;
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
	public void setHost(CommandSender cmdUser, String username) {
		Player cmdr = (Player) cmdUser;
		Optional<Player> p = AshTabletop.findPlayer(username);
		if (p.isEmpty()) {
			Util.msgRaw(cmdUser, Component.text("That player isn't online!", NamedTextColor.RED));
			return;
		}
		UUID candidate = p.get().getUniqueId();
		boolean member = this instanceof GameLobby<?> lobby ? lobby.getPlayers().contains(candidate)
				: this instanceof GameInstance<?> instance && instance.getPlayers().values().stream()
						.anyMatch(player -> player.getUniqueId().equals(candidate));
		if (!member) {
			Util.displayError(cmdr, "The host must be a player in this session.");
			return;
		}
		
		host = p.get().getUniqueId();
		broadcast("<yellow>" + cmdr.getName() + " <gray>set the game's host to </gray>" + p.get().getName());
	}
	public HashMap<String, GameParameter> getParameters() {
		return params;
	}
	public abstract void leavePlayer(Player p);
	public abstract void kickPlayer(Player s, String username);
	public abstract void adminKickPlayer(CommandSender s, String username);
	public abstract void displayInfo(Player viewer, Player viewed);
}
