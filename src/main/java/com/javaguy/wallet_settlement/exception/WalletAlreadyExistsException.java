package com.javaguy.wallet_settlement.exception;

public class WalletAlreadyExistsException extends RuntimeException {
    public WalletAlreadyExistsException(String s) {
        super(s);
    }
}
