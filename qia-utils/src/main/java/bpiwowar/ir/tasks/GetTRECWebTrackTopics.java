package bpiwowar.ir.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.ir.query.QuerySet;
import bpiwowar.ir.trec.TRECWebTopic;
import bpiwowar.log.Logger;
import com.thoughtworks.xstream.XStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

/**
 * Reads a TREC 2009/2010 Web Track topic file
 *
 * @author <a href="mailto:ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
 */
@TaskDescription(name = "get-trec-webtrack-topics",
    project = {"ir", "trec"},
    description = "Read a trec web track topic file")
public class GetTRECWebTrackTopics extends AbstractTask {
    final static private Logger LOGGER = Logger.getLogger();

    /** The topic file */
    @Argument(name = "topic-file", checkers = IOChecker.ValidFile.class, required = true)
    File topicFile;

    /** The output file */
    @Argument(name = "out", checkers = IOChecker.CreateableFile.class, required = true)
    File outFile;

    @Argument(name = "quote-commas", help = "Quote comma separated sequences")
    boolean quoteCommas = false;

    @Override
    public int execute() throws Throwable {
        QuerySet topics = TRECWebTopic.readTopics(new BufferedReader(
            new FileReader(topicFile)));

        FileOutputStream os = new FileOutputStream(outFile);
        new XStream().toXML(topics, os);
        os.close();

        LOGGER.info("Read %d TREC Web Track topics",
            topics.queries().size());
        return 0;
    }
}
