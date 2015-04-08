require 'bundler'
Bundler::GemHelper.install_tasks

def target_jar
  "src/ruby/xn_graph_scale.jar"
end

task :spec => :jar do
  sh 'lein test'
  puts "NOTE: to run the Ruby rspec test suite, run:"
  puts "  rake jar && rspec"
end

XNGemReleaseTasks.setup PacerScale, 'src/ruby/pacer-scale/version.rb'

task :check_version do
  unless File.read('project.clj') =~ /com.xnlogic\/\S+\s+"#{PacerScale::VERSION}"/
    fail "project.clj and version.rb do not have the same version"
  end
end

file 'pom.xml' => FileList['project.clj', 'src/ruby/pacer-scale/version.rb'] do
  Rake::Task['install_lein'].invoke
  sh "lein with-profile compiled pom"
end

file target_jar => FileList['project.clj', 'src/clojure/**/*.clj', 'src/java/**/*.java'] do
  Rake::Task['install_lein'].invoke
  sh "lein do clean, with-profile compiled jar"
  build_jar = "target/mcfly-#{PacerScale::VERSION}.jar"
  FileUtils.cp build_jar, target_jar
end

file 'Jarfile' => 'pom.xml' do
  puts "Rebuilding Jarfile"
  require 'nokogiri'
  xml = Nokogiri::XML(File.read('pom.xml'))
  deps = xml.first_element_child.children.at('dependencies').element_children
  deps = deps.reject do |dep|
    dep.at('groupId').text == 'com.datomic'
  end
  deps = deps.map do |dep|
    "#{dep.at('groupId').text}:#{dep.at('artifactId').text}:#{dep.at('version').text}"
  end
  repos = xml.first_element_child.children.at('repositories').element_children
  repos = repos.map do |repo|
    repo.at('url').text
  end
  File.open 'Jarfile', 'w' do |f|
    repos.each do |repo|
      f.puts "repository \"#{repo}\""
    end
    deps.each do |dep|
      f.puts "jar \"#{dep}\""
    end
    f.puts "exclude 'slf4j-nop'"
  end
end

desc "Compile Jarfile"
task :jarfile => 'Jarfile'

desc "Compile jar"
task :jar => target_jar

task :build => [:check_version, :jarfile, :jar]
task :install => [:check_version, :jarfile, :jar]

task :after_release => :update_lein_version do
  # prevent unneeded rebuilds
  touch target_jar
end

desc "Remove compiled JAR to enable recompilation"
task :clean do
  sh "rm #{target_jar}" if File.exists?( target_jar )
end

