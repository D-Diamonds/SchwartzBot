import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BotRunner {
    private static final String APPLICATION_NAME = "Schwartz Bot";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    // Sheet IDs
    private static final String EMAIL_SHEET_ID = "1CX67KDVUOI1Ane-cxg_W8r2JAy4NHFEhMse7PXN0NYs";
    private static final String QUOTE_SHEET_ID = "1Ao40X-Vn2CZxFPvjVL185g9NASRiyxovRh8MCyyJWUU";

    // Sheet ranges
    private static final String EMAIL_RANGE = "signups!C:C";
    private static final String QUOTE_RANGE = "quotes!A:A";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens / folder.
     */
    private static final List<String> SCOPES = List.of(GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_SEND, SheetsScopes.SPREADSHEETS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = BotRunner.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String... args) throws IOException, GeneralSecurityException, MessagingException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Initialize services
        Gmail gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();


        // Get values
        ValueRange emails = sheetsService.spreadsheets().values()
                .get(EMAIL_SHEET_ID, EMAIL_RANGE)
                .execute();

        ValueRange quotes = sheetsService.spreadsheets().values()
                .get(QUOTE_SHEET_ID, QUOTE_RANGE)
                .execute();

        // Get emails
        List<List<Object>> emailsValues = emails.getValues();

        Set<InternetAddress> internetAddresses = new HashSet<>();

        if (emailsValues == null || emailsValues.isEmpty()) {
            System.out.println("No emails found.");
        } else {
            for (List<Object> row : emailsValues) {
                String address = row.size() > 0 ? (String) row.get(0) : null;
                try {
                    if (address != null) {
                        internetAddresses.add(new InternetAddress(address));
                    } else {
                        throw new AddressException("Illegal address", null);
                    }
                } catch (AddressException e) {
                    System.out.println(e.toString());
                }
            }
        }

        // Get Quotes
        List<List<Object>> quoteValues  = quotes.getValues();

        List<String> quoteList = new ArrayList<>();
        if (quoteValues == null || quoteValues.isEmpty()) {
            quoteList.add("It seems our motivation is missing...");
            System.out.println("No quotes found.");
        } else {
            for (List<Object> row : quoteValues) {
                if (row.size() > 0) {
                    quoteList.add((String) row.get(0));
                }
            }
        }

        // Create and send email
        Email email = new Email(gmailService, internetAddresses, quoteList.get((int) (Math.random() * quoteList.size())));

        email.sendMessage();
    }
}