package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.io.Section;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class CompareColorsTask extends TheCrewTask {
	protected CardMatcher card, comp;
	protected boolean more, allowZero; // If true, win more card than comp. False, win equal card and comp
	protected int cardsLeft = 9, compsLeft = 9;

	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "card: " + card + ", comp: " + comp + ", more: " + more + ", allowZero: " + allowZero + ", cardsLeft: " +
				cardsLeft + ", compsLeft: " + compsLeft);
	}
	
	public CompareColorsTask(Section sec) {
		super(sec);
		card = new CardMatcher(sec.getString("card"));
		comp = new CardMatcher(sec.getString("comp"));
		more = sec.getString("comparator").equals(">");
		allowZero = sec.getBoolean("allow_zero");

		if (more) {
			display = Component.text().content("Win more ")
					.append(card.getDisplay())
					.append(Component.text(" than "))
					.append(comp.getDisplay())
					.append(Component.text(" in one trick.")).build();
		}
		else {
			display = Component.text().content("Win as many ")
					.append(card.getDisplay())
					.append(Component.text(" as "))
					.append(comp.getDisplay())
					.append(Component.text(" in one trick.")).build();
		}
		
		if (allowZero) {
			display.append(Component.text("*0 cards is allowed.", NamedTextColor.GRAY, TextDecoration.ITALIC));
		}
		else {
			display.append(Component.text("*0 cards is not allowed.", NamedTextColor.GRAY, TextDecoration.ITALIC));
		}
	}
	
	public CompareColorsTask(TheCrewPlayer owner, CompareColorsTask src, TheCrewInstance inst) {
		super(owner, src, inst);
		this.card = src.card;
		this.comp = src.comp;
		this.more = src.more;
		this.allowZero = src.allowZero;
	}

	@Override
	public CompareColorsTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new CompareColorsTask(this.owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		int cardsWon = 0;
		int compsWon = 0;
		for (TheCrewCardInstance card : pile) {
			if (this.card.match(card)) {
				cardsWon++;
			}
			else if (this.comp.match(card)) {
				compsWon++;
			}
		}
		
		if (succeeds(cardsWon, compsWon)) {
			return false;
		}
		
		cardsLeft -= cardsWon;
		compsLeft -= compsWon;
		return !canStillSucceed();
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (!winner.getUniqueId().equals(owner.getUniqueId())) {
			return false;
		}
		
		// Only time update happens is if owner is winner
		int cardsWon = 0;
		int compsWon = 0;
		for (TheCrewCardInstance card : pile) {
			if (this.card.match(card)) {
				cardsWon++;
			}
			else if (this.comp.match(card)) {
				compsWon++;
			}
		}
		
		return succeeds(cardsWon, compsWon);
	}

	@Override
	public void reset() {
		cardsLeft = 9;
		compsLeft = 9;
	}
	
	private boolean succeeds(int cardsWon, int compsWon) {
		if (cardsWon == 0) return false;
		if (!allowZero && compsWon == 0) return false;
		
		if (more && cardsWon > compsWon || !more && cardsWon == compsWon) {
			return true;
		}
		return false;
	}
	
	private boolean canStillSucceed() {
		int amt = allowZero ? 0 : 1;
		if (more) {
			return cardsLeft > amt && compsLeft > amt - 1;
		}
		else {
			return cardsLeft > amt && compsLeft > amt;
		}
	}
}
