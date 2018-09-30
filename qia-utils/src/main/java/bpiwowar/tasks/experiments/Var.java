package bpiwowar.tasks.experiments;

import bpiwowar.argparser.GenericHelper;
import bpiwowar.utils.Output;
import bpiwowar.utils.iterators.AbstractIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Var extends Node {
    String name;
    ArrayList<String> values = new ArrayList<String>();

    public String toString() {
        return String.format("%s=%s", name, Output.toString(",", values));
    }

    @Override
    public Iterator<Map<String, String>> iterator() {
        return new AbstractIterator<Map<String, String>>() {
            Iterator<String> it = values.iterator();

            @Override
            protected boolean storeNext() {
                if (!it.hasNext())
                    return false;

                value = GenericHelper.newTreeMap();
                value.put(name, it.next());

                return true;
            }
        };
    }
}
