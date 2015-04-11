# pacer-scale

A Pacer plugin to support timeline or generic data scales with very fast data retrieval.

Associate your data to the scale value vertices as you would any other vertex in your graph.
This library looks after finding values and traversing along the scale with maximum efficiency,
then allows you to perform the same traversals as with any other graph data to find the information
attached at the points in the scale or timeline that interest you.

## Usage

Creating a range is easy. In fact you can create many ranges in a single graph.

```ruby
  require 'pacer-scale'

  # Create a regular vertex to serve as the root of the scale
  v = graph.create_vertex

  # Wrap the vertex in the Root extension (this allows you to treate any vertex as a root).
  root = v.as(PacerScale::Root)

  # Generate a scale from -500 to 2500 in increments of 0.0125.
  # You must give it a name because one root may have multiple data scales attached to it.
  root.generate_scale 'my_data_range', -500, 2500, 0.0125

  # In TinkerGraph, this generates a scale with 240,000 data points in approx 1 second.
```

Once you have created a range, you can find values in the range from the root with either #find
which gets the best approximation to the given value, or #find_range, which produces a route to
elements with a tolerance around the desired value.

```ruby
  root.find('my_data_range', 425.0332)
  # => #<V[2140696]>

  # We can see the appoximation here in "scale_value":
  root.find('my_data_range', 425.0332).properties
  # => {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0375", "scale_step"=>"0.0125"}


  root.find_range('my_data_range', 425.0332, 0.05)
  #<V[2140685]> #<V[2140687]> #<V[2140692]> #<V[2140694]> #<V[2140696]> #<V[2140698]> 
  #<V[2140700]> #<V[2140702]> #<V[2140704]>
  # Total: 9
  #  => #<offset(925.0332000000001, 0.05) -> V>
  root.find_range('my_data_range', 425.0332, 0.05).properties
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"424.9875", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0000", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0125", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0250", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0375", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0500", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0625", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0750", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0875", "scale_step"=>"0.0125"}
  # Total: 9
  #  => #<offset(925.0332000000001, 0.05) -> V -> Hash-Map>
```

That is mildly interesting, but the best part is that you can work with a range starting from
any value in the range as follows. Here we look up the vertex with the value 425.0375 directly,
based on the id we found in the query above:

```ruby
  value = graph.vertex(2140696).as(PacerScale::Value)
  # => #<V[2140696]>
  value.properties
  # => {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"425.0375", "scale_step"=>"0.0125"}
  

  # Now, find the vertex representing 100.0:

  value.find(100)
  # => #<V[2085798]>
  value.find(100).properties
  # => {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"100.0000", "scale_step"=>"0.0125"}

  # Or find a range of vertices relative to the current vertex:

  value.offset(-100, 0.01)
  # #<V[2123807]> #<V[2123809]> #<V[2123811]>
  # Total: 3
  #  => #<offset(-100, 0.01) -> V>
  value.offset(-100, 0.01).properties
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"325.0250", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"325.0375", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"325.0500", "scale_step"=>"0.0125"}
  # Total: 3
  #  => #<offset(-100, 0.01) -> V -> Hash-Map>
```

To calculate the traversal from a single value, its scale information properties are used. But an
offset traversal can also be done from a collection of scale values, in which case it will find
the offset for each vertex, but you must provide the scale information in the query:

```ruby
  # First, let's grab a couple of arbitrary values off of the scale. We might normally get to
  # these values by following an edge from some other vertex...

  values = [2123763, 2157570].id_to_element_route(based_on: graph.v(PacerScale::Value))
  # #<V[2123763]> #<V[2157570]>
  # Total: 2
  # => #<Obj 2 ids -> lookup -> is_not(nil)>
  values.properties
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"324.7875", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"524.9625", "scale_step"=>"0.0125"}
  # Total: 2
  #  => #<Obj 2 ids -> lookup -> is_not(nil) -> Hash-Map>
    
  # Now we can traverse from these values to find the values at some offset from them. Note that
  # we must provide the scale info here with #as_scale:

  values.as_scale(-500, 2500, 0.0125).offset(500, 0.0125)
  # #<V[2208206]> #<V[2208208]> #<V[2208210]> #<V[2242013]> #<V[2242015]> #<V[2242017]>
  # Total: 6
  #  => #<Obj 2 ids -> lookup -> is_not(nil) -> offset(500, 0.0125) -> V>
  values.as_scale(-500, 2500, 0.0125).offset(500, 0.0125).properties
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"824.7750", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"824.7875", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"824.8000", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"1024.9500", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"1024.9625", "scale_step"=>"0.0125"}
  # {"scale_max"=>"2500", "scale_min"=>"-500", "scale_value"=>"1024.9750", "scale_step"=>"0.0125"}
  # Total: 6
  #  => #<Obj 2 ids -> lookup -> is_not(nil) -> offset(500, 0.0125) -> V -> Hash-Map>
```
  

## License

Copyright © 2015 XN Logic

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
