import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class FileSplitterApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese la ruta del archivo: ");
        String filePath = scanner.nextLine();
        System.out.print("Ingrese el tamaño del segmento en bytes: ");
        int segmentSize = scanner.nextInt();
        scanner.nextLine();

        List<String> segmentFiles = splitFile(filePath, segmentSize);
        System.out.println("Segmentos generados: " + segmentFiles);
    }

    public static List<String> splitFile(String filePath, int segmentSize) {
        List<String> segmentFiles = new ArrayList<>();
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            String baseName = filePath.substring(0, filePath.lastIndexOf('.'));
            String extension = filePath.substring(filePath.lastIndexOf('.'));

            int numSegments = (int) Math.ceil((double) fileBytes.length / segmentSize);
            for (int i = 0; i < numSegments; i++) {
                int start = i * segmentSize;
                int end = Math.min(start + segmentSize, fileBytes.length);
                byte[] segment = Arrays.copyOfRange(fileBytes, start, end);

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

    public static void sendEmail(String recipient, List<String> files) {
        String senderEmail = "your_email@example.com";
        String senderPassword = "your_password";
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.example.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

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
            System.out.println("Email enviado con éxito.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error enviando el email.");
        }
    }
}
