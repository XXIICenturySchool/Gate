package com.example.gate;

import com.example.gate.exam.Exam;
import com.example.gate.exam.ExamData;
import com.example.gate.exam.ExamResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpSession;
import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

@RestController
@RequestMapping("/exam")
@Slf4j
public class UIController {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/getAllExamJSON")
    public ExamData getAllExamJson(HttpSession session) {
        return (ExamData) session.getAttribute("examData");
    }

    @SneakyThrows
    @PostMapping("/sendResult")
    public void sendResult(@RequestBody ExamResult result) {
        //По известному алгоритму сходить на маплогин за учителем
        ServiceInstance serviceInstance = Services.MAPLOGIN.pickRandomInstance(discoveryClient);
        URL url = serviceInstance.getUri().toURL();
        url = new URL(url.toString() + "/maplogin/teachers/search/findTeacherById?id=" + result.getTeacherId());
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> teacherResponse = restTemplate.exchange(url.toString(), HttpMethod.GET, entity, String.class);
        Teacher teacher;

        if (teacherResponse.getStatusCode() == HttpStatus.OK) {
            teacher = objectMapper.readValue(teacherResponse.getBody(), Teacher.class);
        } else throw new RuntimeException("No such teacher!!!");


        sendMail(result, teacher);
    }

    @SneakyThrows
    private void sendMail(ExamResult result, Teacher teacher) {
        Email email = new SimpleEmail();
        email.setHostName("smtp.gmail.com");
        email.setAuthenticator(new DefaultAuthenticator("Examinatorovich", "Examinatorovich123"));
        email.setSmtpPort(465);
        email.setSSLOnConnect(true);
        email.setFrom("Examinatorovich@gmail.com");
        email.setSubject(result.getExamId()+" result");
        email.setMsg(result.toString());
        String[] mails = teacher.getMails().split(",");
        for (String mail : mails) {
            email.addTo(mail);
        }
        email.send();
        log.info("mail sent to "+teacher.getMails());
    }


    @GetMapping("/getExam")
    public ExamData getExam(@RequestParam int examId, @RequestParam int teacherId, HttpSession session) {
        startExam(examId, teacherId, session);
        ExamData examData = (ExamData) session.getAttribute("examData");
        session.removeAttribute("examData");
        return examData;
    }

    @SneakyThrows
    @GetMapping("/startExam")
    public int startExam(@RequestParam int examId, @RequestParam int teacherId, HttpSession session) {
        //1. найти адрес клиента у maplogind
        ServiceInstance serviceInstance = Services.MAPLOGIN.pickRandomInstance(discoveryClient);
        URL url = serviceInstance.getUri().toURL();
        url = new URL(url.toString() + "/exams/getExam");
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

        String response = "no response";

        if (loginResponse.getStatusCode() == HttpStatus.OK) {
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

            if (holderResponce.getStatusCode() == HttpStatus.OK) {
                ExamData examData = objectMapper.readValue(holderResponce.getBody(), ExamData.class);
                session.setAttribute("examData", examData);
                return examData.getTasks().size();
                //return objectMapper.readValue(holderResponce.getBody(), ExamData.class);
            } else throw new RuntimeException("Bad exam response");
        } else if (loginResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new RuntimeException("exam not found!");
        }
        return 0;
    }

    @SneakyThrows
    public static void main(String[] args) {

        ExamResult result = new ExamResult();
        result.setExamId("123");
        result.setSeconds(200);
        result.setNumberOfWrongAnswers(10);
        Email email = new SimpleEmail();
        email.setHostName("smtp.gmail.com");
        email.setAuthenticator(new DefaultAuthenticator("Examinatorovich", "Examinatorovich123"));
        email.setSmtpPort(465);
        email.setSSLOnConnect(true);
        email.setFrom("Examinatorovich@gmail.com");
        email.setSubject(result.getExamId()+" result");
        email.setMsg(result.toString());
        Teacher teacher = new Teacher();
        teacher.setMails("bsevgeny@gmail.com");
        String[] mails = teacher.getMails().split(",");
        for (String mail : mails) {
            email.addTo(mail);
        }
        email.send();
        log.info("mail sent to "+teacher.getMails());
    }
}
