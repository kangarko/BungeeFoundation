package org.mineacademy.bfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.collection.StrictCollection;
import org.mineacademy.bfo.collection.StrictMap;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.ConfigSerializable;
import org.mineacademy.bfo.model.IsInList;
import org.mineacademy.bfo.model.SimpleTime;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.config.Configuration;

/**
 * Utility class for serializing objects to writeable YAML data and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SerializeUtil {

	/**
	 * When serializing unknown objects throw an error if strict mode is enabled
	 */
	public static boolean STRICT_MODE = true;

	// ------------------------------------------------------------------------------------------------------------
	// Converting objects into strings so you can save them in your files
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the given object into something you can safely save in file as a string
	 *
	 * @param obj
	 * @return
	 */
	public static Object serialize(final Object obj) {
		if (obj == null)
			return null;

		if (obj instanceof ConfigSerializable)
			return serialize(((ConfigSerializable) obj).serialize().serialize());

		else if (obj instanceof StrictCollection)
			return serialize(((StrictCollection) obj).serialize());

		else if (obj instanceof ChatColor)
			return ((ChatColor) obj).name();

		else if (obj instanceof UUID)
			return obj.toString();

		else if (obj instanceof Enum<?>)
			return obj.toString();

		else if (obj instanceof CommandSender)
			return ((CommandSender) obj).getName();

		else if (obj instanceof SimpleTime)
			return ((SimpleTime) obj).getRaw();

		else if (obj instanceof Iterable || obj.getClass().isArray() || obj instanceof IsInList) {
			final List<Object> serialized = new ArrayList<>();

			if (obj instanceof Iterable || obj instanceof IsInList)
				for (final Object element : obj instanceof IsInList ? ((IsInList<?>) obj).getList() : (Iterable<?>) obj)
					serialized.add(serialize(element));
			else
				for (final Object element : (Object[]) obj)
					serialized.add(serialize(element));

			return serialized;
		} else if (obj instanceof StrictMap) {
			final StrictMap<Object, Object> oldMap = (StrictMap<Object, Object>) obj;
			final StrictMap<Object, Object> newMap = new StrictMap<>();

			for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;
		} else if (obj instanceof Map) {
			final Map<Object, Object> oldMap = (Map<Object, Object>) obj;
			final Map<Object, Object> newMap = new HashMap<>();

			for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;

		}

		else if (obj instanceof Integer || obj instanceof Double || obj instanceof Float || obj instanceof Long
				|| obj instanceof String || obj instanceof Boolean || obj instanceof Map
				|| obj instanceof Configuration)
			return obj;

		if (STRICT_MODE)
			throw new FoException("Does not know how to serialize " + obj.getClass().getSimpleName() + "! Does it extends ConfigSerializable? Data: " + obj);

		else
			return Objects.toString(obj);
	}

	/**
	 * Runs through each item in the list and serializes it
	 * <p>
	 * Returns a new list of serialized items
	 *
	 * @param array
	 * @return
	 */
	public static List<Object> serializeList(final Iterable<?> array) {
		final List<Object> list = new ArrayList<>();

		for (final Object t : array)
			list.add(serialize(t));

		return list;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting stored strings from your files back into classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Attempts to convert the given object into a class
	 * <p>
	 * Example: Call deserialize(Location.class, "worldName 5 -1 47") to convert that into a Bukkit location object
	 *
	 * @param <T>
	 * @param classOf
	 * @param object
	 * @return
	 */
	public static <T> T deserialize(@NonNull final Class<T> classOf, @NonNull final Object object) {
		return deserialize(classOf, object, (Object[]) null);
	}

	/**
	 * Please see {@link #deserialize(Class, Object)}, plus that this method
	 * allows you to parse through more arguments to the static deserialize method
	 *
	 * @param <T>
	 * @param classOf
	 * @param object
	 * @param deserializeParameters use more variables in the deserialize method
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> T deserialize(@NonNull final Class<T> classOf, @NonNull Object object, final Object... deserializeParameters) {
		final SerializedMap map = SerializedMap.of(object);

		// Step 1 - Search for basic deserialize(SerializedMap) method
		Method deserializeMethod = ReflectionUtil.getMethod(classOf, "deserialize", SerializedMap.class);

		if (deserializeMethod != null)
			return ReflectionUtil.invokeStatic(deserializeMethod, map);

		// Step 2 - Search for our deserialize(Params[], SerializedMap) method
		if (deserializeParameters != null) {
			final List<Class<?>> joinedClasses = new ArrayList<>();

			{ // Build parameters
				joinedClasses.add(SerializedMap.class);

				for (final Object param : deserializeParameters)
					joinedClasses.add(param.getClass());
			}

			deserializeMethod = ReflectionUtil.getMethod(classOf, "deserialize", joinedClasses.toArray(new Class[joinedClasses.size()]));

			final List<Object> joinedParams = new ArrayList<>();

			{ // Build parameter instances
				joinedParams.add(map);

				Collections.addAll(joinedParams, deserializeParameters);
			}

			if (deserializeMethod != null) {
				Valid.checkBoolean(joinedClasses.size() == joinedParams.size(), "static deserialize method arguments length " + joinedClasses.size() + " != given params " + joinedParams.size());

				return ReflectionUtil.invokeStatic(deserializeMethod, joinedParams.toArray());
			}
		}

		// Step 3 - Search for "getByName" method used by us or some Bukkit classes such as Enchantment
		if (deserializeMethod == null && object instanceof String) {
			deserializeMethod = ReflectionUtil.getMethod(classOf, "getByName", String.class);

			if (deserializeMethod != null)
				return ReflectionUtil.invokeStatic(deserializeMethod, object);
		}

		// Step 4 - If there is no deserialize method, just deserialize the given object
		if (object != null)
			if (classOf == String.class)
				object = object.toString();

			else if (classOf == Integer.class)
				object = Double.valueOf(object.toString()).intValue();

			else if (classOf == Long.class)
				object = Double.valueOf(object.toString()).longValue();

			else if (classOf == Double.class)
				object = Double.valueOf(object.toString());

			else if (classOf == Float.class)
				object = Float.valueOf(object.toString());

			else if (classOf == Boolean.class)
				object = Boolean.valueOf(object.toString());

			else if (classOf == SerializedMap.class)
				object = SerializedMap.of(object);

			else if (classOf == SimpleTime.class)
				object = SimpleTime.from(object.toString());

			else if (classOf == UUID.class)
				object = UUID.fromString(object.toString());

			else if (Enum.class.isAssignableFrom(classOf))
				object = ReflectionUtil.lookupEnum((Class<Enum>) classOf, object.toString());

			else if (List.class.isAssignableFrom(classOf) && object instanceof List) {
				// Good

			} else if (Map.class.isAssignableFrom(classOf) && object instanceof Map) {
				// Good

			} else if (classOf == Object.class) {
				// pass through

			} else
				throw new FoException("Unable to deserialize " + classOf.getSimpleName() + ", lacking static deserialize method! Data: " + object);

		return (T) object;

	}

	/**
	 * Deserializes a list containing maps
	 *
	 * @param <T>
	 * @param listOfObjects
	 * @param asWhat
	 * @return
	 */
	public static <T extends ConfigSerializable> List<T> deserializeMapList(final Object listOfObjects, final Class<T> asWhat) {
		if (listOfObjects == null)
			return null;

		Valid.checkBoolean(listOfObjects instanceof ArrayList, "Only deserialize a list of maps, nie " + listOfObjects.getClass());
		final List<T> loaded = new ArrayList<>();

		for (final Object part : (ArrayList<?>) listOfObjects) {
			final T deserialized = deserializeMap(part, asWhat);

			if (deserialized != null)
				loaded.add(deserialized);
		}

		return loaded;
	}

	/**
	 * Deserializes a map
	 *
	 * @param <T>
	 * @param rawMap
	 * @param asWhat
	 * @return
	 */
	public static <T extends ConfigSerializable> T deserializeMap(final Object rawMap, final Class<T> asWhat) {
		if (rawMap == null)
			return null;

		Valid.checkBoolean(rawMap instanceof Map, "The object to deserialize must be map, but got: " + rawMap.getClass());

		final Map<String, Object> map = (Map<String, Object>) rawMap;
		final Method deserialize;

		try {
			deserialize = asWhat.getMethod("deserialize", SerializedMap.class);
			Valid.checkBoolean(Modifier.isPublic(deserialize.getModifiers()) && Modifier.isStatic(deserialize.getModifiers()), asWhat + " is missing public 'public static T deserialize()' method");

		} catch (final NoSuchMethodException ex) {
			Common.throwError(ex, "Class lacks a final method deserialize(SerializedMap) metoda. Tried: " + asWhat.getSimpleName());
			return null;
		}

		final Object invoked;

		try {
			invoked = deserialize.invoke(null, SerializedMap.of(map));
		} catch (final ReflectiveOperationException e) {
			Common.throwError(e, "Error calling " + deserialize.getName() + " as " + asWhat.getSimpleName() + " with data " + map);
			return null;
		}

		Valid.checkBoolean(invoked.getClass().isAssignableFrom(asWhat), invoked.getClass().getSimpleName() + " != " + asWhat.getSimpleName());
		return (T) invoked;
	}
}
