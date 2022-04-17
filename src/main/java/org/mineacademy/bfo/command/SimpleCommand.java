package org.mineacademy.bfo.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.Messenger;
import org.mineacademy.bfo.PlayerUtil;
import org.mineacademy.bfo.ReflectionUtil;
import org.mineacademy.bfo.TabUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.collection.expiringmap.ExpiringMap;
import org.mineacademy.bfo.command.SimpleCommandGroup.MainCommand;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.debug.LagCatcher;
import org.mineacademy.bfo.exception.CommandException;
import org.mineacademy.bfo.exception.EventHandledException;
import org.mineacademy.bfo.exception.InvalidCommandArgException;
import org.mineacademy.bfo.model.ChatPaginator;
import org.mineacademy.bfo.model.Replacer;
import org.mineacademy.bfo.model.SimpleComponent;
import org.mineacademy.bfo.model.SimpleTime;
import org.mineacademy.bfo.plugin.SimplePlugin;
import org.mineacademy.bfo.settings.SimpleLocalization;

import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.api.scheduler.ScheduledTask;

/**
 * A simple command used to replace all Bukkit/Spigot command functionality
 * across any plugin that utilizes this.
 */
public abstract class SimpleCommand extends net.md_5.bungee.api.plugin.Command implements TabExecutor {

	/**
	 * Denotes an empty list used to disable tab-completion
	 */
	protected static final List<String> NO_COMPLETE = Collections.unmodifiableList(new ArrayList<>());

	/**
	 * You can set the cooldown time before executing the command again. This map
	 * stores the player uuid and his last execution of the command.
	 */
	private final ExpiringMap<UUID, Long> cooldownMap = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();

	/**
	 * Has this command been already registered?
	 */
	private boolean registered = false;

	/**
	 * The {@link Common#getTellPrefix()} custom prefix only used for sending messages in {@link #onCommand()} method
	 * for this command, null to use the one in Common#getTellPrefix or empty to force no prefix.
	 */
	private String tellPrefix = null;

	/**
	 * Minimum arguments required to run this command
	 */
	@Getter
	private int minArguments = 0;

	/**
	 * The command cooldown before we can run this command again
	 */
	@Getter
	private int cooldownSeconds = 0;

	/**
	 * A custom message when the player attempts to run this command
	 * within {@link #cooldownSeconds}. By default we use the one found in
	 * {@link SimpleLocalization.Commands#COOLDOWN_WAIT}
	 * <p>
	 * TIP: Use {duration} to replace the remaining time till next run
	 */
	private String cooldownMessage = null;

	/**
	 * Should we automatically send usage message when the first argument
	 * equals to "help" or "?" ?
	 */
	private boolean autoHandleHelp = true;

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

	// ----------------------------------------------------------------------
	// Temporary variables
	// ----------------------------------------------------------------------

	/**
	 * The command sender, or null if does not exist
	 * <p>
	 * This variable is updated dynamically when the command is run with the
	 * last known sender
	 */
	protected CommandSender sender;

	/**
	 * The arguments used when the command was last executed
	 * <p>
	 * This variable is updated dynamically when the command is run with the
	 * last known arguments
	 */
	protected String[] args;

	// ----------------------------------------------------------------------

	/**
	 * Create a new simple command with the given label.
	 * <p>
	 * Separate the label with | to split between label and aliases.
	 * Example: remove|r|rm will create a /remove command that can
	 * also be run by typing /r and /rm as its aliases.
	 *
	 * @param label
	 */
	protected SimpleCommand(final String label) {
		this(parseLabel0(label), parseAliases0(label));
	}

	/**
	 * Create a new simple command from the list. The first
	 * item in the list is the main label and the other ones are the aliases.
	 */
	protected SimpleCommand(final StrictList<String> labels) {
		this(parseLabelList0(labels), labels.size() > 1 ? labels.subList(1, labels.size()) : null);
	}

	/**
	 * Create a new simple command
	 *
	 * @param label
	 * @param aliases
	 */
	protected SimpleCommand(final String label, final List<String> aliases) {
		this(label, getDefaultPermission(), aliases == null ? null : Common.toArray(aliases));
	}

	/**
	 * Create a new simple command
	 *
	 * @param label
	 * @param permission
	 * @param aliases
	 */
	protected SimpleCommand(final String label, final String permission, final String... aliases) {
		super(label, null, aliases);

		// Bungee does not handle no permission message properly > no variables replaced etc.,
		// so we make it think there's no required permission and handle on our end
		this.setPermission(permission);
	}

