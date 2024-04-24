//package io.knav.pgjobqueue.configurations;
//
//import com.github.jasync.sql.db.pool.ConnectionPool;
//import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
//import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class PgNotificationConfig {
//
//    @Bean
//    public ConnectionPool<PostgreSQLConnection> connectionPool(
//            @Value("${spring.datasource.url}") String url,
//            @Value("${spring.datasource.username}") String username,
//            @Value("${spring.datasource.password}") String password) {
//
//        String connectionUrl = String.format("%s?user=%s&password=%s", url, username, password);
//        return PostgreSQLConnectionBuilder.createConnectionPool(connectionUrl);
//    }
//
//}
