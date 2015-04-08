source "https://rubygems.org/"

gemspec

unless ENV['CONFIG_FILE'] # ie. deployment
  # Gemfile-custom is .gitignored, but evaluated here so you can add
  # whatever dev tools you like to use to your local environment.
  eval File.read('Gemfile-custom') if File.exist?('Gemfile-custom')
end

