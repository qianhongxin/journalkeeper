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
package com.jd.journalkeeper.rpc.client;

import com.jd.journalkeeper.exceptions.NotLeaderException;
import com.jd.journalkeeper.exceptions.ServerBusyException;
import com.jd.journalkeeper.rpc.BaseResponse;
import com.jd.journalkeeper.rpc.LeaderResponse;
import com.jd.journalkeeper.rpc.StatusCode;

/**
 * @author liyue25
 * Date: 2019-03-14
 */
public class UpdateClusterStateResponse extends LeaderResponse {
    public UpdateClusterStateResponse() {
        super();
    }
    public UpdateClusterStateResponse(Throwable exception) {
        super(exception);
    }

    @Override
    public void setException(Throwable throwable) {
        try {
            throw throwable;
        } catch (ServerBusyException e) {
            setStatusCode(StatusCode.SERVER_BUSY);
        } catch (Throwable t) {
            super.setException(throwable);
        }
    }

}
