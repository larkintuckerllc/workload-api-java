package com.example;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import io.spiffe.provider.SpiffeIdVerifier;
import io.spiffe.provider.SpiffeKeyManager;
import io.spiffe.provider.SpiffeVerificationException;
import io.spiffe.provider.SpiffeTrustManager;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.workloadapi.DefaultX509Source;
import io.spiffe.workloadapi.X509Source;

public class HelloWorldServer {

    public static void main(String[] args) throws Exception {
        String acceptedSpiffeIdEnv = System.getenv("ACCEPTED_SPIFFE_ID");
        if (acceptedSpiffeIdEnv == null) {
            throw new IllegalStateException("ACCEPTED_SPIFFE_ID environment variable is required");
        }
        SpiffeId acceptedSpiffeId = SpiffeId.parse(acceptedSpiffeIdEnv);
        SpiffeIdVerifier verifier = (id, chain) -> {
            if (!id.equals(acceptedSpiffeId)) {
                throw new SpiffeVerificationException(
                        "Peer SPIFFE ID " + id + " is not accepted");
            }
        };

        X509Source x509Source = DefaultX509Source.newSource();

        var sslContext = GrpcSslContexts
                .configure(SslContextBuilder
                        .forServer(new SpiffeKeyManager(x509Source))
                        .trustManager(new SpiffeTrustManager(x509Source, verifier)))
                .clientAuth(ClientAuth.REQUIRE)
                .build();

        Server server = NettyServerBuilder.forPort(50051)
                .addService(new GreeterImpl())
                .addService(ProtoReflectionService.newInstance())
                .sslContext(sslContext)
                .build()
                .start();

        System.out.println("Server started on port 50051");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server");
            server.shutdown();
        }));

        server.awaitTermination();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder()
                    .setMessage("Hello " + request.getName())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
