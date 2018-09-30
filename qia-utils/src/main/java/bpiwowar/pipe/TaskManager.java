package bpiwowar.pipe;

import java.util.Iterator;

/**
 * This object is used to build up an experiment by connecting together components, and calling the method {@link run()}
 * when everything is ready.
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public final class TaskManager {

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> connect(Iterator<?> source,
        Processor<?, T> lastProcessor, Object... previousProcessors) {
        // TODO: class checks to ensure connection is OK
        Iterator<?> current = source;

        for (int i = previousProcessors.length - 1; i >= 0; i--) {
            current = connectOne(current, previousProcessors[i]);
        }

        current = connectOne(current, lastProcessor);

        return (Iterator<T>)current;
    }

    @SuppressWarnings("unchecked")
    private static Iterator<?> connectOne(Iterator<?> current, Object processor) {
        if (processor instanceof Processor) {
            current = new ProcessorAdaptor(current, (Processor)processor);
        }
        else
            throw new RuntimeException("Cannot connect with "
                + processor.getClass());
        return current;
    }

}
