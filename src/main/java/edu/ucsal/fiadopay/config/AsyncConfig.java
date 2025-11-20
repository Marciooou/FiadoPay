package edu.ucsal.fiadopay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean
    public ExecutorService fiadoPayExecutorService() {
        // 10 threads só para processamento de pagamento e webhook (ajuste conforme necessidade)
        return Executors.newFixedThreadPool(10);
    }
}