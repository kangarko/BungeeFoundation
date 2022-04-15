package org.mineacademy.bfo.model;

import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;

/**
 * This class is provided as an easy way to handle scheduling tasks.
 */
public abstract class BukkitRunnable implements Runnable {

	private ScheduledTask task;

	/**
	 * Attempts to cancel this task.
	 *
	 * @throws IllegalStateException if task was not scheduled yet
	 */
	public synchronized void cancel() throws IllegalStateException {
		this.getScheduler().cancel(this.getTaskId());
	}

	/**
	 * Schedules this in the Bukkit scheduler to run on next tick.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 *
	 * @deprecated all tasks on BungeeCord are run async
	 */
	@Deprecated
	@NonNull
	public synchronized ScheduledTask runTask(@NonNull Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		return this.runTaskAsynchronously(plugin);
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit. Great care
	 * should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <p>
	 * Schedules this in the Bukkit scheduler to run asynchronously.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 */
	@NonNull
	public synchronized ScheduledTask runTaskAsynchronously(@NonNull Plugin plugin) throws IllegalArgumentException, IllegalStateException {
		this.checkNotYetScheduled();

		return this.setupTask(this.getScheduler().runAsync(plugin, this));
	}

	/**
	 * Schedules this to run after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 *
	 * @deprecated all tasks on BungeeCord are run async
	 */
	@Deprecated
	@NonNull
	public synchronized ScheduledTask runTaskLater(@NonNull Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		return this.runTaskLaterAsynchronously(plugin, delay);
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit. Great care
	 * should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <p>
	 * Schedules this to run asynchronously after the specified number of
	 * server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 */
	@NonNull
	public synchronized ScheduledTask runTaskLaterAsynchronously(@NonNull Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
		this.checkNotYetScheduled();

		return this.setupTask(this.getScheduler().schedule(plugin, this, delay * 50, TimeUnit.MILLISECONDS));
	}

	/**
	 * Schedules this to repeatedly run until cancelled, starting after the
	 * specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task
	 * @param period the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 *
	 * @deprecated all tasks on BungeeCord are run async
	 */
	@Deprecated
	@NonNull
	public synchronized ScheduledTask runTaskTimer(@NonNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		return this.runTaskTimerAsynchronously(plugin, delay, period);
	}

	/**
	 * <b>Asynchronous tasks should never access any API in Bukkit. Great care
	 * should be taken to assure the thread-safety of asynchronous tasks.</b>
	 * <p>
	 * Schedules this to repeatedly run asynchronously until cancelled,
	 * starting after the specified number of server ticks.
	 *
	 * @param plugin the reference to the plugin scheduling task
	 * @param delay the ticks to wait before running the task for the first
	 *     time
	 * @param period the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 * @throws IllegalArgumentException if plugin is null
	 * @throws IllegalStateException if this was already scheduled
	 */
	@NonNull
	public synchronized ScheduledTask runTaskTimerAsynchronously(@NonNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
		this.checkNotYetScheduled();

		return this.setupTask(this.getScheduler().schedule(plugin, this, delay * 50, period * 50, TimeUnit.MILLISECONDS));
	}

	/**
	 * Gets the task id for this runnable.
	 *
	 * @return the task id that this runnable was scheduled as
	 * @throws IllegalStateException if task was not scheduled yet
	 */
	public synchronized int getTaskId() throws IllegalStateException {
		this.checkScheduled();

		return this.task.getId();
	}

	private void checkScheduled() {
		if (this.task == null)
			throw new IllegalStateException("Not scheduled yet");
	}

	private void checkNotYetScheduled() {
		if (this.task != null)
			throw new IllegalStateException("Already scheduled as " + this.task.getId());
	}

	private TaskScheduler getScheduler() {
		return ProxyServer.getInstance().getScheduler();
	}

	@NonNull
	private ScheduledTask setupTask(@NonNull final ScheduledTask task) {
		this.task = task;

		return task;
	}
}
