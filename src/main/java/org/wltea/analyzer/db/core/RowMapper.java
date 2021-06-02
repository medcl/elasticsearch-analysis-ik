package org.wltea.analyzer.db.core;

/**
 * 行转换器接口
 *
 * @author fsren
 * @date 2021-05-26
 */
public interface RowMapper<T> {
    T map(Row row);
}
