task "publish:fig", :v do |_, args|
  %x"lein jar"
  %x"fig --publish expectations/#{args[:v]}"
end

task "publish:clojars" do |_, args|
  %x"lein jar"
  %x"lein pom"
  %x"scp pom.xml expectations.jar clojars@clojars.org:"
end
