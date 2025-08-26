package com.javaguy.wallet_settlement;

import org.springframework.boot.SpringApplication;

public class TestWalletSettlementApplication {

    public static void main(String[] args) {
        SpringApplication.from(WalletSettlementApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
