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
package org.apache.iceberg;

import static org.apache.iceberg.TestHelpers.ALL_VERSIONS;
import static org.apache.iceberg.types.Types.NestedField.required;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.apache.avro.generic.GenericData;
import org.apache.iceberg.avro.Avro;
import org.apache.iceberg.avro.RandomAvroData;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.inmemory.InMemoryOutputFile;
import org.apache.iceberg.io.FileAppender;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(ParameterizedTestExtension.class)
public class TestScansAndSchemaEvolution {
  private static final Schema SCHEMA =
      new Schema(
          required(1, "id", Types.LongType.get()),
          required(2, "data", Types.StringType.get()),
          required(3, "part", Types.StringType.get()));

  private static final PartitionSpec SPEC =
      PartitionSpec.builderFor(SCHEMA).identity("part").build();

  @Parameters(name = "formatVersion = {0}")
  protected static List<Integer> formatVersions() {
    return ALL_VERSIONS;
  }

  @Parameter private int formatVersion;

  @TempDir private Path temp;

  private DataFile createDataFile(String partValue) throws IOException {
    List<GenericData.Record> expected = RandomAvroData.generate(SCHEMA, 100, 0L);

    OutputFile dataFile =
        new InMemoryOutputFile(FileFormat.AVRO.addExtension(UUID.randomUUID().toString()));
    try (FileAppender<GenericData.Record> writer =
        Avro.write(dataFile).schema(SCHEMA).named("test").build()) {
      for (GenericData.Record rec : expected) {
        rec.put("part", partValue); // create just one partition
        writer.add(rec);
      }
    }

    PartitionData partition = new PartitionData(SPEC.partitionType());
    partition.set(0, partValue);
    return DataFiles.builder(SPEC)
        .withInputFile(dataFile.toInputFile())
        .withPartition(partition)
        .withRecordCount(100)
        .build();
  }

  @AfterEach
  public void cleanupTables() {
    TestTables.clearTables();
  }

  @TestTemplate
  public void testPartitionSourceRename() throws IOException {
    File location = Files.createTempDirectory(temp, "junit").toFile();
    assertThat(location.delete()).isTrue(); // should be created by table create

    Table table = TestTables.create(location, "test", SCHEMA, SPEC, formatVersion);

    DataFile fileOne = createDataFile("one");
    DataFile fileTwo = createDataFile("two");

    table.newAppend().appendFile(fileOne).appendFile(fileTwo).commit();

    List<FileScanTask> tasks =
        Lists.newArrayList(table.newScan().filter(Expressions.equal("part", "one")).planFiles());

    assertThat(tasks).hasSize(1);

    table.updateSchema().renameColumn("part", "p").commit();

    // plan the scan using the new name in a filter
    tasks = Lists.newArrayList(table.newScan().filter(Expressions.equal("p", "one")).planFiles());

    assertThat(tasks).hasSize(1);
  }
}
