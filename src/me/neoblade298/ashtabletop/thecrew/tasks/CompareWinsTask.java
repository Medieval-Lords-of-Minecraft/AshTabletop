package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.io.Section;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;

public class CompareWinsTask extends TheCrewTask {
	protected Comparator comp;
	protected CompareTo with;
	
	@Override
	public void showDebug(CommandSource s) {
		Util.msgRaw(s, "with: " + with + ", comp: " + comp);
	}
	
	public CompareWinsTask(Section cfg) {
		super(cfg);

		comp = Comparator.valueOf(cfg.getString("comparator").toUpperCase());
		with = CompareTo.valueOf(cfg.getString("with").toUpperCase());
		
		display = Component.text("Win " + comp.getDisplay() + " " + with.getDisplay());
	}
	
	public boolean comparesCaptain() {
		return with == CompareTo.CAPTAIN;
	}
	
	public CompareWinsTask(TheCrewPlayer owner, CompareWinsTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.comp = src.comp;
		this.with = src.with;
	}

	@Override
	public CompareWinsTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new CompareWinsTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		int toCompare = getCompareNumber(inst);
		
		switch (comp) {
		case GREATER:
			return owner.getWins() + inst.getRoundsLeft() <= toCompare;
		case EQUAL:
			return owner.getWins() + inst.getRoundsLeft() < toCompare;
		default:
			return owner.getWins() < toCompare;
		}
 	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		int toCompare = getCompareNumber(inst);
		
		switch (comp) {
		case GREATER:
			return owner.getWins() > toCompare + inst.getRoundsLeft();
		case EQUAL:
			return owner.getWins() == toCompare && inst.getRoundsLeft() == 0;
		default:
			return owner.getWins() + inst.getRoundsLeft() < toCompare;
		}
	}

	@Override
	public void reset() {}
	
	private int getCompareNumber(TheCrewInstance inst) {
		int toCompare = 0;
		switch (with) {
		case COMBINED:
			for (TheCrewPlayer p : inst.getPlayers().values()) {
				if (p.equals(owner)) continue;
				toCompare += p.getWins();
			}
			return toCompare;
		case EVERYONE:
			toCompare = comp == Comparator.GREATER ? -1 : 999;
			for (TheCrewPlayer p : inst.getPlayers().values()) {
				if (p.equals(owner)) continue;
				
				if (comp == Comparator.GREATER && p.getWins() > toCompare ||
						comp == Comparator.LESS && p.getWins() < toCompare) {
					toCompare = p.getWins();
				}
			}
			return toCompare;
		default:
			toCompare = inst.getCaptain().getWins();
			return toCompare;
		}
		
	}
	
	private enum CompareTo {
		CAPTAIN("the captain"),
		EVERYONE("everyone"),
		COMBINED("everyone combined");
		
		private String display;
		private CompareTo(String display) {
			this.display = display;
		}
		
		public String getDisplay() {
			return display;
		}
	}
	
	private enum Comparator {
		GREATER("more tricks than"),
		EQUAL("exactly as many tricks as"),
		LESS("less tricks than");
		
		private String display;
		private Comparator(String display) {
			this.display = display;
		}
		
		public String getDisplay() {
			return display;
		}
	}
}