	/*
	 * Split the given label by | and get the first part, used as the main label
	 */
	private static String parseLabel0(final String label) {
		Valid.checkNotNull(label, "Label must not be null!");

		return label.split("(\\||\\/)")[0];
	}

	/*
	 * Split the given label by | and use the second and further parts as aliases
	 */
	private static List<String> parseAliases0(final String label) {
		final String[] aliases = label.split("(\\||\\/)");

		return aliases.length > 0 ? Arrays.asList(Arrays.copyOfRange(aliases, 1, aliases.length)) : new ArrayList<>();
	}

	/*
	 * Return the first index from the list or thrown an error if list empty
	 */
	private static String parseLabelList0(final StrictList<String> labels) {
		Valid.checkBoolean(!labels.isEmpty(), "Command label must not be empty!");

		return labels.get(0);
	}

	// ----------------------------------------------------------------------
	// Registration
	// ----------------------------------------------------------------------

	/**
	 * Registers this command into Bukkit.
	 * <p>
	 * Throws an error if the command is registered already.
	 */
	public final void register() {
		Valid.checkBoolean(!(this instanceof SimpleSubCommand), "Sub commands cannot be registered!");
		Valid.checkBoolean(!registered, "The command /" + getLabel() + " has already been registered!");

		if (!canRegister())
			return;

		final PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();
		final Command oldCommand = findExistingCommand(getLabel());

		if (oldCommand != null) {
			Debugger.debug("command", "Command /" + getLabel() + " already registered, re-registering for plugin " + SimplePlugin.getNamed());

			pluginManager.unregisterCommand(oldCommand);
		}

		pluginManager.registerCommand(SimplePlugin.getInstance(), this);
		registered = true;
	}

	private Command findExistingCommand(final String label) {
		for (final Entry<String, Command> entry : ProxyServer.getInstance().getPluginManager().getCommands())
			if (entry.getKey().equals(label))
				return entry.getValue();

		return null;
	}

	/**
	 * Removes the command from Bukkit.
	 * <p>
	 * Throws an error if the command is not registered.
	 */
	public final void unregister() {
		Valid.checkBoolean(!(this instanceof SimpleSubCommand), "Sub commands cannot be unregistered!");
		Valid.checkBoolean(registered, "The command /" + getLabel() + " is not registered!");

		final PluginManager pluginManager = ProxyServer.getInstance().getPluginManager();

		pluginManager.unregisterCommand(this);
		registered = false;
	}

	/**
	 * Return true if this command can be registered through {@link #register()} methods.
	 * By default true.
	 *
	 * @return
	 */
	protected boolean canRegister() {
		return true;
	}

	// ----------------------------------------------------------------------
	// Execution
	// ----------------------------------------------------------------------

