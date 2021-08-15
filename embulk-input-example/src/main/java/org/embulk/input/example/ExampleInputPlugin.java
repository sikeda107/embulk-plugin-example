package org.embulk.input.example;

import java.util.List;
import java.util.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExampleInputPlugin.
 */
public class ExampleInputPlugin implements InputPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExampleInputPlugin.class);

  /**
   * PluginTask.
   */
  public interface PluginTask extends Task {

    @Config("hostname")
    @ConfigDefault("null")
    Optional<String> getHostname();

    @ConfigInject
    BufferAllocator getBufferAllocator();
  }

  @Override
  public ConfigDiff transaction(ConfigSource config, InputPlugin.Control control) {
    final PluginTask task = config.loadConfig(PluginTask.class);
    Schema schema = Schema.builder()
        .add("file", Types.STRING)
        .add("hostname", Types.STRING)
        .add("col0", Types.LONG)
        .add("col1", Types.DOUBLE)
        .build();
    int taskCount = 1;  // number of run() method calls

    return resume(task.dump(), schema, taskCount, control);
  }

  @Override
  public ConfigDiff resume(TaskSource taskSource, Schema schema, int taskCount,
      InputPlugin.Control control) {
    control.run(taskSource, schema, taskCount);
    return Exec.newConfigDiff();
  }

  @Override
  public void cleanup(TaskSource taskSource, Schema schema, int taskCount,
      List<TaskReport> successTaskReports) {
  }

  @Override
  public TaskReport run(TaskSource taskSource, Schema schema, int taskIndex, PageOutput output) {
    final PluginTask task = taskSource.loadTask(PluginTask.class);
    final BufferAllocator allocator = task.getBufferAllocator();
    final PageBuilder pageBuilder = new PageBuilder(allocator, schema, output);
    for (int i = 10; i > 0; i--) {
      pageBuilder.setString(schema.getColumn(0), "file" + i);
      task.getHostname().ifPresent(v -> {
        pageBuilder.setString(1, v);
      });
      pageBuilder.setLong(2, i);
      pageBuilder.setDouble(3, 10.0);
      pageBuilder.addRecord();
    }
    pageBuilder.finish();
    return Exec.newTaskReport();
  }

  @Override
  public ConfigDiff guess(ConfigSource config) {
    return Exec.newConfigDiff();
  }
}
