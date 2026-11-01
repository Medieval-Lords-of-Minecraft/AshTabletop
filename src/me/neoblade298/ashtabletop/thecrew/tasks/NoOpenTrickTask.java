package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.io.Section;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;

public class NoOpenTrickTask extends TheCrewTask {
	protected ArrayList<CardMatcher> cards = new ArrayList<CardMatcher>();
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "cards: " + cards);
	}
	
	public NoOpenTrickTask(Section cfg) {
		super(cfg);
		
		for (String str : cfg.getStringList("cards")) {
			cards.add(new CardMatcher(str));
		}

		Builder b = Component.text().content("Don't open a trick with ")
				.append(cards.get(0).getDisplay());
		for (int i = 1; i < cards.size(); i++) {
			b.append(Component.text(", ")).append(cards.get(i).getDisplay());
		}
		display = b.build();
	}
	
	public NoOpenTrickTask(TheCrewPlayer owner, NoOpenTrickTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.cards = src.cards;
	}

	@Override
	public NoOpenTrickTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new NoOpenTrickTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		// Check if player is winner and has no unmatched cards
		if (owner.equals(winner)) {
			for (TheCrewCard card : owner.getHand()) {
				boolean matches = false;
				for (CardMatcher cm : cards) {
					if (cm.match(card)) {
						matches = true;
						break;
					}
				}
				if (!matches) return false;
			}
			return true;
		}
		
		// Check if player opened with a matched card
		if (inst.getTurnOrder().get(0).equals(owner)) {
			for (CardMatcher cm : cards) {
				if (cm.match(owner.getLastPlayed())) {
					return true;
				}
			}
		}
		
		return false;
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		return succeeds();
	}

	@Override
	public void reset() {}
	
	private boolean succeeds() {
		for (TheCrewCard card : owner.getHand()) {
			for (CardMatcher cm : cards) {
				if (cm.match(card)) {
					return false;
				}
			}
		}
		return true;
	}
}
