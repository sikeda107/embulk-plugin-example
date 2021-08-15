# embulk-plugin-example

- [embulkサンプルプラグインの実行 - Qiita](https://qiita.com/hiroysato/items/2e4b7e05cdafec138046)
- https://github.com/embulk/embulk/blob/master/embulk-core/src/main/resources/org/embulk/jruby/bundler/template/embulk/input/example.rb
- https://github.com/embulk/embulk/blob/master/embulk-core/src/main/resources/org/embulk/jruby/bundler/template/embulk/filter/example.rb
- https://github.com/embulk/embulk/blob/master/embulk-core/src/main/resources/org/embulk/jruby/bundler/template/embulk/output/example.rb

```bash
# ビルド
cd embulk-input-example && ./gradlew clean package && cd ..
cd embulk-filter-example && ./gradlew clean package && cd ..
cd embulk-output-example && ./gradlew clean package && cd ..
# 実行方法
embulk run -L ./embulk-input-example -L ./embulk-filter-example -L ./embulk-output-example config.yml
```