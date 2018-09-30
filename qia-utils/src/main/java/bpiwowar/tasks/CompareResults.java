package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentPostProcessor;
import bpiwowar.argparser.ArgumentProcessor;
import bpiwowar.argparser.ArgumentRegexp;
import bpiwowar.argparser.IllegalArgumentValue;
import bpiwowar.argparser.ListAdaptator;
import bpiwowar.argparser.checkers.IOChecker.ValidFile;
import bpiwowar.collections.Aggregator;
import bpiwowar.collections.CartesianProduct;
import bpiwowar.collections.CollectionIntersectionIterator;
import bpiwowar.collections.JoinIterator;
import bpiwowar.collections.SingleIterable;
import bpiwowar.collections.TreeMapArray;
import bpiwowar.collections.TreeMapSet;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.lang.RuntimeException;
import bpiwowar.log.Logger;
import bpiwowar.utils.ComparablePair;
import bpiwowar.utils.Formatter;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.MutableInteger;
import bpiwowar.utils.Output;
import bpiwowar.utils.Pair;
import bpiwowar.utils.Time;
import bpiwowar.utils.iterators.ReadLineIterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.math.stat.descriptive.StorelessUnivariateStatistic;
import org.apache.commons.math.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.rank.Max;
import org.apache.commons.math.stat.descriptive.rank.Median;

import static java.lang.Math.ceil;

@TaskDescription(name = "compare-results", project = {"ir"}, description = "Compares results from different runs")
public class CompareResults extends AbstractTask {

    final static private Logger logger = Logger.getLogger();

    private static final String TOPIC = "q";

    static class Result implements Comparable<Result> {
        /**
         * The unique id for this result
         */
        final int id;

        /**
         * The set of properties with their values
         */
        String[] fieldNames;

        /**
         * Values
         */
        Map<String, Integer> fieldNameIndex;

        /**
         * Values
         */
        String[] fieldValues;

        /**
         * The value
         */
        double value;

        public Result(int id, String[] fieldNames,
            Map<String, Integer> fieldNameIndex, String[] fieldValues,
            double value) {
            this.id = id;
            this.fieldNameIndex = fieldNameIndex;
            this.fieldNames = fieldNames;
            this.fieldValues = fieldValues;
            this.value = value;
        }

