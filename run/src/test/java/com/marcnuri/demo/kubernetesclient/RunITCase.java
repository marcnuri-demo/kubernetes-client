package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.extended.run.RunConfigBuilder;
import io.fabric8.kubernetes.client.extended.run.RunOperations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RunITCase {

  private static final String NAMESPACE = "default";

  private static KubernetesClient kc;

  @BeforeAll
  static void initEnvironment() {
    kc = new KubernetesClientBuilder().build();
  }

  @AfterAll
  static void tearDown() {
    kc.pods().inNamespace(NAMESPACE).withLabel("app", "mn-kc-test-run").delete();
    kc = null;
  }

  @Test
  void run() {
    // Given
    final RunOperations.RunConfigNested run = kc.run()
      .inNamespace(NAMESPACE)
      .withNewRunConfig()
      .withName("kc-simple-run")
      .withImage("busybox")
      .withEnv(Collections.singletonMap("GREETING", "Hello Jessica Hyde!"))
      .withCommand("/bin/sh")
      .withArgs("-c", "echo $GREETING && sleep 3600")
      .withLabels(Collections.singletonMap("app", "mn-kc-test-run"));
    // When
    final Pod result = run.done();
    // Then
    assertThat(result)
      .isNotNull()
      .extracting("metadata.creationTimestamp")
      .asString()
      .isNotBlank();
    await()
      .atMost(30, TimeUnit.SECONDS)
      .ignoreExceptions()
      .untilAsserted(() ->
        assertThat(kc.pods()
          .inNamespace(result.getMetadata().getNamespace())
          .withName(result.getMetadata().getName())
          .getLog()
        ).contains("Hello Jessica Hyde!")
      );
  }
}
