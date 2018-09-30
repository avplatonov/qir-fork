package bpiwowar.ml.bn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A list of bayesian networks
 */
public class NetworkVector extends NetworkSet implements Iterable<Network> {
    /** Vector of networks */
    List<Network> networks = new ArrayList<Network>();

    /** Current iterator */
    Iterator<Network> iterator;

    /** Add a new network */
    void add_network(Network bn) {
        networks.add(bn);
    }

    /** Returns the next network */
    public Network next() {
        return iterator.next();
    }

    /** Returns the size of this set */
    public int size() {
        return networks.size();
    }

    public void add(int index, Network element) {
        networks.add(index, element);
    }

    public void add(Network element) {
        networks.add(element);
    }

    /** Reset the iteration on the network set */
    public void reset() {
        iterator = networks.iterator();
    }

    /** Returns true if there is a next network */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Network> iterator() {
        return networks.iterator();
    }

}
