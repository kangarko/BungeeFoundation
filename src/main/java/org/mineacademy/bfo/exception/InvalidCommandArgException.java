package org.mineacademy.bfo.exception;

/**
 * Thrown when a command has invalid argument
 */
public class InvalidCommandArgException extends CommandException {

	private static final long serialVersionUID = 1L;

	public InvalidCommandArgException() {
		super();
	}

	public InvalidCommandArgException(String message) {
		super(message);
	}
}