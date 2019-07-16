package org.mineacademy.bfo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.mineacademy.bfo.Common;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import lombok.AccessLevel;
import lombok.Getter;
import net.md_5.bungee.api.connection.Connection.Unsafe;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.packet.ScoreboardDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardObjective;
import net.md_5.bungee.protocol.packet.ScoreboardObjective.HealthDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardScore;

/**
 * A simple way of creating Scoreboards on Bungee.
 */
@Getter
public final class SimpleScoreboard {

	/**
	 * The Google GSON instance for toString methods
	 */
	private static final Gson gson = new Gson();

	/**
	 * A list of registered scoreboards
	 */
	@Getter(value = AccessLevel.PRIVATE)
	private static volatile List<SimpleScoreboard> registered = new ArrayList<>();

	/**
	 * Is a scoreboard with a unique name registered?
	 */
	public static boolean isRegistered(String id) {
		return getBoard(id) != null;
	}

	/**
	 * Return a scoreboard for that unique name, or null if does not exist.
	 */
	public static SimpleScoreboard getBoard(String id) {
		for (final SimpleScoreboard board : registered)
			if (board.getId().equals(id))
				return board;

		return null;
	}

	/**
	 * The unique identifier that identifiers this board.
	 */
	private final String id;

	/**
	 * The title for this board.
	 */
	private String title = "Scoreboard";

	/**
	 * The variable replacer.
	 */
	private BiFunction<ProxiedPlayer, String, String> replacer;

	/**
	 * Lines for this board.
	 */
	private final List<String> lines = new ArrayList<>();

	/**
	 * Makes a new scoreboard with a unique id
	 */
	public SimpleScoreboard(String id) {
		Preconditions.checkArgument(!isRegistered(id), "Masterboard with uid " + id + " already exists.");

		this.id = id;
	}

	/**
	 * Set or update the title. Colors are replaced in {@link #send(ProxiedPlayer)} method.
	 */
	public SimpleScoreboard setTitle(String title) {
		this.title = title;

		return this;
	}

	/**
	 * Set the replacer for variables. They are replaced in {@link #send(ProxiedPlayer)} method.
	 */
	public SimpleScoreboard setReplacer(BiFunction<ProxiedPlayer, String, String> r) {
		this.replacer = r;

		return this;
	}

	/**
	 * Set or update the lines. Colors are replaced in {@link #send(ProxiedPlayer)} method.
	 */
	public SimpleScoreboard setLines(String... lines) {
		Preconditions.checkArgument(lines.length <= 15, "Scoreboard can only have 15 lines");

		this.lines.clear();
		this.lines.addAll(Arrays.asList(lines));

		return this;
	}

	/**
	 * Send or update the scoreboard to the player. Colors are replaced automatically.
	 * Should you want to replace variables, overwrite the {@link #replaceVariables(ProxiedPlayer, String)} method.
	 */
	public void send(ProxiedPlayer player) {
		final Unsafe unsafe = player.unsafe();

		final String message = gson.toJson(prepareMessage(player, title));

		// Clear old objective
		unsafe.sendPacket(new ScoreboardObjective(id, message, HealthDisplay.INTEGER, (byte) 1));

		// Send objective
		unsafe.sendPacket(new ScoreboardObjective(id, message, HealthDisplay.INTEGER, (byte) 0));

		// Send lines
		for (int i = 0; i < lines.size(); i++)
			unsafe.sendPacket(new ScoreboardScore(prepareMessage(player, lines.get(i)), (byte) 0, id, lines.size() - i));

		// Display
		unsafe.sendPacket(new ScoreboardDisplay((byte) 1, id));
	}

	// Replace colors and variables
	private String prepareMessage(ProxiedPlayer player, String text) {
		text = Common.colorizeLegacy(text);
		text = replaceVariables(player, text);

		if (replacer != null)
			text = replacer.apply(player, text);

		return text;
	}

	/**
	 * Does nothing unless overwritten. You can easily replace variables in both title and lines here.
	 *
	 * This is compatible with {@link #replacer} and comes before it.
	 */
	public String replaceVariables(ProxiedPlayer player, String text) {
		return text;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SimpleScoreboard && ((SimpleScoreboard) object).id.equals(id);
	}
}
