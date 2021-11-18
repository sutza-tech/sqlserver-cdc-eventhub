package com.sohan.student.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CdcMessage {
    private String operation;
    private String tableName;
    private String message;
}
