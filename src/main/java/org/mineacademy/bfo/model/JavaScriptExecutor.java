package org.mineacademy.bfo.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.ReflectionUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.exception.EventHandledException;
import org.mineacademy.bfo.plugin.SimplePlugin;
import org.mineacademy.bfo.remain.Remain;

import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Event;

/**
 * An engine that compiles and executes code on the fly.
 * <p>
 * The code is based off JavaScript with new Java methods, see:
 * https://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/
 */
public final class JavaScriptExecutor {

	/**
	 * The engine singleton
	 */
	private static final ScriptEngine engine;

	// Load the engine
	static {
		Thread.currentThread().setContextClassLoader(SimplePlugin.class.getClassLoader());

		ScriptEngineManager engineManager = new ScriptEngineManager();
		ScriptEngine scriptEngine = engineManager.getEngineByName("Nashorn");

		// Workaround for newer Minecraft releases, still unsure what the cause is
		if (scriptEngine == null) {
			engineManager = new ScriptEngineManager(null);

			scriptEngine = engineManager.getEngineByName("Nashorn");
		}

		// If still fails, try to load our own library for Java 15 and up
		if (scriptEngine == null) {
			final String nashorn = "org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory";

			if (ReflectionUtil.isClassAvailable(nashorn)) {
				final ScriptEngineFactory engineFactory = ReflectionUtil.instantiate(ReflectionUtil.lookupClass(nashorn));

				engineManager.registerEngineName("Nashorn", engineFactory);
				scriptEngine = engineManager.getEngineByName("Nashorn");
			}
		}

		engine = scriptEngine;

		if (engine == null) {
			final List<String> warningMessage = Common.newList(
					"ERROR: JavaScript placeholders will not function!",
					"",
					"Your Java version/distribution lacks the",
					"Nashorn library for JavaScript placeholders.");

			if (Remain.getJavaVersion() >= 15)
				warningMessage.addAll(Arrays.asList(
						"",
						"To fix this, install the NashornPlus",
						"plugin from mineacademy.org/nashorn"));
			else
				warningMessage.addAll(Arrays.asList(
						"",
						"To fix this, install Java 11 from Oracle",
						"or other vendor that supports Nashorn."));

			Common.logFramed(Common.toArray(warningMessage));
		}
	}

	/**
	 * Compiles and executes the given JavaScript code
	 *
	 * @param javascript
	 * @return
	 */
	public static Object run(final String javascript) {
		return run(javascript, null, null);
	}

	/**
	 * Runs the given JavaScript code for the player,
	 * making the "player" variable in the code usable
	 *
	 * @param javascript
	 * @param sender
	 * @return
	 */
	public static Object run(final String javascript, final CommandSender sender) {
		return run(javascript, sender, null);
	}

	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param sender
	 * @param event
	 * @return
	 */
	public static Object run(@NonNull String javascript, final CommandSender sender, final Event event) {
		synchronized (engine) {
			if (engine == null) {
				Common.warning("Not running script" + (sender == null ? "" : " for " + sender.getName()) + " because JavaScript library is missing "
						+ "(install Oracle Java 8, 11 or 16 and download mineacademy.org/nashorn): " + javascript);

				return null;
			}

			try {
				engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

				if (sender != null)
					engine.put("player", sender);

				else
					Valid.checkBoolean(!javascript.contains("player."), "Not running script because it uses 'player' but player was null! Script: " + javascript);

				if (event != null)
					engine.put("event", event);

				return engine.eval(javascript);

			} catch (final Throwable ex) {
				final String message = ex.toString();
				String error = "Script execution failed for";

				if (message.contains("ReferenceError:") && message.contains("is not defined"))
					error = "Found invalid or unparsed variable in";

				// Special support for throwing exceptions in the JS code so that users
				// can send messages to player directly if upstream supports that
				final String cause = ex.getCause().toString();

				if (ex.getCause() != null && cause.contains("event handled")) {
					final String[] errorMessageSplit = cause.contains("event handled: ") ? cause.split("event handled\\: ") : new String[0];

					if (errorMessageSplit.length == 2)
						Common.tellNoPrefix(sender, errorMessageSplit[1]);

					throw new EventHandledException(true);
				}

				throw new RuntimeException(error + " '" + javascript + "', sender: " + (sender == null ? "null" : sender.getClass() + ": " + sender), ex);
			}
		}
	}

	/**
	 * Executes the Javascript code with the given variables - you have to handle the error yourself
	 *
	 * @param javascript
	 * @param replacements
	 *
	 * @return
	 */
	public static Object run(final String javascript, final Map<String, Object> replacements) {

		if (engine == null) {
			Common.warning("Not running script because JavaScript library is missing "
					+ "(install Oracle Java 8, 11 or 16 and download mineacademy.org/nashorn): " + javascript);

			return javascript;
		}

		try {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			if (replacements != null)
				for (final Map.Entry<String, Object> replacement : replacements.entrySet())
					engine.put(replacement.getKey(), replacement.getValue());

			return engine.eval(javascript);

		} catch (final ScriptException ex) {
			throw new RuntimeException("Script execution failed for '" + javascript + "'", ex);
		}
	}
}