package com.ivan.bankapi.config;

import com.ivan.bankapi.model.User;
import com.ivan.bankapi.model.enums.Role;
import com.ivan.bankapi.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByEmail("demo@bank.com").isEmpty()) {
                userRepository.save(new User("demo@bank.com", "password", Role.USER));
            }
        };
    }
}