/**
 *
 */
package bpiwowar.collections;

import bpiwowar.collections.WeakComparable.Status;
import bpiwowar.maths.matrix.Multiply.TrieKey;
import bpiwowar.utils.iterators.AbstractIterator;
import java.io.PrintStream;
import java.util.Iterator;

/**
 * A trie
 *
 * @param <T> The node type (must be comparable)
 * @param <U> The data associated to a node
 * @author bpiwowar
 */
public class Trie<T extends WeakComparable<T>, U> implements
    Iterable<Trie.Node<T, U>> {

    static public class Node<T extends WeakComparable<T>, U> {
        Node<T, U> parent;
        Node<T, U> firstChild;
        Node<T, U> next;
        Node<T, U> previous;

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
        public U getData() {
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

        public Node<T, U> put(T t, U u) {
            return put(new Node<T, U>(t, u));
        }

        /**
         * Remove node
         */
        void remove() {
            if (parent != null && parent.firstChild == this)
                parent.firstChild = next;

            if (next != null)
                next.previous = previous;

            if (previous != null)
                previous.next = next;

            next = previous = parent = firstChild = null;
        }

        /**
         * Replace a node by another
         *
         * @param node
         */
        void replaceBy(Node<T, U> node) {
            // copy
            node.parent = parent;
            node.next = next;
            node.previous = previous;
            node.firstChild = firstChild;

            // Change the pointers for next, previous, first child and parent
            if (next != null)
                next.previous = node;

            if (previous != null)
                previous.next = node;

            if (firstChild != null)
                firstChild.parent = node;

            if (parent.firstChild == this)
                parent.firstChild = node;

            // Set everything null
            next = previous = parent = firstChild = null;
        }

        /**
         * Put a new node in this sub-trie
         *
         * @param node The node to put in this sub-trie
         * @return The replaced node (if any)
         */
        protected Node<T, U> put(Node<T, U> node) {
            final Status compare = key.compare(node.key);

            switch (compare) {
                case GREATER:
                    if (firstChild != null)
                        return firstChild.put(node);
                    firstChild = node;
                    node.parent = this;
                    return null;

                case LESS:
                    // Insert has a parent
                    if (parent != null) {
                        if (parent.firstChild == this) {
                            parent.firstChild = node;
                        }
                        else {

                        }
                        node.parent = parent;
                    }

                    // Replace ourselves by the node
                    replaceBy(node);

                    // And set us as the first child of the node
                    parent = node;
                    node.firstChild = this;

                    // Add all next elements which are inferior like us
                    Node<T, U> last = this;
                    for (Node<T, U> s = next; s != null; ) {
                        // Get the next before hand
                        Node<T, U> n = s;
                        s = s.next;

                        // Process
                        final Status c = n.key.compare(node.key);
                        switch (c) {
                            case LESS:
                                n.remove();
                                n.parent = node;
                                last.next = n;
                                n.previous = last;
                                last = n;
                                break;
                            case NOT_COMPARABLE:
                                // leave it
                                break;
                            default:
                                throw new RuntimeException(
                                    String
                                        .format(
                                            "Should not happen if there is a real order (%s)",
                                            c));
                        }
                    }

                    return null;

                case EQUAL:
                    replaceBy(node);
                    return this;

                case NOT_COMPARABLE:
                    // If we are not the last, just try the next node
                    if (next != null)
                        return next.put(node);

                    // We are the last node: insert after us
                    node.parent = this;
                    node.previous = this;
                    next = node;

                    return null;
            }

            throw new RuntimeException("We shouldn't have reached this line...");
        }

        public void print(PrintStream out, int level) {
            for (int i = level - 1; i >= 0; i--)
                out.print('|');
            out.format("- (%s,%s)", key, data);
            for (Node<T, U> child = firstChild; child != null; child = child.next)
                child.print(out, level + 1);
        }
    }

    /**
     * Root node of this trie
     */
    private Node<T, U> root;

    /**
     * Add a node
     *
     * @param t
     * @param u
     */
    public Node<T, U> put(T t, U u) {
        final Node<T, U> node = Node.create(t, u);

        if (root == null) {
            root = node;
            return null;
        }

        // In case the root has been replaced
        Node<T, U> old = root.put(node);
        if (old == root)
            root = node;
        return old;

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Node<T, U>> iterator() {
        if (root == null)
            return new EmptyIterator<Node<T, U>>();

        return new Iterator<Node<T, U>>() {
            Node<T, U> current = root;

            public void remove() {
                throw new RuntimeException("Cannot modify");
            }

            public Node<T, U> next() {
                // Copy the current
                Node<T, U> old = current;

                // Next one is either the first child, or the closest next
                // sibling of an ancestor
                if (current.firstChild != null)
                    current = current.firstChild;
                else {
                    while (current != null && current.next != null)
                        current = current.parent;
                    if (current != null)
                        current = current.next;
                }

                // return old
                return old;
            }

            public boolean hasNext() {
                return current != null;
            }
        };
    }

    void print(PrintStream out) {
        if (root == null)
            out.println("[empty tree]");
        else
            root.print(out, 0);
    }

    /**
     * Generic helper
     *
     * @param <T>
     * @param <U>
     * @return
     */
    final public static <T extends WeakComparable<T>, U> Trie<T, U> create() {
        return new Trie<T, U>();
    }

    /**
     * Gives all the nodes which are greater or equal to the given key
     *
     * @param key
     * @return
     */
    public Iterator<Node<T, U>> subtreeIterator(TrieKey key) {
        return new AbstractIterator<Node<T, U>>() {
            Node current = null;

            @Override
            protected boolean storeNext() {
                if (current == null) {
                    current = root;
                }

                // The current node is greater or equal

                return false;
            }
        };

    }
}
