package com.richardbrenkus.shiftschedulermodernized;

import com.richardbrenkus.shiftschedulermodernized.config.PasswordEncoderConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShiftSchedulerModernizedApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiftSchedulerModernizedApplication.class, args);
        PasswordEncoderConfig encoder = new PasswordEncoderConfig();

        //System.out.println("Password: " + encoder.passwordEncoder().encode("alajos"));
    }

}
