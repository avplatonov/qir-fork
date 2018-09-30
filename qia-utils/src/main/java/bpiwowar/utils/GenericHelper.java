package bpiwowar.utils;

import bpiwowar.log.Logger;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Helper methods for generics
 *
 * @author bpiwowar
 */
public class GenericHelper {
    final static private Logger logger = Logger.getLogger();

    final public static <T> ArrayList<T> newArrayList() {
        return new ArrayList<T>();
    }

    final public static <K, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<K, V>();
    }

    final public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<K, V>();
    }

    final public static <T> HashSet<T> newHashSet() {
        return new HashSet<T>();
    }

    /**
     * Reflection helper - give the generic parameters of a class.
     *
     * @param candidate The class implementing the superclass or superinterface
     * @param implemented The interface or superclass that is implemented
     * @return a type array of the type arguments of a superclass or a superinterface
     */
    public static Type[] getActualTypeArguments(Class<?> candidate,
        Class<?> implemented) {
        // We are not a derived class ?
        if (!implemented.isAssignableFrom(candidate)) {
            return null;
        }

        // Get the list of candidates
        final boolean isInterface = implemented.isInterface();
        Class<?> current = candidate;

        do {
            final Type[] list = isInterface ? current.getGenericInterfaces()
                : new Type[] {current.getGenericSuperclass()};
            for (Type anInterface : list) {
                if (anInterface instanceof ParameterizedType) {
                    ParameterizedType ptype = (ParameterizedType)anInterface;
                    if (ptype.getRawType() == implemented) {
                        return ptype.getActualTypeArguments();
                    }
                }
            }

            current = current.getSuperclass();
        }
        while (!isInterface && current != null);

        // Should not happen!
        throw new RuntimeException(
            String.format(
                "Should not happen: we did not find the generic type of %s in %s",
                implemented, current));
    }

    public static <T> TreeSet<T> newTreeSet() {
        return new TreeSet<T>();
    }

    public static <T> ArrayList<T> newArrayList(int length) {
        return new ArrayList<T>(length);
    }

    @SuppressWarnings("unchecked") final public static <T, U extends T> U[] newArray(Class<T> theClass,
        int length) {
        return (U[])Array.newInstance(theClass, length);
    }

}
