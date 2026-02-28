package com.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class HelloWorldClient {

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
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
