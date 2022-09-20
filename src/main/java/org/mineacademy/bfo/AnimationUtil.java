package org.mineacademy.bfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.mineacademy.bfo.model.BukkitRunnable;
import org.mineacademy.bfo.model.SimpleScoreboard;
import org.mineacademy.bfo.plugin.SimplePlugin;
import org.mineacademy.bfo.remain.Remain;

import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

/**
 * Utility class for creating text animations for BossBars, Scoreboards, HUD Titles and Inventories.
 *
 * @author parpar8090
 */
public class AnimationUtil {

	// ------------------------------------------------------------------------------------------------------------
	// Frame generators
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Animate a text with colors that move from left to right.
	 * The animation ends once the firstColor reached the length of the message, which results in a half cycle. For a full cycle, use {@link #rightToLeftFull}
	 *
	 * @param message     The message to be animated
	 * @param firstColor  The color for the first section of the message (example color: {@link ChatColor#YELLOW})
	 * @param middleColor The color for the middle section of the message, a 1 character long section that separates between the first and last sections (example color: {@link ChatColor#WHITE}).
	 * @param lastColor   The color for the last section of the message (example color: {@link ChatColor#GOLD}).
	 * @return List of ordered string frames.
	 */
	public static List<String> leftToRight(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		final List<String> result = new ArrayList<>();
		final String msg = Common.colorize(message);

		for (int frame = 0; frame < message.length(); frame++) {
			final String first = msg.substring(0, frame);
			final String middle = frame == msg.length() ? "" : String.valueOf(msg.charAt(frame));
			final String last = frame == msg.length() ? "" : msg.substring(frame + 1);

			final ChatColor middleColorFinal = middleColor != null ? middleColor : firstColor;

			result.add(firstColor + first + middleColorFinal + middle + lastColor + last);
		}
		return result;
	}

	/**
	 * Animate a text with colors that move from right to left.
	 * The animation ends once the firstColor reached the start of the message, which results in a half cycle. For a full cycle, use {@link #rightToLeftFull}
	 *
	 * @param message     The message to be animated
	 * @param firstColor  The color for the first section of the message (example color: {@link ChatColor#YELLOW})
	 * @param middleColor The color for the middle section of the message, a 1 character long section that separates between the first and last sections (example color: {@link ChatColor#WHITE}).
	 * @param lastColor   The color for the last section of the message (example color: {@link ChatColor#GOLD}).
	 * @return List of ordered string frames.
	 */
	public static List<String> rightToLeft(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		final String msg = Common.colorize(message);
		final List<String> result = new ArrayList<>();

		for (int frame = msg.length(); frame >= 0; frame--) {
			final String first = msg.substring(0, frame);
			final String middle = frame == msg.length() ? "" : String.valueOf(msg.charAt(frame));
			final String last = frame == msg.length() ? "" : msg.substring(frame + 1);

			final ChatColor middleColorFinal = middleColor != null ? middleColor : firstColor;

			result.add(firstColor + first + middleColorFinal + middle + lastColor + last);
		}
		return result;
	}

	/**
	 * Animate a text with colors that move from left to right in a full cycle.
	 * A full cycle animation is a cycle in this pattern: lastColor -> firstColor -> lastColor.
	 *
	 * @param message     The message to be animated
	 * @param firstColor  The color for the first section of the message (example color: {@link ChatColor#YELLOW})
	 * @param middleColor The color for the middle section of the message, a 1 character long section that separates between the first and last sections (example color: {@link ChatColor#WHITE}).
	 * @param lastColor   The color for the last section of the message (example color: {@link ChatColor#GOLD}).
	 * @return List of ordered string frames.
	 */
	public static List<String> leftToRightFull(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		final List<String> result = new ArrayList<>();

		result.addAll(leftToRight(message, firstColor, middleColor, lastColor));
		result.addAll(leftToRight(message, lastColor, middleColor, firstColor));

		return result;
	}

	/**
	 * Animate a text with colors that move from right to left in a full cycle.
	 * A full cycle animation is a cycle in this pattern: lastColor -> firstColor -> lastColor.
	 *
	 * @param message     The message to be animated
	 * @param firstColor  The color for the first section of the message (example color: {@link ChatColor#YELLOW})
	 * @param middleColor The color for the middle section of the message, a 1 character long section that separates between the first and last sections (example color: {@link ChatColor#WHITE}).
	 * @param lastColor   The color for the last section of the message (example color: {@link ChatColor#GOLD}).
	 * @return List of ordered string frames.
	 */
	public static List<String> rightToLeftFull(String message, ChatColor firstColor, @Nullable ChatColor middleColor, ChatColor lastColor) {
		final List<String> result = new ArrayList<>();

		result.addAll(rightToLeft(message, firstColor, middleColor, lastColor));
		result.addAll(rightToLeft(message, lastColor, middleColor, firstColor));

		return result;
	}

	/**
	 * Flicker a text with the arranged colors.
	 * NOTE: For randomness, call this method inside the {@link #shuffle(List)} method.
	 *
	 * @param message  The message to be animated
	 * @param amount   The amount of times the message will flicker.
	 * @param duration How many duplicates will each frame have. More duplicates make the frames stay longer.
	 * @param colors   The flickering colors, ordered by array index.
	 * @return List of ordered string frames.
	 */
	public static List<String> flicker(String message, int amount, int duration, ChatColor[] colors) {
		final List<String> result = new ArrayList<>();

		for (int frame = 0; frame < amount; frame++)
			for (int i = 0; i < duration; i++)
				result.add(colors[amount % colors.length] + message);

		return result;
	}

	/**
	 * Duplicate all frames by a specific amount, useful for slowing the animation.
	 *
	 * @param frames The frames to be duplicated.
	 * @param amount The amount of duplications.
	 * @return List of duplicates string frames.
	 */
	public static List<String> duplicate(List<String> frames, int amount) {
		final List<String> result = new ArrayList<>();

		for (int i = 0; i < frames.size(); i++)
			//duplicate j times;
			for (int j = 0; j < amount; j++) {
				final String duplicated = frames.get(i);

				result.add(i, duplicated);
			}
		return result;
	}

	/**
	 * Duplicate a specific frame by a specific amount, useful for slowing the animation.
	 *
	 * @param frame The frame to be duplicated
	 * @param frames The frames in which the frame is contained.
	 * @param amount The amount of duplications.
	 * @return The new result of frames after duplication.
	 */
	public static List<String> duplicateFrame(int frame, List<String> frames, int amount) {
		final List<String> result = new ArrayList<>();

		for (int i = 0; i < amount; i++) {
			final String duplicated = frames.get(frame);

			result.add(frame, duplicated);
		}

		return result;
	}

	/**
	 * Shuffles the order of the frames.
	 *
	 * @param animatedFrames The frames to be shuffled.
	 * @return The new animatedFrames list after shuffling.
	 */
	public static List<String> shuffle(List<String> animatedFrames) {
		Collections.shuffle(animatedFrames);

		return animatedFrames;
	}

	/**
	 * Combines animations in order.
	 *
	 * @param animationsToCombine The animations to combine (in order of the list)
	 * @return The combined list of frames.
	 */
	public static List<String> combine(@NonNull List<String>[] animationsToCombine) {
		final List<String> combined = new ArrayList<>();

		for (final List<String> animation : animationsToCombine)
			combined.addAll(animation);

		return combined;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Animators
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Animates the title of a BossBar.
	 *
	 * @param player
	 * @param animatedFrames The frames (in order) to be displayed in the BossBar.
	 * @param delay          The delay between animation cycles.
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 * @return The repeating BukkitTask (Useful to cancel on reload or shutdown).
	 */
	public static ScheduledTask animateBossBar(ProxiedPlayer player, List<String> animatedFrames, long delay, long period) {
		return new BukkitRunnable() {
			int frame = 0;

			@Override
			public void run() {
				Remain.sendBossbar(player, animatedFrames.get(this.frame), 1F);

				this.frame++;

				if (this.frame == animatedFrames.size())
					this.frame = 0;
			}
		}.runTaskTimerAsynchronously(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * Animates the title of a Scoreboard.
	 *
	 * @param scoreboard     The scoreboard to animate.
	 * @param animatedFrames The frames (in order) to be displayed in the BossBar.
	 * @param delay          The delay (in tick) to wait between animation cycles.
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 * @return The repeating BukkitTask (Useful to cancel on reload or shutdown).
	 */
	public static ScheduledTask animateScoreboardTitle(SimpleScoreboard scoreboard, List<String> animatedFrames, long delay, long period) {
		return new BukkitRunnable() {
			int frame = 0;

			@Override
			public void run() {
				scoreboard.setTitle(animatedFrames.get(this.frame));
				this.frame++;

				if (this.frame == animatedFrames.size())
					this.frame = 0;
			}
		}.runTaskTimerAsynchronously(SimplePlugin.getInstance(), delay, period);
	}

	/**
	 * Animates a Title for the player (Does not repeat).
	 *
	 * @param who            The player to show the Title.
	 * @param titleFrames    The frames (in order) to be displayed in the Title. (set to null to hide)
	 * @param subtitleFrames The frames (in order) to be displayed in the SubTitle. (set to null to hide)
	 * @param period         The period (in ticks) to wait between showing the next frame.
	 * @return the task you can cancel after animation ended
	 */
	public static ScheduledTask animateTitle(ProxiedPlayer who, @Nullable List<String> titleFrames, @Nullable List<String> subtitleFrames, long period) {
		return new BukkitRunnable() {
			int frame = 0;
			String title = "", subtitle = "";

			@Override
			public void run() {
				if (titleFrames != null)
					this.title = titleFrames.get(this.frame % titleFrames.size());
				if (subtitleFrames != null)
					this.subtitle = subtitleFrames.get(this.frame % subtitleFrames.size());

				Remain.sendTitle(who, 10, 70, 20, this.title, this.subtitle);

				this.frame++;

				if (this.frame == Math.max(titleFrames != null ? titleFrames.size() : 0,
						subtitleFrames != null ? subtitleFrames.size() : 0) || SimplePlugin.isReloading())
					this.cancel();
			}
		}.runTaskTimer(SimplePlugin.getInstance(), 0, period);
	}
}