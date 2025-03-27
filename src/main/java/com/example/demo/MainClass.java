package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

@SpringBootApplication
@RestController
@RequestMapping("/file")
public class MainClass {

    private static final String UPLOAD_DIR = "uploads"; // Directorio donde se guardan los segmentos

    public static void main(String[] args) {
        SpringApplication.run(MainClass.class, args);

        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    @PostMapping("/upload")
    public List<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("size") int segmentSize) {
        List<String> segmentFiles = new ArrayList<>();
        try {
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));

            int numSegments = (int) Math.ceil((double) fileBytes.length / segmentSize);
            for (int i = 0; i < numSegments; i++) {
                int start = i * segmentSize;
                int end = Math.min(start + segmentSize, fileBytes.length);
                byte[] segment = new byte[end - start];
                System.arraycopy(fileBytes, start, segment, 0, segment.length);

                String segmentName = baseName + "." + i + extension;
                Path segmentPath = Paths.get(segmentName);
                Files.write(segmentPath, segment);
                segmentFiles.add(segmentName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return segmentFiles;
    }

    @GetMapping("/download/{fileName}")
    public byte[] downloadSegment(@PathVariable String fileName) throws IOException {
        Path filePath = Paths.get(fileName);
        return Files.readAllBytes(filePath);
    }

    @PostMapping("/send-email")
    public String sendEmail(@RequestParam("recipient") String recipient, @RequestParam("files") List<String> files) {
        String senderEmail = "your_email@example.com";
        String senderPassword = "your_password";
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.googlemail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.trust", "stmp.gmail.com");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smpt.ssl.enable", "false");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject("Segmentos de archivo");

            Multipart multipart = new MimeMultipart();
            for (String filePath : files) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(new File(filePath));
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);
            Transport.send(message);
            return "Email enviado con éxito.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error enviando el email.";
        }
    }

    @PostMapping("/merge")
    public String mergeFiles(@RequestParam("baseName") String baseName, @RequestParam("extension") String extension, @RequestParam("segments") int numSegments) {
        try {
            Path mergedFilePath = Paths.get(UPLOAD_DIR, baseName + extension); // Guardar el archivo fusionado en uploads
            try (OutputStream outputStream = new FileOutputStream(mergedFilePath.toFile())) {
                for (int i = 0; i < numSegments; i++) {
                    Path segmentPath = Paths.get(UPLOAD_DIR, baseName + "." + i + extension); // Buscar segmentos en uploads
                    if (Files.exists(segmentPath)) {
                        byte[] segmentBytes = Files.readAllBytes(segmentPath);
                        outputStream.write(segmentBytes);
                    } else {
                        return "Error: No se encontró el segmento " + i;
                    }
                }
            }
            return "Archivo reconstruido: " + mergedFilePath.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error al reconstruir el archivo.";
        }
    }
}
