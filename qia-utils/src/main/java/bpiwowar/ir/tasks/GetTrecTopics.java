package bpiwowar.ir.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.ir.query.QuerySet;
import bpiwowar.ir.trec.TRECTopic;
import bpiwowar.log.Logger;
import com.thoughtworks.xstream.XStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

@TaskDescription(name = "get-trec-topics", project = {"ir", "trec"}, description = "Read a trec topic file")
public class GetTrecTopics extends AbstractTask {
    final static private Logger LOGGER = Logger.getLogger();

    @Argument(name = "topic-file", checkers = IOChecker.ValidFile.class, required = true)
    File topicFile;

    @Argument(name = "out", checkers = IOChecker.CreateableFile.class, required = true)
    File outFile;

    @Argument(name = "quote-commas", help = "Quote comma separated sequences")
    boolean quoteCommas = false;

    @Override
    public int execute() throws Throwable {
        QuerySet topics = TRECTopic.readTopics(new BufferedReader(
            new FileReader(topicFile)), quoteCommas);

        FileOutputStream os = new FileOutputStream(outFile);
        new XStream().toXML(topics, os);
        os.close();

        LOGGER.info("Read %d TREC topics", topics.queries().size());
        return 0;
    }
}
