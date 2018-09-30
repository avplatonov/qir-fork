package bpiwowar.pipe;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * This class takes a simple Processor (that takes objects of type X and produces object of types Y) and makes from it a
 * producer.
 *
 * @param <Input> The input type
 * @param <Output> The output type
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class ProcessorAdaptor<Input, Output> implements
    CloseableIterator<Output> {
    private static final Logger logger = Logger
        .getLogger(ProcessorAdaptor.class);

    private Processor<Input, Output> processor;
    private Iterator<Input> input;
    Output next = null;

    public ProcessorAdaptor(Iterator<Input> input,
        Processor<Input, Output> processor) {
        this.processor = processor;
        this.input = input;
    }

    private void process() {
        if (next == null) {
            while ((next == null) && input.hasNext()) {
                next = processor.process(input.next());
            }
            // No next: close!
            if (next == null)
                close();
        }
    }

    public boolean hasNext() {
        process();
        return (next != null);
    }

    public Output next() {
        if (!hasNext())
            throw new NoSuchElementException();
        Output _next = next;
        next = null;
        return _next;
    }

    public void remove() {
        throw new RuntimeException("Non mutable iterator");
    }

    public void close() {
        logger.debug("Closing ProcessorAdaptator");
        if (input instanceof CloseableIterator)
            ((CloseableIterator<Input>)input).close();
    }

    @Override
    public String toString() {
        return String.format("ProcessorAdaptator(%s)", processor);
    }
}
