package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard.CardType;
import com.velocitypowered.api.command.CommandSource;
import me.neoblade298.neocore.shared.io.Section;
import me.neoblade298.neocore.shared.util.SharedUtil;

public class WinTrickTotalEqualTask extends TheCrewTask {
	protected ArrayList<Integer> totals = new ArrayList<Integer>();
	
	@Override
	public void showDebug(CommandSource s) {
		Util.msgRaw(s, "totals: " + totals);
	}
	
	public WinTrickTotalEqualTask(Section cfg) {
		super(cfg);
		
		for (int i : cfg.getIntList("totals")) {
			totals.add(i);
		}

		display = SharedUtil.color("Win a trick with a total value equal to <yellow>" + totals.get(0) + " </yellow>or <yellow>"
				+ totals.get(1) + "</yellow>. *No subs");
	}
	
	public WinTrickTotalEqualTask(TheCrewPlayer owner, WinTrickTotalEqualTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.totals = src.totals;
	}

	@Override
	public WinTrickTotalEqualTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinTrickTotalEqualTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		int total = 0;
		boolean containsSub = false;
		if (winner.equals(owner)) {
			for (TheCrewCard card : pile) {
				if (card.getType() == CardType.SUB) {
					containsSub = true;
					break;
				}
				
				total += card.getValue();
			}
		}
		
		if (!containsSub && (total == 22 || total == 23)) {
			return false; // succeeds
		}

		// Check if it's possible using existing player hands
		for (TheCrewCard card : owner.getHand()) {
			if (checkTaskBruteForce(card, inst.getTurnOrder(), 0, 0)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		int total = 0;
		boolean containsSub = false;
		if (winner.equals(owner)) {
			for (TheCrewCard card : pile) {
				if (card.getType() == CardType.SUB) {
					containsSub = true;
					break;
				}
				
				total += card.getValue();
			}
		}
		
		return !containsSub && (total == 22 || total == 23);
	}

	@Override
	public void reset() {}
	
	private boolean checkTaskBruteForce(TheCrewCard topCard, ArrayList<TheCrewPlayer> players, int turn, int sum) {
		if (players.get(turn).equals(owner)) {
			return checkTaskBruteForce(topCard, players, turn + 1, sum); // Skip this since they always start
		}
		
		for (TheCrewCard card : players.get(turn).getHand()) {
			if (card.getType() == CardType.SUB) continue;
			if (!isPlayable(topCard, card, players.get(turn))) continue;

			// Recurse case
			if (turn + 1 < players.size()) {
				if (checkTaskBruteForce(topCard, players, turn + 1, sum + card.getValue())) {
					return true;
				}
			}
			
			// Base case
			else {
				int temp = sum + card.getValue();
				if (temp == 22 || temp == 23) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isPlayable(TheCrewCard topCard, TheCrewCard toPlay, TheCrewPlayer p) {
		if (topCard.getType() == toPlay.getType()) { // If card is same type,  can play
			return true;
		}
		
		// If card is different type, check if there is a same type
		for (TheCrewCard card : p.getHand()) {
			if (card.getType() == topCard.getType()) {
				return false;
			}
		}
		return true;
	}
}
