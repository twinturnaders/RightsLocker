package org.rights.locker.Services;

import org.springframework.stereotype.Service;

@Service
public class TokenService {

   public String getToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwtToken = authorizationHeader.substring("Bearer ".length());
            return jwtToken;
        }

        else {
        return null;}
   }

}
