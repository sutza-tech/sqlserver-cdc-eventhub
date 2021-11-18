package com.sohan.student.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * This class provides the configurations required to setup a Debezium connector for the Student Table.
 *
 * @author Sohan
 */
@Configuration
public class DebeziumConnectorConfig {

    /**
     * Student Database details.
     */
    @Value("${student.datasource.host}")
    private String studentDBHost;

    @Value("${student.datasource.databasename}")
    private String studentDBName;

    @Value("${student.datasource.port}")
    private String studentDBPort;

    @Value("${student.datasource.username}")
    private String studentDBUserName;

    @Value("${student.datasource.password}")
    private String studentDBPassword;

    private String STUDENT_TABLE_NAME = "public.student";

    /**
     * SQL Server Student Database details.
     */
    @Value("${student.sqlserver.datasource.host}")
    private String inventoryDBSQLServerHost;

    @Value("${student.sqlserver.datasource.databasename}")
    private String inventoryDBSQLServerDatabaseName;

    @Value("${student.sqlserver.datasource.servername}")
    private String inventoryDBSQLServerServerName;

    @Value("${student.sqlserver.datasource.port}")
    private String inventoryDBSQLServerPort;

    @Value("${student.sqlserver.datasource.username}")
    private String inventoryDBSQLServerUserName;

    @Value("${student.sqlserver.datasource.password}")
    private String inventoryDBSQLServerPassword;

    private String INVENTORY_SQL_SERVER_TABLE_NAME = "dbo.products, dbo.products_on_hand, dbo.customers, dbo.orders";
    /**
     * Student database connector.
     *
     * @return Configuration.
     */
    @Bean
    public io.debezium.config.Configuration studentConnector() throws IOException {
    File offsetStorageTempFile = File.createTempFile("offsets_", ".dat");
        return io.debezium.config.Configuration.create()
                .with("name", "student-postgres-connector")
                .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
                .with("offset.storage",  "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", offsetStorageTempFile.getAbsolutePath())
                .with("offset.flush.interval.ms", 60000)
                .with("database.server.name", studentDBHost+"-"+studentDBName)
                .with("database.hostname", studentDBHost)
                .with("database.port", studentDBPort)
                .with("database.user", studentDBUserName)
                .with("database.password", studentDBPassword)
                .with("database.dbname", studentDBName)
                .with("table.whitelist", STUDENT_TABLE_NAME).build();
    }

    @Bean
    public io.debezium.config.Configuration studentSQLConnector() throws IOException {
        File offsetStorageTempFile = File.createTempFile("offsets_", ".dat");
        return io.debezium.config.Configuration.create()
                .with("name", "inventory-sqlserver-connector")
                .with("connector.class", "io.debezium.connector.sqlserver.SqlServerConnector")
                .with( "tasks.max" , "1")
                .with("offset.storage",  "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", offsetStorageTempFile.getAbsolutePath())
                .with("offset.flush.interval.ms", 60000)
                .with("database.server.name", inventoryDBSQLServerServerName)
                .with("database.hostname", inventoryDBSQLServerHost)
                .with("database.port", inventoryDBSQLServerPort)
                .with("database.user", inventoryDBSQLServerUserName)
                .with("database.password", inventoryDBSQLServerPassword)
                .with("database.dbname", inventoryDBSQLServerDatabaseName)

                .with("include.schema.changes", "false")
                .with("database.history.consumer.security.protocol","SASL_SSL")
                .with("database.history.consumer.ssl.endpoint.identification.algorithm","https")
                .with("database.history.consumer.sasl.mechanism","PLAIN")
                .with("database.history.consumer.sasl.jaas.config","org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"Endpoint=sb://event-hub-pepper.servicebus.windows.net/;SharedAccessKeyName=Conexion;SharedAccessKey=YSPMIpDQWVXUCzQgnQTC0YmvGLNKd2bDtTKWOtvrVzk=\";")

                .with("database.history.producer.security.protocol","SASL_SSL")
                .with("database.history.producer.ssl.endpoint.identification.algorithm","https")
                .with("database.history.producer.sasl.mechanism", "PLAIN")
                .with("database.history.producer.sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"Endpoint=sb://event-hub-pepper.servicebus.windows.net/;SharedAccessKeyName=Conexion;SharedAccessKey=YSPMIpDQWVXUCzQgnQTC0YmvGLNKd2bDtTKWOtvrVzk=\";")

                .with("database.history.kafka.bootstrap.servers" , "event-hub-pepper.servicebus.windows.net:9093")
                .with("database.history.kafka.topic", "cdc-debezium")
                .with("table.include.list", INVENTORY_SQL_SERVER_TABLE_NAME)  // (a, b, c, d)
                .build();
    }
}
