package com.example.gate.exam;

import lombok.Data;

@Data
public class ExamResult {
    private String examId;
    private String teacherId;
    private String name;
    private int numberOfSkippedAnswers;
    private int numberOfWrongAnswers;
    private double seconds;

}
