package com.sohan.student.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sohan.student.elasticsearch.service.StudentService;
import com.sohan.student.kafka.dto.CdcMessage;
import com.sohan.student.kafka.producer.Sender;
import com.sohan.student.utils.Operation;
import io.debezium.config.Configuration;
import io.debezium.data.Envelope;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.debezium.data.Envelope.FieldName.*;
import static java.util.stream.Collectors.toMap;

/**
 * This class creates, starts and stops the EmbeddedEngine, which starts the Debezium engine. The engine also
 * loads and launches the connectors setup in the configuration.
 * <p>
 * The class uses @PostConstruct and @PreDestroy functions to perform needed operations.
 *
 * @author Sohan
 */
@Slf4j
@Component
public class CDCListener {


    @Value("${kafka.eventhub.topic}")
    private String topic;

    @Value("${kafka.eventhub.topic.base}")
    private String topicBaseName;

    public static final String USER_SCHEMA = "{"
            + "\"type\":\"record\"," + "\"name\":\"cdc_operation\","
            + "\"fields\":["
            + "  { \"name\":\"operation\", \"type\":\"string\" },"
            + "  { \"name\":\"message\", \"type\":\"string\" }"
            + "]}";

    /**
     * Single thread pool which will run the Debezium engine asynchronously.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * The Debezium engine which needs to be loaded with the configurations, Started and Stopped - for the
     * CDC to work.
     */
    private final EmbeddedEngine engine;
    //private final DebeziumEngine<ChangeEvent<String, String>> engine2;
    /**
     * Handle to the Service layer, which interacts with ElasticSearch.
     */
    private final StudentService studentService;

    private final Sender sender;

    private static final String TABLE = "table" ;

    /**
     * Constructor which loads the configurations and sets a callback method 'handleEvent', which is invoked when
     * a DataBase transactional operation is performed.
     *
     * @param studentSQLConnector
     * @param studentService
     */
    private CDCListener(Configuration studentSQLConnector, StudentService studentService, Sender sender) {
        // Create the engine with this configuration ...
        /*
        try (DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
                .using(studentSQLConnector)
                .notifying(record -> {
                    System.out.println(record);
                }).build()
        ) {
            // Run the engine asynchronously ...
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(engine);

            // Do something else or wait for a signal or an event
        }
         */
        this.engine = EmbeddedEngine
                .create()
                .using(studentSQLConnector)
                .notifying(this::handleEvent).build();

        this.studentService = studentService;
        this.sender = sender;
    }

    /**
     * The method is called after the Debezium engine is initialized and started asynchronously using the Executor.
     */
    @PostConstruct
    private void start() {
        log.info("@PostConstruct::start");
        this.executor.execute(engine);
    }

    /**
     * This method is called when the container is being destroyed. This stops the debezium, merging the Executor.
     */
    @PreDestroy
    private void stop() {
        log.info("@PreDestroy::stop");
        if (this.engine != null) {
            this.engine.stop();
        }
        try {
            executor.shutdown();
            while (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.info("Waiting another 5 seconds for the embedded engine to shut down");
            }
        }
        catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This method is invoked when a transactional action is performed on any of the tables that were configured.
     *
     * @param sourceRecord
     */
    private void handleEvent(SourceRecord sourceRecord) {
        Struct sourceRecordValue = (Struct) sourceRecord.value();

        if(sourceRecordValue != null) {
            Operation operation = Operation.forCode((String) sourceRecordValue.get(OPERATION));

            //Only if this is a transactional operation.
            if(operation != Operation.READ) {
                Struct sourceStruct = (Struct) sourceRecordValue.get(Envelope.FieldName.SOURCE);

                String tableName =  sourceStruct.getString(TABLE);
                Map<String, Object> message;
                String record = AFTER; //For Update & Insert operations.

                if (operation == Operation.DELETE) {
                    record = BEFORE; //For Delete operations.
                }

                //Build a map with all row data received.
                Struct struct = (Struct) sourceRecordValue.get(record);
                log.info("row data received: {}", struct);
                message = struct.schema().fields().stream()
                        .map(Field::name)
                        .filter(fieldName -> struct.get(fieldName) != null)
                        .map(fieldName -> Pair.of(fieldName, struct.get(fieldName)))
                        .collect(toMap(Pair::getKey, Pair::getValue));

                //Call the service to handle the data change.
                //this.studentService.maintainReadModel(message, operation);
                log.info("Data Changed: {} with Operation: {} and table: {} ", message, operation.name(), tableName);
                CdcMessage cdcMessage = new CdcMessage(operation.name(), tableName, new Gson().toJson(message));
                byte[] data = serialize(cdcMessage);
                sender.sendAsBytes(topicBaseName+tableName, data);
                log.info("Data Changed sent to Kafka");
            }
        }
    }


    private byte[] serialize(CdcMessage data) {
        byte[] retVal = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            retVal = objectMapper.writeValueAsString(data).getBytes();
        } catch (Exception exception) {
            System.out.println("Error in serializing object"+ data);
        }
        return retVal;
    }
}
