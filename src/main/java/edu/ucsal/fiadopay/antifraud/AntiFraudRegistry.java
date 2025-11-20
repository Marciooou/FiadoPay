package edu.ucsal.fiadopay.antifraud;


import edu.ucsal.fiadopay.annotation.AntiFraud;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class AntiFraudRegistry {

    private final List<AntiFraudRule> rules;

    public AntiFraudRegistry(ApplicationContext context) {
        Map<String, Object> beans = context.getBeansWithAnnotation(AntiFraud.class);
        this.rules = beans.values().stream()
                .map(b -> (AntiFraudRule) b)
                .sorted(Comparator.comparingInt(b -> b.getClass().getAnnotation(AntiFraud.class).order()))
                .toList();
    }

    public List<AntiFraudRule> getRules() {
        return rules;
    }
}