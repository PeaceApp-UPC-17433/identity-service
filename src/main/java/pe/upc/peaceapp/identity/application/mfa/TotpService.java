package pe.upc.peaceapp.identity.application.mfa;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

@Component
public class TotpService {
    private static final Duration STEP = Duration.ofSeconds(30);
    private static final int DRIFT_STEPS = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TimeBasedOneTimePasswordGenerator generator = new TimeBasedOneTimePasswordGenerator(STEP, 6);
    private final Base32 base32 = new Base32();
    private final String issuer;

    public TotpService(@Value("${identity.mfa.issuer:PeaceApp}") String issuer) {
        this.issuer = issuer;
    }

    public String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32.encodeToString(bytes).replace("=", "");
    }

    public String otpauthUri(String account, String secret) {
        String label = encode(issuer) + ":" + encode(account);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public OptionalLong verify(String secret, String code, Instant now, long lastUsedStep) {
        if (code == null || !code.matches("\\d{6}")) return OptionalLong.empty();
        var key = new SecretKeySpec(base32.decode(secret), generator.getAlgorithm());
        try {
            for (int offset = -DRIFT_STEPS; offset <= DRIFT_STEPS; offset++) {
                Instant at = now.plus(STEP.multipliedBy(offset));
                long step = stepOf(at);
                String expected = generator.generateOneTimePasswordString(key, at);
                if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))
                        && step > lastUsedStep) {
                    return OptionalLong.of(step);
                }
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        return OptionalLong.empty();
    }

    public String codeAt(String secret, Instant at) {
        try {
            return generator.generateOneTimePasswordString(new SecretKeySpec(base32.decode(secret), generator.getAlgorithm()), at);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static long stepOf(Instant at) {
        return at.getEpochSecond() / STEP.toSeconds();
    }
}
