/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.sql;

import com.google.common.collect.ImmutableList;
import org.apache.drill.BaseTestQuery;
import org.apache.drill.TestBuilder;
import org.junit.Test;

import java.util.List;

/**
 * Contains tests for
 * -- InformationSchema
 * -- Queries on InformationSchema such as SHOW TABLES, SHOW SCHEMAS or DESCRIBE table
 * -- USE schema
 * -- SHOW FILES
 */
public class TestInfoSchema extends BaseTestQuery {
  @Test
  public void selectFromAllTables() throws Exception{
    test("select * from INFORMATION_SCHEMA.SCHEMATA");
    test("select * from INFORMATION_SCHEMA.CATALOGS");
    test("select * from INFORMATION_SCHEMA.VIEWS");
    test("select * from INFORMATION_SCHEMA.`TABLES`");
    test("select * from INFORMATION_SCHEMA.COLUMNS");
  }

  @Test
  public void showTablesFromDb() throws Exception{
    final List<String[]> expected =
        ImmutableList.of(
            new String[] { "INFORMATION_SCHEMA", "VIEWS" },
            new String[] { "INFORMATION_SCHEMA", "COLUMNS" },
            new String[] { "INFORMATION_SCHEMA", "TABLES" },
            new String[] { "INFORMATION_SCHEMA", "CATALOGS" },
            new String[] { "INFORMATION_SCHEMA", "SCHEMATA" }
        );

    final TestBuilder t1 = testBuilder()
        .sqlQuery("SHOW TABLES FROM INFORMATION_SCHEMA")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME");
    for(String[] expectedRow : expected) {
      t1.baselineValues(expectedRow);
    }
    t1.go();

    final TestBuilder t2 = testBuilder()
        .sqlQuery("SHOW TABLES IN INFORMATION_SCHEMA")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME");
    for(String[] expectedRow : expected) {
      t2.baselineValues(expectedRow);
    }
    t2.go();
  }

  @Test
  public void showTablesFromDbWhere() throws Exception{
    testBuilder()
        .sqlQuery("SHOW TABLES FROM INFORMATION_SCHEMA WHERE TABLE_NAME='VIEWS'")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
        .baselineValues("INFORMATION_SCHEMA", "VIEWS")
        .go();
  }

  @Test
  public void showTablesLike() throws Exception{
    testBuilder()
        .sqlQuery("SHOW TABLES LIKE '%CH%'")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE INFORMATION_SCHEMA")
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
        .baselineValues("INFORMATION_SCHEMA", "SCHEMATA")
        .go();
  }

  @Test
  public void showDatabases() throws Exception{
    final List<String[]> expected =
        ImmutableList.of(
            new String[] { "dfs.default" },
            new String[] { "dfs.root" },
            new String[] { "dfs.tmp" },
            new String[] { "cp.default" },
            new String[] { "sys" },
            new String[] { "dfs_test.home" },
            new String[] { "dfs_test.default" },
            new String[] { "dfs_test.tmp" },
            new String[] { "INFORMATION_SCHEMA" }
        );

    final TestBuilder t1 = testBuilder()
        .sqlQuery("SHOW DATABASES")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME");
    for(String[] expectedRow : expected) {
      t1.baselineValues(expectedRow);
    }
    t1.go();

    final TestBuilder t2 = testBuilder()
        .sqlQuery("SHOW SCHEMAS")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME");
    for(String[] expectedRow : expected) {
      t2.baselineValues(expectedRow);
    }
    t2.go();
  }

