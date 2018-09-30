package bpiwowar.ir.query;

import bpiwowar.text.Query.Operator;
import java.util.Set;

/**
 * A term with a restriction
 *
 * @author bpiwowar
 */
public abstract class Text {
    /**
     * A restriction
     */
    public Restriction restriction = Restriction.NONE;

    public Text(Operator operator) {
        restriction = Restriction.get(operator);
    }

    public Text() {
    }

    /**
     * Add all terms in this query
     *
     * @param set
     */
    abstract public void addAllTerms(Set<String> set);

    @Override
    public String toString() {
        return restriction.toString();
    }

    public boolean isNegative() {
        return restriction == Restriction.NEGATIVE;
    }
}
