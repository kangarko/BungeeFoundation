package org.mineacademy.bfo.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.collection.expiringmap.ExpiringMap;
import org.mineacademy.bfo.exception.CommandException;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.exception.InvalidCommandArgException;
import org.mineacademy.bfo.plugin.SimplePlugin;

import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.PluginManager;

/**
 * A simple command used to replace all Bukkit/Spigot command functionality
 * across any plugin that utilizes this.
 */
public abstract class SimpleCommand extends Command {

	/**
	 * The default permission syntax for this command.
	 */
	protected static final String DEFAULT_PERMISSION_SYNTAX = "{plugin.name}.command.{label}";

	/**
	 * You can set the cooldown time before executing the command again. This map
	 * stores the player uuid and his last execution of the command.
	 */
	private final ExpiringMap<UUID, Long> cooldownMap = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();

	/**
	 * A list of placeholders to replace in this command, see {@link Placeholder}
	 *
	 * These are used when sending player messages
	 */
	private final StrictList<BiFunction<CommandSender, String, String>> placeholders = new StrictList<>();

	/**
	 * The command label, eg. boss for /boss
	 *
	 * This variable is updated dynamically when the command is run with the
	 * last known version of the label, e.g. /boss or /b will set it to boss or b
	 * respectively
	 */
	private String label;

	/**
	 * The usage for this cmd
	 */
	private String usage = null;

	/**
	 * The description of how to use this cmd
	 */
	private String description = null;

	/**
	 * The permission for this cmd, or null
	 */
	private String permission = null;

	/**
	 * Has this command been already registered?
	 */
	private boolean registered = false;

	/**
	 * Should we add {@link Common#getTellPrefix()} automatically when calling tell and returnTell methods
	 * from this command?
	 */
	private boolean addTellPrefix = true;

	/**
	 * The {@link Common#getTellPrefix()} custom prefix only used for sending messages in {@link #onCommand()} method
	 * for this command, empty by default, then we use the one in Common
	 */
	private String tellPrefix = "";

	/**
	 * Minimum arguments required to run this command
	 */
	@Getter
	private int minArguments = 0;

	/**
	 * The command cooldown before we can run this command again
	 */
	private int cooldownSeconds = 0;

	/**
	 * A custom message when the player attempts to run this command
	 * within {@link #cooldownSeconds}. By default we use the one found in
	 * {@link SimpleLocalization.Commands#COOLDOWN_WAIT}
	 *
	 * TIP: Use {duration} to replace the remaining time till next run
	 */
	private String cooldownMessage = null;

	/**
	 * The no permission message. Use {permission} to translate into the missing permission
	 */
	private String permissionMessage = null;

	/**
	 * Should we automatically send usage message when the first argument
	 * equals to "help" or "?" ?
	 */
	private boolean autoHandleHelp = true;

	// ----------------------------------------------------------------------
	// Temporary variables
	// ----------------------------------------------------------------------

	/**
	 * The command sender, or null if does not exist
	 *
	 * This variable is updated dynamically when the command is run with the
	 * last known sender
	 */
	protected CommandSender sender;

	/**
	 * The arguments used when the command was last executed
	 *
	 * This variable is updated dynamically when the command is run with the
	 * last known arguments
	 */
	protected String[] args;

	// ----------------------------------------------------------------------

	/**
	 * Create a new simple command with the given label.
	 *
	 * Separate the label with | to split between label and aliases.
	 * Example: remove|r|rm will create a /remove command that can
	 * also be run by typing /r and /rm as its aliases.
	 *
	 * @param label
	 */
	protected SimpleCommand(String label) {
		this(parseLabel0(label), parseAliases0(label));
	}

	/**
	 * Create a new simple command from the list. The first
	 * item in the list is the main label and the other ones are the aliases.
	 */
	protected SimpleCommand(StrictList<String> labels) {
		this(labels.get(0), labels.size() > 1 ? labels.range(1).getSource() : null);
	}

	/**
	 * Create a new simple command
	 *
	 * @param label
	 * @param aliases
	 */
	protected SimpleCommand(String label, List<String> aliases) {
		super(label, null, aliases == null ? null : aliases.toArray(new String[aliases.size()]));

		setLabel(label);

		// Set a default permission for this command
		setPermission(DEFAULT_PERMISSION_SYNTAX);
	}

	/*
	 * Split the given label by | and get the first part, used as the main label
	 */
	private final static String parseLabel0(String label) {
		Valid.checkNotNull(label, "Label must not be null!");

		return label.split("\\|")[0];
	}

