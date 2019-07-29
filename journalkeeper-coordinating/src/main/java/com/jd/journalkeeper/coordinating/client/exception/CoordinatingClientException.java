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
package com.jd.journalkeeper.coordinating.client.exception;

import com.jd.journalkeeper.coordinating.exception.CoordinatingException;

/**
 * CoordinatingClientException
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2019/6/11
 */
public class CoordinatingClientException extends CoordinatingException {

    public CoordinatingClientException() {
    }

    public CoordinatingClientException(String message) {
        super(message);
    }

    public CoordinatingClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoordinatingClientException(Throwable cause) {
        super(cause);
    }

    public CoordinatingClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}