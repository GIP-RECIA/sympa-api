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
package fr.recia.sympaApi.web.rest.api;

import fr.recia.sympaApi.config.custom.impl.UserCustomImplementation;
import fr.recia.sympaApi.dto.request.SympaListRequestForm;
import fr.recia.sympaApi.dto.response.admin.AdminSympaListResponseForDisplay;
import fr.recia.sympaApi.pojo.CreateListInfo;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.service.AdminService;
import fr.recia.sympaApi.service.DomainService;
import fr.recia.sympaApi.sympa.admin.EscoUserAttributeMapping;
import fr.recia.sympaApi.sympa.admin.LdapPerson;
import fr.recia.sympaApi.utils.FormToCriterion;
import fr.recia.sympaApi.utils.SessionAttributesHandler;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Getter
@RestController
@RequestMapping("/api/admin-sympa")
public class AdminSympaController {

  @Autowired
  UserAttributesHandler userAttributesHandler;

  @Autowired
  private EscoUserAttributeMapping userAttributeMapping;

  @Autowired
  private SessionAttributesHandler sessionAttributesHandler;

  @Autowired
  private LdapPerson ldapPerson;


  @Autowired
  private FormToCriterion formToCriterion;

  @Autowired
  private DomainService domainService;

  @Autowired
  private AdminService adminService;

  @GetMapping("/me")
  public ResponseEntity<Map<String,Object>> test(){
    Map<String, Object> responseMap = new HashMap<>();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication.getPrincipal() instanceof UserCustomImplementation) {
      UserCustomImplementation userCustomImplementation = (UserCustomImplementation)authentication.getPrincipal();
      responseMap.put("user info from cas ticket", userCustomImplementation.getAttributes());
      responseMap.put("principal", authentication.getPrincipal());
      responseMap.put("name",       authentication.getName());
    }

    return ResponseEntity.ok().body(responseMap);

  }


  @GetMapping("/list")
  public ResponseEntity<AdminSympaListResponseForDisplay> fetchList(@RequestBody(required = false) SympaListRequestForm sympaListRequestForm) throws Exception {

    if(Objects.isNull(sympaListRequestForm)){
      sympaListRequestForm = new SympaListRequestForm(true, true, true);
    }

    Map<String,Object> map = new HashMap<>();

    Map<String, String> userInfo = new HashMap<>();

    //enhanceUserInfo => add siren
    userInfo.put(UserAttributesHandler.UAI_CURRENT, userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElse(null));

    userInfo = this.getUserAttributeMapping().enhanceUserInfo(userInfo);

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElse(null);

    assert uai != null;

    List<String> isMemberOf = userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF).orElse(null);
    assert isMemberOf != null;


    List<UserSympaListWithUrl> sympaList;


    final String uid = SecurityContextHolder.getContext().getAuthentication().getName();// userInfo.get(UserInfoService.getPortalUidAttribute());

    Assert.hasText(uid, "UID shouldn't be empty !");
    Assert.hasText(uai, "UAI shouldn't be empty !");

    map.put("uai", uai);

    //Filter the user lists to make sure we only display lists that are in the current establishment.  This
    //is done by comparing the domain of the list address (after the @).
    //As domains are 1 to 1 with establishments
    //this can be used to tell what lists belong to which establishment.
    sympaList = this.getDomainService().getWhich(this.formToCriterion.formToCriterion(sympaListRequestForm), false);

    List<String> isMemberOfList = userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF).orElse(null);

    try {
      this.adminService.fetchIsAdmin(map, isMemberOfList, this.ldapPerson.getAdminRegex(), uai);
    } catch (Exception e) {
      log.error("exception during fetchIsAdmin", e);
    }

    if (Boolean.TRUE.equals(map.get("isListAdmin"))) {
      AdminSympaListResponseForDisplay response = this.adminService.fetchCreateListTableData(userInfo, sympaList);
      return ResponseEntity.ok().body(response);

    }

    return ResponseEntity.internalServerError().body(null);
  }


}
