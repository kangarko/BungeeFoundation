package org.mineacademy.bfo.plugin;

import org.mineacademy.bfo.collection.StrictList;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;

/**
 * A simple way of registering events and other things that
 * are cancelled automatically when the plugin is reloaded.
 */
final class Reloadables {

	/**
	 * A list of currently enabled event listeners
	 */
	private final StrictList<Listener> listeners = new StrictList<>();

	// -------------------------------------------------------------------------------------------
	// Main
	// -------------------------------------------------------------------------------------------

	/**
	 * Remove all listeners and cancel all running tasks
	 */
	void reload() {
		for (final Listener listener : listeners)
			ProxyServer.getInstance().getPluginManager().unregisterListener(listener);

		listeners.clear();
	}

	// -------------------------------------------------------------------------------------------
	// Events / Listeners
	// -------------------------------------------------------------------------------------------

	/**
	 * Register events to Bukkit
	 *
	 * @param listener
	 */
	void registerEvents(Listener listener) {
		ProxyServer.getInstance().getPluginManager().registerListener(SimplePlugin.getInstance(), listener);

		listeners.add(listener);
	}
}
