package org.wltea.analyzer.db.core;

/**
 * @author fsren
 * @date 2021-05-26
 */
public interface RecordMapper<T> {

    T map(Record record);
}
