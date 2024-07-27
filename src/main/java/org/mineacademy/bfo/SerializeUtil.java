package org.mineacademy.bfo;

import java.awt.Color;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.collection.StrictCollection;
import org.mineacademy.bfo.collection.StrictMap;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.BoxedMessage;
import org.mineacademy.bfo.model.ConfigSerializable;
import org.mineacademy.bfo.model.IsInList;
import org.mineacademy.bfo.model.RangedSimpleTime;
import org.mineacademy.bfo.model.RangedValue;
import org.mineacademy.bfo.model.SimpleTime;
import org.mineacademy.bfo.remain.CompChatColor;
import org.mineacademy.bfo.remain.Remain;
import org.mineacademy.bfo.settings.ConfigSection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;

/**
 * Utility class for serializing objects to writeable YAML data and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SerializeUtil {

	/**
	 * A list of custom serializers
	 */
	private static final Map<Class<Object>, Function<Object, String>> serializers = new HashMap<>();

	/**
	 * Add a custom serializer to the list
	 *
	 * @param <T>
	 * @param fromClass
	 * @param serializer
	 */
	public static <T> void addSerializer(Class<T> fromClass, Function<T, String> serializer) {
		serializers.put((Class<Object>) fromClass, (Function<Object, String>) serializer);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting objects into strings so you can save them in your files
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the given object into something you can safely save in file as a string
	 *
	 * @param object
	 * @return
	 */
	public static Object serialize(Object object) {
		if (object == null)
			return null;

		object = Remain.getRootOfSectionPathData(object);

		if (serializers.containsKey(object.getClass()))
			return serializers.get(object.getClass()).apply(object);

		if (object instanceof ConfigSerializable)
			return serialize(((ConfigSerializable) object).serialize().serialize());

		else if (object instanceof StrictCollection)
			return serialize(((StrictCollection) object).serialize());

		else if (object instanceof ChatColor)
			return ((ChatColor) object).name();

		else if (object instanceof ServerInfo)
			return ((ServerInfo) object).getName();

		else if (object instanceof net.md_5.bungee.api.ChatColor) {
			final net.md_5.bungee.api.ChatColor color = (net.md_5.bungee.api.ChatColor) object;

			return color.toString();
		}

		else if (object instanceof BoxedMessage) {
			final String message = ((BoxedMessage) object).getMessage();

			return message == null || "".equals(message) || "null".equals(message) ? null : message;

		} else if (object instanceof UUID)
			return object.toString();

		else if (object instanceof Enum<?>)
			return object.toString();

		else if (object instanceof CommandSender)
			return ((CommandSender) object).getName();

		else if (object instanceof SimpleTime)
			return ((SimpleTime) object).getRaw();

		else if (object instanceof Color)
			return "#" + ((Color) object).getRGB();

		else if (object instanceof RangedValue)
			return ((RangedValue) object).toLine();

		else if (object instanceof RangedSimpleTime)
			return ((RangedSimpleTime) object).toLine();

		else if (object instanceof Component)
			return Remain.convertAdventureToJson((Component) object);

		else if (object instanceof BaseComponent)
			return Remain.convertBaseComponentToJson((BaseComponent) object);

		else if (object instanceof BaseComponent[])
			return Remain.convertBaseComponentToJson((BaseComponent[]) object);

		else if (object instanceof HoverEvent) {
			final HoverEvent event = (HoverEvent) object;

			return SerializedMap.ofArray("Action", event.getAction(), "Value", event.getValue()).serialize();
		}

		else if (object instanceof ClickEvent) {
			final ClickEvent event = (ClickEvent) object;

			return SerializedMap.ofArray("Action", event.getAction(), "Value", event.getValue()).serialize();
		}

		else if (object instanceof Path)
			throw new FoException("Cannot serialize Path " + object + ", did you mean to convert it into a name?");

		else if (object instanceof Iterable || object.getClass().isArray() || object instanceof IsInList) {
			final List<Object> serialized = new ArrayList<>();

			if (object instanceof Iterable || object instanceof IsInList)
				for (final Object element : object instanceof IsInList ? ((IsInList<?>) object).getList() : (Iterable<?>) object)
					serialized.add(serialize(element));

			else
				for (final Object element : (Object[]) object)
					serialized.add(serialize(element));

			return serialized;

		} else if (object instanceof StrictMap) {
			final StrictMap<Object, Object> oldMap = (StrictMap<Object, Object>) object;
			final StrictMap<Object, Object> newMap = new StrictMap<>();

			for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;

		} else if (object instanceof Map) {
			final Map<Object, Object> oldMap = (Map<Object, Object>) object;
			final Map<Object, Object> newMap = new LinkedHashMap<>();

			for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;
		}

		else if (object instanceof Configuration)
			return serialize(Common.getMapFromSection(object));

		else if (object instanceof ConfigSection)
			return serialize(((ConfigSection) object).getValues(true));

		else if (object instanceof Pattern)
			return ((Pattern) object).pattern();

		else if (object instanceof Integer || object instanceof Double || object instanceof Float || object instanceof Long || object instanceof Short
				|| object instanceof String || object instanceof Boolean || object instanceof Character)
			return object;

		throw new SerializeFailedException("Does not know how to serialize " + object.getClass().getSimpleName() + "! Does it extends ConfigSerializable? Data: " + object);
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
	 * @param parameters
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static <T> T deserialize(@NonNull final Class<T> classOf, @NonNull Object object, final Object... parameters) {
		if (classOf == String.class)
			object = object.toString();

		else if (classOf == Integer.class)
			object = Integer.parseInt(object.toString());

		else if (classOf == Long.class)
			object = Long.decode(object.toString());

		else if (classOf == Double.class)
			object = Double.parseDouble(object.toString());

		else if (classOf == Float.class)
			object = Float.parseFloat(object.toString());

		else if (classOf == Boolean.class)
			object = Boolean.parseBoolean(object.toString());

		else if (classOf == SerializedMap.class)
			object = SerializedMap.of(object);

		else if (classOf == BoxedMessage.class)
			object = new BoxedMessage(object.toString());

		else if (classOf == SimpleTime.class)
			object = SimpleTime.from(object.toString());

		else if (classOf == RangedValue.class)
			object = RangedValue.parse(object.toString());

		else if (classOf == RangedSimpleTime.class)
			object = RangedSimpleTime.parse(object.toString());

		else if (classOf == ChatColor.class)
			object = ChatColor.of(object.toString());

		else if (classOf == UUID.class)
			object = UUID.fromString(object.toString());

		else if (classOf == Component.class)
			object = Remain.convertJsonToAdventure(object.toString());

		else if (classOf == BaseComponent.class) {
			object = new TextComponent(Remain.convertJsonToBaseComponent(object.toString()));

		} else if (classOf == BaseComponent[].class)
			object = Remain.convertJsonToBaseComponent(object.toString());

		else if (classOf == HoverEvent.class) {
			final SerializedMap serialized = SerializedMap.of(object);
			final HoverEvent.Action action = serialized.get("Action", HoverEvent.Action.class);
			final BaseComponent[] value = serialized.get("Value", BaseComponent[].class);

			object = new HoverEvent(action, value);
		}

		else if (classOf == ClickEvent.class) {
			final SerializedMap serialized = SerializedMap.of(object);

			final ClickEvent.Action action = serialized.get("Action", ClickEvent.Action.class);
			final String value = serialized.getString("Value");

			object = new ClickEvent(action, value);
		}

		else if (Enum.class.isAssignableFrom(classOf)) {
			object = ReflectionUtil.lookupEnum((Class<Enum>) classOf, object.toString());

			if (object == null)
				return null;
		}

		else if (Color.class.isAssignableFrom(classOf))
			object = CompChatColor.of(object.toString()).getColor();

		else if (List.class.isAssignableFrom(classOf) && object instanceof List) {
			// Good

		} else if (Map.class.isAssignableFrom(classOf)) {
			if (object instanceof Map)
				return (T) object;

			if (object instanceof Configuration)
				return (T) Common.getMapFromSection(object);

			if (object instanceof ConfigSection)
				return (T) ((ConfigSection) object).getValues(false);

			throw new SerializeFailedException("Does not know how to turn " + object.getClass().getSimpleName() + " into a Map! (Keep in mind we can only serialize into Map<Object/String, Object> Data: " + object);

		} else if (classOf.isArray()) {
			final Class<?> arrayType = classOf.getComponentType();
			T[] array;

			if (object instanceof List) {
				final List<?> rawList = (List<?>) object;
				array = (T[]) Array.newInstance(classOf.getComponentType(), rawList.size());

				for (int i = 0; i < rawList.size(); i++) {
					final Object element = rawList.get(i);

					array[i] = element == null ? null : (T) deserialize(arrayType, element, (Object[]) null);
				}
			}

			else {
				final Object[] rawArray = (Object[]) object;
				array = (T[]) Array.newInstance(classOf.getComponentType(), rawArray.length);

				for (int i = 0; i < array.length; i++)
					array[i] = rawArray[i] == null ? null : (T) deserialize(classOf.getComponentType(), rawArray[i], (Object[]) null);
			}

			return (T) array;

		}

		// Try to call our own serializers
		else if (ConfigSerializable.class.isAssignableFrom(classOf)) {
			if (parameters != null && parameters.length > 0) {
				final List<Class<?>> argumentClasses = new ArrayList<>();
				final List<Object> arguments = new ArrayList<>();

				// Build parameters
				argumentClasses.add(SerializedMap.class);
				for (final Object param : parameters)
					argumentClasses.add(param.getClass());

				// Build parameter instances
				arguments.add(SerializedMap.of(object));
				Collections.addAll(arguments, parameters);

				// Find deserialize(SerializedMap, args[]) method
				final Method deserialize = ReflectionUtil.getMethod(classOf, "deserialize", argumentClasses.toArray(new Class[argumentClasses.size()]));

				Valid.checkNotNull(deserialize,
						"Expected " + classOf.getSimpleName() + " to have a public static deserialize(SerializedMap, " + Common.join(argumentClasses) + ") method to deserialize: " + object + " when params were given: " + Common.join(parameters));

				Valid.checkBoolean(argumentClasses.size() == arguments.size(),
						classOf.getSimpleName() + "#deserialize(SerializedMap, " + argumentClasses.size() + " args) expected, " + arguments.size() + " given to deserialize: " + object);

				return ReflectionUtil.invokeStatic(deserialize, arguments.toArray());
			}

			final Method deserialize = ReflectionUtil.getMethod(classOf, "deserialize", SerializedMap.class);

			if (deserialize != null)
				return ReflectionUtil.invokeStatic(deserialize, SerializedMap.of(object));

			throw new SerializeFailedException("Unable to deserialize " + classOf.getSimpleName()
					+ ", please write 'public static deserialize(SerializedMap map) or deserialize(SerializedMap map, X arg1, Y arg2, etc.) method to deserialize: " + object);
		}

		// Step 3 - Search for "getByName" method used by us or some Bukkit classes such as Enchantment
		else if (object instanceof String) {
			final Method method = ReflectionUtil.getMethod(classOf, "getByName", String.class);

			if (method != null)
				return ReflectionUtil.invokeStatic(method, object);
		}

		else if (classOf == Object.class) {
			// Good
		}

		else
			throw new SerializeFailedException("Does not know how to turn " + classOf + " into a serialized object from data: " + object);

		return (T) object;
	}

	/**
	 * Thrown when cannot serialize an object because it failed to determine its type
	 */
	public static class SerializeFailedException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public SerializeFailedException(String reason) {
			super(reason);
		}
	}
}
