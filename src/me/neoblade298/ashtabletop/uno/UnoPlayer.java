package me.neoblade298.ashtabletop.uno;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.neoblade298.ashtabletop.GamePlayer;

public class UnoPlayer extends GamePlayer {
	final List<UnoCard> hand = new ArrayList<>();
	long points;
	boolean calledUno;

	public UnoPlayer(Player player) {
		super(player.getUniqueId(), player);
	}
}
