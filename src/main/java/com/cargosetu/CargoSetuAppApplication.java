package com.cargosetu;

import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.SecretKey;
import java.util.Base64;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.cargosetu")
public class CargoSetuAppApplication {

	public static void main(String[] args) {
//        SpringApplication.run(CargoSetuAppApplication.class, args);
        ConfigurableApplicationContext context =
                SpringApplication.run(CargoSetuAppApplication.class, args);

        Environment env = context.getEnvironment();

        System.out.println("************ CargoSetuAppApplication Started Successfully ************");
        System.out.println("Active Profiles : " + String.join(", ", env.getActiveProfiles()));
        System.out.println("Database URL    : " + env.getProperty("spring.datasource.url"));

//        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
//        String password = bCryptPasswordEncoder.encode("superadmin");
//        System.out.println("password: "+ password);
    }

}
