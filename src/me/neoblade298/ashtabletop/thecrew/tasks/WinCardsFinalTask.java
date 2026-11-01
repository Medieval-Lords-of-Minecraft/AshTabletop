package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import net.kyori.adventure.text.Component;

import com.velocitypowered.api.command.CommandSource;
import me.neoblade298.neocore.shared.io.Section;

public class WinCardsFinalTask extends TheCrewTask {
	protected CardMatcher card;
	
	@Override
	public void showDebug(CommandSource s) {
		Util.msgRaw(s, "card: " + card);
	}
	
	public WinCardsFinalTask(Section cfg) {
		super(cfg);
		card = new CardMatcher(cfg.getString("card"));

		display = Component.text().content("Win ")
				.append(card.getDisplay())
				.append(Component.text(" on the last trick of the game")).build();
	}
	
	public WinCardsFinalTask(TheCrewPlayer owner, WinCardsFinalTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.card = src.card;
	}

	@Override
	public WinCardsFinalTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinCardsFinalTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (inst.getRoundsLeft() != 0) {
			for (TheCrewCard card : pile) {
				if (this.card.match(card)) {
					return true;
				}
			}
			return false;
		}
		else {
			return !winner.equals(owner);
		}
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		return inst.getRoundsLeft() == 0 && winner.equals(owner);
	}

	@Override
	public void reset() {}
}
