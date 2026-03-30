package com.innowise.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "randomOrgClient",
        url = "${random-org.url}"
)
public interface PaymentClient {
  @GetMapping("/integers/")
  String generateNumber(
          @RequestParam("num") int num,
          @RequestParam("min") int min,
          @RequestParam("max") int max,
          @RequestParam("col") int col,
          @RequestParam("base") int base,
          @RequestParam("format") String format,
          @RequestParam("rnd") String rnd
  );
}
