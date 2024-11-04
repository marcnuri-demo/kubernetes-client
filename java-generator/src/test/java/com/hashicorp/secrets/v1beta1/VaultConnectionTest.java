package com.hashicorp.secrets.v1beta1;

import io.fabric8.junit.jupiter.api.KubernetesTest;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@KubernetesTest
class VaultConnectionTest {

  KubernetesClient client;

  @BeforeEach
  void setUp() throws Exception {
    try (InputStream is = VaultConnectionTest.class.getResourceAsStream("/vault-connection.yaml")) {
      client.resource(is).createOr(NonDeletingOperation::update);
    }
    // Wait until the CRD is ready and API can accept our custom resources
    client.apiextensions().v1().customResourceDefinitions().waitUntilCondition(c -> {
      if (!Objects.equals(c.getMetadata().getName(), "vaultconnections.secrets.hashicorp.com")) {
        return false;
      }
      return c.getStatus().getConditions().stream()
        .anyMatch(cond -> Objects.equals(cond.getType(), "Established") && Objects.equals(cond.getStatus(), "True"));
    }, 10, TimeUnit.SECONDS);
  }

  @AfterEach
  void tearDown() throws Exception {
    try (InputStream is = VaultConnectionTest.class.getResourceAsStream("/vault-connection.yaml")) {
      client.resource(is).delete();
    }
  }

  @Test
  void create() {
    final var result = client
      .resource(new VaultConnectionBuilder().withNewMetadata().withName("my-vault-connection").endMetadata().build())
      .createOr(NonDeletingOperation::update);
    assertThat(result)
      .extracting(HasMetadata::getMetadata)
      .extracting("creationTimestamp")
      .isNotNull();
  }
}
