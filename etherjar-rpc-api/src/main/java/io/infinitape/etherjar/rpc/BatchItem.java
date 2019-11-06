/*
 * Copyright (c) 2016-2019 Igor Artamonov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.infinitape.etherjar.rpc;

import io.infinitape.etherjar.rpc.json.ResponseJson;
import io.infinitape.etherjar.rpc.transport.RpcTransport;

import java.util.Objects;

public abstract class BatchItem<PROC, JS, RES> implements AutoCloseable {
    protected int id;
    protected RpcCall<JS, RES> call;

    protected BatchItem(int id, RpcCall<JS, RES> call) {
        this.id = id;
        this.call = call;
    }

    /**
     * Called after completion on RPC server
     *
     * @param value value returned from server
     */
    public void onComplete(JS value) {
        RES val = value != null ? call.getConverter().apply(value) : null;
        this.onResult(val);
    }

    /**
     * Called on successful result of the call
     * @param value received value
     */
    public abstract void onResult(RES value);

    /**
     * Called if call failed to complete
     *
     * @param err error details
     * @see RpcException
     */
    public abstract void onError(RpcException err);

    /**
     * Get result
     *
     * @return result value
     */
    public abstract PROC getResult();

    /**
     * Called once batch is fully executed. It's always last call, after onComplete or onError
     * if they were called.
     */
    @Override
    public void close() {

    }

    /**
     * Read value from response
     *
     * @param resp RPC response data
     * @return true if parsed as result, or false if parsed as error
     * @throws ClassCastException if response data cannot be casted to expected data type of RpcCall
     */
    public boolean read(ResponseJson<?, Integer> resp) { //TODO duplicate reads
        if (resp.getError() != null) {
            onError(resp.getError().asException());
            return false;
        } else if (resp.getResult() == null) {
            onComplete(null);
            return true;
        } else if (!call.getJsonType().isAssignableFrom(resp.getResult().getClass())) {
            throw new ClassCastException("Expected " + call.getJsonType() + " but received " + resp.getResult().getClass());
        } else {
            onComplete((JS) resp.getResult());
            return true;
        }
    }

    /**
     * Read value from response
     *
     * @param resp RPC response data
     * @return true if parsed as result, or false if parsed as error
     * @throws ClassCastException if response data cannot be casted to expected data type of RpcCall
     */
    public boolean read(RpcTransport.RpcResponse<JS> resp) {
        if (resp.getError() != null) {
            onError(resp.getError());
            return false;
        } else if (resp.getPayload() == null) {
            onComplete(null);
            return true;
        } else if (!call.getJsonType().isAssignableFrom(resp.getPayload().getClass())) {
            throw new ClassCastException("Expected " + call.getJsonType() + " but received " + resp.getPayload().getClass());
        } else {
            onComplete(resp.getPayload());
            return true;
        }
    }


    public RpcCall<JS, RES> getCall() {
        return call;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchItem<?, ?, ?> batchItem = (BatchItem<?, ?, ?>) o;
        return id == batchItem.id &&
            Objects.equals(call, batchItem.call);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, call);
    }
}
