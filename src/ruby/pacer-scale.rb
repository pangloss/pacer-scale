require 'pacer-scale/version'
require 'pacer'
require 'lock_jar'
LockJar.lock File.join(File.dirname(__FILE__), '../../Jarfile'), lockfile: 'Jarfile.pacer-mcfly.lock'
LockJar.load 'Jarfile.pacer-mcfly.lock'
require 'xn_graph_scale.jar'

module PacerScale
  import java.math.BigDecimal
  import Java::clojure.java.api.Clojure
  import Java::xn.graph.scale.ScaleRangePipe

  class << self
    def generate_scale(graph, min, max, step)
      Clojure.var('clojure.core', 'require').invoke(['xn.graph.scale.core'])
      gs = Clojure.var('xn.graph.scale.core', 'generate-scale')
      v = gs.invoke(graph.blueprints_graph, min.to_i, max.to_i, BigDecimal.new(step.to_s))
      graph.vertex(v.getId, Value) if v
    end
  end

  module Root
    module Vertex
      def generate_scale(label, min, max, step)
        if out_e(label).first
          fail Pacer::ClientError.new("Scale root may not have multiple scales with the same label. (#{label})")
        end
        v = PacerScale.generate_scale(graph, min, max, step)
        add_edges_to(label, v)
        v[:root_edge] = label
        self
      end

      def find(label, val)
        out(label, Value).first.find val
      end

      def find_range(label, val, tolerance)
        out(label, Value).first.find_range val, tolerance
      end
    end
  end

  module Value
    module Vertex
      def value
        v = self[:scale_value]
        v.to_f if v
      end

      def min
        v = self[:scale_min]
        v.to_f if v
      end

      def max
        v = self[:scale_max]
        v.to_f if v
      end

      def step
        v = self[:scale_step]
        v.to_f if v
      end

      def root
        v = find(min)
        v.in(v[:root_edge], Root) if v
      end

      def next
        out(:next_1, Value)
      end

      def prev
        self.in(:next_1, Value)
      end

      def find(val)
        # calculate offset from current position...
        offset(val - value, 0).first
      end

      def offset_and_multiply(offset_by, mult, tol)
        _as_scale.offset_and_multiply(offset_by, mult, tol)
      end

      def multiply_and_offset(mult, offset_by, tol)
        _as_scale.multiply_and_offset(mult, offset_by, tol)
      end

      def offset(offset_by, tolerance)
        _as_scale.offset(offset_by, tolerance)
      end

      def below(offset_by = 0)
        _as_scale.below_traversal(offset_by)
      end

      def above(offset_by = 0)
        _as_scale.above_traversal(offset_by)
      end

      def find_range(val, tolerance)
        # calculate offset_by from current position...
        offset(val - value, tolerance)
      end

      protected

      def _as_scale
        chain_route(filter: PacerScale::RangeTraversal,
                    min: min, max: max, step: step)
      end
    end

    module Route
      def as_scale(min, max, step)
        chain_route(filter: PacerScale::RangeTraversal,
                    min: min, max: max, step: step)
      end
    end
  end

  module RangeTraversal
    attr_accessor :min, :max, :step, :offset_by, :above_tolerance, :below_tolerance,
      :multiply_before, :multiply_after

    def min=(val)
      @min = val
    end

    def max=(val)
      @max = val
    end

    def step=(val)
      @step = bigdec(val)
    end

    def offset_and_multiply(offset_by, mult, t_below, t_above = nil)
      t_above ||= t_below
      @offset_by = bigdec(offset_by || 0)
      @multiply_before = bigdec(1)
      @multiply_after = bigdec(mult || 0)
      @below_tolerance = bigdec(t_below)
      @above_tolerance = bigdec(t_above)
      v.add_extensions(extensions)
    end

    def multiply_and_offset(mult, offset_by, t_below, t_above = nil)
      t_above ||= t_below
      @offset_by = bigdec(offset_by || 0)
      @multiply_before = bigdec(mult || 0)
      @multiply_after = bigdec(1)
      @below_tolerance = bigdec(t_below)
      @above_tolerance = bigdec(t_above)
      v.add_extensions(extensions)
    end

    def offset(offset_by, t_below, t_above = nil)
      t_above ||= t_below
      @offset_by = bigdec(offset_by || 0)
      @multiply_before = bigdec(1)
      @multiply_after = bigdec(1)
      @below_tolerance = bigdec(t_below)
      @above_tolerance = bigdec(t_above)
      v.add_extensions(extensions)
    end

    def above(offset_by = 0)
      @offset_by = bigdec(offset_by)
      @multiply_before = bigdec(1)
      @multiply_after = bigdec(1)
      @below_tolerance = bigdec(0)
      @above_tolerance = nil
      v.add_extensions(extensions)
    end

    def below(offset_by = 0)
      @offset_by = bigdec(offset_by)
      @multiply_before = bigdec(1)
      @multiply_after = bigdec(1)
      @below_tolerance = nil
      @above_tolerance = bigdec(0)
      v.add_extensions(extensions)
    end

    protected

    def bigdec(n)
      BigDecimal.new(n.to_s) if n
    end

    def inspect_string
      if above_tolerance == below_tolerance
        "offset(#{ multiply_before }, #{ offset_by }, #{multiply_after}, #{above_tolerance})"
      else
        "offset(#{ multiply_before }, #{ offset_by }, #{multiply_after}, #{below_tolerance.inspect}, #{above_tolerance.inspect})"
      end
    end

    def attach_pipe(end_pipe)
      pipe = ScaleRangePipe.new(min, max, step,
                                (multiply_before || bigdec(1)),
                                (offset_by || bigdec(0)),
                                (multiply_after || bigdec(1)),
                                below_tolerance, above_tolerance)
      pipe.set_starts end_pipe if end_pipe
      pipe
    end
  end
end
