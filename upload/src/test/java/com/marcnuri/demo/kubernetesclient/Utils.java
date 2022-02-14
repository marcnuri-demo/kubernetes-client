/*
 * Utils.java
 *
 * Created on 2019-11-25, 16:18
 */
package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class Utils {

  private Utils() {}

  static String execCommand(PodResource<Pod> pod, String command) throws IOException, InterruptedException {
    try (
        final ByteArrayOutputStream result = new ByteArrayOutputStream()
    ) {
      final CountDownLatch cdl = new CountDownLatch(1);
      final ExecWatch ew = pod.redirectingInput().writingOutput(result)
          .usingListener(new ExecListener() {
            @Override
            public void onFailure(Throwable t, Response response) {
              cdl.countDown();
            }

            @Override
            public void onClose(int code, String reason) {
              cdl.countDown();
            }
          })
          .exec("sh");
      ew.getInput().write(String.format("%s\nexit\n", command).getBytes());
      cdl.await(10, TimeUnit.SECONDS);
      return result.toString(StandardCharsets.UTF_8.name());
    }
  }
}
