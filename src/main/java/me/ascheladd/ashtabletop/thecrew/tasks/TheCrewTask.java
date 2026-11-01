package me.ascheladd.ashtabletop.thecrew.tasks;

import java.util.ArrayList;
import java.util.HashMap;

import me.neoblade298.neocore.shared.io.Section;
import me.ascheladd.ashtabletop.thecrew.TheCrewCardInstance;
import me.ascheladd.ashtabletop.thecrew.TheCrewInstance;
import me.ascheladd.ashtabletop.thecrew.TheCrewPlayer;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public abstract class TheCrewTask {
	protected TheCrewPlayer owner;
	protected HashMap<Integer, Integer> difficulty = new HashMap<Integer, Integer>();
	protected Component display;
	protected String displayString;
	protected boolean isComplete = false;
	
	public TheCrewTask(TheCrewPlayer owner, TheCrewTask src, TheCrewInstance inst) {
		this.owner = owner;
		this.display = src.display;
		this.displayString = src.displayString;
		this.difficulty = src.difficulty;
	}
	
	public TheCrewTask(Section sec) {
		Section diff = sec.getSection("difficulty");
		for (String key : diff.getKeys()) {
			int ikey = Integer.parseInt(key);
			difficulty.put(ikey, diff.getInt(key));
		}
	}
	
	public HashMap<Integer, Integer> getDifficulty() {
		return difficulty;
	}
	
	public int getDifficulty(int players) {
		return difficulty.getOrDefault(players, 3);
	}
	public Component getDisplay() {
		return display;
	}
	public String getDisplayString() {
		if (displayString == null) {
			displayString = PlainTextComponentSerializer.plainText().serialize(display);
		}
		return displayString;
	}
	public boolean isComplete() {
		return isComplete;
	}
	public void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}
	public TheCrewPlayer getOwner() {
		return owner;
	}

	public abstract void reset();
	public abstract void showDebug(CommandSender s);
	public abstract TheCrewTask clone(TheCrewPlayer owner, TheCrewInstance inst);
	public abstract boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile);
	public abstract boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile);
	
	public enum TaskResult {
		FAIL,
		NONE,
		SUCCESS;
	}
}
