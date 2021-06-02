package org.wltea.analyzer.db.core;

import java.sql.*;

/**
 * 将java.sql.ResultSet转换成自定义的Record，封装底层实现细节
 *
 * @author fsren
 * @date 2021-05-26
 */
public class RecordAdapterForResultSet implements Record, Row {
    private final ResultSet rs;

    public RecordAdapterForResultSet(ResultSet resultSet) {
        this.rs = resultSet;
    }

    @Override
    public Object getObject(String columnLabel) {
        try {
            return rs.getObject(columnLabel);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Object getObject(int columnIndex) {
        try {
            return rs.getObject(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public int getInt(String columnLabel) {
        try {
            return rs.getInt(columnLabel);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public int getInt(int columnIndex) {
        try {
            return rs.getInt(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String getString(String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String getString(int columnIndex) {
        try {
            return rs.getString(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public double getDouble(String columnLabel) {
        try {
            return rs.getDouble(columnLabel);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public double getDouble(int columnIndex) {
        try {
            return rs.getDouble(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Date getDate(int columnIndex) {
        try {
            return rs.getDate(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Date getDate(String columnLabel) {
        try {
            return rs.getDate(columnLabel);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Boolean getBoolean(int columnIndex) {
        try {
            return rs.getBoolean(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Boolean getBoolean(String columnLabel) {
        try {
            return rs.getBoolean(columnLabel);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) {
        try {
            return rs.getTimestamp(columnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) {
        try {
            return rs.getTimestamp(columnLabel);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public int getColumnCount() {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            return metaData.getColumnCount();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String getColumnLabel(int index) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            return metaData.getColumnLabel(index);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Row getCurrentRow() {
        return this;
    }

    @Override
    public boolean next() {
        try {
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}