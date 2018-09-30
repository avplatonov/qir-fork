/**
 *
 */
package bpiwowar.collections;

import bpiwowar.NotImplementedException;
import bpiwowar.collections.WeakComparable.Status;
import bpiwowar.log.Logger;
import bpiwowar.utils.GenericHelper;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A lattice
 *
 * @param <T> The node type (must be comparable)
 * @param <U> The data associated to a node
 * @author bpiwowar
 */
public class Lattice<T extends WeakComparable<T>, U> implements
    Iterable<Lattice.Node<T, U>> {
    final static private Logger logger = Logger.getLogger();

    static public class Node<T extends WeakComparable<T>, U> {
        Set<Node<T, U>> greater = GenericHelper.newHashSet();

        private T key;
        private U data;

        public Node(T key, U data) {
            this.key = key;
            this.data = data;
        }

        static public <T extends WeakComparable<T>, U> Node<T, U> create(T t,
            U u) {
            return new Node<T, U>(t, u);
        }

        public T getKey() {
            return key;
        }

        /**
         * Get the data for this node
         *
         * @return
         */
        public U getValue() {
            return data;
        }

        /**
         * Set the data for this node
         *
         * @param data
         */
        public void setData(U data) {
            this.data = data;
        }

        public void print(PrintStream out, Set<Node<T, U>> explored, int level) {
            for (int i = level - 1; i >= 0; i--)
                out.print('|');

            if (explored.contains(this))
                out.format("- [%s]%n", super.toString());
            else {
                explored.add(this);
                out.format("- [%s] (%s,%s)%n", super.toString(), key, data);
                for (Node<T, U> child : greater)
                    child.print(out, explored, level + 1);
            }
        }
    }

    /**
     * Put a new node in this sub-trie
     *
     * @param node The node to put in this sub-trie
     * @param set
     * @return The replaced node (if any)
     */
    Node<T, U> put(Set<Node<T, U>> explored, Set<Node<T, U>> set,
        Node<T, U> node) {
        Node<T, U> replaced = null;
        Node<T, U> put = null;

        for (Node<T, U> n : set) {
            if (explored.contains(n))
                continue;

            final Status compare = n.key.compare(node.key);
            explored.add(n);

            switch (compare) {
                case GREATER:
                    // n > node
                    node.greater.add(n);
                    set.remove(n);
                    set.add(node);
                    break;

                case LESS:
                    // n < node
                    put = put(explored, n.greater, node);
                    if (replaced == null)
                        replaced = put;
                    else if (replaced != put)
                        throw new RuntimeException();
                    break;

                case EQUAL:
                    // Replace
                    set.remove(n);
                    set.add(node);
                    node.greater = n.greater;

                    // no need to continue since there should be no change for this
                    // branch
                    if (replaced != null)
                        throw new RuntimeException();
                    return n;

                case NOT_COMPARABLE:
                    break;
            }
        }

        // No place for us... let's have one!
        if (put == null) {
            set.add(node);
        }

        return replaced;
    }

    /**
     * Root nodes of this lattice
     */
    private Set<Node<T, U>> roots = GenericHelper.newHashSet();

    /**
     * Add a node
     *
     * @param t
     * @param u
     */
    public Node<T, U> put(T t, U u) {
        final Node<T, U> node = Node.create(t, u);
        final HashSet<Node<T, U>> explored = GenericHelper.newHashSet();
        return put(explored, roots, node);

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Node<T, U>> iterator() {
        throw new NotImplementedException();
    }

    public void print(PrintStream out) {
        if (roots.isEmpty())
            out.println("[empty lattice]");
        else {
            final HashSet<Node<T, U>> explored = GenericHelper.newHashSet();
            for (Node<T, U> n : roots)
                n.print(out, explored, 0);
        }
    }

    /**
     * Generic helper
     *
     * @param <T>
     * @param <U>
     * @return
     */
    final public static <T extends WeakComparable<T>, U> Lattice<T, U> create() {
        return new Lattice<T, U>();
    }

    /**
     * Gives all the nodes which are the greater among all the possible keys
     *
     * @param key
     * @return
     */
    public Iterator<Node<T, U>> getLeastLesser(T key) {
        Map<Node<T, U>, Boolean> explored = GenericHelper.newHashMap();
        Set<Node<T, U>> found = GenericHelper.newHashSet();
        getLeastLesser(key, found, roots, explored);
        return found.iterator();
    }

    private boolean getLeastLesser(T key, Set<Node<T, U>> found,
        Set<Node<T, U>> todo, Map<Node<T, U>, Boolean> explored) {
        boolean hasFound = false;

        logger.debug("Searching for keys less than %s", key);
        for (Node<T, U> node : todo) {
            Boolean b = explored.get(node);
            if (b != null)
                return b;

            Status compare = node.key.compare(key);

            switch (compare) {
                case GREATER:
                case NOT_COMPARABLE:
                    // Nothing there
                    explored.put(node, false);
                    break;

                case EQUAL:
                    found.add(node);
                    explored.put(node, true);
                    // no need to explore more
                    return true;

                case LESS:
                    // explore children
                    boolean foundInGreaters = getLeastLesser(key, found, node.greater,
                        explored);
                    explored.put(node, !foundInGreaters);
                    if (!foundInGreaters) {
                        hasFound = true;
                        found.add(node);
                    }
            }
        }

        return hasFound;
    }
}
