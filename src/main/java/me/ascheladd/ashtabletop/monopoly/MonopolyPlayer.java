package me.ascheladd.ashtabletop.monopoly;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.ascheladd.ashtabletop.GamePlayer;

public class MonopolyPlayer extends GamePlayer {
	record Debt(long amount, MonopolyPlayer creditor) {}

	long money;
	int position, jailTurns, jailFree;
	boolean jailed, bankrupt;
	final char token;
	final List<Debt> debts = new ArrayList<>();

	public MonopolyPlayer(Player player, int money, char token) {
		super(player.getUniqueId(), player);
		this.money = money;
		this.token = token;
	}

	long bills() {
		return debts.stream().mapToLong(Debt::amount).sum();
	}
}
