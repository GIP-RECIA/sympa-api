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
package fr.recia.sympaApi.service;

import fr.recia.sympaApi.dto.request.admin.CloseListRequestPayload;
import fr.recia.sympaApi.dto.request.admin.CreateOrUpdateListRequestPayload;
import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.pojo.RobotSympaInfo;
import fr.recia.sympaApi.repositories.*;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Before;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.validation.constraints.NotBlank;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
  "spring.session.store-type=none"
})
@ActiveProfiles("test")
public class AdminSympaServiceTest {

  @MockBean
  RedisConnectionFactory redisConnectionFactory;

  @MockBean
  DataSource dataSource;

  @MockBean
  EntityManagerFactory entityManagerFactory;

  @MockBean
  ModelRepository modelRepository;

  @MockBean
  ModelRequestRepository modelRequestRepository;

  @MockBean
  ModelSubscribersRepository modelSubscribersRepository;

  @MockBean
  PreparedRequestRepository preparedRequestRepository;

  @MockBean
  ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

  @Autowired
  SympaRemoteQueryService sympaRemoteQueryService;

  @Autowired
  AdminSympaService adminSympaService;

  @MockBean
  UserAttributesHandler userAttributesHandler;

  @MockBean
  RobotDomaineNameResolver robotDomaineNameResolver;

  @MockBean
  RobotSympaConf robotSympaConf;

  @Before
  public void setup() {
    Mockito.doReturn("domain-unit-test")
      .when(robotDomaineNameResolver)
      .resolveRobotDomainName();


  }


  @Test
  public void queryBuilderCloseOK() {
    String listName = "list-test-name@domain-unit-test";
    CloseListRequestPayload payload = new CloseListRequestPayload(listName);

    String queryFromService = sympaRemoteQueryService.createCloseQuery(payload);

    final String expectedQuery =
      "operation=CLOSE&listname=list-test-name@domain-unit-test";

    assertEquals("Query does not match expected output",
      expectedQuery,
      queryFromService);
  }

  @Test
  public void checkCurrentDomaineOK() {
    String listName = "list-test-name@domain-unit-test";
    CloseListRequestPayload payload = new CloseListRequestPayload(listName);

    boolean isCurrentDomain = adminSympaService.isCurrentDomain(payload);

    assertTrue("Domain name should be equals", isCurrentDomain);
  }

  @Test
  public void checkCurrentDomaineKO() {
    String listName = "list-test-name@wrong-domain";
    CloseListRequestPayload payload = new CloseListRequestPayload(listName);

    boolean isCurrentDomain = adminSympaService.isCurrentDomain(payload);

    assertFalse("Domain name should be different", isCurrentDomain);
  }

  //TODO isAdminRobotSympaByUai dois etre testé dans son propre test
//  @Test
//  public void isAdminOnCurrentDomainOK(){
//    String uaiCurrentTest="ABCDEF123456";
//    String group = "test:admin:Listes_Diffusion:local:TEST VALUE_"+uaiCurrentTest;
//    List<String> groups = List.of(group);
//    Mockito.doReturn(uaiCurrentTest)
//      .when(userAttributesHandler)
//      .getAttribute(eq(UserAttributesHandler.UAI_CURRENT));
//    Mockito.doReturn(groups)
//      .when(userAttributesHandler)
//      .getAttributeList(eq(UserAttributesHandler.IS_MEMBER_OF));
//
//     boolean isAdmin = adminSympaService.isAdminOnCurrentDomain();
//     assertTrue("Should be admin", isAdmin);
//  }
//
//
//  @Test
//  public void isAdminOnCurrentDomainKO(){
//    String uaiCurrentTest="ABCDEF123456";
//    String group = "test:admin:Listes_Diffusion:local:TEST VALUE_GHIJKL456789";
//    List<String> groups = List.of(group);
//    Mockito.doReturn(uaiCurrentTest)
//      .when(userAttributesHandler)
//      .getAttribute(eq(UserAttributesHandler.UAI_CURRENT));
//    Mockito.doReturn(groups)
//      .when(userAttributesHandler)
//      .getAttributeList(eq(UserAttributesHandler.IS_MEMBER_OF));
//
//    boolean isAdmin = adminSympaService.isAdminOnCurrentDomain();
//    assertFalse("Should not be admin", isAdmin);
//  }

  // TODO WIP
//  @Test
//  public void queryBuilderCreateOK(){
//
//    String sirenTest = "123456789";
//    String uaiTest = "ABCDEFGHIJ";
//
//    Mockito.doReturn(uaiTest)
//      .when(userAttributesHandler)
//      .getAttribute(eq(UserAttributesHandler.UAI_CURRENT));
//    Mockito.doReturn(sirenTest)
//      .when(userAttributesHandler)
//      .getAttribute(eq(UserAttributesHandler.SIREN_CURRENT));
//
//
//    RobotSympaInfo robotSympaInfo = new RobotSympaInfo();
//    robotSympaInfo.setSympaRemoteDatabaseId("database-id");
//
//    Mockito.doReturn(robotSympaInfo).when(robotSympaConf).getRobotSympaInfoByUai(anyString(), any(), eq(true));
//    String type =""; //todo rename to modelName ?
//
//    String modelId="";
//
//    String editorsAliases="";
//
//    String editorsGroups="";
//
//    String typeParam="";
//
//    CreateOrUpdateListRequestPayload payload = new CreateOrUpdateListRequestPayload(type, modelId, editorsAliases, editorsGroups, typeParam);
//
//    String queryFromService = sympaRemoteQueryService.createCreateOrUpdateQuery("CREATE",payload);
//
//    final String expectedQuery =
//      "operation=CLOSE&listname=list-test-name@domain-unit-test";
//
//    assertEquals("Query does not match expected output",
//      expectedQuery,
//      queryFromService);
//
//    //    return robotSympaConf.getRobotSympaInfoByUai(userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT),
//    //    userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF), true);
//
//    /*ing siren = String.format("&siren=%s", userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT));
//   String rne = String.format("&rne=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT));
//   String uai = String.format("&uai=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT));*/
//
//  }

  //TODO WIP
//  @Test
//  public void queryBuilderCreateKO(){
//
//    /*ing siren = String.format("&siren=%s", userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT));
//   String rne = String.format("&rne=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT));
//   String uai = String.format("&uai=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT));*/
//
//  }


}