	/**
	 * Execute this command, updates the sender, label and args variables,
	 * checks permission and returns if the sender lacks it,
	 * checks minimum arguments and finally passes the command to the child class.
	 * <p>
	 * Also contains various error handling scenarios
	 */
	@Override
	public final void execute(final CommandSender sender, final String[] args) {

		if (SimplePlugin.isReloading() || !SimplePlugin.getInstance().isEnabled()) {
			Common.tell(sender, SimpleLocalization.Commands.USE_WHILE_NULL.replace("{state}", SimplePlugin.isReloading() ? SimpleLocalization.Commands.RELOADING : SimpleLocalization.Commands.DISABLED));

			return;
		}

		// Set variables to re-use later
		this.sender = sender;
		this.args = args;

		// Set tell prefix only if the parent setting was on
		final String oldTellPrefix = Common.getTellPrefix();

		if (this.tellPrefix != null)
			Common.setTellPrefix(this.tellPrefix);

		// Optional sublabel if this is a sub command
		final String sublabel = this instanceof SimpleSubCommand ? " " + ((SimpleSubCommand) this).getSublabel() : "";

		// Catch "errors" that contain a message to send to the player
		// Measure performance of all commands
		final String lagSection = "Command /" + getLabel() + sublabel + (args.length > 0 ? " " + String.join(" ", args) : "");

		try {
			// Prevent duplication since MainCommand delegates this
			if (!(this instanceof MainCommand))
				LagCatcher.start(lagSection);

			// Check if sender has the proper permission
			if (getPermission() != null)
				checkPerm(getPermission());

			// Check for minimum required arguments and print help
			if (args.length < getMinArguments() || autoHandleHelp && args.length == 1 && ("help".equals(args[0]) || "?".equals(args[0]))) {

				Common.runAsync(() -> {
					final String usage = getMultilineUsageMessage() != null ? String.join("\n&c", getMultilineUsageMessage()) : getUsage() != null ? getUsage() : null;
					Valid.checkNotNull(usage, "getUsage() nor getMultilineUsageMessage() not implemented for '/" + getLabel() + sublabel + "' command!");

					final ChatPaginator paginator = new ChatPaginator(SimpleLocalization.Commands.HEADER_SECONDARY_COLOR);
					final List<String> pages = new ArrayList<>();

					if (!Common.getOrEmpty(getDescription()).isEmpty()) {
						pages.add(replacePlaceholders(SimpleLocalization.Commands.LABEL_DESCRIPTION));
						pages.add(replacePlaceholders("&c" + getDescription()));
					}

					if (getMultilineUsageMessage() != null) {
						pages.add("");
						pages.add(replacePlaceholders(SimpleLocalization.Commands.LABEL_USAGES));

						for (final String usagePart : usage.split("\n"))
							pages.add(replacePlaceholders("&c" + usagePart));

					} else {
						pages.add("");
						pages.add(SimpleLocalization.Commands.LABEL_USAGE);
						pages.add("&c" + replacePlaceholders("/" + this.getLabel() + sublabel + (!usage.startsWith("/") ? " " + Common.stripColors(usage) : "")));
					}

					paginator
							.setFoundationHeader(SimpleLocalization.Commands.LABEL_HELP_FOR.replace("{label}", getLabel() + sublabel))
							.setPages(Common.toArray(pages));

					// Force sending on the main thread
					Common.runAsync(() -> paginator.send(sender));
				});

				return;
			}

			// Check if we can run this command in time
			if (cooldownSeconds > 0)
				handleCooldown();

			onCommand();

		} catch (final InvalidCommandArgException ex) {
			if (getMultilineUsageMessage() == null)
				dynamicTellError(ex.getMessage() != null ? ex.getMessage() : SimpleLocalization.Commands.INVALID_SUB_ARGUMENT);
			else {
				dynamicTellError(SimpleLocalization.Commands.INVALID_ARGUMENT_MULTILINE);

				for (final String line : getMultilineUsageMessage())
					tellNoPrefix("&c" + line);
			}

		} catch (final EventHandledException ex) {
			if (ex.getMessages() != null)
				dynamicTellError(ex.getMessages());

		} catch (final CommandException ex) {
			if (ex.getMessages() != null)
				dynamicTellError(ex.getMessages());

		} catch (final Throwable t) {
			dynamicTellError(SimpleLocalization.Commands.ERROR.replace("{error}", t.toString()));

			Common.error(t, "Failed to execute command /" + getLabel() + sublabel + " " + String.join(" ", args));

		} finally {
			Common.setTellPrefix(oldTellPrefix);

			// Prevent duplication since MainCommand delegates this
			if (!(this instanceof MainCommand))
				LagCatcher.end(lagSection, 8, "{section} took {time} ms");
		}
	}

	/*
	 * If messenger is on, we send the message prefixed with Messenger.getErrorPrefix()
	 * otherwise we just send a normal message
	 */
	private void dynamicTellError(final String... messages) {
		if (Messenger.ENABLED)
			for (final String message : messages)
				tellError(message);
		else
			tell(messages);
	}

