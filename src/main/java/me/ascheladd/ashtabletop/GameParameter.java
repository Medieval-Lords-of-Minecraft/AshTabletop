package me.ascheladd.ashtabletop;

public class GameParameter {
	private String key, name, desc;
	private Object defaultVal;
	private Object val;
	private GameParameterSetter setter;
	
	public GameParameter(String key, String name, String desc, Object defaultVal, GameParameterSetter setter) {
		this.key = key;
		this.name = name;
		this.desc = desc;
		this.defaultVal = defaultVal;
		this.setter = setter;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return desc;
	}
	
	public Object get() {
		return val == null ? defaultVal : val;
	}
	
	public boolean set(String str) {
		Object val = setter.set(str);
		if (val == null) {
			return false;
		}
		this.val = val;
		return true;
	}
}
