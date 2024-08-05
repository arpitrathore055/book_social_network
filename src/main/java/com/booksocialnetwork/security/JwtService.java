package com.booksocialnetwork.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public String extractUsername(String jwtToken){
        return extractClaims(jwtToken,Claims::getSubject);
    }

    public <T> T extractClaims(String jwtToken, Function<Claims,T> claimsResolver){
        final Claims claims=extractAllClaims(jwtToken);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String jwtToken){
        return Jwts
                .parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }

    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>(),userDetails);
    }

    public String generateToken(Map<String,Object> claims, UserDetails userDetails){
        return buildToken(claims,userDetails,jwtExpiration);
    }

    public String buildToken(Map<String,Object>extraClaims,UserDetails userDetails,long jwtExpiration){
        var authorities=userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return Jwts
                .builder().claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .claim("authorities",authorities)
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String jwtToken,UserDetails userDetails){
        final String userEmail=extractUsername(jwtToken);
        return (userEmail.equals(userDetails.getUsername())) && !isTokenExpired(jwtToken);
    }

    private boolean isTokenExpired(String jwtToken){
        return extractExpiration(jwtToken).before(new Date());
    }

    private Date extractExpiration(String jwtToken) {
        return extractClaims(jwtToken,Claims::getExpiration);
    }

    private Key getSignInKey(){
        byte[] keyBytes= Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
