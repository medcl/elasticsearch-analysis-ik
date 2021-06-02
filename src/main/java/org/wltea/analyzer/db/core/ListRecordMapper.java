package org.wltea.analyzer.db.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fsren
 * @date 2021-05-26
 */
public class ListRecordMapper<T> implements RecordMapper<List<T>> {
    private final RowMapper<T> rowMapper;

    public ListRecordMapper(RowMapper<T> rowMapper) {
        this.rowMapper = rowMapper;
    }

    @Override
    public List<T> map(Record record) {
        List<T> result = new ArrayList<>();
        while (record.next()) {
            result.add(rowMapper.map(record.getCurrentRow()));
        }
        return result;
    }
}