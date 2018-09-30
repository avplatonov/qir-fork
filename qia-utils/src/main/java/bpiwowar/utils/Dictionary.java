/**
 *
 */
package bpiwowar.utils;

import java.util.ArrayList;
import java.util.TreeMap;

public class Dictionary {
    ArrayList<String> int2string = new ArrayList<String>();
    TreeMap<String, Integer> string2int = new TreeMap<String, Integer>();

    /**
     * Get the index of a given term (creates it if necessary)
     *
     * @param term The term whose index is wanted
     * @return
     */
    public Integer getIndex(String term) {
        Integer i = string2int.get(term);
        if (i == null) {
            string2int.put(term, i = new Integer(int2string.size()));
            int2string.add(term);
        }
        return i;
    }

    public String getWord(int index) {
        return int2string.get(index);
    }

    public void clear() {
        int2string.clear();
        string2int.clear();
    }

    public int size() {
        return int2string.size();
    }

}
