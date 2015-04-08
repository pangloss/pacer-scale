package xn.graph.scale;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.pipes.AbstractPipe;
import com.tinkerpop.pipes.util.PipeHelper;
import com.tinkerpop.pipes.util.FastNoSuchElementException;
import java.math.BigDecimal;
import java.util.Iterator;

public class ScaleRangePipe extends AbstractPipe<Vertex, Vertex> {
    private static final IFn require = Clojure.var("clojure.core", "require");
    private static final IFn scaleRange = Clojure.var("xn.graph.scale.core", "scale-range");

    static {
        require.invoke(Clojure.read("xn.graph.scale.core"));
    }

    protected Iterator<Vertex> nextEnds;
    protected IFn nextRange;

    public ScaleRangePipe(long min, long max, BigDecimal step, BigDecimal offset, BigDecimal belowTolerance, BigDecimal aboveTolerance) {
        this.nextEnds = null;
        this.nextRange = (IFn)scaleRange.invoke(min, max, step, offset, belowTolerance, aboveTolerance);
    }

    protected Vertex processNextStart() {
        while (true) {
            if (this.nextEnds == null || !this.nextEnds.hasNext()) {
                this.nextEnds = (Iterator<Vertex>)nextRange.invoke(this.starts.next());
            } else {
              return this.nextEnds.next();
            }
        }
    }
}

