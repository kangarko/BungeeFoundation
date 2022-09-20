package org.mineacademy.bfo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.RandomUtil;
import org.mineacademy.bfo.Valid;

import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * A pretty specific class for picking up items for the player randomly,
 * used when giving a random class to the player in arena plugins
 * where each picked class is checked against players permission
 *
 * @param <T>
 */
public abstract class RandomNoRepeatPicker<T> {

	/**
	 * The list holding all items
	 */
	private final List<T> list = new ArrayList<>();

	/**
	 * The the items we want to pick from
	 *
	 * @param list
	 */
	public void setItems(final Iterable<T> list) {
		Valid.checkBoolean(list != null && list.iterator().hasNext(), "Cannot set items to an empty list!");

		this.list.clear();
		this.list.addAll(Common.toList(list));
	}

	/**
	 * Return if there are no more items to pick from
	 *
	 * @return
	 */
	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	/**
	 * Randomly picks 1 item from the given list
	 * NB: This also loads up the list
	 *
	 * @param items
	 * @return
	 */
	public T pickFrom(final Iterable<T> items) {
		return this.pickFromFor(items, null);
	}

	/**
	 * Randomly picks 1 item for the player using the canObtain method
	 * NB: This also loads up the list
	 *
	 * @param items
	 * @param player
	 * @return
	 */
	public T pickFromFor(final Iterable<T> items, final ProxiedPlayer player) {
		for (final T item : items)
			this.list.add(item);

		return this.pickRandom(player);
	}

	/**
	 * Picks and removes 1 item randomly from the list until we
	 * run out of items
	 *
	 * @return
	 */
	public T pickRandom() {
		return this.pickRandom(null);
	}

	/**
	 * Picks randomly 1 item and evaluates it against the canObtain method
	 * until we run out of items or find 1 that the player can acquire
	 *
	 * @param player
	 * @return
	 */
	public T pickRandom(final ProxiedPlayer player) {
		if (this.list.isEmpty())
			return null;

		while (!this.list.isEmpty()) {
			final T picked = this.list.remove(RandomUtil.nextInt(this.list.size()));

			if (picked != null && this.canObtain(player, picked))
				return picked;
		}

		return null;
	}

	/**
	 * Should return true if the player can obtain the given item
	 *
	 * @param player
	 * @param picked
	 * @return
	 */
	protected abstract boolean canObtain(ProxiedPlayer player, T picked);

	/**
	 * Creates a new random no repeat picker of the given class type
	 * All players may always obtain it
	 *
	 * @param <T>
	 * @param pickedType
	 * @return
	 */
	public static final <T> RandomNoRepeatPicker<T> newPicker(final Class<T> pickedType) {
		return newPicker((player, type) -> true);
	}

	/**
	 * Creates a new random no repeat picker with function
	 * to check if the player can obtain the class
	 *
	 * @param <T>
	 * @param canObtain
	 * @return
	 */
	public static final <T> RandomNoRepeatPicker<T> newPicker(final BiFunction<ProxiedPlayer, T, Boolean> canObtain) {
		return new RandomNoRepeatPicker<T>() {

			@Override
			protected boolean canObtain(final ProxiedPlayer player, final T picked) {
				return canObtain.apply(player, picked);
			}
		};
	}
}