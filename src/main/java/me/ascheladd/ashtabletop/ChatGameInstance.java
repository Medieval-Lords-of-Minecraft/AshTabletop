package me.ascheladd.ashtabletop;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.neoblade298.neocore.bukkit.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public abstract class ChatGameInstance<T extends GamePlayer> extends GameInstance<T> {
	protected long revision;
	protected boolean started;

	protected ChatGameInstance(GameLobby<T> lobby) {
		super(lobby);
	}

	public abstract void execute(Player player, String[] args);
	public abstract List<String> commands();
	protected abstract String waitingFor();

	protected T participant(Player player) {
		T gp = players.get(player.getName().toLowerCase(Locale.ROOT));
		if (gp == null || !gp.getUniqueId().equals(player.getUniqueId())) {
			Util.displayError(player, "Spectators cannot play. Use /tt leave to stop spectating.");
			return null;
		}
		return gp;
	}

	protected boolean ready(Player player) {
		if (!started || GameManager.getInstance(id) != this) {
			Util.displayError(player, "The game is not ready.");
			return false;
		}
		String waiting = waitingFor();
		if (waiting != null) {
			Util.displayError(player, "Waiting for " + waiting + " to reconnect. The host can use /tt kick <player>.");
			return false;
		}
		return true;
	}

	protected void message(Player player, String text) {
		Util.msgRaw(player, Component.text(text, NamedTextColor.GRAY));
	}

	protected Component button(String label, String command) {
		return Component.text("[" + label + "] ", NamedTextColor.GREEN)
				.clickEvent(ClickEvent.runCommand(command));
	}

	protected void choices(Player player, String title, String body, String[][] options) {
		long expectedRevision = revision;
		List<ActionButton> buttons = Arrays.stream(options).map(option ->
				ActionButton.builder(Component.text(option[0]))
						.action(DialogAction.customClick((response, audience) -> {
							if (!(audience instanceof Player respondent)
									|| !respondent.getUniqueId().equals(player.getUniqueId())) {
								audience.sendMessage(Component.text("This choice belongs to another player.", NamedTextColor.RED));
								return;
							}
							AshTabletop.scheduler().runTask(AshTabletop.inst(), () -> {
								if (!respondent.isOnline() || !respondent.hasPermission("tabletop.use")
										|| GameManager.getSession(respondent.getUniqueId()) != this
										|| revision != expectedRevision) {
									Util.displayError(respondent, "That choice is no longer available. Reopen the game menu.");
									return;
								}
								execute(respondent, option[1].split(" "));
							});
						}, ClickCallback.Options.builder().uses(1).build())).build()).toList();
		player.showDialog(Dialog.create(builder -> builder.empty()
				.base(DialogBase.builder(Component.text(title)).canCloseWithEscape(true).pause(false)
						.afterAction(DialogBase.DialogAfterAction.CLOSE)
						.body(List.of(DialogBody.plainMessage(Component.text(body)))).build())
				.type(DialogType.multiAction(buttons, ActionButton.builder(Component.text("Close")).build(), 2))));
	}

	protected Integer amount(Player player, String text, int maximum) {
		try {
			int value = Integer.parseInt(text);
			if (value >= 0 && value <= maximum) return value;
		}
		catch (NumberFormatException ignored) {
			// Report malformed and out-of-range amounts identically.
		}
		Util.displayError(player, "Enter a whole number between 0 and " + maximum + ".");
		return null;
	}

	@Override
	public void onSpectate(Player player) {
		displayInfo(player, player);
	}

	@Override
	public void onReconnect(Player player) {
		revision++;
		broadcast(Component.text(player.getName() + " reconnected.", NamedTextColor.YELLOW));
		displayInfo(player, player);
	}

	@Override
	public void showDebug(CommandSender sender) {
		sender.sendMessage(Component.text(game.getName() + ": started=" + started
				+ ", revision=" + revision + ", waiting=" + waitingFor()));
	}
}
