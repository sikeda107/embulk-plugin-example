Embulk::JavaPlugin.register_filter(
  "example", "org.embulk.filter.example.ExampleFilterPlugin",
  File.expand_path('../../../../classpath', __FILE__))
