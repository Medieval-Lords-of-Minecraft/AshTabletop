package me.ascheladd.ashtabletop.monopoly;

import java.util.UUID;

import me.ascheladd.ashtabletop.GameInstance;
import me.ascheladd.ashtabletop.GameLobby;
import me.ascheladd.ashtabletop.GameParameter;
import me.ascheladd.ashtabletop.IntegerParameterSetter;

public class MonopolyLobby extends GameLobby<MonopolyPlayer> {
	public MonopolyLobby(String name, UUID host, boolean isPublic) {
		super(name, Monopoly.inst(), host, isPublic);
		params.put("money", new GameParameter("money", "Starting money",
				"Starting cash per player; NeoMonopoly's default is $6000.",
				6000, new IntegerParameterSetter(1, 1000000)));
	}

	public MonopolyLobby(MonopolyInstance instance) {
		super(instance);
	}

	@Override
	public GameInstance<MonopolyPlayer> onStart() {
		return new MonopolyInstance(this);
	}
}
