package com.family.finance.household.web;

import com.family.finance.common.web.ApiResponse;
import com.family.finance.household.dto.MemberResponse;
import com.family.finance.household.dto.UpdateMemberRequest;
import com.family.finance.household.dto.UpdateMemberStatusRequest;
import com.family.finance.household.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) { this.memberService = memberService; }

    @GetMapping
    public ApiResponse<List<MemberResponse>> list() { return ApiResponse.of(memberService.list()); }

    @PatchMapping("/{id}")
    public ApiResponse<MemberResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateMemberRequest request) {
        return ApiResponse.of(memberService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<MemberResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateMemberStatusRequest request) {
        return ApiResponse.of(memberService.updateStatus(id, request));
    }
}
