package com.system.controller;

import com.system.common.Result;
import com.system.dto.LoanApplicationCreateDTO;
import com.system.service.LoanApplicationService;
import com.system.vo.LoanApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer/loan-application")
@RequiredArgsConstructor
public class CustomerLoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    @PostMapping
    public Result<LoanApplicationVO> create(@Valid @RequestBody LoanApplicationCreateDTO dto) {
        return Result.success(loanApplicationService.createApplication(dto));
    }

    @GetMapping
    public Result<List<LoanApplicationVO>> list() {
        return Result.success(loanApplicationService.listMyApplications());
    }

    @GetMapping("/{id}")
    public Result<LoanApplicationVO> detail(@PathVariable("id") Long id) {
        return Result.success(loanApplicationService.getMyApplicationDetail(id));
    }
}
