package id.ac.ui.cs.advprog.backend.auth.grpc;

import id.ac.ui.cs.advprog.bidmart.auth.grpc.AuthInternalServiceGrpc;
import id.ac.ui.cs.advprog.bidmart.auth.grpc.AuthServiceStatusRequest;
import id.ac.ui.cs.advprog.bidmart.auth.grpc.AuthServiceStatusResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;

@GrpcService
public class AuthInternalGrpcService extends AuthInternalServiceGrpc.AuthInternalServiceImplBase {

    private static final String SERVICE_NAME = "bidmart-auth-service";
    private static final String JWKS_PATH = "/.well-known/jwks.json";
    private static final String UNKNOWN_CALLER = "unknown-caller";

    private final String jwtIssuer;

    public AuthInternalGrpcService(@Value("${jwt.issuer:http://localhost:8081}") final String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    @Override
    public void getAuthServiceStatus(
            final AuthServiceStatusRequest request,
            final StreamObserver<AuthServiceStatusResponse> responseObserver
    ) {
        final String callerService = normalizeCaller(request.getCallerService());

        final AuthServiceStatusResponse response = AuthServiceStatusResponse.newBuilder()
                .setHealthy(true)
                .setServiceName(SERVICE_NAME)
                .setIssuer(jwtIssuer)
                .setJwksPath(JWKS_PATH)
                .setMessage("gRPC auth internal service reachable by " + callerService)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private static String normalizeCaller(final String callerService) {
        if (callerService == null || callerService.isBlank()) {
            return UNKNOWN_CALLER;
        }
        return callerService;
    }
}
