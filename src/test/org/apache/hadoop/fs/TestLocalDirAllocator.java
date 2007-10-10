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
package org.apache.hadoop.fs;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

import junit.framework.TestCase;

/** This test LocalDirAllocator works correctly;
 * Every test case uses different buffer dirs to 
 * enforce the AllocatorPerContext initialization*/ 
public class TestLocalDirAllocator extends TestCase {
  final static private Configuration conf = new Configuration();
  final static private String BUFFER_DIR_ROOT = "build/test/temp";
  final static private Path BUFFER_PATH_ROOT = new Path(BUFFER_DIR_ROOT);
  final static private File BUFFER_ROOT = new File(BUFFER_DIR_ROOT);
  final static private String BUFFER_DIR[] = new String[] {
    BUFFER_DIR_ROOT+"/tmp0",  BUFFER_DIR_ROOT+"/tmp1", BUFFER_DIR_ROOT+"/tmp2",
    BUFFER_DIR_ROOT+"/tmp3", BUFFER_DIR_ROOT+"/tmp4"};
  final static private Path BUFFER_PATH[] = new Path[] {
    new Path(BUFFER_DIR[0]), new Path(BUFFER_DIR[1]), new Path(BUFFER_DIR[2]),
    new Path(BUFFER_DIR[3]), new Path(BUFFER_DIR[4])};
  final static private String CONTEXT = "dfs.client.buffer.dir";
  final static private String FILENAME = "block";
  final static private LocalDirAllocator dirAllocator = 
    new LocalDirAllocator(CONTEXT);
  static LocalFileSystem localFs;
  
  /** constructor */
  
  static {
    try {
      localFs = FileSystem.getLocal(conf);
      rmBufferDirs();
    } catch(IOException e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static void rmBufferDirs() throws IOException {
    assertTrue(!localFs.exists(BUFFER_PATH_ROOT) ||
        localFs.delete(BUFFER_PATH_ROOT));
  }
  
  private void validateTempDirCreation(int i) throws IOException {
    File result = createTempFile();
    assertTrue(result.getPath().startsWith(BUFFER_DIR[i]+"/"+FILENAME));
  }
  
  private File createTempFile() throws IOException {
    File result = dirAllocator.createTmpFileForWrite(FILENAME, -1, conf);
    result.delete();
    return result;
  }
  /** Two buffer dirs. The first dir does not exist & is on a read-only disk; 
   * The second dir exists & is RW
   * @throws Exception
   */
  public void test0() throws Exception {
    try {
      conf.set(CONTEXT, BUFFER_DIR[0]+","+BUFFER_DIR[1]);
      assertTrue(localFs.mkdirs(BUFFER_PATH[1]));
      BUFFER_ROOT.setReadOnly();
      validateTempDirCreation(1);
      validateTempDirCreation(1);
    } finally {
      ShellCommand.execCommand(new String[]{"chmod", "u+w", BUFFER_DIR_ROOT});
      rmBufferDirs();
    }
  }
    
  /** Two buffer dirs. The first dir exists & is on a read-only disk; 
   * The second dir exists & is RW
   * @throws Exception
   */
  public void test1() throws Exception {
    try {
      conf.set(CONTEXT, BUFFER_DIR[1]+","+BUFFER_DIR[2]);
      assertTrue(localFs.mkdirs(BUFFER_PATH[2]));
      BUFFER_ROOT.setReadOnly();
      validateTempDirCreation(2);
      validateTempDirCreation(2);
    } finally {
      ShellCommand.execCommand(new String[]{"chmod", "u+w", BUFFER_DIR_ROOT});
      rmBufferDirs();
    }
  }
  /** Two buffer dirs. Both do not exist but on a RW disk.
   * Check if tmp dirs are allocated in a round-robin 
   */
  public void test2() throws Exception {
    try {
      conf.set(CONTEXT, BUFFER_DIR[2]+","+BUFFER_DIR[3]);
      validateTempDirCreation(2);
      validateTempDirCreation(3);
      validateTempDirCreation(2);
      validateTempDirCreation(3);
    } finally {
      rmBufferDirs();
    }
  }

  /** Two buffer dirs. Both exists and on a R/W disk. 
   * Later disk1 becomes read-only.
   * @throws Exception
   */
  public void test3() throws Exception {
    try {
      conf.set(CONTEXT, BUFFER_DIR[3]+","+BUFFER_DIR[4]);
      assertTrue(localFs.mkdirs(BUFFER_PATH[3]));
      assertTrue(localFs.mkdirs(BUFFER_PATH[4]));
      
      validateTempDirCreation(3);
      validateTempDirCreation(4);

      // change buffer directory 2 to be read only
      new File(BUFFER_DIR[4]).setReadOnly();
      validateTempDirCreation(3);
      validateTempDirCreation(3);
    } finally {
      rmBufferDirs();
    }
  }
}