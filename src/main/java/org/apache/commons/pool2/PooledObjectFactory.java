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
package org.apache.commons.pool2;

/**
 * An interface defining life-cycle methods for instances to be served by an
 * {@link ObjectPool}.
 * <p>
 * By contract, when an {@link ObjectPool} delegates to a
 * {@link PooledObjectFactory},
 * <ol>
 *  <li>
 *   {@link #makeObject} is called whenever a new instance is needed.
 *  </li>
 *  <li>
 *   {@link #activateObject} is invoked on every instance that has been
 *   {@link #passivateObject passivated} before it is
 *   {@link ObjectPool#borrowObject borrowed} from the pool.
 *  </li>
 *  <li>
 *   {@link #validateObject} may be invoked on {@link #activateObject activated}
 *   instances to make sure they can be {@link ObjectPool#borrowObject borrowed}
 *   from the pool. {@link #validateObject} may also be used to
 *   test an instance being {@link ObjectPool#returnObject returned} to the pool
 *   before it is {@link #passivateObject passivated}. It will only be invoked
 *   on an activated instance.
 *  </li>
 *  <li>
 *   {@link #passivateObject} is invoked on every instance when it is returned
 *   to the pool.
 *  </li>
 *  <li>
 *   {@link #destroyObject} is invoked on every instance when it is being
 *   "dropped" from the pool (whether due to the response from
 *   {@link #validateObject}, or for reasons specific to the pool
 *   implementation.) There is no guarantee that the instance being destroyed
 *   will be considered active, passive or in a generally consistent state.
 *  </li>
 * </ol>
 * {@link PooledObjectFactory} must be thread-safe. The only promise
 * an {@link ObjectPool} makes is that the same instance of an object will not
 * be passed to more than one method of a <code>PoolableObjectFactory</code>
 * at a time.
 * <p>
 * While clients of a {@link KeyedObjectPool} borrow and return instances of
 * the underlying value type {@code V}, the factory methods act on instances of
 * {@link PooledObject PooledObject&lt;V&gt;}.  These are the object wrappers that
 * pools use to track and maintain state information about the objects that
 * they manage.
 *
 * @param <T> Type of element managed in this factory.
 *
 * @see ObjectPool
 *
 * @since 2.0
 * INFO:工厂类，负责池化对象的创建，对象的初始化，对象状态的销毁和对象状态的验证。
 *
 * TODO:这里的对象最好都实现一遍
 */
public interface PooledObjectFactory<T> {
  /**
   * Create an instance that can be served by the pool and wrap it in a
   * {@link PooledObject} to be managed by the pool.
   *
   *
   *
   * @return a {@code PooledObject} wrapping an instance that can be served by the pool
   *
   * @throws Exception if there is a problem creating a new instance,
   *    this will be propagated to the code requesting an object.
   */
  PooledObject<T> makeObject() throws Exception;

  /**
   * Destroys an instance no longer needed by the pool.
   * <p>
   * It is important for implementations of this method to be aware that there
   * is no guarantee about what state <code>obj</code> will be in and the
   * implementation should be prepared to handle unexpected errors.
   * <p>
   * Also, an implementation must take in to consideration that instances lost
   * to the garbage collector may never be destroyed.
   * </p>
   *
   * @param p a {@code PooledObject} wrapping the instance to be destroyed
   *
   * @throws Exception should be avoided as it may be swallowed by
   *    the pool implementation.
   *
   * @see #validateObject
   * @see ObjectPool#invalidateObject
   */
  void destroyObject(PooledObject<T> p) throws Exception;

  /**
   * Ensures that the instance is safe to be returned by the pool.
   * 检测对象是否"有效";Pool中不能保存无效的"对象",因此"后台检测线程"会周期性的检测Pool中"对象"的有效性,
   * 如果对象无效则会导致此对象从Pool中移除,并destroy;此外在调用者从Pool获取一个"对象"时,也会检测"对象"的有效性,
   * 确保不能讲"无效"的对象输出给调用者;当调用者使用完毕将"对象归还"到Pool时,仍然会检测对象的有效性.
   * 所谓有效性,就是此"对象"的状态是否符合预期,是否可以对调用者直接使用;如果对象是Socket,
   * 那么它的有效性就是socket的通道是否畅通/阻塞是否超时等.
   *
   * @param p a {@code PooledObject} wrapping the instance to be validated
   *
   * @return <code>false</code> if <code>obj</code> is not valid and should
   *         be dropped from the pool, <code>true</code> otherwise.
   */
  boolean validateObject(PooledObject<T> p);

  /**
   * Reinitialize（重新预置） an instance to be returned by the pool.
   * 激活"对象,当Pool中决定移除一个对象交付给调用者时额外的"激活"操作,比如可以在activateObject方法中"重置"
   * 参数列表让调用者使用时感觉像一个"新创建"的对象一样;如果object是一个线程,可以在"激活"操作中重置"线程
   * 中断标记",或者让线程从阻塞中唤醒等;如果 object是一个socket,那么可以在"激活操作"中刷新通道,
   * 或者对socket进行链接重建(假如socket意外关闭)等.
   *
   * @param p a {@code PooledObject} wrapping the instance to be activated
   *
   * @throws Exception if there is a problem activating <code>obj</code>,
   *    this exception may be swallowed by the pool.
   *
   * @see #destroyObject
   */
  void activateObject(PooledObject<T> p) throws Exception;

  /**
   * Uninitialize an instance to be returned to the idle object pool.
   * "钝化"对象,当调用者"归还对象"时,Pool将会"钝化对象";钝化的言外之意,就是此"对象"暂且需要"休息"一下.
   * 如果object是一个 socket,那么可以passivateObject中清除buffer,将socket阻塞;如果object是一个线程,
   * 可以在"钝化"操作中将线程sleep或者将线程中的某个对象wait.需要注意的时,activateObject和
   * passivateObject两个方法需要对应,避免死锁或者"对象"状态的混乱.
   *
   * @param p a {@code PooledObject} wrapping the instance to be passivated
   *
   * @throws Exception if there is a problem passivating <code>obj</code>,
   *    this exception may be swallowed by the pool.
   *
   * @see #destroyObject
   */
  void passivateObject(PooledObject<T> p) throws Exception;
}
