package org.mineacademy.bfo.bungee;

import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.plugin.SimplePlugin;

/**
 * Represents an action sent over BungeeCord containing
 * a set of data. We recommend you create an enum that implements this.
 */
public interface BungeeAction {

	/**
	 * Stores all valid values in this action in the order of which they
	 * are being sent. Only primitive types and String are supported.
	 */
	Class<?>[] getContent();

	/**
	 * The name of this action
	 *
	 * @return
	 */
	String name();

	/**
	 * Retrieve BungeeAction by its name
	 *
	 * @param name
	 * @return
	 */
	static BungeeAction getByName(String name) {
		final BungeeAction[] actions = SimplePlugin.getBungee().getActions();
		Valid.checkNotNull(actions, "Cannot get an action by name if getBungeeActions is not implemented in " + SimplePlugin.getNamed());

		for (final BungeeAction action : actions)
			if (action.name().equals(name))
				return action;

		return null;
	}
}
