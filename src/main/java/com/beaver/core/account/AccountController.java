package com.beaver.core.account;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private record Account(String id, String name) {}

    List<Account> accounts = new ArrayList<>(List.of(
        new Account("1", "Alice"),
        new Account("2", "Bob"),
        new Account("3", "Charlie")
    ));

    @GetMapping("")
    public List<Account> getAccounts() {
        return accounts;
    }

    @PostMapping("")
    public Account createAccount(@RequestBody() Account account) {
        accounts.add(account);
        return account;
    }
}
