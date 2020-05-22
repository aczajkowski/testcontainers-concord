package ca.ibodrov.concord.testcontainers;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2020 Ivan Bodrov
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Call;
import com.walmartlabs.concord.ApiClient;
import com.walmartlabs.concord.ApiException;
import com.walmartlabs.concord.client.ProcessApi;
import com.walmartlabs.concord.client.ProcessEntry;
import com.walmartlabs.concord.client.ProcessEntry.StatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

public class ProcessLogStreamer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ProcessLogStreamer.class);

    private static final String PATH_TEMPLATE = "/api/v1/process/%s/log";
    private static final long ERROR_DELAY = 5000;
    private static final long REQUEST_DELAY = 3000;
    private static final long RANGE_INCREMENT = 1024;
    private static final Type BYTE_ARRAY_TYPE = new TypeToken<byte[]>() {
    }.getType();
    private static final Set<StatusEnum> FINAL_STATUSES = new HashSet<>(Arrays.asList(
            StatusEnum.FINISHED,
            StatusEnum.CANCELLED,
            StatusEnum.FAILED,
            StatusEnum.TIMED_OUT
    ));

    private final ApiClient client;
    private final UUID instanceId;

    private long rangeStart = 0L;
    private Long rangeEnd;

    public ProcessLogStreamer(ApiClient client, UUID instanceId) {
        this.client = client;
        this.instanceId = instanceId;
    }

    @Override
    public void run() {
        Set<String> auths = client.getAuthentications().keySet();
        String[] authNames = auths.toArray(new String[0]);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("Range", "bytes=" + rangeStart + "-" + (rangeEnd != null ? rangeEnd : ""));

                String path = String.format(PATH_TEMPLATE, instanceId);
                Call c = client.buildCall(path, "GET", new ArrayList<>(), new ArrayList<>(), null, headers, new HashMap<>(), authNames, null);

                byte[] ab = client.<byte[]>execute(c, BYTE_ARRAY_TYPE).getData();
                if (ab.length > 0) {
                    String data = new String(ab);
                    for (String line : data.split("\n")) {
                        System.out.print("[PROCESS] ");
                        System.out.println(line);
                    }

                    rangeStart += ab.length;
                    rangeEnd = rangeStart + RANGE_INCREMENT;
                } else {
                    ProcessApi processApi = new ProcessApi(client);
                    ProcessEntry e = processApi.get(instanceId);
                    StatusEnum s = e.getStatus();
                    if (FINAL_STATUSES.contains(s)) {
                        log.info("Process {} is completed, stopping the log streaming...", instanceId);
                        break;
                    }
                }

                sleep(REQUEST_DELAY);
            } catch (ApiException e) {
                log.warn("Error while streaming the process' ({}) log: {}. Retrying in {}ms...", instanceId, e.getMessage(), ERROR_DELAY);
                sleep(ERROR_DELAY);
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
