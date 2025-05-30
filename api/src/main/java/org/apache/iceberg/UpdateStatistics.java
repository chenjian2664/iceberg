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

import java.util.List;

/** API for updating statistics files in a table. */
public interface UpdateStatistics extends PendingUpdate<List<StatisticsFile>> {
  /**
   * Set the table's statistics file for given snapshot, replacing the previous statistics file for
   * the snapshot if any exists.
   *
   * @return this for method chaining
   * @deprecated since 1.8.0, will be removed in 2.0.0, use {@link #setStatistics(StatisticsFile)}.
   */
  @Deprecated
  default UpdateStatistics setStatistics(long snapshotId, StatisticsFile statisticsFile) {
    return setStatistics(statisticsFile);
  }

  /**
   * Set the table's statistics file for given snapshot, replacing the previous statistics file for
   * the snapshot if any exists. The snapshot id of the statistics file will be used.
   *
   * @return this for method chaining
   */
  default UpdateStatistics setStatistics(StatisticsFile statisticsFile) {
    throw new UnsupportedOperationException("Setting statistics is not supported");
  }

  /**
   * Remove the table's statistics file for given snapshot.
   *
   * @return this for method chaining
   */
  UpdateStatistics removeStatistics(long snapshotId);
}
