package com.system.controller;

import com.system.common.Result;
import com.system.service.CustomerAccountService;
import com.system.vo.CustomerAccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer/account")
@RequiredArgsConstructor
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;

    @GetMapping("/my")
    public Result<List<CustomerAccountVO>> myAccounts() {
        return Result.success(customerAccountService.listMyAccounts());
    }
}
