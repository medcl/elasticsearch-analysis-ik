package org.wltea.analyzer.db.core;

import org.elasticsearch.SpecialPermission;

import javax.sql.DataSource;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 连接管理器
 * 封装了线程安全的数据库连接
 *
 * @author fsren
 * @date 2021-05-26
 */
public class ConnectionManager {


    private final DataSource dataSource;
    private final ThreadLocal<Connection> connHolder = new ThreadLocal<>();

    public ConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() {
        SpecialPermission.check();
        return AccessController.doPrivileged((PrivilegedAction<Connection>) () -> {
            try {
                Connection conn = connHolder.get();
                if (conn == null) {
                    conn = dataSource.getConnection();
                    connHolder.set(conn);
                }
                return conn;
            } catch (SQLException e) {
                throw new RuntimeException("An error occurred while creating a database connection.", e);
            }
        });
    }

    public void close(Connection conn, Statement stmt) {
        SpecialPermission.check();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
            }
            if (conn != null) {
                try {
                    if (conn.getAutoCommit()) {
                        conn.close();
                        connHolder.remove();
                    }
                } catch (SQLException ignored) {
                }
            }
            return null;
        });

    }

    public void close(Connection conn, Statement stmt, ResultSet rs) {
        SpecialPermission.check();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignored) {
                }
            }
            return null;
        });
        close(conn, stmt);
    }

    public void startTransaction() {
        SpecialPermission.check();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            try {
                Connection conn = connHolder.get();
                if (conn != null) {
                    conn.close();
                    connHolder.remove();
                }
                conn = dataSource.getConnection();
                conn.setAutoCommit(false);
                connHolder.set(conn);
            } catch (SQLException e) {
                throw new RuntimeException("An error occurred while starting transaction.", e);
            }
            return null;
        });

    }

    public void commit() {
        SpecialPermission.check();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Connection conn = connHolder.get();
            if (conn != null) {
                try {
                    conn.commit();
                    conn.close();
                    connHolder.remove();
                } catch (SQLException e) {
                    throw new RuntimeException("An error occurred while committing transaction.", e);
                }
            }
            return null;
        });

    }

    public void rollback() {
        SpecialPermission.check();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Connection conn = connHolder.get();
            if (conn != null) {
                try {
                    conn.rollback();
                    conn.close();
                    connHolder.remove();
                } catch (SQLException e) {
                    throw new RuntimeException("An error occurred while committing transaction.", e);
                }
            }
            return null;
        });

    }

    public boolean inTransaction() {
        SpecialPermission.check();
        return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            Connection conn = connHolder.get();
            try {
                return conn != null && !conn.getAutoCommit();
            } catch (SQLException e) {
                throw new RuntimeException("An error occurred while getting auto commit.", e);
            }
        });

    }
}
