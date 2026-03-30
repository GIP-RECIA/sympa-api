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
package fr.recia.sympaApi.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Optional;

@Service
@Slf4j
public class SessionAttributesHandler {

  @Autowired
  private HttpSession session;




  public <T> void setSessionAttribute(String key, T value) {
    if (session == null) {
      log.info("session is null");
      return;
    }
    session.setAttribute(key, value);
  }


  public <T> Optional<T> getSessionAttribute(String key, Class<T> type) {
    if (session == null) {
      log.info("session is null");
      return Optional.empty();
    }

    Object attributeRaw = session.getAttribute(key);
    if (type.isInstance(attributeRaw)) {
      return Optional.of(type.cast(attributeRaw));
    }

    return Optional.empty();
  }
}
