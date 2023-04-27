package com.anf.core.utils;

import java.util.Map;
import java.util.Iterator;

/**
 * 
 * @author appba59
 *
 * Usage:
 * Map<String, String> myMap = new HashMap<String, String>();
 * System.out.println(new PrettyMap<String, String>(myMap));
 * 
 * 
 * 
 */
public class PrettyPrintMap<K, V> {
    private Map<K, V> map;

    public PrettyPrintMap(Map<K, V> map) {
        this.map = map;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<K, V> entry = iter.next();
            sb.append(entry.getKey());
            sb.append('=').append('"');
            sb.append(entry.getValue());
            sb.append('"');
            if (iter.hasNext()) {
                sb.append(',').append(' ');
            }
        }
        return sb.toString();

    }
}