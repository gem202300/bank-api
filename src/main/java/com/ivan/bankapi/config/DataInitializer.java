package com.ivan.bankapi.config;

import com.ivan.bankapi.dto.CreateAccountRequest;
import com.ivan.bankapi.dto.DepositRequest;
import com.ivan.bankapi.model.User;
import com.ivan.bankapi.model.enums.Role;
import com.ivan.bankapi.repository.AccountRepository;
import com.ivan.bankapi.repository.UserRepository;
import com.ivan.bankapi.service.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final String DEMO_EMAIL = "demo@bank.com";

    @Bean
    CommandLineRunner initDemoData(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountService accountService
    ) {
        return args -> {
            User user = userRepository.findByEmail(DEMO_EMAIL)
                    .orElseGet(() -> userRepository.save(
                            new User(DEMO_EMAIL, "password", Role.USER)
                    ));

            List<?> accounts = accountRepository.findByOwnerId(user.getId());
            if (!accounts.isEmpty()) {
                System.out.println("=== DEMO DATA already present, userId=" + user.getId() + " ===");
                return;
            }

            CreateAccountRequest create = new CreateAccountRequest();
            create.setOwnerId(user.getId());
            create.setCurrency("PLN");

            var account1 = accountService.create(create);
            var account2 = accountService.create(create);

            DepositRequest deposit = new DepositRequest();
            deposit.setAmount(new BigDecimal("1000.00"));
            deposit.setDescription("Initial demo balance");
            accountService.deposit(account1.getId(), deposit);

            System.out.println("=== DEMO DATA CREATED ===");
            System.out.println("email: " + DEMO_EMAIL);
            System.out.println("userId: " + user.getId());
            System.out.println("account1: " + account1.getId() + " (1000 PLN)");
            System.out.println("account2: " + account2.getId() + " (0 PLN)");
        };
    }
}