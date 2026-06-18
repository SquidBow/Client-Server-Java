package app.server.helpers;

import app.generic.helpers.Tuple;
import com.auth0.jwt.*;
import com.auth0.jwt.algorithms.Algorithm;
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
}
