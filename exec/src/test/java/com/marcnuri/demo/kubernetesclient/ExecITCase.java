package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExecITCase {

  private static KubernetesClient kc;

  @BeforeAll
  static void initEnvironment() {
    kc = new KubernetesClientBuilder().build();
    deletePod();
    kc.pods().resource(
      new PodBuilder()
          .withNewSpec()
            .addNewContainer()
              .withImage("busybox")
              .withName("busybox")
              .withCommand(Collections.singletonList("top"))
            .endContainer()
          .endSpec()
          .withNewMetadata()
            .withName("busybox")
          .endMetadata()
      .build()
    ).create();
    kc.pods().withName("busybox").waitUntilReady(10, TimeUnit.SECONDS);
  }

  @AfterAll
  static void tearDown() {
    deletePod();
    kc = null;
  }

  private static void deletePod() {
    kc.pods().withName("busybox").withGracePeriod(1L).delete();
    kc.pods().withName("busybox").waitUntilCondition(p ->
      p == null || p.getMetadata().getDeletionTimestamp() != null, 10, TimeUnit.SECONDS);
  }

  @Test
  void execShouldWaitToCompleteAndCaptureOutput() throws Exception {
    // Given
    final CountDownLatch cdl = new CountDownLatch(1);
    final ExecListener waitToComplete = new ExecListener() {

      @Override
      public void onFailure(Throwable t, Response response) {
        cdl.countDown();
      }

      @Override
      public void onClose(int code, String reason) {
        cdl.countDown();
      }
    };
    try (
      final ByteArrayOutputStream result = new ByteArrayOutputStream()
    ) {
      // When
      kc.pods().withName("busybox")
        .redirectingInput().writingOutput(result)
        .usingListener(waitToComplete)
        .exec("sh", "-c", "sleep 8 && echo hello");
      cdl.await(10, TimeUnit.SECONDS);
      // Then
      assertThat(result.toString(StandardCharsets.UTF_8)).isEqualTo("hello\n");
    }
  }
}
