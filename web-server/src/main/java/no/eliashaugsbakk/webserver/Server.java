package no.eliashaugsbakk.webserver;

import com.sun.net.httpserver.HttpServer;
import no.eliashaugsbakk.webserver.api.BlogHandler;
import no.eliashaugsbakk.webserver.api.UploadHandler;
import no.eliashaugsbakk.webserver.db.PageRepository;
import no.eliashaugsbakk.webserver.db.TokenRepository;
import no.eliashaugsbakk.webserver.service.PostStorageService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
    private final int port = 8000;

    public void start(PageRepository pageRepo, PostStorageService postStorage, TokenRepository tokenRepo) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        BlogHandler blogHandler = new BlogHandler(pageRepo);
        server.createContext("/blog", blogHandler);

        UploadHandler uploadHandler = new UploadHandler(postStorage, tokenRepo);
        server.createContext("/upload", uploadHandler);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        IO.println("Server is live on port " + port);
    }
}
