package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import net.kyori.adventure.text.Component;

import org.bukkit.command.CommandSender;
import me.neoblade298.neocore.shared.io.Section;

public class WinEdgeTrickTask extends TheCrewTask {
	protected boolean first, last, exclusive;
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "first: " + first + ", last: " + last + ", exclusive: " + exclusive);
	}
	
	public WinEdgeTrickTask(Section cfg) {
		super(cfg);

		first = cfg.getBoolean("first");
		last = cfg.getBoolean("last");
		exclusive = cfg.getBoolean("exclusive");
		
		String str;
		if (first && last) {
			str = "first and last";
		}
		else if (first) {
			str = "first";
		}
		else {
			str = "last";
		}
		
		display = Component.text("Win" + (exclusive ? " only" : "") + " the " + str + " trick");
	}
	
	public WinEdgeTrickTask(TheCrewPlayer owner, WinEdgeTrickTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.first = src.first;
		this.last = src.last;
		this.exclusive = src.exclusive;
	}

	@Override
	public WinEdgeTrickTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinEdgeTrickTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		int round = inst.getRound(), roundsLeft = inst.getRoundsLeft();
		if (winner.equals(owner) && exclusive) {
			if (first && last) {
				return round != 1 && roundsLeft != 0;
			}
			else if (first) {
				return round != 1;
			}
			else {
				return roundsLeft != 0;
			}
		}
		
		else if (!winner.equals(owner)) {
			if (first && round == 1) {
				return true;
			}
			else if (last && roundsLeft == 0) {
				return true;
			}
		}
		return false;
 	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (winner.equals(owner) && !exclusive && first && !last && inst.getRound() == 1) {
			return true;
		}
		
		if (inst.getRoundsLeft() == 0) {
			return true; // If hasFailed hasn't stopped the game, we've completed this task
		}
		return false;
	}

	@Override
	public void reset() {}
}
