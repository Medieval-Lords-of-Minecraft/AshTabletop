package me.neoblade298.ashtabletop;

import java.util.UUID;

import com.velocitypowered.api.proxy.Player;

public class GamePlayer {
	protected UUID uuid;
	protected String name;
	protected Player p;
	public GamePlayer(UUID uuid, Player p) {
		super();
		this.uuid = uuid;
		this.name = p.getUsername();
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
	@Override
	public String toString() {
		return name;
	}
}
