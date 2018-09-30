package bpiwowar.pipe;

/**
 * An object that generates one output object (max) for every input.
 *
 * @param <Input> the type of the input object
 * @param <Output> the type of the output object
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public interface Processor<Input, Output> {
    public Output process(final Input input);
}
