package org.mineacademy.bfo.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class NextConfiguration {

	private static final ConfigurationProvider PROVIDER = ConfigurationProvider.getProvider(YamlConfiguration.class);

	@Getter
	private Configuration rootConfig;

	public void save(File file) {
		try {
			PROVIDER.save(rootConfig, file);

		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	public void load(File file) {
		try {
			rootConfig = PROVIDER.load(file);

		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	public static NextConfiguration fromFile(File file) {
		final NextConfiguration config = new NextConfiguration();

		try {
			config.rootConfig = PROVIDER.load(file);

		} catch (final IOException ex) {
			ex.printStackTrace();
		}

		return config;
	}

	public static NextConfiguration fromInputStream(InputStream is) {
		final NextConfiguration config = new NextConfiguration();

		try {
			config.rootConfig = PROVIDER.load(is);

		} catch (final Throwable t) {
			t.printStackTrace();
		}

		return config;
	}
}
