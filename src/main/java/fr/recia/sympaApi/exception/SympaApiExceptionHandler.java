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
package fr.recia.sympaApi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SympaApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .collect(Collectors.toMap(
        FieldError::getField,
        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage())
          .orElse("Invalid value")
      ));

    log.info("errors {}", errors);
    Map<String, Object> response = new HashMap<>();
    response.put("message", errors);
    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(CacheNameNotDefinedException.class)
  public ResponseEntity<?> handleCacheNameNotDefinedException(CacheNameNotDefinedException ex) {
    return ResponseEntity
      .internalServerError()
      .build();
  }

  @ExceptionHandler(IsNotAdminException.class)
  public ResponseEntity<?> handleIsNotAdminException(IsNotAdminException ex) {
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(Map.of(
        "message", ex.getMessage()
      ));
  }
}
