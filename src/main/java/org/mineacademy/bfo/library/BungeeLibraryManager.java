package org.mineacademy.bfo.library;

import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Path;

import net.md_5.bungee.api.plugin.Plugin;

/**
 * A runtime dependency manager for Bungee plugins.
 */
public class BungeeLibraryManager extends LibraryManager {

	/**
	 * Plugin classpath helper
	 */
	private final URLClassLoaderHelper classLoader;

	private final Plugin plugin;

	/**
	 * Creates a new Bungee library manager.
	 *
	 * @param plugin the plugin to manage
	 */
	public BungeeLibraryManager(Plugin plugin) {
		super(plugin.getDataFolder().toPath());

		this.classLoader = new URLClassLoaderHelper((URLClassLoader) plugin.getClass().getClassLoader(), this);
		this.plugin = plugin;
	}

	/**
	 * Adds a file to the Bungee plugin's classpath.
	 *
	 * @param file the file to add
	 */
	@Override
	protected void addToClasspath(Path file) {
		classLoader.addToClasspath(file);
	}

	@Override
	protected InputStream getResourceAsStream(String path) {
		return plugin.getResourceAsStream(path);
	}
}