        @Override
        public int compareTo(Result o) {
            return id - o.id;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fieldNames.length; i++) {
                if (i != 0)
                    sb.append(", ");
                sb.append(fieldNames[i]);
                sb.append(": ");
                sb.append(fieldValues[i]);
            }
            return String.format("Result %d (%s)", id, sb);
        }
    }

    /**
     * Contains a set of results
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    public final class Results {
        int currentResultId = 0;

        /**
         * Field name -> (Field value -> list of results)
         */
        @SuppressWarnings("unchecked")
        Map<String, TreeMapSet<String, Result, SortedSet<Result>>> index = TreeMapSet
            .newInstance(TreeSet.class);

        /**
         * Specific index for models
         */
        SortedSet<Result> data = new TreeSet<Result>();

        /**
         * List of fields for
         */
        Set<String> resultFields = null;

        @ArgumentProcessor()
        void add(
            @Argument(checkers = ValidFile.class, help = "The model name") File file)
            throws FileNotFoundException, IllegalArgumentValue {
            logger.info("Loading content of file %s", file);
            ReadLineIterator lines = new ReadLineIterator(file);

            // --- Get the fields from the first line
            int nbFields = 0;
            // This contains the mapping field name -> field index

            // Build the map between a field name and an id
            Map<String, Integer> fields = GenericHelper.newTreeMap();
            final String[] fieldNames = lines.next().split("\\s+");
            int valueIndex = -1;

            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];
                if (!fieldName.equals(measureField))
                    fields.put(fieldName, nbFields);
                else
                    valueIndex = i;
                nbFields++;
            }

            if (valueIndex < 0)
                throw new IllegalArgumentValue(
                    "Could not find field 'value' in fields of file %s",
                    file);

            if (resultFields != null) {
                if (!resultFields.equals(fields.keySet()))
                    throw new bpiwowar.lang.RuntimeException(
                        "Field names for model %s are different between data files: [%s] and [%s]",
                        Output.toString(",", fields.keySet()), Output
                        .toString(",", resultFields));
            }
            else
                resultFields = fields.keySet();

            // Prepare for the use of indices: build an array
            // field index -> {field value -> list of results}
            @SuppressWarnings("unchecked")
            TreeMapSet<String, Result, SortedSet<Result>> fieldIndex[] = new TreeMapSet[nbFields];
            for (Map.Entry<String, Integer> e : fields.entrySet()) {
                final Integer id = e.getValue();
                final String fieldname = e.getKey();
                // We don't want an index on values
                if (!fieldname.equals(measureField)) {
                    fieldIndex[id] = index.get(fieldname);
                    if (fieldIndex[id] == null) {
                        @SuppressWarnings("unchecked")
                        TreeMapSet<String, Result, SortedSet<Result>> n = TreeMapSet
                            .newInstance(TreeSet.class);
                        fieldIndex[id] = n;
                        index.put(fieldname, n);
                    }
                }
            }

            // Read the data
            logger.info("Reading the data");
            int nbLines = 0;
            while (lines.hasNext()) {
                String[] values = lines.next().split("\\s+");
                Result result = new Result(currentResultId, fieldNames, fields,
                    values, Double.parseDouble(values[valueIndex]));
                currentResultId++;
                nbLines++;
                // Add to the corresponding index
                for (int i = 0; i < fieldIndex.length; i++)
                    if (fieldIndex[i] != null)
                        fieldIndex[i].add(values[i], result);
                data.add(result);
            }
            logger.info("Read %d lines of data (%d fields)", nbLines,
                fields.size());

        }

        /**
         * Find results that have the same field values for a specific set of fields
         *
         * @param fieldNames (R) The names of the field
         * @param index (R) The current index of the field to process
         * @param sets (RW) An array containing the different sets from which to select. Warning: these sets will be
         * modified.
         * @param allowsNull If nulls should be allowed if no result is found
         * @param resultSet (W) A set of of tuples of lists of elements
         */
        void filterCommon(List<String> fieldNames,
            List<SortedSet<Result>[]> resultSet, final boolean allowsNull,
            final int minValues, SortedSet<Result>... sets) {
            // Get the field names
            final ArrayList<String> sortedFieldNames = sortFieldByEntropy(
                fieldNames, true);

            // Compares on the selected field values
            final Comparator<Result> keyComparator = new Comparator<Result>() {
                @Override
                public int compare(Result r1, Result r2) {
                    for (String n : sortedFieldNames) {
                        Integer i1 = r1.fieldNameIndex.get(n);
                        Integer i2 = r2.fieldNameIndex.get(n);
                        if (i1 == null)
                            if (i2 == null)
                                return 0;
                            else
                                return 1;
                        if (i2 == null)
                            return -1;

                        int z = r1.fieldValues[i1]
                            .compareTo(r2.fieldValues[i2]);
                        if (z != 0)
                            return z;
                    }
                    return 0;
                }
            };

            final Comparator<Map.Entry<Result, ArrayList<Result>>> comparator = new Comparator<Map.Entry<Result, ArrayList<Result>>>() {
                @Override
                public int compare(Map.Entry<Result, ArrayList<Result>> r1,
                    Map.Entry<Result, ArrayList<Result>> r2) {
                    return keyComparator.compare(r1.getKey(), r2.getKey());
                }
            };

            // Sort each list on the fields to match
            @SuppressWarnings("unchecked") final Iterator<Map.Entry<Result, ArrayList<Result>>> iterators[] = new Iterator[sets.length];
            for (int i = 0; i < sets.length; i++) {
                TreeMapArray<Result, Result> map = TreeMapArray
                    .newInstance(keyComparator);
                for (Result r : sets[i])
                    map.add(r, r);
                iterators[i] = map.entrySet().iterator();
            }

            Aggregator<Map.Entry<Result, ArrayList<Result>>, SortedSet<Result>[]> aggregator = new Aggregator<Map.Entry<Result, ArrayList<Result>>, SortedSet<Result>[]>() {
                private SortedSet<Result>[] array;

                @Override
                public void set(int index,
                    Map.Entry<Result, ArrayList<Result>> x) {
                    (array[index] = GenericHelper.newTreeSet()).addAll(x
                        .getValue());
                }

                @Override
                @SuppressWarnings("unchecked")
                public void reset() {
                    array = new SortedSet[iterators.length];
                }

                @Override
                public SortedSet<Result>[] aggregate() {
                    return array;
                }

                @Override
                public boolean accept(int n, int size) {
                    return (allowsNull || n == size) && n >= minValues;
                }

            };

            JoinIterator<Map.Entry<Result, ArrayList<Result>>, SortedSet<Result>[]> join = new JoinIterator<Map.Entry<Result, ArrayList<Result>>, SortedSet<Result>[]>(
                comparator, aggregator, iterators);

            while (join.hasNext()) {
                final Pair<Entry<Result, ArrayList<Result>>, SortedSet<Result>[]> next = join
                    .next();
                resultSet.add(next.getSecond());
            }

        }

        /**
         * Sort a set of fields by entropy
         *
         * @param fieldNames
         * @param increasing
         * @return
         */
        ArrayList<String> sortFieldByEntropy(List<String> fieldNames,
            boolean increasing) {
            // Re-order the field names so that we have better selectivity
            // first, i.e. order by entropy
            ArrayList<ComparablePair<Double, String>> list = GenericHelper
                .newArrayList();
            for (String fieldName : fieldNames) {
                TreeMapSet<String, Result, SortedSet<Result>> map = index
                    .get(fieldName);
                double logsum = 0;
                double N = 0;
                for (SortedSet<Result> x : map.values()) {
                    final double size = (double)x.size();
                    N += size;
                    logsum = Math.log(x.size()) * size;
                }

                list.add(ComparablePair.create((increasing ? 1. : -1.)
                    * (N * Math.log(N) - logsum), fieldName));
            }
            Collections.sort(list);
            logger.debug("New order: %s", Output.toString(",", list));
            ArrayList<String> sortedNames = GenericHelper.newArrayList();
            for (ComparablePair<Double, String> x : list)
                sortedNames.add(x.getSecond());
            return sortedNames;
        }

        /**
         * Filter a sort of results by selecting only those where a given field is equal to a given value
         *
         * @param results The set to be filtered
         * @param fieldname Field name
         * @param fieldvalue Field value
         * @param remove True if selected results should be removed from the provided set (e.g. in order to speed up
         * further selections on the same set with the same field).
         * @return The results from <code>results</code> that have the value
         * <code>fieldvalue</code> for the field <code>fieldname</code>
         */
        SortedSet<Result> filterResults(SortedSet<Result> results,
            String fieldname, String fieldvalue, boolean remove) {
            SortedSet<Result> fieldIndex = index.get(fieldname).get(fieldvalue);
            if (fieldIndex == null) {
                logger.warn("There is no result where %s = %s", fieldname,
                    fieldvalue);
                if (remove) {
                    results.clear();
                    return results;
                }
                return GenericHelper.newTreeSet();
            }

            return filterResults(results, fieldIndex, remove);
        }

        /**
         * Filter a set of results
         *
         * @param source The set of results
         * @param filter The set of results that should be kept from the source
         * @param remove If source should be modified
         * @return
         */
        private SortedSet<Result> filterResults(SortedSet<Result> source,
            SortedSet<Result> filter, boolean remove) {

            TreeSet<Result> newResults = GenericHelper.newTreeSet();
            if (filter.size() > source.size() / 5) {
                SortedSet<Result> tailSet = source;
                // Special algorithm in that case
                for (Result x : filter) {
                    tailSet = tailSet.tailSet(x);
                    if (tailSet.isEmpty())
                        break;
                    if (x.compareTo(tailSet.first()) == 0)
                        newResults.add(x);
                }

            }
            else {
                @SuppressWarnings("unchecked") final SingleIterable<Pair<Result, Integer>> iterable = SingleIterable
                    .create(CollectionIntersectionIterator.newInstance(
                        filter, source));
                for (Pair<Result, Integer> y : iterable) {
                    final Result result = y.getFirst();
                    newResults.add(result);
                }
            }

            // Remove results from source
            if (remove)
                source.removeAll(newResults);
            return newResults;
        }

    }

    @Argument(name = "measure", help = "The name of the field that contains the measure")
    String measureField = "value";

    @Argument(name = "result", help = "File of results to load", required = true)
    Results results = new Results();

    @Argument(name = "control-fields", help = "Name of the fields on which to analyse values")
    List<String> controlfields = GenericHelper.newArrayList();

    @Argument(name = "min-values", help = "Minimum number of values for output (default 1)")
    int minValues = 1;

    @Argument(name = "show-common", help = "Show common values")
    boolean showCommonValues;

    @Argument(name = "auto-ignore", help = "Ignore automatically fields where there is no value that belongs to at least the given ratio of possible values")
    float automaticFieldIgnore = 0;

    @Argument(name = "filter", help = "Filter the results with name=value")
    @ArgumentRegexp("(.*)=(.*)")
    void filter(String name, String value) throws IllegalArgumentValue {
        logger.debug("Added filter %s = %s", name, value);
        filters.add(new String[] {name, value});
    }

    @Argument(name = "selection", help = "Don't use cartesian product")
    boolean selection = false;

    @Argument(name = "field", help = "Name of the field to explore", required = true)
    @ArgumentRegexp("([^=]*)(?:=(.*))?")
    void field(String name, ArrayList<String> values) {
        fields.addAll(name, values);
    }

    /**
     * Associate a field name to a list of values
     */
    TreeMapArray<String, String> fields = TreeMapArray.newInstance();

    private ArrayList<String[]> filters = GenericHelper.newArrayList();

    @Argument(name = "no-match-fields", help = "Fields that should not be considered when matching")
    TreeSet<String> noMatchFields;

    @ArgumentPostProcessor
    void check() throws IllegalArgumentValue {
        logger.info("Checking argument");
        if (selection) {
            final Entry<String, ArrayList<String>> firstEntry = fields
                .firstEntry();
            int size = firstEntry.getValue().size();
            for (Entry<String, ArrayList<String>> s : fields.entrySet()) {
                if (s.getValue().size() == 0)
                    throw new IllegalArgumentValue("Fields %s has not values",
                        firstEntry.getKey());

                if (size != s.getValue().size())
                    throw new IllegalArgumentValue(
                        "Fields %s and %s have a different number of values (%d and %d)",
                        firstEntry.getKey(), s.getKey(), size, s.getValue()
                        .size());
            }
        }
    }

    Output.PrintFormatter<Pair<String, String>> PAIR_FORMATTER = new Output.PrintFormatter<Pair<String, String>>() {
        @Override
        public void print(PrintStream out, Pair<String, String> t) {
            out.format("%s=%s", t.getFirst(), t.getSecond());
        }
    };

    @SuppressWarnings("unchecked")
    @Override
    public int execute() throws Throwable {
        final long start = System.currentTimeMillis();

        // --- Filter the results

        // Get the results for the given model
        SortedSet<Result> modelResults = results.data;

        // filter them using the filters (name=value)
        for (String[] filter : filters) {
            logger.info("Filtering results on %s=%s", filter[0], filter[1]);
            modelResults = results.filterResults(modelResults, filter[0],
                filter[1], false);
        }

        // --- Field to match selection

        // Get the potential fields to match
        List<String> fieldNames = GenericHelper.newArrayList();
        for (String s : results.resultFields)
            if (!fields.containsKey(s) && !s.equals("metric")
                && (noMatchFields == null || !noMatchFields.contains(s))) {
                fieldNames.add(s);
            }
        logger.info("Fields to match: %s", Output.toString(", ", fieldNames));

        // Get the values for the field(s) to explore, if needed
        int product = 1;
        for (Entry<String, ArrayList<String>> v : fields.entrySet()) {
            ArrayList<String> list = v.getValue();
            if (list.isEmpty()) {
                final String fieldName = v.getKey();

                // Get the values
                TreeSet<String> values = GenericHelper.newTreeSet();
                final Result firstModel = modelResults.first();
                Integer fieldNum = firstModel.fieldNameIndex
                    .get(fieldName);

                for (Result r : modelResults)
                    values.add(r.fieldValues[fieldNum]);

                if (values.isEmpty())
                    throw new RuntimeException(
                        "Field name %s has no values for the selected set of results",
                        fieldName);

                v.setValue(list = new ArrayList<String>(values));
                if (logger.isDebugEnabled())
                    logger.debug("Field %s can take the values %s", fieldName,
                        Output.toString(", ", values));
            }
            product *= list.size();
        }

        // Get the control fields
        final int nbcontrol = controlfields.size();
        if (nbcontrol > 0) {
            Output.print(System.out, "\t", controlfields);
            System.out.print('\t');
        }

        // Get the list of values
        if (fields.size() == 1) {
            logger.info("Only one field: selection mode");
            selection = true;
        }

        final int nbPossibilities = selection ? fields.firstEntry().getValue()
            .size() : product;
        logger.info("We have %d possible sets of values", nbPossibilities);

        // This contains a list of pairs name = value to filter results
        // in each configuration
        final ArrayList<Pair<String, String>>[] factorValues = GenericHelper
            .newArray(ArrayList.class, nbPossibilities);

        if (selection) {
            for (int i = 0; i < nbPossibilities; i++)
                factorValues[i] = GenericHelper.newArrayList();
            for (Entry<String, ArrayList<String>> v : fields.entrySet()) {
                for (int i = 0; i < nbPossibilities; i++)
                    factorValues[i].add(Pair.create(v.getKey(), v.getValue()
                        .get(i)));
            }
        }
        else {
            // Otherwise, caretesian product
            int N = fields.size();
            String[] selFieldNames = new String[N];
            Iterable<String>[] lists = GenericHelper
                .newArray(Iterable.class, N);
            {
                int j = 0;
                for (Entry<String, ArrayList<String>> v : fields.entrySet()) {
                    selFieldNames[j] = v.getKey();
                    lists[j] = v.getValue();
                    logger.info("Field %s has values %s", v.getKey(),
                        Output.toString(",", lists[j]));
                    j++;
                }
            }

            int i = 0;
            for (String[] v : new CartesianProduct<String>(String.class, true,
                lists)) {
                factorValues[i] = GenericHelper.newArrayList();
                for (int j = 0; j < N; j++)
                    factorValues[i].add(Pair.create(selFieldNames[j], v[j]));
                i++;
            }

        }

        // Output the header, i.e. the set of values for the field to explore
        Output.print(System.out, "\t", factorValues,
            new Output.PrintFormatter<ArrayList<Pair<String, String>>>() {
                @Override
                public void print(PrintStream out,
                    ArrayList<Pair<String, String>> t) {
                    out.print('"');
                    Output.print(out, ":", t, PAIR_FORMATTER);
                    out.print('"');
                }
            });
        System.out.println();

        // Group by control field
        List<SortedSet<Result>[]> resultSet = GenericHelper.newArrayList();
        if (nbcontrol > 0) {
            results.filterCommon(controlfields, resultSet, true, 1,
                modelResults);
            logger.info("Found %d different possibilities for control fields",
                resultSet.size());
        }
        else
            resultSet.add(new SortedSet[] {modelResults});

        // --- If we have to find out the fields to ignore
        // conservative policy:
        // If a field value is matched for at least two factor
        // combinations, we keep it

        if (automaticFieldIgnore > 0.) {
            int minCount = (int)ceil(automaticFieldIgnore * nbPossibilities);
            if (automaticFieldIgnore > 1)
                minCount = (int)automaticFieldIgnore;
            if (minCount <= 1)
                minCount = 2;

            logger.info(
                "Finding the fields to ignore (disjoint values): minimum count %d",
                minCount);

            // Copy the results
            SortedSet<Result> theResults = new TreeSet<Result>(modelResults);

            // Keeps track of the set of possible values
            final Result first = theResults.first();
            TreeMap<Integer, TreeMap<String, MutableInteger>> fieldValues = GenericHelper
                .newTreeMap();
            for (String fieldname : fieldNames) {
                fieldValues.put(first.fieldNameIndex.get(fieldname),
                    new TreeMap<String, MutableInteger>());
            }
            fieldNames.clear();

            // Get all the different sets
            for (int i = 0; i < nbPossibilities && !fieldValues.isEmpty(); i++) {
                // Get the set of values for this factor settings
                SortedSet<Result> set = theResults;
                for (Pair<String, String> namevalue : factorValues[i]) {
                    logger.debug(" [%d] factor %s=%s", i, namevalue.getFirst(),
                        namevalue.getSecond());
                    set = results.filterResults(set, namevalue.getFirst(),
                        namevalue.getSecond(), false);
                }
                theResults.removeAll(set);

                // Compute the intersection of values for each field
                Iterator<Entry<Integer, TreeMap<String, MutableInteger>>> it = fieldValues
                    .entrySet().iterator();
                mainloop:
                while (it.hasNext()) {
                    Entry<Integer, TreeMap<String, MutableInteger>> name_values = it
                        .next();
                    int fieldNum = name_values.getKey();
                    Map<String, MutableInteger> values = name_values.getValue();
                    // Get the set of values
                    TreeSet<String> rValues = new TreeSet<String>();
                    for (Result r : set) {
                        if (rValues.add(r.fieldValues[fieldNum])) {
                            MutableInteger count = values
                                .get(r.fieldValues[fieldNum]);
                            if (count == null)
                                values.put(r.fieldValues[fieldNum],
                                    count = new MutableInteger());

                            if (++count.value >= minCount) {
                                // We have a non null intersection:
                                // we stop here, and keep this field
                                logger.debug("Keeping %s",
                                    r.fieldNames[fieldNum]);
                                fieldNames.add(r.fieldNames[fieldNum]);
                                it.remove();
                                continue mainloop;
                            }

                        }
                    }
                }
            }

            logger.info("Removed automatically the following fields: %s",
                Output.toString(",", fieldValues.keySet(),
                    new Formatter<Integer>() {
                        @Override
                        public String format(Integer t) {
                            return first.fieldNames[t];
                        }
                    }));
            logger.info("Remaining fields: %s", fieldNames);

        }

        // --- We just loop over results (over the control fields values)
        for (SortedSet<Result>[] theResults : resultSet) {
            if (theResults.length > 1)
                throw new RuntimeException("Did not expect that");

            // let's go
            Result first = theResults[0].first();

            // Get the value of the control fields
            String[] controlValues = new String[nbcontrol];
            for (int i = 0; i < nbcontrol; i++)
                controlValues[i] = first.fieldValues[first.fieldNameIndex
                    .get(controlfields.get(i))];

            // Filtering each set with respect to the values
            // of the factors
            SortedSet<Result>[] sets = GenericHelper.newArray(SortedSet.class,
                nbPossibilities);
            for (int i = 0; i < nbPossibilities; i++) {
                sets[i] = theResults[0];
                for (Pair<String, String> namevalue : factorValues[i]) {
                    sets[i] = results.filterResults(sets[i],
                        namevalue.getFirst(), namevalue.getSecond(), false);
                }
                theResults[0].removeAll(sets[i]);
            }

            // Get paired results
            List<SortedSet<Result>[]> rs = GenericHelper.newArrayList();
            results.filterCommon(fieldNames, rs, true, minValues, sets);

            for (SortedSet<Result>[] x : rs) {
                // Output the control values
                if (nbcontrol > 0) {
                    Output.print(System.out, "\t",
                        ListAdaptator.create(controlValues));
                    System.out.print('\t');
                }

                // Output the list of values
                Output.print(System.out, "\t", ListAdaptator.create(x),
                    PAIRLIST_FORMATTER);

                // Be more verbose, and output the common values
                // at the end
                if (showCommonValues) {
                    SortedSet<Result> ref = null;
                    for (int i = 0; i < x.length
                        && (ref == null || ref.isEmpty()); i++)
                        ref = x[i];

                    System.out.print("\t# [");
                    Output.print(System.out, ",", x,
                        new Formatter<SortedSet<Result>>() {
                            @Override
                            public String format(SortedSet<Result> t) {
                                return Integer.toString(t == null ? 0 : t
                                    .size());
                            }
                        });
                    System.out.print("] ");
                    for (String fieldName : fieldNames)
                        System.out
                            .format("%s=%s, ",
                                fieldName,
                                ref.first().fieldValues[ref.first().fieldNameIndex
                                    .get(fieldName)]);

                } // end show common values
                System.out.println();
            }
        }

        long stop = System.currentTimeMillis();
        logger.info("Time taken: "
            + Time.formatTimeInMilliseconds((stop - start)));
        return 0;
    }

    @Argument(name = "aggregation", help = "How to aggregate several values")
    Aggregation aggregation = Aggregation.MEDIAN;

    private final Formatter<SortedSet<Result>> PAIRLIST_FORMATTER = new Formatter<SortedSet<Result>>() {
        @Override
        public String format(SortedSet<Result> t) {
            return String.format("%g", aggregation.compute(t));
        }
    };

    /**
     * How to aggregate results
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static public enum Aggregation {
        MEAN(new Mean()),

        MEDIAN(new Median()),

        MAX(new Max());

        private UnivariateStatistic sus;

        Aggregation(UnivariateStatistic sus) {
            this.sus = sus;
        }

        double compute(Set<Result> y) {
            if (y == null)
                return Double.NaN;

            if (sus instanceof StorelessUnivariateStatistic) {
                StorelessUnivariateStatistic slus = (StorelessUnivariateStatistic)sus;
                slus.clear();
                for (Result x : y)
                    slus.increment(x.value);
                return slus.getResult();
            }

            double[] values = new double[y.size()];
            int i = 0;
            for (Result x : y)
                values[i++] = x.value;
            return sus.evaluate(values);
        }
    }
}
