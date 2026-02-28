package com.example;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.spiffe.provider.SpiffeIdVerifier;
import io.spiffe.provider.SpiffeKeyManager;
import io.spiffe.provider.SpiffeTrustManager;
import io.spiffe.workloadapi.DefaultX509Source;
import io.spiffe.workloadapi.X509Source;

public class HelloWorldClient {

    public static void main(String[] args) throws Exception {
        X509Source x509Source = DefaultX509Source.newSource();

        var sslContext = GrpcSslContexts
                .configure(SslContextBuilder
                        .forClient()
                        .keyManager(new SpiffeKeyManager(x509Source))
                        .trustManager(new SpiffeTrustManager(x509Source, (SpiffeIdVerifier) (id, chain) -> {})))
                .build();

        String host = System.getenv().getOrDefault("GRPC_SERVER_HOST", "localhost");

        ManagedChannel channel = NettyChannelBuilder.forAddress(host, 50051)
                .sslContext(sslContext)
                .build();

        try {
            GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
            HelloReply response = stub.sayHello(
                    HelloRequest.newBuilder().setName("World").build()
            );
            System.out.println("Response: " + response.getMessage());
        } finally {
            channel.shutdown();
        }
    }
}
