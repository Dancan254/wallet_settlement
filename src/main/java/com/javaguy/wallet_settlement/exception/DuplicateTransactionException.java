package com.javaguy.wallet_settlement.exception;

public class DuplicateTransactionException extends RuntimeException{
    public DuplicateTransactionException(String s) {
        super(s);
    }
}
