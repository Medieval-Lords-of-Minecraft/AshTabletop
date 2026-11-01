package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import net.kyori.adventure.text.Component;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard.CardType;
import com.velocitypowered.api.command.CommandSource;
import me.neoblade298.neocore.shared.io.Section;

public class WinTrickEvenOddTask extends TheCrewTask {
	protected boolean even; // Either even or odd
	
	@Override
	public void showDebug(CommandSource s) {
		Util.msgRaw(s, "even: " + even);
	}
	
	public WinTrickEvenOddTask(Section cfg) {
		super(cfg);

		even = cfg.getString("test").equalsIgnoreCase("even");
		
		display = Component.text("Win a trick with only " + (even ? "even" : "odd") + " cards. *No subs");
	}
	
	public WinTrickEvenOddTask(TheCrewPlayer owner, WinTrickEvenOddTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.even = src.even;
	}

	@Override
	public WinTrickEvenOddTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinTrickEvenOddTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (update(inst, winner, pile)) return false;
		
		for (TheCrewPlayer p : inst.getPlayers().values()) {
			boolean canPlayCard = false;
			for (TheCrewCard card : p.getHand()) {
				if (even ? card.getValue() % 2 == 0 : card.getValue() % 2 == 1) {
					canPlayCard = true;
					break;
				}
			}
			if (!canPlayCard) return true;
		}
		return false;
 	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (!winner.equals(owner)) return false;
		
		for (TheCrewCard card : pile) {
			if (card.getType() == CardType.SUB) return false;
			if (even ? card.getValue() % 2 == 1 : card.getValue() % 2 == 0) return false;
		}
		return true;
	}

	@Override
	public void reset() {}
}
