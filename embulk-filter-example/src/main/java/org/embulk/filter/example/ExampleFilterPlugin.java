package org.embulk.filter.example;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;

/**
 * ExampleFilterPlugin.
 */
public class ExampleFilterPlugin implements FilterPlugin {

  /**
   * PluginTask.
   */
  public interface PluginTask extends Task {

    @Config("key")
    @ConfigDefault("\"filter_key\"")
    String getKey();

    @Config("value")
    @ConfigDefault("\"filter_value\"")
    String getValue();

    @ConfigInject
    BufferAllocator getBufferAllocator();
  }

  @Override
  public void transaction(ConfigSource config, Schema inputSchema, FilterPlugin.Control control) {
    PluginTask task = config.loadConfig(PluginTask.class);
    List<Column> outputColumns = new ArrayList<>(inputSchema.getColumns());
    int idx = inputSchema.getColumnCount();
    outputColumns.add(new Column(idx, task.getKey(), Types.STRING));
    control.run(task.dump(), new Schema(outputColumns));
  }

  @Override
  public PageOutput open(TaskSource taskSource, Schema inputSchema,
      Schema outputSchema, PageOutput output) {
    PluginTask task = taskSource.loadTask(PluginTask.class);

    // Write your code here :)
    final BufferAllocator allocator = task.getBufferAllocator();
    return new PageOutput() {
      private final PageReader pageReader = new PageReader(inputSchema);
      private final PageBuilder pageBuilder = new PageBuilder(allocator, outputSchema, output);

      @Override
      public void finish() {
        pageBuilder.finish();
      }

      @Override
      public void close() {
        pageBuilder.close();
      }

      @Override
      public void add(Page page) {
        pageReader.setPage(page);
        while (pageReader.nextRecord()) {
          inputSchema.visitColumns(new ColumnVisitor() {
            @Override
            public void booleanColumn(Column column) {
              if (pageReader.isNull(column)) {
                pageBuilder.setNull(column);
              } else {
                pageBuilder.setBoolean(column, pageReader.getBoolean(column));
              }
            }

            @Override
            public void longColumn(Column column) {
              if (pageReader.isNull(column)) {
                pageBuilder.setNull(column);
              } else {
                pageBuilder.setLong(column, pageReader.getLong(column));
              }
            }

            @Override
            public void doubleColumn(Column column) {
              if (pageReader.isNull(column)) {
                pageBuilder.setNull(column);
              } else {
                pageBuilder.setDouble(column, pageReader.getDouble(column));
              }
            }

            @Override
            public void stringColumn(Column column) {
              if (pageReader.isNull(column)) {
                pageBuilder.setNull(column);
              } else {
                pageBuilder.setString(column, pageReader.getString(column));
              }
            }

            @Override
            public void timestampColumn(Column column) {
              if (pageReader.isNull(column)) {
                pageBuilder.setNull(column);
              } else {
                pageBuilder.setTimestamp(column, pageReader.getTimestamp(column));
              }
            }

            @Override
            public void jsonColumn(Column column) {
              if (pageReader.isNull(column)) {
                pageBuilder.setNull(column);
              } else {
                pageBuilder.setJson(column, pageReader.getJson(column));
              }
            }
          });
          Column column = outputSchema.getColumn(inputSchema.getColumnCount());
          pageBuilder.setString(column, task.getValue());
          pageBuilder.addRecord();
        }
      }
    };
  }
}
