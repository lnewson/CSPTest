import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.provider.exception.ProviderException;
import org.jboss.pressgang.ccms.rest.v1.client.PressGangCCMSProxyFactoryV1;
import org.jboss.pressgang.ccms.rest.v1.entities.contentspec.RESTTextCSProcessingOptionsV1;
import org.jboss.pressgang.ccms.rest.v1.entities.contentspec.RESTTextContentSpecV1;
import org.jboss.pressgang.ccms.rest.v1.jaxrsinterfaces.RESTInterfaceV1;
import org.jboss.pressgang.ccms.utils.common.ResourceUtilities;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final RESTTextCSProcessingOptionsV1 processingOptions = new RESTTextCSProcessingOptionsV1();
    private static final RESTInterfaceV1 restInterface = PressGangCCMSProxyFactoryV1.create(
            "http://localhost:8080/pressgang-ccms/rest/").getRESTClient();
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        processingOptions.setPermissive(true);
        final Date date = new Date();
        log.info("Please execute the following SQL on the server first:\n\n" +
                "INSERT INTO `REVINFO` (`REV`, `REVTSTMP`, `Flag`, `Message`, `Username`) VALUES (NULL, UNIX_TIMESTAMP() * 1000, '1', " +
                "'', NULL);\nSET @REV = LAST_INSERT_ID();\n" +
                "INSERT INTO `ContentSpec`(`ContentSpecID`, `ContentSpecType`, `Locale`, `LastModified`) VALUES ('6895', '0', 'en-US', " +
                "'" + dateFormat.format(date) + "');\n" +
                "INSERT INTO `ContentSpec_AUD`(`ContentSpecID`, `REV`, `REVTYPE`, `ContentSpecType`, `Locale`, " +
                "`LastModified`) VALUES ('6895', @REV, '0', '0', 'en-US', '" + dateFormat.format(date) + "');\n");
        final String first = ResourceUtilities.resourceFileToString("/", "6895-369735.contentspec");
        final String second = ResourceUtilities.resourceFileToString("/", "6895-436615.contentspec");

        Scanner scanner = new Scanner(System.in);
        String answer = "";
        while (!answer.toLowerCase().matches("y|yes|exit")) {
            log.info("Has the SQL been run on the server? (y/n)");
            answer = scanner.nextLine();
        }

        if (answer.toLowerCase().matches("y|yes")) {
            migrateContentSpec(6895, first);
            migrateContentSpec(6895, second);
        } else {
            System.exit(-1);
        }
    }

    public static RESTTextContentSpecV1 migrateContentSpec(final Integer id, final String contentSpec) {
        try {
            final String textContentSpec = restInterface.getTEXTContentSpec(id);
            final String checksum = ContentSpecUtilities.getContentSpecChecksum(textContentSpec);
            String newTextContentSpec = contentSpec.toString();
            newTextContentSpec = ContentSpecUtilities.replaceChecksum(newTextContentSpec, checksum);

            final RESTTextContentSpecV1 newContentSpecEntity = new RESTTextContentSpecV1();
            newContentSpecEntity.setId(id);
            newContentSpecEntity.explicitSetText(newTextContentSpec);
            newContentSpecEntity.setProcessingOptions(processingOptions);

            return restInterface.updateJSONTextContentSpec("", newContentSpecEntity, "Content Spec Migration from topic 1", 1, "89");
        } catch (ClientResponseFailure responseFailure) {
            log.error("Failed to Migrate Content Spec", responseFailure);
        } catch (ProviderException e) {
            log.error("Failed to Migrate Content Spec", e);
        }

        return null;
    }
}