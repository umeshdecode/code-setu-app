package com.cargosetu;

import io.jsonwebtoken.security.Keys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.SecretKey;
import java.util.Base64;

@SpringBootApplication(scanBasePackages = "com.cargosetu")
public class CargoSetuAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(CargoSetuAppApplication.class, args);
        System.out.println("************CargoSetuAppApplication Started Successfully************");

//        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
//        String password = bCryptPasswordEncoder.encode("superadmin");
//        System.out.println("password: "+ password);
    }

}
