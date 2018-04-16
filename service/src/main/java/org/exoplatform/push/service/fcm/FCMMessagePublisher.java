package org.exoplatform.push.service.fcm;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.PemReader;
import com.google.api.client.util.SecurityUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.push.domain.Message;
import org.exoplatform.push.service.MessagePublisher;
import org.exoplatform.push.util.StringUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;

/**
 * Message publisher using the HTTP API v1 of Firebase Cloud Messaging
 */
public class FCMMessagePublisher implements MessagePublisher {

  private static final Log LOG = ExoLogger.getLogger(FCMMessagePublisher.class);

  private CloseableHttpClient httpClient;

  private String fcmServiceAccountFilePath;

  private FCMServiceAccountConfiguration fcmServiceAccountConfiguration;

  private GoogleCredential googleCredential;

  // How long (in seconds) the message should be kept in FCM storage if the device is offline
  private Integer fcmMessageExpirationTime = null;

  public FCMMessagePublisher(InitParams initParams) {
    this(initParams, HttpClientBuilder.create().build());
  }

  public FCMMessagePublisher(InitParams initParams, CloseableHttpClient httpClient) {
    if(initParams != null) {
      // FCM configuration file
      ValueParam serviceAccountFilePathValueParam = initParams.getValueParam("serviceAccountFilePath");
      if(serviceAccountFilePathValueParam != null) {
        fcmServiceAccountFilePath = serviceAccountFilePathValueParam.getValue();
      }
      if(StringUtils.isNotBlank(fcmServiceAccountFilePath)) {
        try {
          if (!fcmServiceAccountFilePath.startsWith("/")) {
            // relative path
            String gateinConfDir = System.getProperty("gatein.conf.dir");
            fcmServiceAccountFilePath = gateinConfDir + "/" + fcmServiceAccountFilePath;
          }

          fcmServiceAccountConfiguration = loadConfiguration(fcmServiceAccountFilePath);

          googleCredential = getCredentialsFromStream(fcmServiceAccountConfiguration);

        } catch (FileNotFoundException e) {
          LOG.error("Push notifications - Firebase Cloud Messaging service account config file is mandatory, " +
                  "please add it at " + fcmServiceAccountFilePath);
        } catch (Exception e) {
          LOG.error("Push notifications - Error while loading Firebase Cloud Messaging configuration from config file "
                  + fcmServiceAccountFilePath, e);
        }
      } else {
        LOG.error("Push notifications - Firebase Cloud Messaging service account config file path is mandatory, " +
                "please configure it with exo.push.fcm.serviceAccountFilePath property.");

      }

      // FCM message expiration
      ValueParam fcmMessageExpirationTimeValueParam = initParams.getValueParam("messageExpirationTime");
      if(fcmMessageExpirationTimeValueParam != null && StringUtils.isNotBlank(fcmMessageExpirationTimeValueParam.getValue())) {
        try {
          fcmMessageExpirationTime = Integer.parseInt(fcmMessageExpirationTimeValueParam.getValue());
        } catch (NumberFormatException e) {
          LOG.error("Push Notifications - FCM message expiration time is not a valid number ("
                  + fcmMessageExpirationTimeValueParam.getValue() + "), using default value from FCM", e);
        }
      }
    }

    this.httpClient = httpClient;
  }

  @Override
  public void send(Message message) throws Exception {
    if(googleCredential == null) {
      return;
    }

    HttpPost post = new HttpPost("https://fcm.googleapis.com/v1/projects/"
            + fcmServiceAccountConfiguration.getServiceAccountProjectId() + "/messages:send");
    post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken());
    post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

    StringBuilder requestBody = new StringBuilder()
            .append("{")
            .append("  \"validate_only\": false,")
            .append("  \"message\": {");
    // TODO sending notifications differently between Android and iOS, waiting for message handling implementation in iOS
    if(StringUtils.isNotBlank(message.getDeviceType()) && message.getDeviceType().equals("android")) {
      requestBody.append("    \"data\": {");
    } else {
      requestBody.append("    \"notification\": {");
    }
    requestBody.append("      \"title\": \"").append(message.getTitle().replaceAll("\"", "\\\\\"")).append("\",")
            .append("      \"body\": \"").append(message.getBody().replaceAll("\"", "\\\\\"")).append("\"")
            .append("    },");
    if(fcmMessageExpirationTime != null && StringUtils.isNotBlank(message.getDeviceType())) {
      if(message.getDeviceType().equals("android")) {
        requestBody
                .append("    \"android\": {")
                .append("      \"ttl\": \"").append(fcmMessageExpirationTime).append("s\"")
                .append("    },");
      } else if(message.getDeviceType().equals("ios")) {
        Instant expirationInstant = Instant.now().minus(fcmMessageExpirationTime, ChronoUnit.SECONDS);
        requestBody
                .append("    \"apns\": {")
                .append("      \"headers\": {")
                .append("        \"apns-expiration\": \"").append(expirationInstant.getEpochSecond()).append("\"")
                .append("      },")
                .append("    },");
      }
    }
    requestBody.append("    \"token\":\"").append(message.getToken()).append("\"")
            .append("  }")
            .append("}");

    post.setEntity(new ByteArrayEntity(requestBody.toString().getBytes()));

