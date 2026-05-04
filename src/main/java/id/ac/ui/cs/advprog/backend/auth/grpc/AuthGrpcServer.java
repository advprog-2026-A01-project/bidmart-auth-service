package id.ac.ui.cs.advprog.backend.auth.grpc;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "auth.grpc.enabled", havingValue = "true", matchIfMissing = true)
public class AuthGrpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthGrpcServer.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5L;

    private final AuthInternalGrpcService authInternalGrpcService;
    private final int grpcPort;

    private Server server;

    public AuthGrpcServer(
            final AuthInternalGrpcService authInternalGrpcService,
            @Value("${auth.grpc.port:9091}") final int grpcPort
    ) {
        this.authInternalGrpcService = authInternalGrpcService;
        this.grpcPort = grpcPort;
    }

    @PostConstruct
    public void start() {
        try {
            server = NettyServerBuilder.forPort(grpcPort)
                    .addService(authInternalGrpcService)
                    .build()
                    .start();

            LOGGER.info("BidMart Auth gRPC server started on port {}", grpcPort);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start BidMart Auth gRPC server", exception);
        }
    }

    @PreDestroy
    public void stop() {
        if (server == null) {
            return;
        }

        server.shutdown();

        try {
            if (!server.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
            LOGGER.info("BidMart Auth gRPC server stopped");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
            LOGGER.warn("Interrupted while stopping BidMart Auth gRPC server", exception);
        }
    }
}
