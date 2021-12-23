package org.mineacademy.bfo.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.bfo.FileUtil;
import org.mineacademy.bfo.Messenger;
import org.mineacademy.bfo.plugin.SimplePlugin;
import org.mineacademy.bfo.settings.SimpleLocalization;

/**
 * A simple predefined sub-command for quickly reloading the plugin
 * using /{label} reload|rl
 */
public final class ReloadCommand extends SimpleCommand {

	/**
	 * Create a new reload sub-command with the given permission.
	 *
	 * @param label
	 * @param permission
	 */
	public ReloadCommand(String label, String permission) {
		this(label);

		setPermission(permission);
	}

	/**
	 * Create a new reload sub-command
	 *
	 * @param label
	 */
	public ReloadCommand(String label) {
		super(label);

		setDescription("Reload the plugin and its configuration.");
	}

	@Override
	protected void onCommand() {
		try {
			tell(Messenger.getInfoPrefix() + "Reloading " + SimplePlugin.getNamed() + " " + SimplePlugin.getVersion() + "..");

			// Syntax check YML files before loading
			boolean syntaxParsed = true;
			Throwable errorFromReloading = null;

			final List<File> yamlFiles = new ArrayList<>();

			collectYamlFiles(SimplePlugin.getData(), yamlFiles);

			for (final File file : yamlFiles) {
				try {
					FileUtil.loadConfigurationStrict(file);

				} catch (final Throwable t) {
					t.printStackTrace();

					errorFromReloading = t;
					syntaxParsed = false;
				}
			}

			if (!syntaxParsed) {
				tell(SimpleLocalization.Commands.RELOAD_FAIL.replace("{error}", errorFromReloading.getMessage() != null ? errorFromReloading.getMessage() : "unknown"));

				return;
			}

			SimplePlugin.getInstance().reload();
			tell(Messenger.getSuccessPrefix() + SimpleLocalization.Commands.RELOAD_SUCCESS);

		} catch (final Throwable t) {
			tell(SimpleLocalization.Commands.RELOAD_FAIL.replace("{error}", t.getMessage() != null ? t.getMessage() : "unknown"));

			t.printStackTrace();
		}
	}

	/*
	 * Get a list of all files ending with "yml" in the given directory
	 * and its subdirectories
	 */
	private List<File> collectYamlFiles(File directory, List<File> list) {

		if (directory.exists())
			for (final File file : directory.listFiles()) {
				if (file.getName().endsWith("yml"))
					list.add(file);

				if (file.isDirectory())
					collectYamlFiles(file, list);
			}

		return list;
	}
}