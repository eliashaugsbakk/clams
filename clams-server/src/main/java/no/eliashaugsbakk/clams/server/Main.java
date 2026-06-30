package no.eliashaugsbakk.clams.server;

import io.javalin.Javalin;

public class Main {
  void main() {
    var app = Javalin.create(config -> {
      config.routes.get("/", ctx -> ctx.result("Hello World"));
    }).start(7070);
  }
}
