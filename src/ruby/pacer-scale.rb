require 'pacer-scale/version'
require 'pacer'
require 'lock_jar'
LockJar.lock File.join(File.dirname(__FILE__), '../../Jarfile'), lockfile: 'Jarfile.pacer-mcfly.lock'
LockJar.load 'Jarfile.pacer-mcfly.lock'
require 'xn_graph_scale.jar'

module Pacer
  module Routes
    module RouteOperations
      #g.v(SomeUnit).as_scale(label: 'next_mass', min: 100, max: 1500, step: 0.005)
      def as_scale(opts = {})
        chain_route(opts.merge(filter: ScaleValue::Route))
      end
    end
  end
end

module PacerScale
  import java.math.BigDecimal
  import Java::xn.graph.scale.ScaleRangePipe

  class << self
    def generate_scale(label, min, max, step)
      Clojure.var('clojure.core', 'require').invoke('xn.graph.scale.core')
      gs = Clojure.var('xn.graph.scale.core', 'generate-scale')
      v = gs.invoke(graph.blueprints_graph, min.to_i, max.to_i, BigDecimal.new(step.to_s))
      nil
    end
  end


  module ScaleValue
    module Route
      attr_accessor :min, :max, :step, :offset, :above_tolerance, :below_tolerance

      def range(offset, tolerance)
        @offset = offset
        @below_tolerance = tolerance
        @above_tolerance = tolerance
        self
      end

      def above(offset)
        @offset = offset
        @below_tolerance = 0
        @above_tolerance = nil
        self
      end

      def below(offset)
        @offset = offset
        @below_tolerance = nil
        @above_tolerance = 0
        self
      end

      protected

      def bigdec(n)
        BigDecimal.new(n.to_s) if n
      end

      def attach_pipe(end_pipe)
        pipe = ScaleRangePipe.new(min, max, bigdec(step), bigdec(offset), bigdec(below_tolerance), bigdec(above_tolerance))
        pipe.set_starts end_pipe if end_pipe
        pipe
      end
    end
  end
end
