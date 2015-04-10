package xn.graph.scale;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.ISeq;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.AbstractPipe;
import java.math.BigDecimal;

public class ScaleRangePipe extends AbstractPipe<Vertex, Vertex> {
    private static final IFn require = Clojure.var("clojure.core", "require");
    private static final IFn scaleRange = Clojure.var("xn.graph.scale.core", "scale-range");
    private static final IFn first = Clojure.var("clojure.core", "first");
    private static final IFn rest = Clojure.var("clojure.core", "rest");

    static {
        require.invoke(Clojure.read("xn.graph.scale.core"));
    }

    protected ISeq range;
    protected IFn nextRange;

    public ScaleRangePipe(long min, long max, BigDecimal step, BigDecimal offset, BigDecimal belowTolerance, BigDecimal aboveTolerance) {
        this.range = null;
        this.nextRange = (IFn)scaleRange.invoke(min, max, step, offset, belowTolerance, aboveTolerance);
    }

    protected Vertex processNextStart() {
        while (true) {
            Vertex v = (Vertex)first.invoke(range);
            if (v == null) {
                this.range = (ISeq)nextRange.invoke(this.starts.next());
            } else {
                this.range = (ISeq)rest.invoke(this.range);
                return v;
            }
        }
    }
}

