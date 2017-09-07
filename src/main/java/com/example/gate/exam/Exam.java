package com.example.gate.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam {
    private int id;
    private int teacherId;
    private String serviceName;
    private String url;
}