package com.booksocialnetwork;

import com.booksocialnetwork.entities.Role;
import com.booksocialnetwork.entities.Token;
import com.booksocialnetwork.entities.User;
import com.booksocialnetwork.repositories.RoleRepository;
import com.booksocialnetwork.repositories.TokenRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
public class BookSocialNetworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookSocialNetworkApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner(RoleRepository roleRepository, TokenRepository tokenRepository){
        return args->{
          if(roleRepository.findByName("USER").isEmpty()){
              roleRepository.save(
                      Role.builder()
                              .name("USER")
                              .build()
              );
          }
        };
    }
}
