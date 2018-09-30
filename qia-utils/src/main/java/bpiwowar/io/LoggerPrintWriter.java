/**
 *
 */
package bpiwowar.io;

import java.io.PrintWriter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author bpiwowar
 */
public class LoggerPrintWriter extends PrintWriter {
    private Logger logger;
    private Level level;

    /**
     * Creates a PrintStream for a given logger at a given output level
     */
    public LoggerPrintWriter(Logger logger, Level level) {
        super(new LoggerOutputStream(logger, level));
        this.logger = logger;
        this.level = level;
    }

    public boolean isEnabled() {
        return logger.isEnabledFor(level);
    }
}
