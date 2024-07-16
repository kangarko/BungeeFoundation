package org.mineacademy.bfo.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mineacademy.bfo.command.SimpleCommand;
import org.mineacademy.bfo.command.SimpleCommandGroup;
import org.mineacademy.bfo.model.SimpleExpansion;
import org.mineacademy.bfo.proxy.ProxyListener;
import org.mineacademy.bfo.settings.YamlConfig;

import net.md_5.bungee.api.plugin.Listener;

/**
 * Place this annotation over any of the following classes to make Foundation
 * automatically register it when the plugin starts, and properly reload it.
 *
 * Supported classes:
 * - {@link ProxyListener}
 * - {@link SimpleCommand}
 * - {@link SimpleCommandGroup}
 * - {@link SimpleExpansion}
 * - {@link YamlConfig} (we will load your config when the plugin starts and reload it properly)
 * - any class that "implements {@link Listener}"
 *
 * In addition, the following classes will self-register automatically regardless
 * if you place this annotation on them or not:
 * - Tool (and its derivates such as Rocket)
 * - SimpleEnchantment
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AutoRegister {

	/**
	 * When false, we wont print console warnings such as that registration failed
	 * because the server runs outdated MC version (example: SimpleEnchantment) or lacks
	 * necessary plugins to be hooked into (example: DiscordListener, PacketListener)
	 *
	 * @return
	 */
	boolean hideIncompatibilityWarnings() default false;
}
