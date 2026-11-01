package me.neoblade298.ashtabletop;

import me.neoblade298.neocore.shared.util.SharedUtil;

public class IntegerParameterSetter implements GameParameterSetter {
	private int min, max;
	
	public IntegerParameterSetter(int min, int max) {
		this.min = min;
		this.max = max;
	}

	// Returns the object to set, or null if the validation fails
	@Override
	public Object set(String str) {
		if (!SharedUtil.isNumeric(str)) {
			return null;
		}
		Integer i = Integer.parseInt(str);
		if (i < min || i > max) {
			return null;
		}
		return i;
	}
	
}
