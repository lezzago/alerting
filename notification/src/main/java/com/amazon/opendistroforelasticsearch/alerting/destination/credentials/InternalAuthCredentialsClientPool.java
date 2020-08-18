/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package com.amazon.opendistroforelasticsearch.alerting.destination.credentials;

import java.util.HashMap;
import java.util.Map;


/**
 * This class fetches credentials provider from different sources(based on priority) and uses the first one that works.
 */
public final class InternalAuthCredentialsClientPool {

    private static final InternalAuthCredentialsClientPool instance = new InternalAuthCredentialsClientPool();

    private Map<String, InternalAuthCredentialsClient> clientPool;

    public static InternalAuthCredentialsClientPool getInstance() {
        return instance;
    }

    public synchronized InternalAuthCredentialsClient getInternalAuthClient(String factoryName) {
        if (clientPool.containsKey(factoryName)) {
            return clientPool.get(factoryName);
        }

        return newClient(factoryName);
    }

    private InternalAuthCredentialsClientPool() {
        this.clientPool = new HashMap<String, InternalAuthCredentialsClient>();
    }

    private InternalAuthCredentialsClient newClient(String factoryName) {
        InternalAuthCredentialsClient client = new InternalAuthCredentialsClient();
        clientPool.put(factoryName, client);
        return client;
    }
}
