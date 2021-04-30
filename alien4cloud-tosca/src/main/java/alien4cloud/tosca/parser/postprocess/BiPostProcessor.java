package alien4cloud.tosca.parser.postprocess;

import org.apache.commons.lang3.tuple.ImmutablePair;

public abstract class BiPostProcessor<T,U> implements IPostProcessor<ImmutablePair<T,U>> {

    @Override
    public final void process(ImmutablePair<T, U> pair) {
        var t =pair.getLeft();
        var u = pair.getRight();

        process(t,u);
    }

    public abstract void process(T t,U u);

    public ImmutablePair<T,U> build(T t,U u) {
        return new ImmutablePair<>(t,u);
    }
}
