package alien4cloud.rest.health;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.async.ConsulResponseCallback;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.QueryOptions;

@Slf4j
public class ConsulSandbox {
    public static final String KEY = "service/A4C/leader";

    public static final int TIMEOUT_IN_MIN = 10;

    public static void main(String[] args) {

        Consul consul = Consul.builder().withConnectTimeoutMillis(1000 * 60).withReadTimeoutMillis(1000 * 60 * 60).build();
        final KeyValueClient kvClient = consul.keyValueClient();

        ConsulResponseCallback<Optional<Value>> callback = new ConsulResponseCallback<Optional<Value>>() {

            AtomicReference<BigInteger> index = new AtomicReference<BigInteger>(null);

            @Override
            public void onComplete(ConsulResponse<Optional<Value>> consulResponse) {

                if (consulResponse.getResponse().isPresent()) {
                    log.info("Response received");
                    Value response = consulResponse.getResponse().get();
                    if (response.getValue().isPresent()) {
                        String valueAsString = new String(BaseEncoding.base64().decode(response.getValue().get()));
                        log.info("Value is found: {}", valueAsString);
                    } else {
                        log.info("no value found");
                    }
                    if (response.getSession().isPresent()) {
                        log.info("Session is found: {}", response.getSession().get());
                    } else {
                        log.info("no session found");
                    }
                } else {
                    log.info("No Response received");
                }
                index.set(consulResponse.getIndex());
                watch();
            }

            void watch() {
                log.info("Watch: Block read key <{}>", KEY);
                log.info("--------------------------");
                kvClient.getValue(KEY, QueryOptions.blockMinutes(TIMEOUT_IN_MIN, index.get()).build(), this);
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Error encountered", throwable);
                watch();
            }
        };

        log.info("Block read key <{}>", KEY);
        kvClient.getValue(KEY, QueryOptions.blockMinutes(TIMEOUT_IN_MIN, new BigInteger("0")).build(), callback);
    }

}
