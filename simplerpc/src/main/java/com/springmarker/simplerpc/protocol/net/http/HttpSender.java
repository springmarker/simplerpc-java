package com.springmarker.simplerpc.protocol.net.http;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.springmarker.simplerpc.core.client.SenderInterface;
import com.springmarker.simplerpc.core.client.SyncCallback;
import com.springmarker.simplerpc.enums.FailedType;
import com.springmarker.simplerpc.exception.DeserializationException;
import com.springmarker.simplerpc.exception.SerializationException;
import com.springmarker.simplerpc.pojo.ExchangeRequest;
import com.springmarker.simplerpc.pojo.RpcRequest;
import com.springmarker.simplerpc.pojo.RpcResponse;
import com.springmarker.simplerpc.protocol.serialization.DataSerialization;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Springmarker
 * @date 2018/10/21 23:00
 */
public class HttpSender implements SenderInterface {

    private String url;

    /**
     * 用于反序列化的处理器。
     */
    private DataSerialization dataSerialization;

    /**
     * 此Sender类默认的OkHttp客户端。
     */
    private OkHttpClient okHttpClient = new OkHttpClient();

    public HttpSender(String url, DataSerialization dataSerialization) {
        this.url = url;
        this.dataSerialization = dataSerialization;
    }

    /**
     * 专门缓存Method的缓存，
     */
    private Cache<Integer, Method> cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private static final MediaType mediaType = MediaType.parse("application/octet-stream");

    /**
     * 根据RpcRequest构建OkHTTP的Request。
     *
     * @param rpcRequest
     * @return
     * @throws SerializationException
     */
    private Request buildOkHttpRequest(RpcRequest rpcRequest) throws SerializationException {
        byte[] serialize = dataSerialization.serialize(new ExchangeRequest(1, 1, rpcRequest));
        RequestBody requestBody = RequestBody.create(mediaType, serialize);
        return new Request.Builder().url(url).post(requestBody).build();
    }

    @Override
    public Object syncSend(RpcRequest rpcRequest) {
        Request request = null;

        try {
            request = buildOkHttpRequest(rpcRequest);
            Call call = okHttpClient.newCall(request);
            Response execute = call.execute();
            byte[] bytes = execute.body().bytes();
            RpcResponse rpcResponse = dataSerialization.deserializeResponse(bytes);
            return rpcResponse.getResult();
        } catch (SerializationException | DeserializationException | IOException e) {
            e.printStackTrace();
        }
        return null;

//
//        CompletableFuture<Object> future = send(rpcRequest);
//        Object join = future.join();
//        return join;
    }

    @Override
    public CompletableFuture<Object> asyncSend(RpcRequest rpcRequest) {
        return send(rpcRequest);
    }

    /**
     * 发送的主方法
     *
     * @param rpcRequest
     * @return
     */
    private CompletableFuture<Object> send(RpcRequest rpcRequest) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Request request;
        try {
            request = buildOkHttpRequest(rpcRequest);
        } catch (SerializationException e) {
            future.completeExceptionally(e);
            return future;
        }
        Call call = okHttpClient.newCall(request);

        SyncCallback syncCallback = new SyncCallback() {
            @Override
            public void onSuccess(Object object) {
                future.complete(object);
            }

            @Override
            public void onFailed(Throwable throwable, FailedType type) {
                future.completeExceptionally(throwable);
            }
        };
        call.enqueue(new OkHttpSyncCallback(syncCallback));
        return future;
    }


    private class OkHttpSyncCallback implements Callback {

        private SyncCallback syncCallback;

        OkHttpSyncCallback(SyncCallback syncCallback) {
            this.syncCallback = syncCallback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            syncCallback.onFailed(e, FailedType.RPC_EXCEPTION);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            try {
                Object result = deserializeResponse(response);
                syncCallback.onSuccess(result);
            } catch (DeserializationException e) {
                syncCallback.onFailed(e, FailedType.RPC_EXCEPTION);
            }
        }

        /**
         * 将OkHTTP的Response中的返回结果反序列化。
         *
         * @param response
         * @return
         */
        private Object deserializeResponse(Response response) throws DeserializationException, IOException {
            if (response.body() == null) {
                throw new DeserializationException("Http body is null");
            }
            byte[] bytes = response.body().bytes();
            RpcResponse rpcResponse = dataSerialization.deserializeResponse(bytes);
            return rpcResponse.getResult();
        }
    }
}