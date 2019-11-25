/*
 * Utils.java
 *
 * Created on 2019-11-25, 16:18
 */
package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Marc Nuri <marc@marcnuri.com> on 2019-11-25.
 */
class Utils {
  private Utils() {

  }

  static String execCommand(PodResource<Pod, DoneablePod> pod, String command) throws IOException, InterruptedException {
    try (
        final ByteArrayOutputStream result = new ByteArrayOutputStream()
    ) {
      final CountDownLatch cdl = new CountDownLatch(1);
      final ExecWatch ew = pod.redirectingInput().writingOutput(result)
          .usingListener(new AdaptedExecListener() {
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

  private interface AdaptedExecListener extends ExecListener {
    @Override
    default void onOpen(Response response) {
    }

    @Override
    default void onFailure(Throwable t, Response response) {
    }

    @Override
    default void onClose(int code, String reason) {
    }
  }
}
