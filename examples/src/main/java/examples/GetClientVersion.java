package examples;

import io.emeraldpay.etherjar.domain.Wei;
import io.emeraldpay.etherjar.rpc.Commands;
import io.emeraldpay.etherjar.rpc.DefaultRpcClient;
import io.emeraldpay.etherjar.rpc.RpcClient;
import io.emeraldpay.etherjar.rpc.transport.DefaultRpcTransport;
import io.emeraldpay.etherjar.rpc.transport.RpcTransport;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GetClientVersion {
    public static void main(String[] args)
            throws URISyntaxException, IOException, ExecutionException, InterruptedException {

        try (RpcTransport trans = new DefaultRpcTransport(new URI("http://127.0.0.1:8545"))) {
            RpcClient client = new DefaultRpcClient(trans);
            Future<String> req = client.execute(Commands.web3().clientVersion());

            System.out.println(String.format("Client version: %s", req.get()));
        }
    }
}
