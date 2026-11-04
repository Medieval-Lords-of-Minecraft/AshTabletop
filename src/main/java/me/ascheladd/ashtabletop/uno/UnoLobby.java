package me.ascheladd.ashtabletop.uno;

import java.util.UUID;

import me.ascheladd.ashtabletop.GameInstance;
import me.ascheladd.ashtabletop.GameLobby;
import me.ascheladd.ashtabletop.GameParameter;
import me.ascheladd.ashtabletop.IntegerParameterSetter;

public class UnoLobby extends GameLobby<UnoPlayer> {
	public UnoLobby(UUID host, boolean isPublic) {
		super(Uno.inst(), host, isPublic);
		params.put("points", new GameParameter("points", "Winning score",
				"0 plays continuous rounds; otherwise the first player to reach this score wins.",
				0, new IntegerParameterSetter(0, 100000)));
	}

	public UnoLobby(UnoInstance instance) {
		super(instance);
	}

	@Override
	public GameInstance<UnoPlayer> onStart() {
		return new UnoInstance(this);
	}
}
