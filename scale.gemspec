# -*- encoding: utf-8 -*-
$:.push File.expand_path("../src/ruby", __FILE__)
require "pacer-scale/version"

Gem::Specification.new do |s|
  s.name        = "pacer-scale"
  s.version     = PacerScale::VERSION
  s.platform    = 'java'
  s.authors     = ["Darrick Wiebe"]
  s.email       = ["dw@xnlogic.com"]
  s.homepage    = "http://xnlogic.com"
  s.summary     = %q{Scale plugin for Pacer}
  s.description = "Generate and produce ranges in scale data structures in the graph"

  s.add_dependency "lock_jar", "~> 0.10.2"
  s.add_development_dependency 'rspec'
  s.add_development_dependency 'rspec-its'
  s.add_development_dependency 'rspec-core',         '>=2.13.1'
  s.add_development_dependency 'rspec-expectations', '>=2.13.0'
  s.add_development_dependency 'rake'
  s.add_development_dependency 'builder'
  s.add_development_dependency 'autotest-standalone'
  s.add_development_dependency 'nokogiri'
  s.add_development_dependency 'xn_gem_release_tasks', '>= 0.1.21'

  s.rubyforge_project = "pacer-scale"

  s.files         = `git ls-files -- *.rb`.split("\n") + ['Jarfile']
  local_jar_file = "src/ruby/xn_graph_scale.jar"
  if File.exist? local_jar_file
    s.files << local_jar_file
  end
  s.test_files    = `git ls-files -- {test,spec,features}/*`.split("\n")
  s.require_paths = ["src/ruby"]
end
