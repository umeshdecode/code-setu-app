package com.cargosetu.demo;

import com.cargosetu.utils.ApplicationUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/demo")
@RestController
public class DemoController {

    @GetMapping
    public String homePage(){
        return "Hello User: " + (ApplicationUtils.getCurrentUser().getUsername() !=null ? ApplicationUtils.getCurrentUser().getUsername() : "");
    }
}
