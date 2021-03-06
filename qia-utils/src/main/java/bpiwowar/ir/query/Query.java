package bpiwowar.ir.query;

import java.util.Set;

/**
 * A query representation
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class Query {
    abstract public void addTerms(Set<String> set);
}
