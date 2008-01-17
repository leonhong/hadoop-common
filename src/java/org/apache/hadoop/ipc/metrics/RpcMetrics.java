/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ipc.metrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.metrics.MetricsContext;
import org.apache.hadoop.metrics.MetricsRecord;
import org.apache.hadoop.metrics.MetricsUtil;
import org.apache.hadoop.metrics.Updater;
import org.apache.hadoop.metrics.jvm.JvmMetrics;
import org.apache.hadoop.metrics.util.MetricsTimeVaryingRate;

/**
 * 
 * This class is for maintaining  the various RPC statistics
 * and publishing them through the metrics interfaces.
 * This also registers the JMX MBean for RPC.
 * <p>
 * This class has a number of metrics variables that are publicly accessible;
 * these variables (objects) have methods to update their values;
 * for example:
 *  <p> {@link #rpcDiscardedOps}.inc(time)
 *
 */
public class RpcMetrics implements Updater {
  private MetricsRecord metricsRecord;
  private static Log LOG = LogFactory.getLog(JvmMetrics.class);
  
  private void setTags(String serverName, String port) {
    metricsRecord.setTag("serverName", serverName);
    metricsRecord.setTag("port", port);
    LOG.info("Initializing RPC Metrics with serverName=" 
        + serverName + ", port=" + port);
  }

  public RpcMetrics(String serverName, String port, Server server) {
    MetricsContext context = MetricsUtil.getContext("rpc");
    metricsRecord = MetricsUtil.createRecord(context, "metrics");
    setTags(serverName, port);
    context.registerUpdater(this);
    
    // Need to clean up the interface to RpcMgt - don't need both metrics and server params
    new RpcMgt(serverName, port, this, server);
  }
  
  
  /**
   * The metrics variables are public:
   *  - they can be set directly by calling their set/inc methods
   *  -they can also be read directly - e.g. JMX does this.
   */
  
  public MetricsTimeVaryingRate rpcQueueTime = new MetricsTimeVaryingRate("RpcQueueTime");
  public MetricsTimeVaryingRate rpcProcessingTime = new MetricsTimeVaryingRate("RpcProcessingTime");
  public MetricsTimeVaryingRate rpcDiscardedOps = new MetricsTimeVaryingRate("RpcDiscardedOps");
  
  
  /**
   * Push the metrics to the monitoring subsystem on doUpdate() call.
   */
  public void doUpdates(MetricsContext context) {
    rpcQueueTime.pushMetric(metricsRecord);
    rpcProcessingTime.pushMetric(metricsRecord);
    rpcDiscardedOps.pushMetric(metricsRecord);
    
  }
}