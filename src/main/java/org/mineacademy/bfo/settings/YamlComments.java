package org.mineacademy.bfo.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.FileUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.model.ConfigSerializable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import lombok.NonNull;
import net.md_5.bungee.config.Configuration;

/**
 * A class to update/add new sections/keys to your config while keeping your current values and keeping your comments
 * Algorithm:
 * Read the new file and scan for comments and ignored sections, if ignored section is found it is treated as a comment.
 * Read and write each line of the new config, if the old config has value for the given key it writes that value in the new config.
 * If a key has an attached comment above it, it is written first.
 *
 * @author tchristofferson, kangarko
 *
 * Source: https://github.com/tchristofferson/Config-Updater
 * Modified by MineAcademy.org
 */
public final class YamlComments {

	/**
	 * Update a yaml file from a resource inside your plugin jar
	 *
	 * @param jarPath The yaml file name to update from, typically config.yml
	 * @param diskFile The yaml file to update
	 *
	 * @throws IOException If an IOException occurs
	 */
	public static void writeComments(@NonNull String jarPath, @NonNull File diskFile) {
		try {
			writeComments(jarPath, diskFile, new ArrayList<>());

		} catch (final IOException ex) {
			Common.error(ex,
					"Failed writing comments!",
					"Path in plugin jar wherefrom comments are fetched: " + jarPath,
					"Disk file where comments are written: " + diskFile);
		}
	}

