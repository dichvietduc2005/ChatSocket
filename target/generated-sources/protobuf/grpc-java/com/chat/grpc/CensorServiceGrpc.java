package com.chat.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.54.0)",
    comments = "Source: censor.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class CensorServiceGrpc {

  private CensorServiceGrpc() {}

  public static final String SERVICE_NAME = "CensorService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.chat.grpc.CensorProto.TextRequest,
      com.chat.grpc.CensorProto.ProfanityResponse> getCheckProfanityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckProfanity",
      requestType = com.chat.grpc.CensorProto.TextRequest.class,
      responseType = com.chat.grpc.CensorProto.ProfanityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.chat.grpc.CensorProto.TextRequest,
      com.chat.grpc.CensorProto.ProfanityResponse> getCheckProfanityMethod() {
    io.grpc.MethodDescriptor<com.chat.grpc.CensorProto.TextRequest, com.chat.grpc.CensorProto.ProfanityResponse> getCheckProfanityMethod;
    if ((getCheckProfanityMethod = CensorServiceGrpc.getCheckProfanityMethod) == null) {
      synchronized (CensorServiceGrpc.class) {
        if ((getCheckProfanityMethod = CensorServiceGrpc.getCheckProfanityMethod) == null) {
          CensorServiceGrpc.getCheckProfanityMethod = getCheckProfanityMethod =
              io.grpc.MethodDescriptor.<com.chat.grpc.CensorProto.TextRequest, com.chat.grpc.CensorProto.ProfanityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckProfanity"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.chat.grpc.CensorProto.TextRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.chat.grpc.CensorProto.ProfanityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CensorServiceMethodDescriptorSupplier("CheckProfanity"))
              .build();
        }
      }
    }
    return getCheckProfanityMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CensorServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CensorServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CensorServiceStub>() {
        @java.lang.Override
        public CensorServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CensorServiceStub(channel, callOptions);
        }
      };
    return CensorServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CensorServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CensorServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CensorServiceBlockingStub>() {
        @java.lang.Override
        public CensorServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CensorServiceBlockingStub(channel, callOptions);
        }
      };
    return CensorServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CensorServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CensorServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CensorServiceFutureStub>() {
        @java.lang.Override
        public CensorServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CensorServiceFutureStub(channel, callOptions);
        }
      };
    return CensorServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void checkProfanity(com.chat.grpc.CensorProto.TextRequest request,
        io.grpc.stub.StreamObserver<com.chat.grpc.CensorProto.ProfanityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckProfanityMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service CensorService.
   */
  public static abstract class CensorServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return CensorServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service CensorService.
   */
  public static final class CensorServiceStub
      extends io.grpc.stub.AbstractAsyncStub<CensorServiceStub> {
    private CensorServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CensorServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CensorServiceStub(channel, callOptions);
    }

    /**
     */
    public void checkProfanity(com.chat.grpc.CensorProto.TextRequest request,
        io.grpc.stub.StreamObserver<com.chat.grpc.CensorProto.ProfanityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckProfanityMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service CensorService.
   */
  public static final class CensorServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<CensorServiceBlockingStub> {
    private CensorServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CensorServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CensorServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.chat.grpc.CensorProto.ProfanityResponse checkProfanity(com.chat.grpc.CensorProto.TextRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckProfanityMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service CensorService.
   */
  public static final class CensorServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<CensorServiceFutureStub> {
    private CensorServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CensorServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CensorServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.chat.grpc.CensorProto.ProfanityResponse> checkProfanity(
        com.chat.grpc.CensorProto.TextRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckProfanityMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CHECK_PROFANITY = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CHECK_PROFANITY:
          serviceImpl.checkProfanity((com.chat.grpc.CensorProto.TextRequest) request,
              (io.grpc.stub.StreamObserver<com.chat.grpc.CensorProto.ProfanityResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getCheckProfanityMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.chat.grpc.CensorProto.TextRequest,
              com.chat.grpc.CensorProto.ProfanityResponse>(
                service, METHODID_CHECK_PROFANITY)))
        .build();
  }

  private static abstract class CensorServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CensorServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.chat.grpc.CensorProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CensorService");
    }
  }

  private static final class CensorServiceFileDescriptorSupplier
      extends CensorServiceBaseDescriptorSupplier {
    CensorServiceFileDescriptorSupplier() {}
  }

  private static final class CensorServiceMethodDescriptorSupplier
      extends CensorServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    CensorServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (CensorServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CensorServiceFileDescriptorSupplier())
              .addMethod(getCheckProfanityMethod())
              .build();
        }
      }
    }
    return result;
  }
}
