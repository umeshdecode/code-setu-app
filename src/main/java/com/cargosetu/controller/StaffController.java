package com.cargosetu.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.urls.BaseURL;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = BaseURL.BASE_URL +UrlConstants.StaffUrlConstants.STAFF)
public class StaffController {


}
