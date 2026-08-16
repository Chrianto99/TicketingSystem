package com.Chrianto.TicketingSystem.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/statistics")
public class StatisticsViewController {

    @GetMapping
    public String statistics() {
        return "statistics/list";
    }
}
