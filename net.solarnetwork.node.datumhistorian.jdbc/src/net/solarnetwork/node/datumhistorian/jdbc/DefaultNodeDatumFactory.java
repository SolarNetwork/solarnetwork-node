/* ==================================================================
 * NodeDatumFactory.java - 10 Nov 2026 9:25:47 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.datumhistorian.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.domain.datum.SimpleDayDatum;
import net.solarnetwork.node.domain.datum.SimpleDcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleEnergyStorageDatum;
import net.solarnetwork.node.domain.datum.SimplePriceDatum;

/**
 * Create {@link NodeDatum} instances from {@link StoredDatum} instances.
 *
 * <p>
 * The stored datum class is created when it can be loaded and has one of these
 * public constructors:
 * </p>
 *
 * <ol>
 * <li>{@code (DatumId, DatumSamples)}</li>
 * <li>{@code (Long locationId, String sourceId, Instant timestamp, DatumSamples)},
 * for location datum</li>
 * <li>{@code (String sourceId, Instant timestamp, DatumSamples)}</li>
 * </ol>
 *
 * <p>
 * The class is loaded first with the class loader given to this factory, and
 * then with the class loader of the type requested by the caller. When the
 * class cannot be created, the {@code Simple*} class from the
 * {@code net.solarnetwork.node.domain.datum} package that shares the most types
 * with the stored class is created instead, for example
 * {@link SimpleAcEnergyDatum} for a class that implements
 * {@code AcEnergyDatum}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DefaultNodeDatumFactory implements NodeDatumFactory {

	/** The classes to create when a stored class cannot be created. */
	private static final List<Class<? extends NodeDatum>> FALLBACK_CLASSES = List.of(
			SimpleAcDcEnergyDatum.class, SimpleAcEnergyDatum.class, SimpleDcEnergyDatum.class,
			SimpleEnergyStorageDatum.class, SimpleEnergyDatum.class, SimpleAtmosphericDatum.class,
			SimpleDayDatum.class, SimplePriceDatum.class, SimpleDatum.class);

	private static final ClassValue<Set<String>> TYPE_NAMES = new ClassValue<>() {

		@Override
		protected Set<String> computeValue(Class<?> type) {
			final Set<String> names = new LinkedHashSet<>(16);
			final Deque<Class<?>> interfaces = new ArrayDeque<>(8);
			for ( Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass() ) {
				names.add(c.getName());
				interfaces.addAll(Arrays.asList(c.getInterfaces()));
			}
			while ( !interfaces.isEmpty() ) {
				Class<?> c = interfaces.removeFirst();
				if ( names.add(c.getName()) ) {
					interfaces.addAll(Arrays.asList(c.getInterfaces()));
				}
			}
			return Collections.unmodifiableSet(names);
		}

	};

	private static final Logger log = LoggerFactory.getLogger(DefaultNodeDatumFactory.class);

	private final ClassLoader classLoader;
	private final List<Instantiator> fallbacks;
	private final ConcurrentMap<String, Instantiator> instantiators = new ConcurrentHashMap<>(16, 0.9f,
			2);

	/**
	 * Constructor.
	 *
	 * <p>
	 * The class loader of this class will be used.
	 * </p>
	 */
	public DefaultNodeDatumFactory() {
		this(DefaultNodeDatumFactory.class.getClassLoader());
	}

	/**
	 * Constructor.
	 *
	 * @param classLoader
	 *        the class loader to load stored datum classes with
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DefaultNodeDatumFactory(ClassLoader classLoader) {
		super();
		this.classLoader = requireNonNullArgument(classLoader, "classLoader");
		this.fallbacks = FALLBACK_CLASSES.stream().map(DefaultNodeDatumFactory::instantiator).toList();
	}

	@Override
	public Set<String> typeNames(Class<?> type) {
		return TYPE_NAMES.get(type);
	}

	/**
	 * Get the names of all classes and interfaces a class can be assigned to.
	 * 
	 * @param type
	 *        the class to get the type names for
	 * @return the type names, never empty
	 * @see NodeDatumFactory#typeNames(Class)
	 */
	public static Set<String> typeNamesForType(Class<?> type) {
		return TYPE_NAMES.get(type);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends NodeDatum> @Nullable T createDatum(StoredDatum datum, @Nullable Class<T> type) {
		Instantiator inst = instantiators.computeIfAbsent(datum.className(),
				n -> resolve(n, datum.typeNames()));
		if ( type != null && !type.isAssignableFrom(inst.datumClass()) ) {
			// try the class loader of the requested type, which might see the stored class
			final ClassLoader typeClassLoader = type.getClassLoader();
			final Instantiator alt = (typeClassLoader != null && typeClassLoader != classLoader
					? instantiator(datum.className(), typeClassLoader)
					: null);
			if ( alt == null || !type.isAssignableFrom(alt.datumClass()) ) {
				log.debug("Cannot create stored datum class [{}] as type [{}]", datum.className(),
						type.getName());
				return null;
			}
			instantiators.put(datum.className(), alt);
			inst = alt;
		}
		final NodeDatum result = inst.create(datum);
		return (result != null && (type == null || type.isInstance(result)) ? (T) result : null);
	}

	private Instantiator resolve(String className, Set<String> typeNames) {
		Instantiator result = instantiator(className, classLoader);
		if ( result == null ) {
			result = fallback(typeNames);
			log.debug("Stored datum class [{}] not available; will create as [{}]", className,
					result.datumClass().getName());
		}
		return result;
	}

	private Instantiator fallback(Set<String> typeNames) {
		Instantiator best = fallbacks.get(fallbacks.size() - 1);
		int bestShared = -1;
		int bestExtra = Integer.MAX_VALUE;
		for ( Instantiator inst : fallbacks ) {
			int shared = 0;
			int extra = 0;
			for ( String name : typeNames(inst.datumClass()) ) {
				if ( typeNames.contains(name) ) {
					shared++;
				} else {
					extra++;
				}
			}
			if ( shared > bestShared || (shared == bestShared && extra < bestExtra) ) {
				best = inst;
				bestShared = shared;
				bestExtra = extra;
			}
		}
		return best;
	}

	private static @Nullable Instantiator instantiator(String className, ClassLoader loader) {
		final Class<?> clazz;
		try {
			clazz = Class.forName(className, false, loader);
		} catch ( ClassNotFoundException | LinkageError e ) {
			return null;
		}
		if ( !NodeDatum.class.isAssignableFrom(clazz) ) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Instantiator result = instantiator((Class<? extends NodeDatum>) clazz);
		return (result.hasConstructor() ? result : null);
	}

	private static Instantiator instantiator(Class<? extends NodeDatum> clazz) {
		return new Instantiator(clazz, constructor(clazz, DatumId.class, DatumSamples.class),
				constructor(clazz, Long.class, String.class, Instant.class, DatumSamples.class),
				constructor(clazz, String.class, Instant.class, DatumSamples.class));
	}

	private static <T> @Nullable Constructor<T> constructor(Class<T> clazz, Class<?>... types) {
		try {
			return clazz.getConstructor(types);
		} catch ( NoSuchMethodException | SecurityException e ) {
			return null;
		}
	}

	/**
	 * Create datum instances of a specific class.
	 *
	 * @param datumClass
	 *        the class to create
	 * @param idConstructor
	 *        the {@code (DatumId, DatumSamples)} constructor
	 * @param locationConstructor
	 *        the {@code (Long, String, Instant, DatumSamples)} constructor
	 * @param nodeConstructor
	 *        the {@code (String, Instant, DatumSamples)} constructor
	 */
	private static record Instantiator(Class<? extends NodeDatum> datumClass,
			@Nullable Constructor<? extends NodeDatum> idConstructor,
			@Nullable Constructor<? extends NodeDatum> locationConstructor,
			@Nullable Constructor<? extends NodeDatum> nodeConstructor) {

		private boolean hasConstructor() {
			return (idConstructor != null || locationConstructor != null || nodeConstructor != null);
		}

		private @Nullable NodeDatum create(StoredDatum datum) {
			try {
				if ( idConstructor != null ) {
					return idConstructor.newInstance(datum.datumId(), datum.samples());
				}
				if ( locationConstructor != null
						&& (datum.locationId() != null || nodeConstructor == null) ) {
					return locationConstructor.newInstance(datum.locationId(), datum.sourceId(),
							datum.timestamp(), datum.samples());
				}
				if ( nodeConstructor != null ) {
					return nodeConstructor.newInstance(datum.sourceId(), datum.timestamp(),
							datum.samples());
				}
			} catch ( InstantiationException | IllegalAccessException | InvocationTargetException
					| IllegalArgumentException e ) {
				log.warn("Error creating stored datum {} as [{}]: {}", datum.datumId(),
						datumClass.getName(), e.toString());
			}
			return null;
		}

	}

}
