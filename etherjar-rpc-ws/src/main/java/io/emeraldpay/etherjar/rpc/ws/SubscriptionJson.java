/*
 * Copyright (c) 2016-2019 Igor Artamonov, All Rights Reserved.
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
package io.emeraldpay.etherjar.rpc.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.emeraldpay.etherjar.rpc.RpcResponseError;
import io.emeraldpay.etherjar.rpc.json.BlockJson;
import io.emeraldpay.etherjar.rpc.json.BlockJsonDeserializer;
import io.emeraldpay.etherjar.rpc.json.TransactionRefJson;

/**
 * JSON emitted from Websocket
 *
 * @author Igor Artamonov
 */
@JsonDeserialize(using = SubscriptionJsonDeserializer.class)
public class SubscriptionJson {

    private static BlockJsonDeserializer blockJsonDeserializer = new BlockJsonDeserializer();

    private String subscription;
    private JsonNode result;
    private Integer id;
    private JsonNode error;

    public String getSubscription() {
        return subscription;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    public JsonNode getResult() {
        return result;
    }

    public void setResult(JsonNode result) {
        this.result = result;
    }

    @SuppressWarnings("unchecked")
    public BlockJson<TransactionRefJson> getBlockResult() {
        return (BlockJson<TransactionRefJson>) blockJsonDeserializer.deserialize(result);
    }

    public String getStringResult() {
        return result == null ? null : result.asText();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public JsonNode getError() {
        return error;
    }

    public void setError(JsonNode error) {
        this.error = error;
    }

    public RpcResponseError extractError() {
        if (this.error == null) {
            return null;
        }
        return new RpcResponseError(
            this.error.has("code") ? this.error.get("code").asInt() : 0,
            this.error.has("message") ? this.error.get("message").asText() : "UNKNOWN ERROR",
            this.error.has("data") ? this.error.get("data").asText() : null
        );
    }
}
