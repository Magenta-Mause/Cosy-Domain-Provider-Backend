package com.magentamause.cosydomainprovider.services.billing;

import com.magentamause.cosydomainprovider.configuration.stripe.StripeProperties;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeProperties stripeProperties;
    private final UserRepository userRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    public String createBillingPortalSession(UserEntity user) {
        try {
            String customerId = getOrCreateCustomerId(user);
            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setCustomer(customerId)
                            .setReturnUrl(frontendUrl + "/billing")
                            .build();
            return com.stripe.model.billingportal.Session.create(params).getUrl();
        } catch (StripeException e) {
            log.error(
                    "Failed to create billing portal session for user {}: {}",
                    user.getUuid(),
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to create billing portal session");
        }
    }

    public String createCheckoutSession(UserEntity user) {
        try {
            String customerId = getOrCreateCustomerId(user);
            com.stripe.param.checkout.SessionCreateParams params =
                    com.stripe.param.checkout.SessionCreateParams.builder()
                            .setMode(
                                    com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
                            .setCustomer(customerId)
                            .addLineItem(
                                    com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                                            .setPrice(stripeProperties.getPriceId())
                                            .setQuantity(1L)
                                            .build())
                            .setSuccessUrl(frontendUrl + "/billing?success=true")
                            .setCancelUrl(frontendUrl + "/billing")
                            .putMetadata("userId", user.getUuid())
                            .build();
            return com.stripe.model.checkout.Session.create(params).getUrl();
        } catch (StripeException e) {
            log.error(
                    "Failed to create checkout session for user {}: {}",
                    user.getUuid(),
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to create checkout session");
        }
    }

    public void handleWebhookEvent(String payload, String sigHeader)
            throws SignatureVerificationException {
        Event event =
                Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());

        switch (event.getType()) {
            case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private String getOrCreateCustomerId(UserEntity user) throws StripeException {
        if (user.getStripeCustomerId() != null) {
            return user.getStripeCustomerId();
        }
        CustomerCreateParams params =
                CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(user.getUsername())
                        .putMetadata("userId", user.getUuid())
                        .build();
        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
        return customer.getId();
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = deserialize(event, Invoice.class);
        if (invoice == null) return;
        String customerId = invoice.getCustomer();

        UserEntity user = findByStripeCustomerId(customerId);
        if (user == null) return;

        user.setPlan(Plan.PLUS);
        if (invoice.getPeriodEnd() != null) {
            user.setPlanExpiresAt(Instant.ofEpochSecond(invoice.getPeriodEnd()));
        }
        userRepository.save(user);
        log.info(
                "User {} plan set to PLUS, expires at {}", user.getUuid(), user.getPlanExpiresAt());
    }

    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = deserialize(event, Subscription.class);
        if (subscription == null) return;
        String customerId = subscription.getCustomer();

        UserEntity user = findByStripeCustomerId(customerId);
        if (user == null) return;

        user.setPlan(Plan.FREE);
        user.setPlanExpiresAt(null);
        userRepository.save(user);
        log.info("User {} downgraded to FREE via subscription cancellation", user.getUuid());
    }

    @SuppressWarnings("unchecked")
    private <T extends StripeObject> T deserialize(Event event, Class<T> type) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return (T) deserializer.getObject().get();
        }
        try {
            return (T) deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            log.warn("Could not deserialise {} payload: {}", event.getType(), e.getMessage());
            return null;
        }
    }

    private UserEntity findByStripeCustomerId(String customerId) {
        if (customerId == null) return null;
        return userRepository
                .findByStripeCustomerId(customerId)
                .orElseGet(
                        () -> {
                            log.warn("No user found for Stripe customerId: {}", customerId);
                            return null;
                        });
    }
}
