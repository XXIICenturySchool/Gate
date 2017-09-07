package com.example.gate.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskData {
    private String question;
    private String pictureUrl;
    private List<String> options;
    private List<String> answer;
}
