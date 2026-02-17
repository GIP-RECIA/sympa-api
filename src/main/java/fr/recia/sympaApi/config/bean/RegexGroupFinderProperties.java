/**
 * Copyright © 2026 GIP-RECIA (https://www.recia.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.recia.sympaApi.config.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "groupfinder.regex")
@Data
@Validated
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class RegexGroupFinderProperties {

  String regularExpressionFilter;

  @PostConstruct
  public void setupAndDebug() {
    log.info("RegexGroupFinderProperties {}", this);
  }

  @Override
  public String toString() {
    return "RegexGroupFinderProperties{" +
      "regularExpressionFilter='" + regularExpressionFilter + '\'' +
      '}';
  }
}
