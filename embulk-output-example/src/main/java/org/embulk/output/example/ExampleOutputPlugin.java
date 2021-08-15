package org.embulk.output.example;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.Exec;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.TransactionalPageOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleOutputPlugin implements OutputPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExampleOutputPlugin.class);
  private int recordsCnt = 0;

  ExampleOutputPlugin() {
    LOGGER.info("ExampleOutputPlugin : thread {} object {}...", Thread.currentThread().getId(),
        System.identityHashCode(this));
  }

  @Override
  public ConfigDiff transaction(ConfigSource config, Schema schema, int taskCount,
      Control control) {
    LOGGER.info("ExampleOutputPlugin(transaction) : thread {} object {}...",
        Thread.currentThread().getId(),
        System.identityHashCode(this));
    PluginTask task = config.loadConfig(PluginTask.class);

    // retryable (idempotent) output:
    // return resume(task.dump(), schema, taskCount, control);

    // non-retryable (non-idempotent) output:
    List<TaskReport> taskReports = control.run(task.dump());
    LOGGER.info("Example output finished. Commit reports = {}", taskReports);
    return Exec.newConfigDiff();
  }

  @Override
  public ConfigDiff resume(TaskSource taskSource, Schema schema, int taskCount,
      OutputPlugin.Control control) {
    throw new UnsupportedOperationException("example output plugin does not support resuming");
  }

  @Override
  public void cleanup(TaskSource taskSource, Schema schema, int taskCount,
      List<TaskReport> successTaskReports) {
  }

  @Override
  public TransactionalPageOutput open(TaskSource taskSource, Schema schema, int taskIndex) {
    PluginTask task = taskSource.loadTask(PluginTask.class);
    LOGGER.info("ExampleOutputPlugin(open) : thread {} object {}...",
        Thread.currentThread().getId(),
        System.identityHashCode(this));
    // Write your code here :)
    return new TransactionalPageOutput() {
      private final PageReader pageReader = new PageReader(schema);
      private final Gson gson = new Gson();

      private void printRecord(Map map) {
        System.out.println(task.getMessage() + " : " + gson.toJson(map));
      }

      @Override
      public void add(Page page) {
        pageReader.setPage(page);
        while (pageReader.nextRecord()) {
          HashMap<String, Object> hashMap = new HashMap<>();
          schema.visitColumns(new ColumnVisitor() {
            @Override
            public void booleanColumn(Column column) {
              if (pageReader.isNull(column)) {
                hashMap.put(column.getName(), null);
                return;
              }
              hashMap.put(column.getName(), pageReader.getBoolean(column));
            }

            @Override
            public void longColumn(Column column) {
              if (pageReader.isNull(column)) {
                hashMap.put(column.getName(), null);
                return;
              }
              hashMap.put(column.getName(), pageReader.getLong(column));
            }

            @Override
            public void doubleColumn(Column column) {
              if (pageReader.isNull(column)) {
                hashMap.put(column.getName(), null);
                return;
              }
              hashMap.put(column.getName(), pageReader.getDouble(column));
            }

            @Override
            public void stringColumn(Column column) {
              if (pageReader.isNull(column)) {
                hashMap.put(column.getName(), null);
                return;
              }
              hashMap.put(column.getName(), pageReader.getString(column));
            }

            @Override
            public void timestampColumn(Column column) {
              if (pageReader.isNull(column)) {
                hashMap.put(column.getName(), null);
                return;
              }
              hashMap.put(column.getName(), pageReader.getTimestamp(column));
            }

            @Override
            public void jsonColumn(Column column) {
              if (pageReader.isNull(column)) {
                hashMap.put(column.getName(), null);
                return;
              }
              hashMap.put(column.getName(), pageReader.getJson(column));
            }
          });
          printRecord(hashMap);
          recordsCnt++;
        }
      }

      @Override
      public void finish() {
        System.out.flush();
      }

      @Override
      public void close() {
        pageReader.close();
      }

      @Override
      public void abort() {
      }

      @Override
      public TaskReport commit() {
        TaskReport taskReport = Exec.newTaskReport();
        taskReport.set("records", recordsCnt);
        return taskReport;
      }
    };
  }

  public interface PluginTask extends Task {

    @Config("message")
    @ConfigDefault("\"record\"")
    String getMessage();

  }
}
