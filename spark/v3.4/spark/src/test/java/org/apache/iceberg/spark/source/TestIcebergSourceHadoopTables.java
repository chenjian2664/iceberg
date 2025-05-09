/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg.spark.source;

import java.io.File;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.hadoop.HadoopTables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

public class TestIcebergSourceHadoopTables extends TestIcebergSourceTablesBase {

  private static final HadoopTables TABLES = new HadoopTables(new Configuration());
  @TempDir private File tableDir;
  String tableLocation = null;

  @BeforeEach
  public void setupTable() {
    this.tableLocation = tableDir.toURI().toString();
  }

  @Override
  public Table createTable(
      TableIdentifier ident, Schema schema, PartitionSpec spec, Map<String, String> properties) {
    return TABLES.create(schema, spec, properties, tableLocation);
  }

  @Override
  public void dropTable(TableIdentifier ident) {
    TABLES.dropTable(tableLocation);
  }

  @Override
  public Table loadTable(TableIdentifier ident, String entriesSuffix) {
    return TABLES.load(loadLocation(ident, entriesSuffix));
  }

  @Override
  public String loadLocation(TableIdentifier ident, String entriesSuffix) {
    return String.format("%s#%s", loadLocation(ident), entriesSuffix);
  }

  @Override
  public String loadLocation(TableIdentifier ident) {
    return tableLocation;
  }
}
