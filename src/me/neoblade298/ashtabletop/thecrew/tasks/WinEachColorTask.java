package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import net.kyori.adventure.text.Component;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard.CardType;
import org.bukkit.command.CommandSender;
import me.neoblade298.neocore.shared.io.Section;

public class WinEachColorTask extends TheCrewTask {
	protected HashMap<CardType, Integer> cardsRemaining = new HashMap<CardType, Integer>();
	protected HashSet<CardType> cardsWon = new HashSet<CardType>();
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "cardsRemaining: " + cardsRemaining);
	}
	
	public WinEachColorTask(Section cfg) {
		super(cfg);
		display = Component.text("Win at least one card of each color");
	}
	
	public WinEachColorTask(TheCrewPlayer owner, WinEachColorTask src, TheCrewInstance inst) {
		super(owner, src, inst);
		reset();
	}

	@Override
	public WinEachColorTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinEachColorTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (winner.equals(owner)) return false;
		
		HashMap<CardType, Integer> cardsLost = new HashMap<CardType, Integer>();
		for (TheCrewCard card : pile) {
			if (card.getType() == CardType.SUB) continue;
			if (cardsWon.contains(card.getType())) continue;
			
			cardsLost.put(card.getType(), cardsLost.getOrDefault(card.getType(), 0) + 1);
		}
		
		for (CardType type : CardType.values()) {
			if (type == CardType.SUB) continue;
			
			if (cardsRemaining.getOrDefault(type, 0) - cardsLost.getOrDefault(type, 0) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (!winner.equals(owner)) return false;
		for (TheCrewCard card : pile) {
			if (card.getType() == CardType.SUB) continue;
			cardsWon.add(card.getType());
		}
		
		if (cardsWon.size() == 4) {
			return true;
		}
		return false;
	}

	@Override
	public void reset() {
		cardsWon.clear();
		cardsRemaining.clear();
	}
}
