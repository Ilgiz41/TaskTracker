package org.example.infrastructure.cache;

import org.example.domain.model.Identifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class Cache <K, V extends Identifiable<K>> {

    private final Map<K, V> cache;

    public Cache(int initialCapacity) {
        cache = new ConcurrentHashMap<>(initialCapacity);
    }

    public void computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        cache.computeIfPresent(key, remappingFunction);
    }

    public void compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        cache.compute(key, remappingFunction);
    }

    public void merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        cache.merge(key, value, remappingFunction);
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void put(V value){
        cache.put(value.getId(), value);
    }

    public void remove(V value){
        cache.remove(value.getId());
    }

    public boolean isEmpty(){
        return cache.isEmpty();
    }

    public int size(){
        return cache.size();
    }

    public void clear(){
        cache.clear();
    }

    public List<V> getAll(){
        return new ArrayList<>(cache.values());
    }
}
