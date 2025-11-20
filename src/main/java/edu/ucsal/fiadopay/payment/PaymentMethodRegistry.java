package edu.ucsal.fiadopay.payment;

import edu.ucsal.fiadopay.annotation.PaymentMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentMethodRegistry {

    private final Map<String, PaymentMethodHandler> handlers = new HashMap<>();

    public PaymentMethodRegistry(ApplicationContext context) {
        // pega todos os beans anotados com @PaymentMethod
        Map<String, Object> beans = context.getBeansWithAnnotation(PaymentMethod.class);
        for (Object bean : beans.values()) {
            PaymentMethod ann = bean.getClass().getAnnotation(PaymentMethod.class);
            String type = ann.type().toUpperCase();
            handlers.put(type, (PaymentMethodHandler) bean);
        }
    }

    public PaymentMethodHandler getHandler(String method) {
        PaymentMethodHandler handler = handlers.get(method.toUpperCase());
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return handler;
    }
}
