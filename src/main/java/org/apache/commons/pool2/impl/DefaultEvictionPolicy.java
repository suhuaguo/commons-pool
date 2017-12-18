/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool2.impl;

import org.apache.commons.pool2.PooledObject;

/**
 * Provides the default implementation of {@link EvictionPolicy} used by the
 * pools. Objects will be evicted if the following conditions are met:
 * <ul>
 * <li>the object has been idle longer than
 *     {@link GenericObjectPool#getMinEvictableIdleTimeMillis()} /
 *     {@link GenericKeyedObjectPool#getMinEvictableIdleTimeMillis()}</li>
 * <li>there are more than {@link GenericObjectPool#getMinIdle()} /
 *     {@link GenericKeyedObjectPoolConfig#getMinIdlePerKey()} idle objects in
 *     the pool and the object has been idle for longer than
 *     {@link GenericObjectPool#getSoftMinEvictableIdleTimeMillis()} /
 *     {@link GenericKeyedObjectPool#getSoftMinEvictableIdleTimeMillis()}
 * </ul>
 * This class is immutable and thread-safe.
 *
 * @param <T> the type of objects in the pool
 *
 * @since 2.0
 * INFO:默认的驱逐对象的策略
 */
public class DefaultEvictionPolicy<T> implements EvictionPolicy<T> {

    /**
     * 只是做了很简单的判断
     * @param config    The pool configuration settings related to eviction
     * @param underTest The pooled object being tested for eviction
     * @param idleCount The current number of idle objects in the pool including
     *                      the object under test
     * @return
     */
    @Override
    public boolean evict(final EvictionConfig config, final PooledObject<T> underTest,
            final int idleCount) {

        if ((config.getIdleSoftEvictTime() < underTest.getIdleTimeMillis() &&
                config.getMinIdle() < idleCount) ||
                config.getIdleEvictTime() < underTest.getIdleTimeMillis()) {
            return true;
        }
        return false;
    }
}
