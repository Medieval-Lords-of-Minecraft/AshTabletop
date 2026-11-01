package me.neoblade298.ashtabletop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import me.neoblade298.ashtabletop.commands.CmdTabletopKicklist;
import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.commands.SubcommandManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

class PaperPortTest {
	@TempDir
	Path dataDirectory;

	@AfterEach
	void clearSessions() {
		GameManager.clear();
	}

	@Test
	void missingConfigurationDisablesPluginWithAnError() {
		AshTabletop plugin = mock(AshTabletop.class, CALLS_REAL_METHODS);
		Server server = mock(Server.class);
		PluginManager manager = mock(PluginManager.class);
		Logger logger = mock(Logger.class);
		when(plugin.getServer()).thenReturn(server);
		when(server.getPluginManager()).thenReturn(manager);
		when(plugin.getLogger()).thenReturn(logger);
		when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
		plugin.onEnable();
		verify(logger).severe(contains("Missing game configuration:"));
		verify(manager).disablePlugin(plugin);
	}

	@Test
	void startupRegistersBukkitCommandsAndListener() throws Exception {
		Path crew = Files.createDirectory(dataDirectory.resolve("The Crew"));
		for (String file : new String[] {"config.yml", "manual.yml", "tasks.yml"}) {
			Files.createFile(crew.resolve(file));
		}
		AshTabletop plugin = mock(AshTabletop.class, CALLS_REAL_METHODS);
		Server server = mock(Server.class);
		PluginManager manager = mock(PluginManager.class);
		when(plugin.getServer()).thenReturn(server);
		when(server.getPluginManager()).thenReturn(manager);
		when(plugin.getLogger()).thenReturn(mock(Logger.class));
		when(plugin.getDataFolder()).thenReturn(dataDirectory.toFile());
		try (MockedStatic<NeoCore> core = mockStatic(NeoCore.class);
				MockedConstruction<SubcommandManager> commands = mockConstruction(
				SubcommandManager.class, (command, context) -> {
					assertEquals(4, context.arguments().size());
					assertSame(plugin, context.arguments().get(3));
				});
				MockedConstruction<GameManager> listeners = mockConstruction(GameManager.class)) {
			core.when(NeoCore::miniMessage).thenReturn(MiniMessage.miniMessage());
			plugin.onEnable();
			assertEquals(3, commands.constructed().size());
			verify(commands.constructed().get(0)).register(isA(CmdTabletopKicklist.class));
			verify(manager).registerEvents(listeners.constructed().get(0), plugin);
			verify(manager, never()).disablePlugin(plugin);
		}
	}

	@Test
	void playerLookupPreservesOptionalResults() {
		Player player = mock(Player.class);
		UUID uuid = UUID.randomUUID();
		try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			bukkit.when(() -> Bukkit.getPlayerExact("Alex")).thenReturn(player);
			bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(player);
			assertSame(player, AshTabletop.findPlayer("Alex").orElseThrow());
			assertSame(player, AshTabletop.findPlayer(uuid).orElseThrow());
			assertTrue(AshTabletop.findPlayer("Offline").isEmpty());
		}
	}

	@Test
	void delayedCallbacksUseTicksAndIgnoreEndedInstances() {
		GameInstance<?> instance = mock(GameInstance.class, CALLS_REAL_METHODS);
		instance.name = "test";
		GameManager.getInstances().put("test", instance);
		BukkitScheduler scheduler = mock(BukkitScheduler.class);
		AshTabletop plugin = mock(AshTabletop.class);
		AtomicReference<Runnable> pending = new AtomicReference<>();
		when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(80L)))
				.thenAnswer(invocation -> {
					pending.set(invocation.getArgument(1));
					return null;
				});
		Runnable callback = mock(Runnable.class);
		try (MockedStatic<AshTabletop> tabletop = mockStatic(AshTabletop.class)) {
			tabletop.when(AshTabletop::scheduler).thenReturn(scheduler);
			tabletop.when(AshTabletop::inst).thenReturn(plugin);
			instance.runLater(callback, 4);
			pending.get().run();
			verify(callback).run();
			GameManager.getInstances().remove("test");
			pending.get().run();
			verifyNoMoreInteractions(callback);
		}
	}
}
