package org.wltea.analyzer.db.core;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author fsren
 * @date 2021-05-26
 */
public class MapRowMapper implements RowMapper<Map<String, Object>> {
    @Override
    public Map<String, Object> map(Row row) {
        Map<String, Object> map = new Hashtable<>();
        int count = row.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String key = row.getColumnLabel(i);
            Object value = row.getObject(i);
            map.put(key, value);
        }
        return map;
    }
}
