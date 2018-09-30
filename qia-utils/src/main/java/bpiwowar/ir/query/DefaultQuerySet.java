package bpiwowar.ir.query;

import bpiwowar.utils.GenericHelper;
import java.io.Serializable;
import java.util.Map;

/**
 * Default query set implementation.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class DefaultQuerySet implements QuerySet, Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Topic> queries = GenericHelper.newTreeMap();

    public DefaultQuerySet() {
    }

    @Override
    public Map<String, Topic> queries() {
        return queries;
    }

    /**
     * Add a new {@link Topic} to the query set
     *
     * @param id the topic ID
     * @param topic the {@link Topic} object to add
     */
    public void put(String id, Topic topic) {
        queries.put(id, topic);
    }
}
