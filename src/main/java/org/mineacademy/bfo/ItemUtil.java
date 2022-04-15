package org.mineacademy.bfo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for managing items.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemUtil {

	// ----------------------------------------------------------------------------------------------------
	// Enumeration - fancy names
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Removes _ from the enum, lowercases everything and finally capitalizes it
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountifyCapitalized(Enum<?> enumeration) {
		return ChatUtil.capitalizeFully(bountify(enumeration.toString().toLowerCase()));
	}

	/**
	 * Removes _ from the name, lowercases everything and finally capitalizes it
	 *
	 * @param name
	 * @return
	 */
	public static String bountifyCapitalized(String name) {
		return ChatUtil.capitalizeFully(bountify(name));
	}

	/**
	 * Lowercases the given enum and replaces _ with spaces
	 *
	 * @param enumeration
	 * @return
	 */
	public static String bountify(Enum<?> enumeration) {
		return bountify(enumeration.toString());
	}

	/**
	 * Lowercases the given name and replaces _ with spaces
	 *
	 * @param name
	 * @return
	 */
	public static String bountify(String name) {
		return name.toLowerCase().replace("_", " ");
	}
}