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

import fr.recia.sympaApi.config.bean.CorsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class SympaApiExceptionHandler {

  @Autowired
  private CorsProperties corsProperties;


  private HttpHeaders headersHandler(HttpServletRequest request){
    String origin = request.getHeader("Origin");
    HttpHeaders headers = new HttpHeaders();
    if(corsProperties.isAllowCredentials() && corsProperties.getAllowedOrigins().contains(origin)){
      headers.set("Access-Control-Allow-Origin", origin);
      headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      headers.set("Access-Control-Allow-Credentials", String.valueOf(corsProperties.isAllowCredentials()));
      log.info("allowed  origins contains {}", origin);
    } else {
      log.info("allowed origins does not contains {} - {}", origin, corsProperties.getAllowedOrigins());
    }
    return headers;
  }



  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
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
    return ResponseEntity.badRequest().headers(headersHandler(request)).body(response);
  }

  @ExceptionHandler(CacheNameNotDefinedException.class)
  public ResponseEntity<?> handleCacheNameNotDefinedException(CacheNameNotDefinedException ex, HttpServletRequest request) {
    return ResponseEntity
      .internalServerError()
      .headers(headersHandler(request))
      .build();
  }

  @ExceptionHandler(IsNotAdminException.class)
  public ResponseEntity<?> handleIsNotAdminException(IsNotAdminException ex, HttpServletRequest request) {
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .headers(headersHandler(request))
      .body(Map.of(
        "message", "Forbidden"
      ));
  }

  @ExceptionHandler(UserAttributeNotFoundException.class)
  public ResponseEntity<?> handleUserAttributeNotFoundException(UserAttributeNotFoundException ex, HttpServletRequest request) {
    log.error("User attribute not found for [{}]", ex.getMessage());
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .headers(headersHandler(request))
      .body(Map.of(
        "message", "Unexpected error"
      ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleException(UserAttributeNotFoundException ex, HttpServletRequest request) {
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .headers(headersHandler(request))
      .body(Map.of(
        "message", "Unexpected error"
      ));
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<?> handleIOException(IOException ex, HttpServletRequest request) {
    log.error("IOException occurred: ", ex);
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .headers(headersHandler(request))
      .body(Map.of("message", "Error during external request"));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<?> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
    log.error("RuntimeException occurred: ", ex);
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .headers(headersHandler(request))
      .body(Map.of("message", "Unexpected runtime error"));
  }
}