	/**
	 * Check if the command cooldown is active and if the command
	 * is run within the given limit, we stop it and inform the player
	 */
	private void handleCooldown() {
		if (isPlayer()) {
			final ProxiedPlayer player = getPlayer();

			final long lastExecution = cooldownMap.getOrDefault(player.getUniqueId(), 0L);
			final long lastExecutionDifference = (System.currentTimeMillis() - lastExecution) / 1000;

			// Check if the command was not run earlier within the wait threshold
			checkBoolean(lastExecution == 0 || lastExecutionDifference > cooldownSeconds, Common.getOrDefault(cooldownMessage, SimpleLocalization.Commands.COOLDOWN_WAIT).replace("{duration}", cooldownSeconds - lastExecutionDifference + 1 + ""));

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
			throw new CommandException("&c" + SimpleLocalization.Commands.NO_CONSOLE);
	}

	/**
	 * Checks if the current sender has the given permission
	 *
	 * @param perm
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull final String perm) throws CommandException {
		if (isPlayer() && !hasPerm(perm))
			throw new CommandException(getPermissionMessage().replace("{permission}", perm));
	}

	/**
	 * Checks if the given sender has the given permission
	 *
	 * @param sender
	 * @param perm
	 * @throws CommandException
	 */
	public final void checkPerm(@NonNull CommandSender sender, @NonNull final String perm) throws CommandException {
		if (isPlayer() && !hasPerm(sender, perm))
			throw new CommandException(getPermissionMessage().replace("{permission}", perm));
	}

	/**
	 * Check if the command arguments are of the minimum length
	 *
	 * @param minimumLength
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkArgs(final int minimumLength, final String falseMessage) throws CommandException {
		if (args.length < minimumLength)
			returnTell((Messenger.ENABLED ? "" : "&c") + falseMessage);
	}

	/**
	 * Checks if the given boolean is true
	 *
	 * @param value
	 * @param falseMessage
	 * @throws CommandException
	 */
	protected final void checkBoolean(final boolean value, final String falseMessage) throws CommandException {
		if (!value)
			returnTell((Messenger.ENABLED ? "" : "&c") + falseMessage);
	}

	/**
	 * Check if the given boolean is true or returns {@link #returnInvalidArgs()}
	 *
	 * @param value
	 *
	 * @throws CommandException
	 */
	protected final void checkUsage(final boolean value) throws CommandException {
		if (!value)
			returnInvalidArgs();
	}

	/**
	 * Checks if the given object is not null
	 *
	 * @param value
	 * @param messageIfNull
	 * @throws CommandException
	 */
	protected final void checkNotNull(final Object value, final String messageIfNull) throws CommandException {
		if (value == null)
			returnTell((Messenger.ENABLED ? "" : "&c") + messageIfNull);
	}

	/**
	 * Attempts to find a non-vanished online player, failing with the message
	 * found at {@link SimpleLocalization.Player#NOT_ONLINE}
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	protected final ProxiedPlayer findPlayer(final String name) throws CommandException {
		return findPlayer(name, SimpleLocalization.Player.NOT_ONLINE);
	}

	/**
	 * Attempts to find a non-vanished online player, failing with a false message
	 *
	 * @param name
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final ProxiedPlayer findPlayer(final String name, final String falseMessage) throws CommandException {
		final ProxiedPlayer player = findPlayerInternal(name);
		checkBoolean(player != null, falseMessage.replace("{player}", name));

		return player;
	}

	/**
	 * Return the player by the given name, and, when the name is null, return the sender if sender is player.
	 *
	 * @param name
	 * @return
	 * @throws CommandException
	 */
	protected final ProxiedPlayer findPlayerOrSelf(final String name) throws CommandException {
		if (name == null) {
			checkBoolean(isPlayer(), SimpleLocalization.Commands.CONSOLE_MISSING_PLAYER_NAME);

			return getPlayer();
		}

		final ProxiedPlayer player = findPlayerInternal(name);
		checkBoolean(player != null, SimpleLocalization.Player.NOT_ONLINE.replace("{player}", name));

		return player;
	}

	/**
	 * A simple call to Bukkit.getPlayer(name) meant to be overriden
	 * if you have a custom implementation of getting players by name.
	 *
	 * Example use: ChatControl can find players by their nicknames too
	 *
	 * @param name
	 * @return
	 */
	protected ProxiedPlayer findPlayerInternal(String name) {
		return ProxyServer.getInstance().getPlayer(name);
	}

	/**
	 * Attempts to convert the given input (such as 1 hour) into
	 * a {@link SimpleTime} object
	 *
	 * @param raw
	 * @return
	 */
	protected final SimpleTime findTime(String raw) {
		try {
			return SimpleTime.from(raw);

		} catch (final IllegalArgumentException ex) {
			returnTell(SimpleLocalization.Commands.INVALID_TIME.replace("{input}", raw));

			return null;
		}
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
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String name, final String falseMessage) throws CommandException {
		return this.findEnum(enumType, name, null, falseMessage);
	}

	/**
	 * Finds an enumeration of a certain type, if it fails it prints a false message to the player
	 * You can use the {enum} variable in the false message for the name parameter
	 *
	 * You can also use the condition to filter certain enums and act as if they did not existed
	 * if your function returns false for such
	 *
	 * @param <T>
	 * @param enumType
	 * @param name
	 * @param condition
	 * @param falseMessage
	 * @return
	 * @throws CommandException
	 */
	protected final <T extends Enum<T>> T findEnum(final Class<T> enumType, final String name, final Function<T, Boolean> condition, final String falseMessage) throws CommandException {
		T found = null;

		try {
			found = ReflectionUtil.lookupEnum(enumType, name);

			if (!condition.apply(found))
				found = null;

		} catch (final Throwable t) {
			// Not found, pass through below to error out
		}

		checkNotNull(found, falseMessage.replace("{enum}", name).replace("{available}", Common.join(enumType.getEnumConstants())));
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
	protected final int findNumber(final int index, final int min, final int max, final String falseMessage) {
		return findNumber(Integer.class, index, min, max, falseMessage);
	}

	/**
	 * A convenience method for parsing a number at the given args index
	 *
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final int findNumber(final int index, final String falseMessage) {
		return findNumber(Integer.class, index, falseMessage);
	}

	/**
	 * A convenience method for parsing any number type that is between two bounds
	 * Number can be of any type, that supports method valueOf(String)
	 * You can use {min} and {max} in the message to be automatically replaced
	 *
	 * @param <T>
	 * @param numberType
	 * @param index
	 * @param min
	 * @param max
	 * @param falseMessage
	 * @return
	 */
	protected final <T extends Number & Comparable<T>> T findNumber(final Class<T> numberType, final int index, final T min, final T max, final String falseMessage) {
		final T number = findNumber(numberType, index, falseMessage);
		checkBoolean(number.compareTo(min) >= 0 && number.compareTo(max) <= 0, falseMessage.replace("{min}", min + "").replace("{max}", max + ""));

		return number;
	}

	/**
	 * A convenience method for parsing any number type at the given args index
	 * Number can be of any type, that supports method valueOf(String)
	 *
	 * @param <T>
	 * @param numberType
	 * @param index
	 * @param falseMessage
	 * @return
	 */
	protected final <T extends Number> T findNumber(final Class<T> numberType, final int index, final String falseMessage) {
		checkBoolean(index < args.length, falseMessage);

		try {
			return (T) numberType.getMethod("valueOf", String.class).invoke(null, args[index]); // Method valueOf is part of all main Number sub classes, eg. Short, Integer, Double, etc.
		}

		catch (final IllegalAccessException | NoSuchMethodException e) {
			e.printStackTrace();

		} catch (final InvocationTargetException e) {

			// Print stack trace for all exceptions, except NumberFormatException
			// NumberFormatException is expected to happen, in this case we just want to display falseMessage without stack trace
			if (!(e.getCause() instanceof NumberFormatException))
				e.printStackTrace();
		}

		throw new CommandException(replacePlaceholders((Messenger.ENABLED ? "" : "&c") + falseMessage));
	}

	/**
	 * A convenience method for parsing a boolean at the given args index
	 *
	 * @param index
	 * @param invalidMessage
	 * @return
	 */
	protected final boolean findBoolean(final int index, final String invalidMessage) {
		checkBoolean(index < args.length, invalidMessage);

		if (args[index].equalsIgnoreCase("true"))
			return true;

		else if (args[index].equalsIgnoreCase("false"))
			return false;

		throw new CommandException(replacePlaceholders((Messenger.ENABLED ? "" : "&c") + invalidMessage));
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
		return this.hasPerm(sender, permission);
	}

	/**
	 * A convenience check for quickly determining if the sender has a given
	 * permission.
	 *
	 * TIP: For a more complete check use {@link #checkPerm(String)} that
	 * will automatically return your command if they lack the permission.
	 *
	 * @param sender
	 * @param permission
	 * @return
	 */
	protected final boolean hasPerm(CommandSender sender, String permission) {
		return permission == null ? true : PlayerUtil.hasPerm(sender, permission.replace("{label}", getLabel()));
	}

	// ----------------------------------------------------------------------
	// Messaging
	// ----------------------------------------------------------------------

	/**
	 * Sends a message to the player
	 *
	 * @see Replacer#replaceArray
	 *
	 * @param message
	 * @param replacements
	 */
	protected final void tellReplaced(final String message, final Object... replacements) {
		tell(Replacer.replaceArray(message, replacements));
	}

	/**
	 * Sends a interactive chat component to the sender, not replacing any special
	 * variables just executing the {@link SimpleComponent#send(CommandSender...)} method
	 * as a shortcut
	 *
	 * @param components
	 */
	protected final void tell(List<SimpleComponent> components) {
		if (components != null)
			tell(components.toArray(new SimpleComponent[components.size()]));
	}

	/**
	 * Sends a interactive chat component to the sender, not replacing any special
	 * variables just executing the {@link SimpleComponent#send(CommandSender...)} method
	 * as a shortcut
	 *
	 * @param components
	 */
	protected final void tell(SimpleComponent... components) {
		if (components != null)
			for (final SimpleComponent component : components)
				component.send(sender);
	}

	/**
	 * Send a list of messages to the player
	 *
	 * @param messages
	 */
	protected final void tell(Collection<String> messages) {
		if (messages != null)
			tell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Sends a multiline message to the player without plugin's prefix.
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(Collection<String> messages) {
		tellNoPrefix(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Sends a multiline message to the player without plugin's prefix.
	 *
	 * @param messages
	 */
	protected final void tellNoPrefix(String... messages) {
		final String oldLocalPrefix = this.tellPrefix;

		this.tellPrefix = "";

		tell(messages);

		this.tellPrefix = oldLocalPrefix;
	}

	/**
	 * Sends a multiline message to the player, avoiding prefix if 3 lines or more
	 *
	 * @param messages
	 */
	protected final void tell(String... messages) {

		if (messages == null)
			return;

		final String oldTellPrefix = Common.getTellPrefix();

		if (this.tellPrefix != null)
			Common.setTellPrefix(this.tellPrefix);

		try {
			messages = replacePlaceholders(messages);

			if (messages.length > 2) {
				Common.tellNoPrefix(sender, messages);

			} else
				Common.tell(sender, messages);

		} finally {
			Common.setTellPrefix(oldTellPrefix);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellSuccess(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.success(sender, message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellInfo(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.info(sender, message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellWarn(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.warn(sender, message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellError(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.error(sender, message);
		}
	}

	/**
	 * Sends a no prefix message to the player
	 *
	 * @param message
	 */
	protected final void tellQuestion(String message) {
		if (message != null) {
			message = replacePlaceholders(message);

			Messenger.question(sender, message);
		}
	}

	/**
	 * Convenience method for returning the command with the {@link SimpleLocalization.Commands#INVALID_ARGUMENT}
	 * message for player
	 */
	protected final void returnInvalidArgs() {
		tellError(SimpleLocalization.Commands.INVALID_ARGUMENT.replace("{label}", getLabel()));

		throw new CommandException();
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param messages
	 * @throws CommandException
	 */
	protected final void returnTell(final Collection<String> messages) throws CommandException {
		returnTell(messages.toArray(new String[messages.size()]));
	}

	/**
	 * Sends a message to the player and throws a message error, preventing further execution
	 *
	 * @param messages
	 * @throws CommandException
	 */
	protected final void returnTell(final String... messages) throws CommandException {
		throw new CommandException(replacePlaceholders(messages));
	}

	/**
	 * Ho ho ho, returns the command usage to the sender
	 *
	 * @throws InvalidCommandArgException
	 */
	protected final void returnUsage() throws InvalidCommandArgException {
		throw new InvalidCommandArgException();
	}

	// ----------------------------------------------------------------------
	// Placeholder
	// ----------------------------------------------------------------------

	/**
	 * Replaces placeholders in all messages
	 * To change them override {@link #replacePlaceholders(String)}
	 *
	 * @param messages
	 * @return
	 */
	protected final String[] replacePlaceholders(final String[] messages) {
		for (int i = 0; i < messages.length; i++)
			messages[i] = replacePlaceholders(messages[i]).replace("{prefix}", Common.getTellPrefix());

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

		return message;
	}

	/**
	 * Internal method for replacing {label} and {sublabel}
	 *
	 * @param message
	 * @return
	 */
	private String replaceBasicPlaceholders0(final String message) {
		return message
				.replace("{label}", getLabel())
				.replace("{sublabel}", this instanceof SimpleSubCommand ? ((SimpleSubCommand) this).getSublabels()[0] : this.args != null && this.args.length > 0 ? this.args[0] : getLabel());
	}

	/**
	 * Utility method to safely update the args, increasing them if the position is too high
	 * <p>
	 * Used in placeholders
	 *
	 * @param position
	 * @param value
	 */
	protected final void setArg(final int position, final String value) {
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
	protected final String[] rangeArgs(final int from) {
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
	protected final String[] rangeArgs(final int from, final int to) {
		return Arrays.copyOfRange(args, from, to);
	}

	/**
	 * Copies and returns the arguments {@link #args} from the given range
	 * to their end joined by spaces
	 *
	 * @param from
	 * @return
	 */
	protected final String joinArgs(final int from) {
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
	protected final String joinArgs(final int from, final int to) {
		String message = "";

		for (int i = from; i < args.length && i < to; i++)
			message += args[i] + (i + 1 == args.length ? "" : " ");

		return message;
	}

	// ----------------------------------------------------------------------
	// Tab completion
	// ----------------------------------------------------------------------

	/**
	 * Show tab completion suggestions when the given sender
	 * writes the command with the given arguments
	 *
	 * Tab completion is only shown if the sender has {@link #getPermission()}.
	 */
	@Override
	public final Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		this.sender = sender;
		this.args = args;

		if (hasPerm(getPermission())) {
			List<String> suggestions = tabComplete();

			// Return online player names when suggestions are null - simulate Bukkit behaviour
			if (suggestions == null)
				suggestions = completeLastWordPlayerNames();

			return suggestions;
		}

		return new ArrayList<>();
	}

	/**
	 * Override this method to support tab completing in your command.
	 * <p>
	 * You can then use "sender", "label" or "args" fields from {@link SimpleCommand}
	 * class normally and return a list of tab completion suggestions.
	 * <p>
	 * We already check for {@link #getPermission()} and only call this method if the
	 * sender has it.
	 * <p>
	 * TIP: Use {@link #completeLastWord(Iterable)} and {@link #getLastArg()} methods
	 * in {@link SimpleCommand} for your convenience
	 *
	 * @return the list of suggestions to complete, or null to complete player names automatically
	 */
	protected List<String> tabComplete() {
		return null;
	}

	/**
	 * Convenience method for completing all player names that the sender can see
	 * and that are not vanished
	 * <p>
	 * TIP: You can simply return null for the same behaviour
	 *
	 * @return
	 */
	protected List<String> completeLastWordPlayerNames() {
		return TabUtil.complete(getLastArg(), isPlayer() ? Common.getPlayerNames() : Common.getPlayerNames());
	}

	/**
	 * Convenience method for automatically completing the last word
	 * with the given suggestions. We sort them and only select ones
	 * that the last word starts with.
	 *
	 * @param <T>
	 * @param suggestions
	 * @return
	 */
	@SafeVarargs
	protected final <T> List<String> completeLastWord(final T... suggestions) {
		return TabUtil.complete(getLastArg(), suggestions);
	}

	/**
	 * Convenience method for automatically completing the last word
	 * with the given suggestions. We sort them and only select ones
	 * that the last word starts with.
	 *
	 * @param <T>
	 * @param suggestions
	 * @return
	 */
	protected final <T> List<String> completeLastWord(final Iterable<T> suggestions) {
		final List<T> list = new ArrayList<>();

		for (final T suggestion : suggestions)
			list.add(suggestion);

		return TabUtil.complete(getLastArg(), list.toArray());
	}

	/**
	 * Convenience method for automatically completing the last word
	 * with the given suggestions converting them to a string. We sort them and only select ones
	 * that the last word starts with.
	 *
	 * @param <T>
	 * @param suggestions
	 * @param toString
	 * @return
	 */
	protected final <T> List<String> completeLastWord(final Iterable<T> suggestions, Function<T, String> toString) {
		final List<String> list = new ArrayList<>();

		for (final T suggestion : suggestions)
			list.add(toString.apply(suggestion));

		return TabUtil.complete(getLastArg(), list.toArray());
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
	 * Sets a custom prefix used in tell messages for this command.
	 * This overrides {@link Common#getTellPrefix()} however won't work if
	 * {@link #addTellPrefix} is disabled
	 *
	 * @param tellPrefix
	 */
	protected final void setTellPrefix(final String tellPrefix) {
		this.tellPrefix = tellPrefix;
	}

	/**
	 * Sets the minimum number of arguments to run this command
	 *
	 * @param minArguments
	 */
	protected final void setMinArguments(final int minArguments) {
		Valid.checkBoolean(minArguments >= 0, "Minimum arguments must be 0 or greater");

		this.minArguments = minArguments;
	}

	/**
	 * Set the time before the same player can execute this command again
	 *
	 * @param cooldown
	 * @param unit
	 */
	protected final void setCooldown(final int cooldown, final TimeUnit unit) {
		Valid.checkBoolean(cooldown >= 0, "Cooldown must be >= 0 for /" + getLabel());

		cooldownSeconds = (int) unit.toSeconds(cooldown);
	}

	/**
	 * Set a custom cooldown message, by default we use the one found in {@link SimpleLocalization.Commands#COOLDOWN_WAIT}
	 * <p>
	 * Use {duration} to dynamically replace the remaining time
	 *
	 * @param cooldownMessage
	 */
	protected final void setCooldownMessage(final String cooldownMessage) {
		this.cooldownMessage = cooldownMessage;
	}

	/**
	 * Get the permission for this command, either the one you set or our from Localization
	 */
	@Override
	public final String getPermissionMessage() {
		return Common.getOrDefault(super.getPermissionMessage(), SimpleLocalization.NO_PERMISSION);
	}

	/**
	 * By default we check if the player has the permission you set in setPermission.
	 * <p>
	 * If that is null, we check for the following:
	 * {yourpluginname}.command.{label} for {@link SimpleCommand}
	 * {yourpluginname}.command.{label}.{sublabel} for {@link SimpleSubCommand}
	 * <p>
	 * We handle lacking permissions automatically and return with an no-permission message
	 * when the player lacks it.
	 *
	 * @return
	 */
	@Override
	public final String getPermission() {
		return this.permission == null ? null : replaceBasicPlaceholders0(this.permission);
	}

	/**
	 * Set the permission to use this command, checked automatically.
	 *
	 * @param permission
	 */
	public void setPermission(String permission) {
		this.permission = permission;
	}

	/**
	 * Get the permission without replacing variables
	 *
	 * @return
	 * @deprecated internal use only
	 */
	@Deprecated
	protected final String getRawPermission() {
		return this.permission;
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
	 * Get description for this command
	 */
	public final String getDescription() {
		return this.description;
	}

	/**
	 * Set description for this command
	 *
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
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
		final String bukkitUsage = this.usage;

		return bukkitUsage.equals("/" + getLabel()) ? "" : bukkitUsage;
	}

	/**
	 * Set the usage message for this command
	 *
	 * @param usage
	 */
	public final void setUsage(String usage) {
		this.usage = usage;
	}

	/**
	 * Get the most recent label for this command
	 */
	public final String getLabel() {
		return super.getName();
	}

	/**
	 * Set whether we automatically show usage params in {@link #getMinArguments()}
	 * and when the first arg == "help" or "?"
	 * <p>
	 * True by default
	 *
	 * @param autoHandleHelp
	 */
	protected final void setAutoHandleHelp(final boolean autoHandleHelp) {
		this.autoHandleHelp = autoHandleHelp;
	}

	// ----------------------------------------------------------------------
	// Scheduling
	// ----------------------------------------------------------------------

	/**
	 * Runs the given task later, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param runnable
	 * @return
	 */
	protected final ScheduledTask runLater(Runnable runnable) {
		return Common.runAsync(() -> this.delegateTask(runnable));
	}

	/**
	 * Runs the given task later, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final ScheduledTask runLater(int delayTicks, Runnable runnable) {
		return Common.runLaterAsync(delayTicks, () -> this.delegateTask(runnable));
	}

	/**
	 * Runs the given task asynchronously, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param runnable
	 * @return
	 */
	protected final ScheduledTask runAsync(Runnable runnable) {
		return Common.runAsync(() -> this.delegateTask(runnable));
	}

	/**
	 * Runs the given task asynchronously, this supports checkX methods
	 * where we handle sending messages to player automatically
	 *
	 * @param delayTicks
	 * @param runnable
	 * @return
	 */
	protected final ScheduledTask runAsync(int delayTicks, Runnable runnable) {
		return Common.runLaterAsync(delayTicks, () -> this.delegateTask(runnable));
	}

	/*
	 * A helper method to catch command-related exceptions from runnables
	 */
	private void delegateTask(Runnable runnable) {
		try {
			runnable.run();

		} catch (final CommandException ex) {
			if (ex.getMessages() != null)
				for (final String message : ex.getMessages()) {
					if (Messenger.ENABLED)
						Messenger.error(sender, message);
					else
						Common.tell(sender, message);
				}

		} catch (final Throwable t) {
			final String errorMessage = SimpleLocalization.Commands.ERROR.replace("{error}", t.toString());

			if (Messenger.ENABLED)
				Messenger.error(sender, errorMessage);
			else
				Common.tell(sender, errorMessage);

			throw t;
		}
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SimpleCommand ? ((SimpleCommand) obj).getLabel().equals(getLabel()) && ((SimpleCommand) obj).getAliases().equals(getAliases()) : false;
	}

	@Override
	public final String toString() {
		return "Command{label=/" + this.getLabel() + "}";
	}

	/**
	 * Return the default permission syntax
	 *
	 * @return
	 */
	protected static final String getDefaultPermission() {
		return SimplePlugin.getNamed().toLowerCase() + ".command.{label}";
	}
}
