package bpiwowar.ir.trec;

import bpiwowar.lang.RuntimeException;
import bpiwowar.log.Logger;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.iterators.ReadLineIterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static java.lang.Math.signum;

/**
 * A set of TREC results as contained in one TREC file
 *
 * Format is supposed to be: <div><code>qid iter   docno      rank  sim   run_id</code>
 * </div>
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TRECResults {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * our judgments
     */
    Map<String, SortedSet<Result>> results = GenericHelper.newTreeMap();

    String runid;

    public static class Result implements Comparable<Result> {
        private double rsv;
        public String docno;

        public Result(double rsv, String docno) {
            super();
            this.setRsv(rsv);
            this.docno = docno;
        }

        @Override
        public int compareTo(Result other) {
            // we want documents with higher rsv first, so we invert the order
            int z = (int)signum(other.getRsv() - this.getRsv());
            if (z != 0)
                return z;
            return docno.compareTo(other.docno);
        }

        /**
         * @param rsv the rsv to set
         */
        public void setRsv(double rsv) {
            this.rsv = rsv;
        }

        /**
         * @return the rsv
         */
        public double getRsv() {
            return rsv;
        }

    }

    public TRECResults(Reader in) {
        for (String line : new ReadLineIterator(in)) {
            String[] fields = line.split("\\s+");
            if (fields.length != Fields.values().length)
                throw new RuntimeException(
                    "Non TREC qrels format for line: %s", line);

            String qid = fields[Fields.QID.ordinal()];
            String docno = fields[Fields.DOCNO.ordinal()];
            double rsv = Double.parseDouble(fields[Fields.SIM.ordinal()]);
            runid = fields[Fields.RUN_ID.ordinal()];

            SortedSet<Result> set = results.get(qid);
            if (set == null)
                results.put(qid, set = GenericHelper.newTreeSet());

            boolean ok = set.add(new Result(rsv, docno));
            if (!ok)
                LOGGER.warn("Duplicate result for document %s and topic %s",
                    docno, qid);
        }
    }

    public TRECResults(File file) throws FileNotFoundException {
        this(new FileReader(file));
    }

    /**
     * Get a set of results
     *
     * @param qid
     * @return
     */
    public SortedSet<Result> get(String qid) {
        return results.get(qid);
    }

    public Set<String> getTopics() {
        return results.keySet();
    }

    static public enum Fields {
        QID, ITER, DOCNO, RANK, SIM, RUN_ID
    }
}


