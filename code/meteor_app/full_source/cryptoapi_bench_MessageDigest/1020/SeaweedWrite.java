package seaweedfs.client;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.List;
import java.util.Base64;

public class SeaweedWrite {

    private static final Logger LOG = LoggerFactory.getLogger(SeaweedWrite.class);

    private static final SecureRandom random = new SecureRandom();

   

    private static String multipartUpload(String targetUrl,
                                          String auth,
                                          final byte[] bytes,
                                          final long bytesOffset, final long bytesLength,
                                          byte[] cipherKey) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
        }

        InputStream inputStream = null;
        if (cipherKey == null || cipherKey.length == 0) {
            md.update(bytes, (int) bytesOffset, (int) bytesLength);
            inputStream = new ByteArrayInputStream(bytes, (int) bytesOffset, (int) bytesLength);
        } else {
            try {
                byte[] encryptedBytes = SeaweedCipher.encrypt(bytes, (int) bytesOffset, (int) bytesLength, cipherKey);
                md.update(encryptedBytes);
                inputStream = new ByteArrayInputStream(encryptedBytes, 0, encryptedBytes.length);
            } catch (Exception e) {
                throw new IOException("fail to encrypt data", e);
            }
        }

        HttpPost post = new HttpPost(targetUrl);
        if (auth != null && auth.length() != 0) {
            post.addHeader("Authorization", "BEARER " + auth);
        }
        post.addHeader("Content-MD5", Base64.getEncoder().encodeToString(md.digest()));

        post.setEntity(MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("upload", inputStream)
                .build());

        CloseableHttpResponse response = SeaweedUtil.getClosableHttpClient().execute(post);

        try {
            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                if (response.getEntity().getContentType() != null && response.getEntity().getContentType().getValue().equals("application/json")) {
                    throw new IOException(EntityUtils.toString(response.getEntity(), "UTF-8"));
                } else {
                    throw new IOException(response.getStatusLine().getReasonPhrase());
                }
            }

            String etag = response.getLastHeader("ETag").getValue();

            if (etag != null && etag.startsWith("\"") && etag.endsWith("\"")) {
                etag = etag.substring(1, etag.length() - 1);
            }

            EntityUtils.consume(response.getEntity());

            return etag;
        } finally {
            response.close();
            post.releaseConnection();
        }

    }

}