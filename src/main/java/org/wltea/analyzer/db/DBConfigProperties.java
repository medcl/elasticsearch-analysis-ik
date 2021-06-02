package org.wltea.analyzer.db;

import java.io.Serializable;

/**
 * @author fsren
 * @date 2021-05-25
 */
public class DBConfigProperties implements Serializable {

    private static final long serialVersionUID = 688310733642302993L;
    private String dbUrl;
    private String user;
    private String password;
    private Integer reloadInterval;

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getReloadInterval() {
        return reloadInterval;
    }

    public void setReloadInterval(Integer reloadInterval) {
        this.reloadInterval = reloadInterval;
    }

    @Override
    public String toString() {
        return "DBConfigProperties{" +
                "dbUrl='" + dbUrl + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", reloadInterval=" + reloadInterval +
                '}';
    }
}
