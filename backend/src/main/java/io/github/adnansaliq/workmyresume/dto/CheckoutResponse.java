package io.github.adnansaliq.workmyresume.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutResponse {
    private String orderId;
    private String keyId;
    private String currency;
    private Long amount;
}