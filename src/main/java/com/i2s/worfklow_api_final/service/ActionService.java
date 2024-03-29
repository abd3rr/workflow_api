package com.i2s.worfklow_api_final.service;

import com.i2s.worfklow_api_final.model.Method;
import com.i2s.worfklow_api_final.model.Parameter;
import com.i2s.worfklow_api_final.repository.MethodRepository;
import com.i2s.worfklow_api_final.util.ReflectionUtil;
import com.vonage.client.VonageClient;
import com.vonage.client.sms.MessageStatus;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.SmsSubmissionResponseMessage;
import com.vonage.client.sms.messages.TextMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ActionService {
    private final MethodRepository methodRepository;
    private final JavaMailSender javaMailSender;
    private final VonageClient vonageClient;

    public ActionService(MethodRepository methodRepository, JavaMailSender javaMailSender, VonageClient vonageClient) {

        this.methodRepository = methodRepository;
        this.javaMailSender = javaMailSender;

        this.vonageClient = vonageClient;
    }

    @PostConstruct
    public void init() {
        registerMethods();
    }

    // Registers methods during initialization
    private void registerMethods() {
        List<String> excludedMethods = Arrays.asList("init", "registerMethods", "createMethodFromReflection");
        List<Method> actionMethods = Arrays.stream(getClass().getDeclaredMethods())
                .filter(method -> !method.isSynthetic() && !excludedMethods.contains(method.getName()))
                .map(this::createMethodFromReflection).collect(Collectors.toList());
        List<Method> existingMethods = methodRepository.findAll();

        // Remove methods that are no longer present in the ActionService class
        existingMethods.forEach(existingMethod -> {
            if (actionMethods.stream().noneMatch(method -> method.getMethodName().equals(existingMethod.getMethodName()))) {
                methodRepository.deleteById(existingMethod.getId());
            }
        });

        // Add new methods that are not in the database
        for (Method method : actionMethods) {
            if (!methodRepository.findByMethodName(method.getMethodName()).isPresent()) {
                methodRepository.save(method);
            }
        }
    }


    // Creates a Method entity with associated Parameter entities based on a java.lang.reflect.Method
    private Method createMethodFromReflection(java.lang.reflect.Method method) {
        Method methodEntity = new Method();
        methodEntity.setMethodName(method.getName());
        java.lang.reflect.Parameter[] reflectedParameters = method.getParameters();
        List<String> parameterTypes = ReflectionUtil.getParameterTypes(reflectedParameters);

        for (int i = 0; i < reflectedParameters.length; i++) {
            Parameter parameter = new Parameter();
            parameter.setParameterName(reflectedParameters[i].getName());
            parameter.setParameterType(parameterTypes.get(i));
            methodEntity.getParameters().add(parameter);
        }
        return methodEntity;
    }

    // Methods

    public void sendEmail(String sender, String receiver, String subject, String content) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(receiver);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
        System.out.println("Email sent successfully");
    }


    public void sendSms(String from, String to, String text) throws Exception {
        TextMessage message = new TextMessage(from, to, text);
        SmsSubmissionResponse response = vonageClient.getSmsClient().submitMessage(message);

        if (response.getMessages() == null) {
            throw new Exception("Failed to send message, response is null");
        }

        for (SmsSubmissionResponseMessage msg : response.getMessages()) {
            if (msg.getStatus() != MessageStatus.OK) {
                throw new Exception("Failed to send message, error: " + msg.getErrorText());
            }
        }

        System.out.println("Message sent successfully");
    }

    public void ridil(int tkhbi9a, String chiL3ba) {
      System.out.println("ridil ");

    }
    public void ridil1(String tkhbi9a, String chiL3ba) {
        System.out.println("ridil ");

    }
}
