package bpiwowar.ml.rankboost;

import java.util.Iterator;

public class UnionExampleSet<T> implements ExampleSet<T> {

    private ExampleSet<T> firstSet;
    private ExampleSet<T> secondSet;

    public UnionExampleSet(ExampleSet<T> firstSet, ExampleSet<T> secondSet) {
        this.firstSet = firstSet;
        this.secondSet = secondSet;
    }

    public int size() {
        return firstSet.size() + secondSet.size();
    }

    public T get(int i) {
        if (i < firstSet.size())
            return firstSet.get(i);
        return secondSet.get(i - firstSet.size());
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Iterator<T> iterator = firstSet.iterator();
            boolean first = true;

            public boolean hasNext() {
                if (iterator.hasNext())
                    return true;
                if (!first)
                    return false;
                first = false;
                iterator = secondSet.iterator();
                return iterator.hasNext();
            }

            public T next() {
                return iterator.next();
            }

            public void remove() {
                iterator.remove();
            }

        };
    }

}
