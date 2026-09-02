package com.family.finance.household.web;

import com.family.finance.common.web.ApiResponse;
import com.family.finance.household.dto.CreateHouseholdRequest;
import com.family.finance.household.dto.HouseholdResponse;
import com.family.finance.household.dto.JoinHouseholdRequest;
import com.family.finance.household.service.HouseholdService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households")
public class HouseholdController {
    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) { this.householdService = householdService; }

    @PostMapping
    public ApiResponse<HouseholdResponse> create(@Valid @RequestBody CreateHouseholdRequest request) {
        return ApiResponse.of(householdService.create(request));
    }

    @PostMapping("/join")
    public ApiResponse<HouseholdResponse> join(@Valid @RequestBody JoinHouseholdRequest request) {
        return ApiResponse.of(householdService.join(request));
    }

    @GetMapping("/current")
    public ApiResponse<HouseholdResponse> current() { return ApiResponse.of(householdService.current()); }

    @PostMapping("/invite-code/rotate")
    public ApiResponse<HouseholdResponse> rotateInviteCode() { return ApiResponse.of(householdService.rotateInviteCode()); }
}
