/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.journalkeeper.examples.kv;

/**
 * kv 操作命令
 * @author liyue25
 * Date: 2019-04-03
 */
public class KvEntry {
    public final static int CMD_SET = 0;
    public final static int CMD_DEL = 2;

    private int cmd;
    private String key;
    private String value;

    public KvEntry(){}
    public KvEntry(int cmd, String key, String value) {
        this.cmd = cmd;
        this.key = key;
        this.value = value;
    }

    public int getCmd() {
        return cmd;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
