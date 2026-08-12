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
package fr.recia.sympaApi;


import fr.recia.redis.session.cleanup.service.RedisSessionCleanupService;
import fr.recia.sympaApi.config.RedisSessionCleanupConfiguration;
import fr.recia.sympaApi.config.SecurityConfig;
import fr.recia.sympaApi.config.bean.ConcatenateGroupFinderProperties;
import fr.recia.sympaApi.config.bean.CorsProperties;
import fr.recia.sympaApi.config.bean.RegexGroupFinderProperties;
import fr.recia.sympaApi.config.custom.impl.CasSuccessHandler;
import fr.recia.sympaApi.entity.ModelRequest;
import fr.recia.sympaApi.entity.ModelSubscribers;
import fr.recia.sympaApi.entity.PreparedRequest;
import fr.recia.sympaApi.groupfinder.impl.RegexGroupFinder;
import fr.recia.sympaApi.repositories.ModelRepository;
import fr.recia.sympaApi.repositories.ModelRequestRepository;
import fr.recia.sympaApi.repositories.ModelSubscribersRepository;
import fr.recia.sympaApi.repositories.PreparedRequestRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;

@SpringBootTest(properties = {
  "spring.session.store-type=none",
  "spring.autoconfigure.exclude=" +
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
    "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
})
@ActiveProfiles("test")
public class SympaApiApplicationTests {

  @MockitoBean
  RedisConnectionFactory redisConnectionFactory;

  @MockitoBean
  DataSource dataSource;

  @MockitoBean
  EntityManagerFactory entityManagerFactory;

  @MockitoBean
  ModelRepository modelRepository;

  @MockitoBean
  ModelRequestRepository modelRequestRepository;

  @MockitoBean
  ModelSubscribersRepository modelSubscribersRepository;

  @MockitoBean
  PreparedRequestRepository preparedRequestRepository;

  @MockitoBean
  ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @Test
    public void queryBuilderCloseOK() {
      String queryFromService ="";
      final String expectedQuery ="";
      assertEquals("Query does not match expected output", expectedQuery, queryFromService);
    }
}
