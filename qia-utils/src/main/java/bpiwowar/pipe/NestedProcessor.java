package bpiwowar.pipe;

public interface NestedProcessor<Input, Output> {
    CloseableIterator<Output> process(Input input);
}
