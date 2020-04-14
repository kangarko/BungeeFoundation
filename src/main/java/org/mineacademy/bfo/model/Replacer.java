package org.mineacademy.bfo.model;

import java.util.Objects;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Valid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.CommandSender;

/**
 * An elegant way to find {variables} and replace them.
 * <p>
 * Use {@link #find(String...)} method to find {} variables such as find("health") and then
 * replace them with {@link #replace(Object...)} with the objects such as replace(20)
 * <p>
 * Something like {@link String#format(String, Object...)} but more useful for Minecraft
 */
@AllArgsConstructor
public final class Replacer {

	/**
	 * The internal deliminer used to separate variables
	 */
	private final static String DELIMITER = "%D3L1M1T3R%";

	/**
	 * The messages we are replacing variables in
	 */
	private final String[] messages;

	// ------------------------------------------------------------------------------------------------------------
	// Temporary variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The variables we are looking for to replace
	 */
	private String[] variables;

	/**
	 * The finished replaced message
	 */
	@Getter
	private String[] replacedMessage;

	/**
	 * Create a new instance for the given messages
	 *
	 * @param messages
	 */
	private Replacer(String... messages) {
		this.messages = messages;
	}

	/**
	 * Find the {} variables
	 *
	 * @param variables
	 * @return
	 */
	public Replacer find(String... variables) {
		this.variables = variables;

		return this;
	}

	/**
	 * Replaces stored variables with their counterparts
	 * <p>
	 * Call this after {@link #find(String...)}!
	 *
	 * @param replacements
	 * @return
	 */
	public Replacer replace(Object... replacements) {
		Valid.checkNotNull(variables, "call find() first");
		Valid.checkBoolean(replacements.length == variables.length, "Variables " + variables.length + " != replacements " + replacements.length);

		// Join and replace as 1 message for maximum performance
		String message = String.join(DELIMITER, messages);

		for (int i = 0; i < variables.length; i++) {
			String find = variables[i];

			{ // Auto insert brackets
				if (!find.startsWith("{"))
					find = "{" + find;

				if (!find.endsWith("}"))
					find = find + "}";
			}

			final Object rep = i < replacements.length ? replacements[i] : null;

			// Convert it into a human readable string

			message = message.replace(find, rep != null ? Objects.toString(rep) : "");
		}

		this.replacedMessage = message.split(DELIMITER);

		return this;
	}

	/**
	 * Sends the finished message with variables replaced to the player
	 *
	 * @param recipient
	 */
	public void tell(CommandSender recipient) {

		Valid.checkNotNull(replacedMessage, "Replaced message not yet set, use find() and replace()");
		Common.tell(recipient, replacedMessage);
	}

	/**
	 * Return the replaced message joined with spaces
	 *
	 * @return
	 */
	public String getReplacedMessageJoined() {
		Valid.checkNotNull(replacedMessage, "Replaced message not yet set, use find() and replace()");

		return String.join(" ", messages);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static access
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new replacer instance in which you can easily find and replace variables and send the message to player
	 *
	 * @param messages
	 * @return
	 */
	public static Replacer of(String... messages) {
		return new Replacer(messages);
	}
}
