/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.jsonpath.spi.cache;

import com.jayway.jsonpath.JsonPath;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache implements Cache {

    private final ReentrantLock lock = new ReentrantLock();

    private final Map<String, JsonPath> map = new ConcurrentHashMap<String, JsonPath>();
    private final Deque<String> queue = new LinkedList<String>();
    private final int limit;

    public LRUCache(int limit) {
        this.limit = limit;
    }

    public void put(String key, JsonPath value) {
        JsonPath oldValue = map.put(key, value);
        if (oldValue != null) {
            removeThenAddKey(key);
        } else {
            addKey(key);
        }
        if (map.size() > limit) {
            map.remove(removeLast());
        }
    }

    public JsonPath get(String key) {
        JsonPath jsonPath = map.get(key);
        if(jsonPath != null){
            removeThenAddKey(key);
        }
        return jsonPath;
    }

    private void addKey(String key) {
        lock.lock();
        try {
            queue.addFirst(key);
        } finally {
            lock.unlock();
        }
    }

    private String removeLast() {
        lock.lock();
        try {
            final String removedKey = queue.removeLast();
            return removedKey;
        } finally {
            lock.unlock();
        }
    }

    private void removeThenAddKey(String key) {
        lock.lock();
        try {
            queue.removeFirstOccurrence(key);
            queue.addFirst(key);
        } finally {
            lock.unlock();
        }

    }

    private void removeFirstOccurrence(String key) {
        lock.lock();
        try {
            queue.removeFirstOccurrence(key);
        } finally {
            lock.unlock();
        }
    }

    public JsonPath getSilent(String key) {
        return map.get(key);
    }

    public void remove(String key) {
        removeFirstOccurrence(key);
        map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public String toString() {
        return map.toString();
    }
}