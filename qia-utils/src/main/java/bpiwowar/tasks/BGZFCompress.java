package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.OrderedArgument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.log.Logger;
import bpiwowar.utils.iterators.ReadLineIterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import net.sf.samtools.util.BlockCompressedOutputStream;

@TaskDescription(name = "bgzf-compress", project = {"io"})
public class BGZFCompress extends AbstractTask {
    final static private Logger LOGGER = Logger.getLogger();

    @Argument(name = "gzipped", help = "Use if the input file(s) is (are) gzipped")
    boolean gzipped = false;

    @OrderedArgument(name = "file")
    File file;

    @Override
    public int execute() throws Throwable {
        final byte[] buffer = new byte[8192];

        if (file != null)
            convert(gzipped ? new GZIPInputStream(System.in) : System.in, file,
                buffer);
        else {
            for (String line : new ReadLineIterator(System.in)) {
                String[] fields = line.split("\t");
                if (fields.length == 2) {
                    FileInputStream in = new FileInputStream(fields[0]);
                    File file = new File(fields[1]);
                    if (file.exists())
                        LOGGER
                            .warn("Won't touch file %s since it exists",
                                file);
                    else {
                        LOGGER.info("Compressing %s into %s", fields[0],
                            fields[1]);
                        convert(gzipped ? new GZIPInputStream(in) : in, file,
                            buffer);
                    }
                    in.close();
                }
                else
                    LOGGER.warn("Skipping line %s (%d fields instead of 2)",
                        line, fields.length);
            }
        }

        return 0;
    }

    /**
     * @param buffer
     * @throws IOException
     */
    static private void convert(InputStream in, File file, final byte[] buffer)
        throws IOException {

        BlockCompressedOutputStream stream = new BlockCompressedOutputStream(
            file);
        int read;
        while ((read = in.read(buffer)) >= 0) {
            if (read > 0)
                stream.write(buffer, 0, read);
        }
        stream.close();
    }
}
