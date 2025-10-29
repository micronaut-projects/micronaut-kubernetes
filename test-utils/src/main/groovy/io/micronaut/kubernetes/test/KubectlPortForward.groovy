/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.test

import io.kubernetes.client.util.wait.Wait
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.concurrent.TimeUnit

class KubectlPortForward implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(KubectlPortForward.class)

    private final Process process

    KubectlPortForward(String namespace, String podName, int podPort, int localPort) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("kubectl", "port-forward", "pod/" + podName, "$localPort:$podPort", "-n", namespace)
        pb.redirectErrorStream(true)
        process = pb.start()

        boolean ready = false
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine()
                while (line != null) {
                    if (line.contains("Forwarding from")) {
                        ready = true
                    }
                    LOG.info(line)
                    line = reader.readLine()
                }
            } catch (IOException ignored) {}
        }).start()
        Wait.poll(Duration.ofMillis(100), Duration.ofSeconds(5),() -> ready)
    }

    @Override
    void close() {
        if (process != null) {
            process.destroy()
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
