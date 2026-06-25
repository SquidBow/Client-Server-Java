package app.server.helpers;

import app.generic.helpers.Tuple;
import com.auth0.jwt.*;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;

public class JWTToken {

    @SafeVarargs
    public static String createToken(Tuple<String, String>... claims) {
        Algorithm algorithm = Algorithm.HMAC256(
            "secret_key_do_not_tell_anyone_or_you_will_be_fired_key_is_very_secure_and_very_long_and_very_long_is_very_secure_!!!!!!!!!!!!!"
        );

        JWTCreator.Builder token = JWT.create();

        for (Tuple<String, String> tuple : claims) {
            token.withClaim(tuple.first, tuple.second);
        }

        token.withExpiresAt(Instant.now().plusSeconds(300));

        return token.sign(algorithm);
    }

    public static String decodeToken(String token, String claim) {
        if (token == null || token.isBlank()) {
            // System.out.println("\nDecodning: Got 401 ");
            return "401";
        }

        try {
            Algorithm alg = Algorithm.HMAC256(
                "secret_key_do_not_tell_anyone_or_you_will_be_fired_key_is_very_secure_and_very_long_and_very_long_is_very_secure_!!!!!!!!!!!!!"
            );

            // System.out.println("Decoding: verifying token");
            DecodedJWT decoded = JWT.require(alg).build().verify(token);

            // System.out.println("Decoding: Returning token");
            return decoded.getClaim(claim).asString();
        } catch (JWTVerificationException e) {
            // System.out.println("Decoding: failed to decode it");
            e.printStackTrace();
            return "Expired token";
        }
        // } catch (JWTDecodeException e) {
        //     e.printStackTrace();
        //     return "401";
    }
}
