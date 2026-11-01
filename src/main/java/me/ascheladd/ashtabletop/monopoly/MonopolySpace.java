package me.ascheladd.ashtabletop.monopoly;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MonopolySpace {
	public enum Type {
		GO, FREE, PROPERTY, RAILROAD, UTILITY, GOTOJAIL, JAIL, COMMUNITYCHEST, CHANCE, TAX
	}

	final int position, price, buildingPrice;
	final Type type;
	final String name, group;
	final List<Integer> rents;
	final NamedTextColor color;
	MonopolyPlayer owner;
	int buildings;
	boolean mortgaged;

	MonopolySpace(int position, Type type, String name, int price, int buildingPrice,
			String group, List<Integer> rents, NamedTextColor color) {
		this.position = position;
		this.type = type;
		this.name = name;
		this.price = price;
		this.buildingPrice = buildingPrice;
		this.group = group;
		this.rents = rents;
		this.color = color;
	}

	MonopolySpace copy() {
		return new MonopolySpace(position, type, name, price, buildingPrice, group, rents, color);
	}

	boolean isProperty() {
		return type == Type.PROPERTY || type == Type.RAILROAD || type == Type.UTILITY;
	}

	Component display() {
		return Component.text(name, color);
	}

	String details() {
		return position + ": " + name + "\nOwner: " + (owner == null ? "Bank" : owner.getName())
				+ (isProperty() ? "\nPrice: $" + price + "; rent: " + rents + "; building price: $" + buildingPrice
						+ "\nBuildings: " + (buildings == 5 ? "Hotel" : buildings) + "; mortgaged: " + mortgaged : "");
	}
}
