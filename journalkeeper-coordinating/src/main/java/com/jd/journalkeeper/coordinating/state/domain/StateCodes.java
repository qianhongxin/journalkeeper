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
package com.jd.journalkeeper.coordinating.state.domain;

/**
 * StateCodes
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2019/5/30
 */
public enum StateCodes {

    SUCCESS(0),

    ERROR(1),

    ;

    private int code;

    StateCodes(int type) {
        this.code = type;
    }

    public int getCode() {
        return code;
    }

    public static StateCodes valueOf(int type) {
        switch (type) {
            case 0:
                return SUCCESS;
            case 1:
                return ERROR;
            default:
                throw new UnsupportedOperationException(String.valueOf(type));
        }
    }
}