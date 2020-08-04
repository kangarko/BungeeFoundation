package org.mineacademy.bfo.collection.expiringmap;

/**
 * Determines how ExpiringMap entries should be expired.
 */
public enum ExpirationPolicy {
	/** Expires entries based on when they were last accessed */
	ACCESSED,
	/** Expires entries based on when they were created */
	CREATED;
}