	/**
	 * Update a yaml file from a resource inside your plugin jar
	 *
	 * @param jarPath The yaml file name to update from, typically config.yml
	 * @param diskFile The yaml file to update
	 * @param ignoredSections The sections to ignore from being forcefully updated & comments set
	 *
	 * @throws IOException If an IOException occurs
	 */
	public static void writeComments(@NonNull String jarPath, @NonNull File diskFile, @NonNull List<String> ignoredSections) throws IOException {

		final InputStream internalResource = FileUtil.getInternalResource(jarPath);

		if (internalResource == null)
			return;

		final BufferedReader newReader = new BufferedReader(new InputStreamReader(internalResource, StandardCharsets.UTF_8));
		final List<String> newLines = newReader.lines().collect(Collectors.toList());
		newReader.close();

		final Configuration oldConfig = YamlConfig.PROVIDER.load(diskFile);
		final Configuration newConfig = YamlConfig.PROVIDER.load(FileUtil.getInternalResource(jarPath));

		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(diskFile), StandardCharsets.UTF_8));

		// ignoredSections can ONLY contain configurations sections
		for (final String ignoredSection : ignoredSections)
			if (newConfig.contains(ignoredSection))
				Valid.checkBoolean(newConfig.get(ignoredSection) instanceof Configuration, "Can only ignore config sections in " + jarPath + " (file " + diskFile + ")" + " not '" + ignoredSection + "' that is " + newConfig.get(ignoredSection));

		// Save keys added to config that are not in default and would otherwise be lost
		final List<String> newKeys = getDeepKeys(newConfig);
		final Map<String, Object> removedKeys = new HashMap<>();

		outerLoop:
		for (final Map.Entry<String, Object> oldEntry : getMap(oldConfig).entrySet()) {
			final String oldKey = oldEntry.getKey();

			for (final String ignoredKey : ignoredSections)
				if (oldKey.startsWith(ignoredKey))
					continue outerLoop;

			if (!newKeys.contains(oldKey))
				removedKeys.put(oldKey, oldEntry.getValue());
		}

		// Move to unused/ folder and retain old path
		if (!removedKeys.isEmpty()) {
			final File backupFile = FileUtil.getOrMakeFile("unused/" + diskFile.getName());

			final Configuration backupConfig = YamlConfig.PROVIDER.load(backupFile);

			for (final Map.Entry<String, Object> entry : removedKeys.entrySet())
				backupConfig.set(entry.getKey(), entry.getValue());

			YamlConfig.PROVIDER.save(backupConfig, new FileWriter(backupFile));

			Common.warning("The following entries in " + diskFile.getName() + " are unused and were moved into " + backupFile.getName() + ": " + removedKeys.keySet());
		}

		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setWidth(4096);

		final Yaml yaml = new Yaml(dumperOptions);
		final Map<String, String> comments = parseComments(newLines, ignoredSections, oldConfig, yaml);

		write(newConfig, oldConfig, comments, ignoredSections, writer, yaml);
	}

	// Write method doing the work.
	// It checks if key has a comment associated with it and writes comment then the key and value
	private static void write(Configuration newConfig, Configuration oldConfig, Map<String, String> comments, List<String> ignoredSections, BufferedWriter writer, Yaml yaml) throws IOException {

		final Set<String> copyAllowed = new HashSet<>();
		final Set<String> reverseCopy = new HashSet<>();

		outerloop:
		for (final String key : getDeepKeys(newConfig)) {

			checkIgnore:
			{

				for (final String allowed : copyAllowed)
					if (key.startsWith(allowed))
						break checkIgnore;

				// These keys are already written below
				for (final String allowed : reverseCopy)
					if (key.startsWith(allowed))
						continue outerloop;

				for (final String ignoredSection : ignoredSections) {
					if (key.equals(ignoredSection)) {

						// Write from new to old config
						if ((!oldConfig.contains(ignoredSection) || oldConfig.getSection(ignoredSection).getKeys().isEmpty())) {
							copyAllowed.add(ignoredSection);

							break;
						}

						// Write from old to new, copying all keys and subkeys manually
						else {
							write0(key, true, newConfig, oldConfig, comments, ignoredSections, writer, yaml);

							for (final String oldKey : getDeepKeys(oldConfig.getSection(ignoredSection))) {
								write0(ignoredSection + "." + oldKey, true, oldConfig, newConfig, comments, ignoredSections, writer, yaml);
							}

							reverseCopy.add(ignoredSection);
							continue outerloop;
						}
					}

					if (key.startsWith(ignoredSection))
						continue outerloop;
				}
			}

			write0(key, false, newConfig, oldConfig, comments, ignoredSections, writer, yaml);
		}

		final String danglingComments = comments.get(null);

		if (danglingComments != null)
			writer.write(danglingComments);

		writer.close();
	}

	private static void write0(String key, boolean forceNew, Configuration newConfig, Configuration oldConfig, Map<String, String> comments, List<String> ignoredSections, BufferedWriter writer, Yaml yaml) throws IOException {
		final String[] keys = key.split("\\.");
		final String actualKey = keys[keys.length - 1];
		final String comment = comments.remove(key);

		final StringBuilder prefixBuilder = new StringBuilder();
		final int indents = keys.length - 1;
		appendPrefixSpaces(prefixBuilder, indents);
		final String prefixSpaces = prefixBuilder.toString();

		// No \n character necessary, new line is automatically at end of comment
		if (comment != null)
			writer.write(comment);

		final Object newObj = newConfig.get(key);
		final Object oldObj = oldConfig.get(key);

		// Write the old section
		if (newObj instanceof Configuration && !forceNew && oldObj instanceof Configuration)
			writeSection(writer, actualKey, prefixSpaces, (Configuration) oldObj);

		// Write the new section, old value is no more
		else if (newObj instanceof Configuration)
			writeSection(writer, actualKey, prefixSpaces, (Configuration) newObj);

		// Write the old object
		else if (oldObj != null && !forceNew)
			write(oldObj, actualKey, prefixSpaces, yaml, writer);

		// Write new object
		else
			write(newObj, actualKey, prefixSpaces, yaml, writer);

	}

	// Doesn't work with configuration sections, must be an actual object
	// Auto checks if it is serializable and writes to file
	private static void write(Object obj, String actualKey, String prefixSpaces, Yaml yaml, BufferedWriter writer) throws IOException {
		if (obj instanceof ConfigSerializable)
			writer.write(prefixSpaces + actualKey + ": " + yaml.dump(((ConfigSerializable) obj).serialize()));

		else if (obj instanceof String || obj instanceof Character) {
			if (obj instanceof String) {
				final String string = (String) obj;

				// Split multi line strings using |-
				if (string.contains("\n")) {
					writer.write(prefixSpaces + actualKey + ": |-\n");

					for (final String line : string.split("\n"))
						writer.write(prefixSpaces + "    " + line + "\n");

					return;
				}
			}

			writer.write(prefixSpaces + actualKey + ": " + yaml.dump(obj));

		} else if (obj instanceof List)
			writeList((List<?>) obj, actualKey, prefixSpaces, yaml, writer);

		else
			writer.write(prefixSpaces + actualKey + ": " + yaml.dump(obj));

	}

	// Writes a configuration section
	private static void writeSection(BufferedWriter writer, String actualKey, String prefixSpaces, Configuration section) throws IOException {
		if (getDeepKeys(section).isEmpty())
			writer.write(prefixSpaces + actualKey + ":");

		else
			writer.write(prefixSpaces + actualKey + ":");

		writer.write("\n");
	}

	// Writes a list of any object
	private static void writeList(List<?> list, String actualKey, String prefixSpaces, Yaml yaml, BufferedWriter writer) throws IOException {
		writer.write(getListAsString(list, actualKey, prefixSpaces, yaml));
	}

	private static String getListAsString(List<?> list, String actualKey, String prefixSpaces, Yaml yaml) {
		final StringBuilder builder = new StringBuilder(prefixSpaces).append(actualKey).append(":");

		if (list.isEmpty()) {
			builder.append(" []\n");
			return builder.toString();
		}

		builder.append("\n");

		for (int i = 0; i < list.size(); i++) {
			final Object o = list.get(i);

			if (o instanceof String || o instanceof Character) {
				builder.append(prefixSpaces).append("- '").append(o.toString().replace("'", "''")).append("'");

			} else if (o instanceof List) {
				builder.append(prefixSpaces).append("- ").append(yaml.dump(o));

			} else {
				builder.append(prefixSpaces).append("- ").append(o);
			}

			if (i != list.size()) {
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	//Key is the config key, value = comment and/or ignored sections
	//Parses comments, blank lines, and ignored sections
	private static Map<String, String> parseComments(List<String> lines, List<String> ignoredSections, Configuration oldConfig, Yaml yaml) {
		final Map<String, String> comments = new HashMap<>();
		final StringBuilder builder = new StringBuilder();
		final StringBuilder keyBuilder = new StringBuilder();
		int lastLineIndentCount = 0;

		//outer:
		for (final String line : lines) {
			if (line != null && line.trim().startsWith("-"))
				continue;

			if (line == null || line.trim().equals("") || line.trim().startsWith("#")) {
				builder.append(line).append("\n");
			} else {
				lastLineIndentCount = setFullKey(keyBuilder, line, lastLineIndentCount);

				if (keyBuilder.length() > 0) {
					comments.put(keyBuilder.toString(), builder.toString());
					builder.setLength(0);
				}
			}
		}

		if (builder.length() > 0) {
			comments.put(null, builder.toString());
		}

		return comments;
	}

	//Counts spaces in front of key and divides by 2 since 1 indent = 2 spaces
	private static int countIndents(String s) {
		int spaces = 0;

		for (final char c : s.toCharArray()) {
			if (c == ' ') {
				spaces += 1;
			} else {
				break;
			}
		}

		return spaces / 2;
	}

	//Ex. keyBuilder = key1.key2.key3 --> key1.key2
	private static void removeLastKey(StringBuilder keyBuilder) {
		String temp = keyBuilder.toString();
		final String[] keys = temp.split("\\.");

		if (keys.length == 1) {
			keyBuilder.setLength(0);
			return;
		}

		temp = temp.substring(0, temp.length() - keys[keys.length - 1].length() - 1);
		keyBuilder.setLength(temp.length());
	}

	//Updates the keyBuilder and returns configLines number of indents
	private static int setFullKey(StringBuilder keyBuilder, String configLine, int lastLineIndentCount) {
		final int currentIndents = countIndents(configLine);
		final String key = configLine.trim().split(":")[0];

		if (keyBuilder.length() == 0) {
			keyBuilder.append(key);
		} else if (currentIndents == lastLineIndentCount) {
			//Replace the last part of the key with current key
			removeLastKey(keyBuilder);

			if (keyBuilder.length() > 0) {
				keyBuilder.append(".");
			}

			keyBuilder.append(key);
		} else if (currentIndents > lastLineIndentCount) {
			//Append current key to the keyBuilder
			keyBuilder.append(".").append(key);
		} else {
			final int difference = lastLineIndentCount - currentIndents;

			for (int i = 0; i < difference + 1; i++) {
				removeLastKey(keyBuilder);
			}

			if (keyBuilder.length() > 0) {
				keyBuilder.append(".");
			}

			keyBuilder.append(key);
		}

		return currentIndents;
	}

	private static String getPrefixSpaces(int indents) {
		final StringBuilder builder = new StringBuilder();

		for (int i = 0; i < indents; i++) {
			builder.append("  ");
		}

		return builder.toString();
	}

	private static void appendPrefixSpaces(StringBuilder builder, int indents) {
		builder.append(getPrefixSpaces(indents));
	}

	private static List<String> getDeepKeys(Configuration configuration) {
		final List<String> keys = new ArrayList<>();
		mapChildrenKeys("", keys, getMap(configuration));

		return keys;
	}

	private static void mapChildrenKeys(String prefix, List<String> deepKeys, Map<String, Object> values) {

		for (final Map.Entry<String, Object> entry : values.entrySet()) {
			final String section = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof Configuration) {
				deepKeys.add((prefix.isEmpty() ? "" : prefix + ".") + section);

				mapChildrenKeys((prefix.isEmpty() ? "" : prefix + ".") + section, deepKeys, getMap(value));

			} else
				deepKeys.add((prefix.isEmpty() ? "" : prefix + ".") + section);
		}
	}

	private static Map<String, Object> getMap(Object configuration) {
		try {
			final Field self = configuration.getClass().getDeclaredField("self");

			self.setAccessible(true);
			return (Map<String, Object>) self.get(configuration);

		} catch (final ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}
}