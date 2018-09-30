package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.log.Logger;
import bpiwowar.system.FileIterator;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.Output;
import bpiwowar.utils.iterators.ReadLineIterator;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

@TaskDescription(name = "trec-result-table", description = "Create a result table from a recursively organised set of folders", project = "")
public class TRECEvalResultTable extends AbstractTask {
    final static private Logger LOGGER = Logger.getLogger();

    @Argument(name = "param-filename")
    String paramFilename = "params";

    @Argument(name = "measure-file-suffix")
    String measureExtension = ".mea";

    @Argument(name = "assessed-measure-file-suffix")
    String assMeasureExtension = null;

    @Argument(name = "topic-prefix")
    String topicPrefix = "";

    @Argument(name = "out", checkers = IOChecker.CreateableFile.class)
    File file = new File("all.dat");

    @Argument(name = "collapse-metrics", help = "Use one column per metric (analyse the first file of results)")
    boolean collapseMetrics = false;

    @Argument(name = "metrics", help = "Restrict to a set of metrics")
    Set<String> metrics = GenericHelper.newTreeSet();

    String[] folders;

    @Override
    public String[] processTrailingArguments(String[] args) throws Exception {
        this.folders = args;
        return null;
    }

    @Override
    public int execute() throws Throwable {
        if (folders == null || folders.length == 0) {
            LOGGER.warn("No folders to process");
            return 1;
        }

        TreeMap<String, String> parameters = GenericHelper.newTreeMap();

        TreeMap<String, Integer> availableMetrics = GenericHelper.newTreeMap();

        for (String folder : folders)
            addParameters(parameters, new File(folder));

        PrintStream out = new PrintStream(file);

        if (collapseMetrics) {
            final boolean metricRestriction = !metrics.isEmpty();
            LOGGER.info("Searching for a result file");
            loop:
            for (int i = 0; i < folders.length; i++) {
                FileIterator it = new FileIterator(new File(folders[i]), -1,
                    new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            String name = file.getName();
                            final boolean realMeasure = name
                                .endsWith(measureExtension);
                            return file.isFile()
                                && (realMeasure || (assMeasureExtension != null && name
                                .endsWith(assMeasureExtension)));
                        }
                    });
                while (it.hasNext()) {
                    File file = it.next();
                    LOGGER.info("Found file [%s]", file);
                    for (String line : new ReadLineIterator(file)) {
                        String[] fields = line.split("\\s+");
                        if (fields.length == 3
                            && (!metricRestriction || metrics
                            .contains(fields[0]))) {
                            if (!availableMetrics.containsKey(fields[0]))
                                availableMetrics.put(fields[0],
                                    availableMetrics.size());
                        }

                    }
                    break loop;
                }
                it.close();
            }

            LOGGER.info("Found %d metrics", availableMetrics.size());
            out.print("q\t");
            Output.print(out, "\t", availableMetrics.keySet());
            out.print("\t");
        }
        else
            out.format("metric\tq\tvalue\t");

        Output.print(out, "\t", parameters.keySet());
        out.println();

        for (String folder : folders)
            output(out, availableMetrics, parameters, new File(folder));

        out.close();
        return 0;
    }

    void output(final PrintStream out,
        final TreeMap<String, Integer> availableMetrics,
        TreeMap<String, String> oldvalues, File folder) {
        // Add values

        File paramFile = new File(folder, paramFilename);

        if (paramFile.isFile()) {
            // Create new map if we modify
            oldvalues = (TreeMap<String, String>)oldvalues.clone();

            try {
                for (String line : new ReadLineIterator(paramFile)) {
                    String[] fields = line.split("\\s+");
                    for (int i = 1; i < fields.length; i += 2) {
                        oldvalues.put(fields[i - 1], fields[i]);
                    }
                }
            }
            catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        final TreeMap<String, String> values = oldvalues;
        final boolean metricRestriction = !metrics.isEmpty();

        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                File file = new File(dir, name);
                final boolean realMeasure = name.endsWith(measureExtension);
                if (file.isFile()
                    && (realMeasure || (assMeasureExtension != null && name
                    .endsWith(assMeasureExtension)))) {
                    try {
                        LOGGER.info("Processing [%b] %s", realMeasure, file);
                        if (collapseMetrics) {
                            // Collect
                            TreeMap<String, double[]> allMeasures = new TreeMap<String, double[]>();

                            for (String line : new ReadLineIterator(file)) {
                                String[] fields = line.split("\\s+");

                                if (fields.length == 3
                                    && (!metricRestriction || metrics
                                    .contains(fields[0]))) {
                                    String metricName = fields[0];
                                    String topic = fields[1];

                                    Integer idx = availableMetrics
                                        .get(metricName);
                                    if (idx == null)
                                        throw new bpiwowar.lang.RuntimeException(
                                            "Unexpected metric: %s",
                                            metricName);
                                    double[] measures = allMeasures.get(topic);
                                    if (measures == null) {
                                        allMeasures.put(topic, measures = new double[availableMetrics
                                            .size()]);
                                        for (int i = measures.length; --i >= 0; )
                                            measures[i] = Double.NaN;
                                    }
                                    measures[idx] = Double
                                        .parseDouble(fields[2]);
                                }
                            }

                            // Print
                            for (Entry<String, double[]> entry : allMeasures
                                .entrySet()) {
                                out.format("%s\t", entry.getKey());
                                double[] measures = entry.getValue();
                                for (int i = 0; i < measures.length; i++)
                                    out.format("%g\t", measures[i]);
                                Output.print(out, "\t", values.values());
                                out.println();
                            }
                        }
                        else
                            for (String line : new ReadLineIterator(file)) {
                                String[] fields = line.split("\\s+");
                                if (fields.length == 3
                                    && (!metricRestriction || metrics
                                    .contains(fields[0]))) {
                                    out.format(realMeasure ? "%s\t%s\t%s\t"
                                            : "A.%s\t%s\t%s\t", fields[0],
                                        topicPrefix + fields[1], fields[2]);
                                    Output.print(out, "\t", values.values());
                                    out.println();
                                }
                                else {
                                }
                            }
                    }
                    catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                }
                else if (file.isDirectory())
                    output(out, availableMetrics, values, file);

                return false;
            }
        };

        folder.list(filter);

    }

    /**
     * Browse all the parameter files and collect the fields
     *
     * @param parameters
     * @param folder
     */
    void addParameters(final Map<String, String> parameters, File folder) {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                File file = new File(dir, name);
                if (file.isFile()) {
                    if (name.equals(paramFilename)) {
                        try {
                            for (String line : new ReadLineIterator(file)) {
                                String[] fields = line.split("\\s+");
                                if (fields.length > 1)
                                    for (int i = 0; i < fields.length; i += 2)
                                        parameters.put(fields[i], "NA");
                            }
                        }
                        catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                else if (file.isDirectory()) {
                    addParameters(parameters, file);
                }
                return false;
            }
        };
        folder.list(filter);
    }
}
