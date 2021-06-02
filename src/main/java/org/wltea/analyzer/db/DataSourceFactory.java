package org.wltea.analyzer.db;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.SpecialPermission;
import org.wltea.analyzer.help.ESPluginLoggerFactory;

import javax.sql.DataSource;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.SQLException;

/**
 * @author fsren
 * @date 2021-05-25
 */
public class DataSourceFactory {


    private static final Logger logger = ESPluginLoggerFactory.getLogger(DataSourceFactory.class.getName());


    public static DataSource getDataSource(DBConfigProperties configProperties) {

        SpecialPermission.check();
        return AccessController.doPrivileged((PrivilegedAction<DataSource>) () -> {
            logger.info("load datasource start");
            MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
            dataSource.setURL(configProperties.getDbUrl());
            dataSource.setUser(configProperties.getUser());
            dataSource.setPassword(configProperties.getPassword());
            dataSource.setAllowMultiQueries(true);
            try {
                dataSource.setSocketTimeout(1000);
            } catch (SQLException ignore) {
            }
            logger.info("load datasource end");
            return dataSource;
        });
    }


}
