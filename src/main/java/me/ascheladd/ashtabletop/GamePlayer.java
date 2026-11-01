package me.ascheladd.ashtabletop;

import java.util.UUID;

import org.bukkit.entity.Player;

public class GamePlayer {
	protected UUID uuid;
	protected String name;
	protected Player p;
	public GamePlayer(UUID uuid, Player p) {
		super();
		this.uuid = uuid;
		this.name = p.getName();
		this.p = p;
	}
	public UUID getUniqueId() {
		return uuid;
	}
	public String getName() {
		return name;
	}
	public Player getPlayer() {
		return p;
	}
	public void reconnect(Player player) {
		if (!uuid.equals(player.getUniqueId())) {
			throw new IllegalArgumentException("Cannot replace a game player with a different UUID.");
		}
		p = player;
		name = player.getName();
	}
	@Override
	public String toString() {
		return name;
	}
}
