package com.example.gate;

import com.example.gate.exam.Exam;
import com.example.gate.exam.ExamData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;

@RestController
@RequestMapping("/exam")
public class UIController {
    @Autowired
    private DiscoveryClient discoveryClient;

    @SneakyThrows
    @GetMapping("/startExam")
    public ExamData startExam(@RequestParam int examId, @RequestParam int teacherId){
        //1. найти адрес клиента у maplogin
        ServiceInstance serviceInstance = Services.MAPLOGIN.pickRandomInstance(discoveryClient);
        URL url = serviceInstance.getUri().toURL();
        url = new URL(url.toString()+"/exams/getExam");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
       //headers.setContentType(MediaType.APPLICATION_JSON);
        //ObjectMapper objectMapper = new ObjectMapper();
       // String jsonExam = ВАШ ДЖСОН

        HttpEntity<String> entity = new HttpEntity<>(headers);

        URI uri = UriComponentsBuilder.fromHttpUrl(url.toString())
                .queryParam("examId", examId)
                .queryParam("teacherId", teacherId)
                .build().encode().toUri();

        ResponseEntity<String> loginResponse = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        String response ="no response";

        if (loginResponse.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            Exam exam = objectMapper.readValue(loginResponse.getBody(), Exam.class);

            Services service = Services.getServiceById(exam.getServiceName());

            URI examHolderUri = service.pickRandomInstance(discoveryClient).getUri();
            String uristring = examHolderUri.toString() + exam.getUrl();
            URL examHolderURL = new URL(uristring);

            RestTemplate holderTemplate = new RestTemplate();
            HttpHeaders holderHeaders = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<String> holderEntity = new HttpEntity<>(holderHeaders);

            ResponseEntity<String> holderResponce = holderTemplate.exchange(examHolderURL.toString(), HttpMethod.GET, holderEntity, String.class);
            //Шаг 2 - забрать у этого сервиса экзамен!

            if (loginResponse.getStatusCode() == HttpStatus.OK) {
               return objectMapper.readValue(holderResponce.getBody(), ExamData.class);
            }
            else throw new RuntimeException("Bad exam response");







        } else if (loginResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new RuntimeException("not found!");
        }




        return null;
    }
}
