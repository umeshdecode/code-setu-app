package com.cargosetu.controller;

import com.cargosetu.config.utils.UrlConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(UrlConstants.pages)
public class ViewController {

    @GetMapping("/login")
    public String getLoginPage(){
        return "login";
    }
}