	/*
	 * Split the given label by | and use the second and further parts as aliases
	 */
	private final static List<String> parseAliases0(String label) {
		final String[] aliases = label.split("\\|");

		return aliases.length > 0 ? Arrays.asList(Arrays.copyOfRange(aliases, 1, aliases.length)) : new ArrayList<>();
	}

	// ----------------------------------------------------------------------
	// Registration
	// ----------------------------------------------------------------------

	/**
	 * Registers this command into Bukkit.
	 *
	 * Throws an error if the command {@link #isRegistered()} already.
	 */
	public final void register() {
		Valid.checkBoolean(!registered, "The command /" + getLabel() + " has already been registered!");

		final PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();
		final Command oldCommand = findExistingCommand(getLabel());

		if (oldCommand != null) {
			Common.log("&eCommand &f/" + getLabel() + " &ealready used, unregistering...");

			pluginManager.unregisterCommand(oldCommand);
		}

		pluginManager.registerCommand(SimplePlugin.getInstance(), this);
		registered = true;
	}

	private Command findExistingCommand(String label) {
		for (final Entry<String, Command> entry : ProxyServer.getInstance().getPluginManager().getCommands())
			if (entry.getKey().equals(label))
				return entry.getValue();

		return null;
	}

	/**
	 * Removes the command from Bukkit.
	 *
	 * Throws an error if the command is not {@link #isRegistered()}.
	 */
	public final void unregister() {
		Valid.checkBoolean(registered, "The command /" + getLabel() + " is not registered!");

		ProxyServer.getInstance().getPluginManager().unregisterCommand(this);
		registered = false;
	}

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * Execute this command, updates the {@link #sender}, {@link #label} and {@link #args} variables,
	 * checks permission and returns if the sender lacks it,
	 * checks minimum arguments and finally passes the command to the child class.
	 *
	 * Also contains various error handling scenarios
	 */
	@Override
	public void execute(CommandSender sender, String[] args) {
		// Set variables to re-use later
		this.sender = sender;
		this.args = args;

		// Catch "errors" that contain a message to send to the player
		try {

			// Check if sender has the proper permission
			if (getPermission() != null)
				checkPerm(getPermission());

			// Check for minimum required arguments and print help
			if (args.length < getMinArguments() ||
					autoHandleHelp && args.length == 1 && ("help".equals(args[0]) || "?".equals(args[0]))) {

				// Enforce setUsage being used
				if (Common.getOrEmpty(getUsage()).isEmpty())
					throw new FoException("If you set getMinArguments you must also call setUsage for /" + getLabel() + " command!");

				if (!Common.getOrEmpty(getDescription()).isEmpty())
					tellNoPrefix("&cDescription: " + getDescription());

				if (getMultilineUsageMessage() != null) {
					tellNoPrefix("&cUsages: ");
					tellNoPrefix(getMultilineUsageMessage());

				} else {
					if (getMultilineUsageMessage() != null) {
						tellNoPrefix("&cUsages:");
						tellNoPrefix(getMultilineUsageMessage());

					} else {
						tellNoPrefix("&cUsage: /" + label + (!getUsage().startsWith("/") ? " " + Common.stripColors(getUsage()) : ""));
					}
				}

				return;
			}

			// Check if we can run this command in time
			if (cooldownSeconds > 0)
				handleCooldown();

			onCommand();

		} catch (final InvalidCommandArgException ex) {
			if (getMultilineUsageMessage() == null)
				tellNoPrefix(ex.getMessage() != null ? ex.getMessage() : "&cInvalid sub argument for this command.");

			else {
				tellNoPrefix("Usage:");
				tellNoPrefix(getMultilineUsageMessage());
			}

		} catch (final CommandException ex) {
			if (ex.getMessages() != null)
				tell(ex.getMessages());

		} catch (final Throwable t) {
			tellNoPrefix("&l&4Oups! &cThere was a problem running this command: {error}".replace("{error}", t.toString()));

			Common.error(t, "Failed to execute command /" + getLabel() + " " + String.join(" ", args));
		}
	}

	/**
	 * Check if the command cooldown is active and if the command
	 * is run within the given limit, we stop it and inform the player
	 */
	private final void handleCooldown() {
		if (isPlayer()) {
			final ProxiedPlayer player = getPlayer();

			final long lastExecution = cooldownMap.getOrDefault(player.getUniqueId(), 0L);
			final long lastExecutionDifference = (System.currentTimeMillis() - lastExecution) / 1000;

			// Check if the command was not run earlier within the wait threshold
			checkBoolean(lastExecution == 0 || lastExecutionDifference > cooldownSeconds, Common.getOrDefault(cooldownMessage, "Wait {duration} second(s) before running this command again.").replace("{duration}", cooldownSeconds - lastExecutionDifference + 1 + ""));

			// Update the last try with the current time
			cooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
		}
	}

	/**
	 * Executed when the command is run. You can get the variables sender and args directly,
	 * and use convenience checks in the simple command class.
	 */
	protected abstract void onCommand();

	/**
	 * Get a custom multilined usagem message to be shown instead of the one line one
	 *
	 * @return the multiline custom usage message, or null
	 */
	protected String[] getMultilineUsageMessage() {
		return null;
	}

	// ----------------------------------------------------------------------
	// Convenience checks
	//
	// Here is how they work: When you command is executed, simply call any
	// of these checks. If they fail, an error will be thrown inside of
	// which will be a message for the player.
	//
	// We catch that error and send the message to the player without any
	// harm or console errors to your plugin. That is intended and saves time.
	// ----------------------------------------------------------------------

	/**
	 * Checks if the player is a console and throws an error if he is
	 *
	 * @throws CommandException
	 */
	protected final void checkConsole() throws CommandException {
		if (!isPlayer())
			throw new CommandException("&cOnly players may execute this command.");
	}

	/**
	 * Checks if the player has the given permission
	 *
	 * @param perm
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull String perm) throws CommandException {
		if (isPlayer() && !sender.hasPermission(perm))
			throw new CommandException(getPermissionMessage().replace("{permission}", perm));
	}

	/**
	 * Check if the command arguments are of the minimum length
	 *
	 * @param minimumLength
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkArgs(int minimumLength, String falseMessage) throws CommandException {
		if (args.length < minimumLength)
			returnTell("&c" + falseMessage);
	}

	/**
	 * Checks if the given boolean is true
	 *
	 * @param value
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkBoolean(boolean value, String falseMessage) throws CommandException {
		if (!value)
			returnTell("&c" + falseMessage);
	}

	/**
	 * Checks if the given object is not null
	 *
	 * @param value
	 * @param messageIfNull
	 * @throws CommandException
	 */
	protected final void checkNotNull(Object value, String messageIfNull) throws CommandException {
		if (value == null)
			returnTell("&c" + messageIfNull);
	}

	/**
	 * Attempts to find a non-vanished online player, failing with the message
	 * found at {@link SimpleLocalization.Player#NOT_ONLINE}
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	protected final ProxiedPlayer findPlayer(String name) throws CommandException {
		return findPlayer(name, "The player {player} is not online.");
	}

	/**
	 * Attempts to find a non-vanished online player, failing with a false message
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final ProxiedPlayer findPlayer(String name, String falseMessage) throws CommandException {
		final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
		checkBoolean(player != null, falseMessage.replace("{player}", name));

		return player;
	}

	/**
	 * Finds an enumeration of a certain type, if it fails it prints a false message to the player
	 * You can use the {enum} variable in the false message for the name parameter
	 *
	 * @param <T>
	 * @param enumType
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(Class<T> enumType, String name, String falseMessage) throws CommandException {
		T found = null;

		try {
			found = Enum.valueOf(enumType, name.toUpperCase());
		} catch (final Throwable t) {
		}

		checkNotNull(found, falseMessage.replace("{enum}", name));
		return found;
	}

	/**
	 * A convenience method for parsing a number that is between two bounds
	 * You can use {min} and {max} in the message to be automatically replaced
	 *
	 * @param index
	 * @param min
	 * @param max
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(int index, int min, int max, String falseMessage) {
		final int number = findNumber(index, falseMessage);
		checkBoolean(number >= min && number <= max, falseMessage.replace("{min}", min + "").replace("{max}", max + ""));

		return number;
	}

	/**
	 * A convenience method for parsing a number at the given args index
	 *
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(int index, String falseMessage) {
		checkBoolean(index < args.length, falseMessage);

		Integer parsed = null;

		try {
			parsed = Integer.parseInt(args[index]);

		} catch (final NumberFormatException ex) {
		}

		checkNotNull(parsed, falseMessage);
		return parsed;
	}

	// ----------------------------------------------------------------------
	// Other checks
	// ----------------------------------------------------------------------

	/**
	 * A convenience check for quickly determining if the sender has a given
	 * permission.
	 *
	 * TIP: For a more complete check use {@link #checkPerm(String)} that
	 * will automatically return your command if they lack the permission.
	 *
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(String permission) {
		return sender.hasPermission(permission);
	}

	// ----------------------------------------------------------------------
	// Messaging
	// ----------------------------------------------------------------------

	/**
	 * Send a list of messages to the player
	 *
	 * @param messages
	 */
	protected final void tell(Collection<String> messages) {
		tell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Sends a multiline message to the player without plugins prefix
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(String... messages) {
		final boolean localPrefix = addTellPrefix;
		addTellPrefix = false;

		tell(messages);

		addTellPrefix = localPrefix;
	}

	/**
	 * Sends a multiline message to the player, avoiding prefix if 3 lines or more
	 *
	 * @param messages
	 */
	protected final void tell(String... messages) {
		if (messages != null) {
			messages = replacePlaceholders(messages);

			if (!addTellPrefix || messages.length > 2)
				Common.tell(sender, messages);
			else {
				if (tellPrefix.isEmpty())
					Common.tell(sender, messages);
				else
					for (final String message : messages)
						Common.tell(sender, tellPrefix + " " + message);
			}
		}
	}

	/**
	 * Convenience method for returning the command with the {@link SimpleLocalization.Commands#INVALID_ARGUMENT}
	 * message for player
	 */
	protected final void returnInvalidArgs() {
		returnTell("Invalid command argument.");
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param messages
	 * @throws CommandException
	 */
	protected final void returnTell(Collection<String> messages) throws CommandException {
		returnTell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param messages
	 * @throws CommandException
	 */
	protected final void returnTell(String... messages) throws CommandException {
		throw new CommandException(replacePlaceholders(messages));
	}

	// ----------------------------------------------------------------------
	// Placeholder
	// ----------------------------------------------------------------------

	/**
	 * Registers a new placeholder to be used when sending messages to the player
	 *
	 * @param placeholder
	 */
	protected final void addPlaceholder(BiFunction<CommandSender, String, String> placeholder) {
		placeholders.add(placeholder);
	}

	/**
	 * Replaces placeholders in all messages
	 * To change them override {@link #replacePlaceholders(String)}
	 *
	 * @param messages
	 * @return
	 */
	protected final String[] replacePlaceholders(String[] messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = replacePlaceholders(messages[i]);

		return messages;
	}

	/**
	 * Replaces placeholders in the message
	 *
	 * @param message
	 * @return
	 */
	protected String replacePlaceholders(String message) {
		// Replace basic labels
		message = replaceBasicPlaceholders0(message);

		// Replace {X} with arguments
		for (int i = 0; i < args.length; i++)
			message = message.replace("{" + i + "}", Common.getOrEmpty(args[i]));

		// Replace saved placeholders
		for (final BiFunction<CommandSender, String, String> placeholder : placeholders)
			message = placeholder.apply(sender, message);

		return message;
	}

	/**
	 * Internal method for replacing {label} {sublabel} and {plugin.name} placeholders
	 *
	 * @param message
	 * @return
	 */
	private final String replaceBasicPlaceholders0(String message) {
		return message.replace("{label}", getLabel());
	}

	/**
	 * Utility method to safely update the args, increasing them if the position is too high
	 *
	 * Used in placeholders
	 *
	 * @param position
	 * @param value
	 */
	protected final void setArg(int position, String value) {
		if (args.length <= position)
			args = Arrays.copyOf(args, position + 1);

		args[position] = value;
	}

	/**
	 * Convenience method for returning the last word in arguments
	 *
	 * @return
	 */
	protected final String getLastArg() {
		return args.length > 0 ? args[args.length - 1] : "";
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to their end
	 *
	 * @param from
	 * @return
	 */
	protected final String[] rangeArgs(int from) {
		return rangeArgs(from, args.length);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to the given end
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected final String[] rangeArgs(int from, int to) {
		return Arrays.copyOfRange(args, from, to);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to their end joined by spaces
	 *
	 * @param from
	 * @return
	 */
	protected final String joinArgs(int from) {
		return joinArgs(from, args.length);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to the given end joined by spaces
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	protected final String joinArgs(int from, int to) {
		String message = "";

		for (int i = from; i < args.length && i < to; i++)
			message += args[i] + (i + 1 == args.length ? "" : " ");

		return message;
	}

	// ----------------------------------------------------------------------
	// Temporary variables and safety
	// ----------------------------------------------------------------------

	/**
	 * Attempts to get the sender as player, only works if the sender is actually a player,
	 * otherwise we return null
	 *
	 * @return
	 */
	protected final ProxiedPlayer getPlayer() {
		return isPlayer() ? (ProxiedPlayer) getSender() : null;
	}

	/**
	 * Return whether the sender is a living player
	 *
	 * @return
	 */
	protected final boolean isPlayer() {
		return sender instanceof ProxiedPlayer;
	}

	/**
	 * Should we add {@link Common#getTellPrefix()} automatically when calling tell and returnTell methods
	 * from this command?
	 *
	 * @param addTellPrefix
	 */
	protected final void addTellPrefix(boolean addTellPrefix) {
		this.addTellPrefix = addTellPrefix;
	}

	/**
	 * Sets a custom prefix used in tell messages for this command.
	 * This overrides {@link Common#getTellPrefix()} however won't work if
	 * {@link #addTellPrefix} is disabled
	 *
	 * @param tellPrefix
	 */
	protected final void setTellPrefix(String tellPrefix) {
		this.tellPrefix = tellPrefix;
	}

	/**
	 * Sets the minimum number of arguments to run this command
	 *
	 * @param minArguments
	 */
	protected final void setMinArguments(int minArguments) {
		Valid.checkBoolean(minArguments >= 0, "Minimum arguments must be 0 or greater");

		this.minArguments = minArguments;
	}

	/**
	 * Set the time before the same player can execute this command again
	 *
	 * @param cooldown
	 * @param unit
	 */
	protected final void setCooldown(int cooldown, TimeUnit unit) {
		Valid.checkBoolean(cooldown >= 0, "Cooldown must be >= 0 for /" + getLabel());

		this.cooldownSeconds = (int) unit.toSeconds(cooldown);
	}

	/**
	 * Set a custom cooldown message, by default we use the one found in {@link SimpleLocalization.Commands#COOLDOWN_WAIT}
	 *
	 * Use {duration} to dynamically replace the remaining time
	 *
	 * @param cooldownMessage
	 */
	protected final void setCooldownMessage(String cooldownMessage) {
		this.cooldownMessage = cooldownMessage;
	}

	/**
	 * Sets the usage message for this command
	 *
	 * @param usage
	 */
	protected final void setUsage(String usage) {
		this.usage = usage;
	}

	/**
	 * Sets the description message of this command
	 *
	 * @param description
	 */
	protected final void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the permission for this command, either the one you set or our from Localization
	 */
	public final String getPermissionMessage() {
		return Common.getOrDefault(permissionMessage, "&cInsufficient permission ({permission})");
	}

	/**
	 * Set the permission message, you can use {permission} to replace with the missing permission
	 *
	 * @param permissionMessage the permissionMessage to set
	 */
	public final void setPermissionMessage(String permissionMessage) {
		this.permissionMessage = permissionMessage;
	}

	/**
	 * By default we check if the player has the permission you set in setPermission.
	 *
	 * If that is null, we check for the following:
	 * {yourpluginname}.command.{label} for {@link SimpleCommand}
	 * {yourpluginname}.command.{label}.{sublabel} for {@link SimpleSubCommand}
	 *
	 * We handle lacking permissions automatically and return with an no-permission message
	 * when the player lacks it.
	 *
	 * @return
	 */
	@Override
	public final String getPermission() {
		return super.getPermission() == null ? null : replaceBasicPlaceholders0(super.getPermission());
	}

	/**
	 * Get the permission without replacing {plugin.name}, {label} or {sublabel}
	 *
	 * @deprecated internal use only
	 * @return
	 */
	@Deprecated
	public final String getRawPermission() {
		return permission;
	}

	/**
	 * Sets the permission required for this command to run. If you set the
	 * permission to null we will not require any permission (unsafe).
	 *
	 * @param
	 */
	public final void setPermission(String permission) {
		this.permission = permission;
	}

	/**
	 * Get the sender of this command
	 *
	 * @return
	 */
	protected final CommandSender getSender() {
		Valid.checkNotNull(sender, "Sender cannot be null");

		return sender;
	}

	/**
	 * Get aliases for this command
	 */
	@Override
	public final String[] getAliases() {
		return super.getAliases();
	}

	/**
	 * Get description for this command
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Get the name of this command
	 */
	@Override
	public final String getName() {
		return super.getName();
	}

	/**
	 * Get the usage message of this command
	 */
	public final String getUsage() {
		return usage;
	}

	/**
	 * Get the most recent label for this command
	 */
	public final String getLabel() {
		return label;
	}

	/**
	 * Updates the label of this command
	 */
	public final void setLabel(String name) {
		this.label = name;
	}

	/**
	 * Set whether we automatically show usage params in {@link #getMinArguments()}
	 * and when the first arg == "help" or "?"
	 *
	 * True by default
	 *
	 * @param autoHandleHelp
	 */
	protected final void setAutoHandleHelp(boolean autoHandleHelp) {
		this.autoHandleHelp = autoHandleHelp;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SimpleCommand ? ((SimpleCommand) obj).getLabel().equals(this.getLabel()) && ((SimpleCommand) obj).getAliases().equals(this.getAliases()) : false;
	}

	@Override
	public String toString() {
		return "Command{label=/" + label + "}";
	}
}
