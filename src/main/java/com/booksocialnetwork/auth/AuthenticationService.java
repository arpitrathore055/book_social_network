package com.booksocialnetwork.auth;

import com.booksocialnetwork.email.EmailService;
import com.booksocialnetwork.email.EmailTemplateName;
import com.booksocialnetwork.email.EmailService;
import com.booksocialnetwork.entities.Token;
import com.booksocialnetwork.entities.User;
import com.booksocialnetwork.repositories.RoleRepository;
import com.booksocialnetwork.repositories.TokenRepository;
import com.booksocialnetwork.repositories.UserRepository;
import com.booksocialnetwork.security.JwtService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.activation-url}")
    private String activationUrl;

    @Value("${application.activationCodeLength}")
    private Integer activationCodeLength;
    
    public void register(RegistrationRequest request) throws MessagingException {
        var userRole=roleRepository.findByName("USER").orElseThrow(()->new IllegalStateException("Role USER was not initialized"));
        var user= User
                .builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
        // sending validation email to user
        sendValidationEmail(user);
    }

    private void sendValidationEmail(User user) throws MessagingException {
        var newToken=generateAndSaveActivationToken(user);
        //send email
        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation"
        );
    }

    private String generateAndSaveActivationToken(User user) {
        //generating token
        String generatedToken=generateActivationCode(activationCodeLength);
        var token= Token
                .builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);
        return generatedToken;
    }

    private String generateActivationCode(int tokenLength){
        String characters="0123456789";
        StringBuilder codebuilder=new StringBuilder();
        SecureRandom secureRandom=new SecureRandom();
        for(int i=0;i<tokenLength;i++){
            int randomIndex= secureRandom.nextInt(characters.length());
            codebuilder.append(characters.charAt(randomIndex));
        }
        return codebuilder.toString();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var auth=authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var claims=new HashMap<String,Object>();
        var user=((User)auth.getPrincipal());
        claims.put("fullname",user.getFullName());
        var jwtToken=jwtService.generateToken(claims,user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
    
    public void activateAccount(String token) throws MessagingException {
        Token savedToken=tokenRepository.findByToken(token).orElseThrow(()->new RuntimeException("Invalid Token"));
        if(LocalDateTime.now().isAfter(savedToken.getExpiresAt())){
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException(("Activation Token has expired. A new token has been sent ot the same email address"));
        }
        var user=userRepository.findById(savedToken.getId()).orElseThrow(()->new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }
}