  @Test
  public void showDatabasesWhere() throws Exception{
    testBuilder()
        .sqlQuery("SHOW DATABASES WHERE SCHEMA_NAME='dfs_test.tmp'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues("dfs_test.tmp")
        .go();
  }

  @Test
  public void showDatabasesLike() throws Exception{
    testBuilder()
        .sqlQuery("SHOW DATABASES LIKE '%y%'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues("sys")
        .go();
  }

  @Test
  public void describeTable() throws Exception{
    testBuilder()
        .sqlQuery("DESCRIBE CATALOGS")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE INFORMATION_SCHEMA")
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("CATALOG_NAME", "VARCHAR", "NO")
        .baselineValues("CATALOG_DESCRIPTION", "VARCHAR", "NO")
        .baselineValues("CATALOG_CONNECT", "VARCHAR", "NO")
        .go();
  }

  @Test
  public void describeTableWithSchema() throws Exception{
    testBuilder()
        .sqlQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES`")
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("TABLE_CATALOG", "VARCHAR", "NO")
        .baselineValues("TABLE_SCHEMA", "VARCHAR", "NO")
        .baselineValues("TABLE_NAME", "VARCHAR", "NO")
        .baselineValues("TABLE_TYPE", "VARCHAR", "NO")
        .go();
  }

  @Test
  public void describeWhenSameTableNameExistsInMultipleSchemas() throws Exception{
    try {
      test("USE dfs_test.tmp");
      test("CREATE OR REPLACE VIEW `TABLES` AS SELECT full_name FROM cp.`employee.json`");

      testBuilder()
          .sqlQuery("DESCRIBE `TABLES`")
          .unOrdered()
          .optionSettingQueriesForTestQuery("USE dfs_test.tmp")
          .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
          .baselineValues("full_name", "ANY", "YES")
          .go();

      testBuilder()
          .sqlQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES`")
          .unOrdered()
          .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
          .baselineValues("TABLE_CATALOG", "VARCHAR", "NO")
          .baselineValues("TABLE_SCHEMA", "VARCHAR", "NO")
          .baselineValues("TABLE_NAME", "VARCHAR", "NO")
          .baselineValues("TABLE_TYPE", "VARCHAR", "NO")
          .go();
    } finally {
      test("DROP VIEW dfs_test.tmp.`TABLES`");
    }
  }

  @Test
  public void describeTableWithColumnName() throws Exception{
    testBuilder()
        .sqlQuery("DESCRIBE `TABLES` TABLE_CATALOG")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE INFORMATION_SCHEMA")
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("TABLE_CATALOG", "VARCHAR", "NO")
        .go();
  }

  @Test
  public void describeTableWithSchemaAndColumnName() throws Exception{
    testBuilder()
        .sqlQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES` TABLE_CATALOG")
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("TABLE_CATALOG", "VARCHAR", "NO")
        .go();
  }

  @Test
  public void describeTableWithColQualifier() throws Exception{
    testBuilder()
        .sqlQuery("DESCRIBE COLUMNS 'TABLE%'")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE INFORMATION_SCHEMA")
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("TABLE_CATALOG", "VARCHAR", "NO")
        .baselineValues("TABLE_SCHEMA", "VARCHAR", "NO")
        .baselineValues("TABLE_NAME", "VARCHAR", "NO")
        .go();
  }

  @Test
  public void describeTableWithSchemaAndColQualifier() throws Exception{
    testBuilder()
        .sqlQuery("DESCRIBE INFORMATION_SCHEMA.SCHEMATA 'SCHEMA%'")
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("SCHEMA_NAME", "VARCHAR", "NO")
        .baselineValues("SCHEMA_OWNER", "VARCHAR", "NO")
        .go();
  }

  @Test
  public void defaultSchemaDfs() throws Exception{
    testBuilder()
        .sqlQuery("SELECT R_REGIONKEY FROM `[WORKING_PATH]/../../sample-data/region.parquet` LIMIT 1")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE dfs_test")
        .baselineColumns("R_REGIONKEY")
        .baselineValues(0L)
        .go();
  }

  @Test
  public void defaultSchemaClasspath() throws Exception{
    testBuilder()
        .sqlQuery("SELECT full_name FROM `employee.json` LIMIT 1")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE cp")
        .baselineColumns("full_name")
        .baselineValues("Sheri Nowmer")
        .go();
  }


  @Test
  public void queryFromNonDefaultSchema() throws Exception{
    testBuilder()
        .sqlQuery("SELECT full_name FROM cp.`employee.json` LIMIT 1")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE dfs_test")
        .baselineColumns("full_name")
        .baselineValues("Sheri Nowmer")
        .go();
  }

  @Test
  public void useSchema() throws Exception{
    testBuilder()
        .sqlQuery("USE dfs_test.`default`")
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Default schema changed to 'dfs_test.default'")
        .go();
  }

  @Test
  public void useSchemaNegative() throws Exception{
    testBuilder()
        .sqlQuery("USE invalid.schema")
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, "Failed to change default schema to 'invalid.schema'")
        .go();
  }

  // Tests using backticks around the complete schema path
  // select * from `dfs_test.tmp`.`/tmp/nation.parquet`;
  @Test
  public void completeSchemaRef1() throws Exception {
    test("SELECT * FROM `cp.default`.`employee.json` limit 2");
  }

  @Test
  public void showFiles() throws Exception {
    test("show files from dfs_test.`/tmp`");
    test("show files from `dfs_test.default`.`/tmp`");
  }

  @Test
  public void showFilesWithDefaultSchema() throws Exception{
    test("USE dfs_test.`default`");
    test("SHOW FILES FROM `/tmp`");
  }
}