    try(CloseableHttpResponse response = httpClient.execute(post)) {
      if (response == null || response.getStatusLine() == null) {
        throw new Exception("Error sending Push Notification, response is null");
      } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw new Exception("Error sending Push Notification, response is " + response.getStatusLine().getStatusCode()
                + " - " + response.getStatusLine().getReasonPhrase());
      } else {
        LOG.info("Message sent to Firebase : username={}, token={}, type={}",
                message.getReceiver(), StringUtil.mask(message.getToken(), 4), message.getDeviceType());
      }
    }
  }

  /**
   * Load Firebase Cloud Messaging configuration from file
   *
   * @param fcmServiceAccountFilePath
   * @return
   * @throws IOException
   */
  protected FCMServiceAccountConfiguration loadConfiguration(String fcmServiceAccountFilePath) throws IOException {
    LOG.info("Loading Firebase configuration for Push Notifications from file " + fcmServiceAccountFilePath);
    File jsonFile = new File(fcmServiceAccountFilePath);
    FileInputStream fcmServiceAccountFile = new FileInputStream(jsonFile);

    JacksonFactory jsonFactory = new JacksonFactory();
    JsonObjectParser parser = new JsonObjectParser(jsonFactory);

    GenericJson fileContents = parser.parseAndClose(fcmServiceAccountFile, Charset.forName("UTF-8"), GenericJson.class);

    String clientId = (String)fileContents.get("client_id");
    String clientEmail = (String)fileContents.get("client_email");
    String privateKeyPem = (String)fileContents.get("private_key");
    String privateKeyId = (String)fileContents.get("private_key_id");

    if (clientId != null && clientEmail != null && privateKeyPem != null && privateKeyId != null) {
      FCMServiceAccountConfiguration fcmServiceAccountConfiguration = new FCMServiceAccountConfiguration();
      fcmServiceAccountConfiguration.setServiceAccountId(clientEmail);
      fcmServiceAccountConfiguration.setServiceAccountClientId(clientId);
      fcmServiceAccountConfiguration.setServiceAccountPrivateKeyPem(privateKeyPem);
      fcmServiceAccountConfiguration.setServiceAccountPrivateKeyId(privateKeyId);

      String projectId = (String)fileContents.get("project_id");
      if (projectId != null) {
        fcmServiceAccountConfiguration.setServiceAccountProjectId(projectId);
      }

      String tokenUri = (String)fileContents.get("token_uri");
      if (tokenUri != null) {
        fcmServiceAccountConfiguration.setTokenServerEncodedUrl(tokenUri);
      }

      return fcmServiceAccountConfiguration;
    } else {
      throw new IOException("Error reading service account configuration from file, expecting 'client_id', 'client_email', 'private_key' and 'private_key_id'.");
    }
  }



  /**
   * Build GoogleCredential object from configuration
   *
   * @param configuration Firebase Cloud Messaging configuration
   * @return GoogleCredential
   * @throws IOException
   * @throws GeneralSecurityException
   */
  protected GoogleCredential getCredentialsFromStream(FCMServiceAccountConfiguration configuration) throws Exception {
    PrivateKey privateKey = getPrivateKeyFromPkcs8(configuration.getServiceAccountPrivateKeyPem());
    GoogleCredential.Builder credentialBuilder = (new GoogleCredential.Builder())
            .setTransport(GoogleNetHttpTransport.newTrustedTransport())
            .setJsonFactory(new JacksonFactory())
            .setServiceAccountId(configuration.getServiceAccountId())
            .setServiceAccountPrivateKey(privateKey);

    // Workaround to call credentialBuilder.setServiceAccountScopes method with both signatures (with argument Iterable
    // and Collection) to support multiple versions of Google API (at least 14 and 17).
    // To be removed when old signature not supported anymore.
    java.lang.reflect.Method setServiceAccountScopesMethod;
    try {
      setServiceAccountScopesMethod = credentialBuilder.getClass().getDeclaredMethod("setServiceAccountScopes", Iterable.class);
    } catch (NoSuchMethodException e) {
      try {
        setServiceAccountScopesMethod = credentialBuilder.getClass().getDeclaredMethod("setServiceAccountScopes", Collection.class);
      } catch (NoSuchMethodException e1) {
        throw new Exception("Cannot find suitable method setServiceAccountScopes in GoogleCredential.Builder class", e1);
      }
    }

    if(setServiceAccountScopesMethod != null) {
      setServiceAccountScopesMethod.invoke(credentialBuilder, Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
    }

    return credentialBuilder.build();
  }

  /**
   * Build a PrivateKey from its string value
   *
   * @param privateKeyPem Private key value
   * @return PrivateKey
   * @throws IOException
   */
  protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
    Reader reader = new StringReader(privateKeyPem);
    PemReader.Section section = PemReader.readFirstSectionAndClose(reader, "PRIVATE KEY");
    if (section == null) {
      throw new IOException("Invalid PKCS8 data.");
    }
    byte[] bytes = section.getBase64DecodedBytes();
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
    Exception unexpectedException = null;
    try {
      KeyFactory keyFactory = SecurityUtils.getRsaKeyFactory();
      PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
      return privateKey;
    } catch (NoSuchAlgorithmException exception) {
      unexpectedException = exception;
    } catch (InvalidKeySpecException exception) {
      unexpectedException = exception;
    }
    throw new IOException("Unexpected exception reading PKCS data", unexpectedException);
  }

  /**
   * Retrieve the access token
   *
   * @return The access token
   * @throws IOException
   */
  protected String getAccessToken() throws IOException {
    googleCredential.refreshToken();
    return googleCredential.getAccessToken();
  }
}
