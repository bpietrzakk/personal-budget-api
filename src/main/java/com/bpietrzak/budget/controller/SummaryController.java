package com.bpietrzak.budget.controller;

import com.bpietrzak.budget.dto.SummaryResponse;
import com.bpietrzak.budget.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping
    public SummaryResponse getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return summaryService.getSummary(from, to);
    }
}
