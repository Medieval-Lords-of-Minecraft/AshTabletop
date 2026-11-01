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

public class WinOneColorTask extends TheCrewTask {
	protected HashMap<CardType, Integer> numWon = new HashMap<CardType, Integer>();
	protected HashSet<CardType> colorsRemaining = new HashSet<CardType>();
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "numWon: " + numWon);
		Util.msgRaw(s, "colorsRemaining: " + colorsRemaining);
	}
	
	public WinOneColorTask(Section cfg) {
		super(cfg);
		display = Component.text("Win all cards of at least one color");
	}
	
	public WinOneColorTask(TheCrewPlayer owner, WinOneColorTask src, TheCrewInstance inst) {
		super(owner, src, inst);
		reset();
	}

	@Override
	public WinOneColorTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinOneColorTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (winner.getUniqueId().equals(owner.getUniqueId())) {
			return false; // Can only fail if not winner
		}
		
		HashSet<CardType> toRemove = new HashSet<CardType>();
		int colorsRemoved = 0;
		for (TheCrewCard card : pile) {
			if (!toRemove.contains(card.getType()) && colorsRemaining.contains(card.getType())) {
				toRemove.add(card.getType());
				colorsRemoved++; // Remove a color if it's in colorsRemaining and hasn't been removed already
			}
		}
		
		if (colorsRemaining.size() == colorsRemoved) {
			return true;
		}
		return false;
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		for (TheCrewCard card : pile) {
			colorsRemaining.remove(card.getType());
			if (winner.getUniqueId().equals(owner.getUniqueId())) {
				int put = numWon.getOrDefault(card.getType(), 0) + 1;
				numWon.put(card.getType(), put);
				
				if (put == 9) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void reset() {
		for (CardType type : CardType.values()) {
			if (type == CardType.SUB) continue;
			colorsRemaining.add(type);
		}
		numWon.clear();
	}
}
