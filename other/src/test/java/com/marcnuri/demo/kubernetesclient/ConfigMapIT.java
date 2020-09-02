package com.marcnuri.demo.kubernetesclient;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigMapIT {

  private static final String NAMESPACE = "default";
  private static final String APP_LABEL = "mn-kc-test-config-map";

  private static KubernetesClient kc;

  @BeforeAll
  static void initEnvironment() {
    kc = new DefaultKubernetesClient();
  }

  @AfterAll
  static void tearDown() {
    kc.configMaps().inNamespace(NAMESPACE).withLabel("app", APP_LABEL).delete();
    kc = null;
  }

  @Test
  void createOrReplace() {
    // Given
    // @formatter:off
    final ConfigMap cm = new ConfigMapBuilder()
      .withNewMetadata()
        .withName("kc-simple-config-map")
        .withLabels(Collections.singletonMap("app", APP_LABEL))
      .endMetadata()
      .build();
    // @formatter:on
    final ConfigMap original = kc.configMaps().inNamespace(NAMESPACE).withName("kc-simple-config-map").create(cm);
    cm.setData(Collections.singletonMap("NEW_VAR", "NEW"));
    // When
    final ConfigMap replaced = kc.configMaps().inNamespace(NAMESPACE).createOrReplace(cm);
    // Then
    assertThat(replaced)
      .isNotEqualTo(original)
      .extracting(ConfigMap::getData)
      .asInstanceOf(InstanceOfAssertFactories.MAP)
      .containsEntry("NEW_VAR", "NEW");
  }

  @Test
  void createOrReplaceFromFile() {
    // When
    final List<HasMetadata> result = kc.load(ConfigMapIT.class.getResourceAsStream("/config-map.yaml"))
      .createOrReplace();
    // Then
    assertThat(result)
      .hasSize(1)
      .element(0)
      .hasFieldOrPropertyWithValue("metadata.name", "app-config")
      .extracting("data").asInstanceOf(InstanceOfAssertFactories.MAP)
      .containsKey("app-config.yml");
  }
}
