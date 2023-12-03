package org.mineacademy.bfo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;

import lombok.Getter;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.Connection.Unsafe;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.Either;
import net.md_5.bungee.protocol.NumberFormat;
import net.md_5.bungee.protocol.packet.ScoreboardDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardObjective;
import net.md_5.bungee.protocol.packet.ScoreboardObjective.HealthDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardScore;

/**
 * A simple way of creating Scoreboards on Bungee.
 */
@Getter
public class SimpleScoreboard {

	/**
	 * Internal ID of the scoreboards created.
	 */
	private static int ids = 0;

	/**
	 * This is the unique identifier that identifiers this board.
	 */
	private final int id;

	/**
	 * The title for this board.
	 */
	private String title = "Scoreboard";

	/**
	 * Lines for this board.
	 */
	private final List<String> lines = new ArrayList<>();

	/**
	 * Makes a new scoreboard with a unique id
	 */
	public SimpleScoreboard() {
		this.id = ids++;
	}

	/**
	 * Set or update the title.
	 *
	 * Colors are replaced in {@link #send(ProxiedPlayer)} method.
	 *
	 * @param title
	 * @return
	 */
	public final SimpleScoreboard setTitle(String title) {
		this.title = title;

		return this;
	}

	/**
	 * Set or update the lines. Colors are replaced in {@link #send(ProxiedPlayer)} method.
	 * @param lines
	 * @return
	 */
	public final SimpleScoreboard setLines(String... lines) {
		Preconditions.checkArgument(lines.length <= 15, "Scoreboard can only have 15 lines");

		this.lines.clear();
		this.lines.addAll(Arrays.asList(lines));

		return this;
	}

	/**
	 * Send or update the scoreboard to the player. Colors are replaced automatically.
	 * Should you want to replace variables, overwrite the replaceVariables method.
	 * @param player
	 */
	public final void send(ProxiedPlayer player) {
		final Unsafe unsafe = player.unsafe();
		final String idString = Integer.toString(this.id);
		final String titleMessage = this.prepareMessage(player, this.title);

		// Clear old objective
		unsafe.sendPacket(new ScoreboardObjective(idString, Either.right(TextComponent.fromLegacy(titleMessage)), HealthDisplay.INTEGER, (byte) 1, new NumberFormat(NumberFormat.Type.FIXED, 1)));

		// Send objective
		unsafe.sendPacket(new ScoreboardObjective(idString, Either.right(TextComponent.fromLegacy(titleMessage)), HealthDisplay.INTEGER, (byte) 0, new NumberFormat(NumberFormat.Type.FIXED, 1)));

		// Send lines
		for (int i = 0; i < this.lines.size(); i++)
			unsafe.sendPacket(new ScoreboardScore(this.prepareMessage(player, this.lines.get(i)), (byte) 0, idString, this.lines.size() - i, TextComponent.fromLegacy(""), new NumberFormat(NumberFormat.Type.FIXED, 0)));

		// Display
		unsafe.sendPacket(new ScoreboardDisplay((byte) 1, idString));
	}

	// Replace colors and variables
	private String prepareMessage(ProxiedPlayer player, String text) {
		text = this.replaceVariables(player, text);
		text = Variables.replace(text, player);

		return text;
	}

	/**
	 * Does nothing unless overwritten. You can easily replace variables in both title and lines here.
	 *
	 * This is compatible with {@link #replacer} and comes before it.
	 */
	protected String replaceVariables(ProxiedPlayer player, String text) {
		return text;
	}

	@Override
	public final boolean equals(Object object) {
		return object instanceof SimpleScoreboard && ((SimpleScoreboard) object).id == this.id;
	}
}