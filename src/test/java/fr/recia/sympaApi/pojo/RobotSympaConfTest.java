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
package fr.recia.sympaApi.pojo;

import fr.recia.sympaApi.config.bean.RobotSympaConfProperties;
import fr.recia.sympaApi.repositories.ModelRepository;
import fr.recia.sympaApi.repositories.ModelRequestRepository;
import fr.recia.sympaApi.repositories.ModelSubscribersRepository;
import fr.recia.sympaApi.repositories.PreparedRequestRepository;
import fr.recia.sympaApi.service.AdminSympaService;
import fr.recia.sympaApi.service.RobotDomaineNameResolver;
import fr.recia.sympaApi.service.SympaRemoteQueryService;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
  "spring.session.store-type=none"
})
@ActiveProfiles("test")
public class RobotSympaConfTest {


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

  @Autowired
  SympaRemoteQueryService sympaRemoteQueryService;

  @Autowired
  AdminSympaService adminSympaService;

  @MockitoBean
  UserAttributesHandler userAttributesHandler;

  @MockitoBean
  RobotDomaineNameResolver robotDomaineNameResolver;

  @Before
  public void setup() {
//    Mockito.doReturn("domain-unit-test")
//      .when(robotDomaineNameResolver)
//      .resolveRobotDomainName();


  }

  @Autowired
  RobotSympaConf robotSympaConf;


  @Autowired
  RobotSympaConfProperties robotSympaConfProperties;



  @Test
  public void isAdminRobotSympaByUaiOK(){

//    Mockito.doReturn("").when(robotSympaConfProperties).getRegexFormatAdminByUai();

    log.info("regex is {}", robotSympaConfProperties.getRegexFormatAdminByUai());
    String uaiCurrentTest="ABCDEF123456";
    String group = "test:admin:Listes_Diffusion:local:TEST VALUE_"+uaiCurrentTest;
    List<String> groups = List.of(group);
    boolean isAdmin = robotSympaConf.isAdminRobotSympaByUai(uaiCurrentTest, groups);
    assertTrue("Should be admin", isAdmin);
  }

  @Test
  public void isAdminRobotSympaByUaiKO(){

//    Mockito.doReturn("").when(robotSympaConfProperties).getRegexFormatAdminByUai();

    log.info("regex is {}", robotSympaConfProperties.getRegexFormatAdminByUai());
    String uaiCurrentTest="ABCDEF123456";
    String group = "test:admin:Listes_Diffusion:local:TEST VALUE_GIJKL789123";
    List<String> groups = List.of(group);
    boolean isAdmin = robotSympaConf.isAdminRobotSympaByUai(uaiCurrentTest, groups);
    assertFalse("Should be admin", isAdmin);

  }


  /*
  *  public boolean isAdminRobotSympaByUai(String uai, List<String> userGrps) {
    String stem = findGrpStem(uai, userGrps, props.getRegexFormatAdminByUai());
    return  (stem != null) ;

  }*/

}
