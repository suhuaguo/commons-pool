/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/java/org/apache/commons/pool/impl/SoftReferenceObjectPool.java,v 1.1 2002/03/19 17:27:54 rwaldhoff Exp $
 * $Revision: 1.1 $
 * $Date: 2002/03/19 17:27:54 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.pool.impl;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.lang.ref.SoftReference;

/**
 * A {@link java.lang.ref.SoftReference SoftReference} based
 * {@link ObjectPool}.
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.1 $ $Date: 2002/03/19 17:27:54 $
 */
public class SoftReferenceObjectPool implements ObjectPool {
    public SoftReferenceObjectPool() {
        _pool = new ArrayList();
        _factory = null;
    }

    public SoftReferenceObjectPool(PoolableObjectFactory factory) {
        _pool = new ArrayList();
        _factory = factory;
    }

    public SoftReferenceObjectPool(PoolableObjectFactory factory, int initSize) throws Exception {
        _pool = new ArrayList();
        _factory = factory;
        if(null != _factory) {
            for(int i=0;i<initSize;i++) {
                Object obj = _factory.makeObject();
                _factory.passivateObject(obj);
                _pool.add(new SoftReference(obj));
            }
        }
    }

    public synchronized Object borrowObject() throws Exception {        
        Object obj = null;
        while(null == obj) {
            if(_pool.isEmpty()) {
                if(null == _factory) {
                    throw new NoSuchElementException();
                } else {
                    obj = _factory.makeObject();
                }
            } else {
                SoftReference ref = (SoftReference)(_pool.remove(_pool.size() - 1));
                obj = ref.get();
                if(null == obj) {
                    System.out.println("Found a SoftReference " + ref + " in the pool, but its refferant was null.");
                }
            }
        }
        if(null != _factory && null != obj) {
            _factory.activateObject(obj);
        }
        _numActive++;
        return obj;
    }

    public synchronized void returnObject(Object obj) throws Exception {
        _numActive--;
        if(null == _factory || _factory.validateObject(obj)) {
            if(null != _factory) {
                try {
                    _factory.passivateObject(obj);
                } catch(Exception e) {
                    _factory.destroyObject(obj);
                    return;
                }
            }
        }
        _pool.add(new SoftReference(obj));
    }

    /** Returns an approximation not less than the of the number of idle instances in the pool. */
    public int numIdle() {
        return _pool.size();
    }

    public int numActive() {
        return _numActive;
    }

    public synchronized void clear() {
        if(null != _factory) {
            Iterator iter = _pool.iterator();
            while(iter.hasNext()) {
                try {
                    Object obj = ((SoftReference)iter.next()).get();
                    if(null != obj) {
                        _factory.destroyObject(obj);
                    }
                } catch(Exception e) {
                    // ignore error, keep destroying the rest
                }
            }
        }
        _pool.clear();
    }

    synchronized public void close() throws Exception {
        clear();
        _pool = null;
        _factory = null;
    }

    synchronized public void setFactory(PoolableObjectFactory factory) throws IllegalStateException {
        if(0 < numActive()) {
            throw new IllegalStateException("Objects are already active");
        } else {
            clear();
            _factory = factory;
        }
    }

    /** My pool. */
    private List _pool = null;

    /** My {@link PoolableObjectFactory}. */
    private PoolableObjectFactory _factory = null;

    /** Number of active objects. */
    private int _numActive = 0;
}
