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

import fr.recia.sympaApi.dto.request.SympaListRequestForm;
import fr.recia.sympaApi.dto.response.SympaListResponseForDisplay;
import fr.recia.sympaApi.dto.response.UserSympaListDetail;
import fr.recia.sympaApi.pojo.SympaListCriterion;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.service.DomainService;
import fr.recia.sympaApi.service.DomainService.SympaListFields;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sympa")
public class SympaController {

  @Autowired
  DomainService domainService;

  @Autowired
  UserAttributesHandler userAttributesHandler;

  @GetMapping("/list")
  public ResponseEntity<SympaListResponseForDisplay> fetchSympaList( @RequestBody(required = false) SympaListRequestForm sympaListRequestForm) throws Exception {

  if(Objects.isNull(sympaListRequestForm)){
    sympaListRequestForm = new SympaListRequestForm(true, true, true);
  }

  List<UserSympaListWithUrl> sympaList = domainService.getWhich(formToCriterion(sympaListRequestForm),false);
    SympaListResponseForDisplay response = new SympaListResponseForDisplay();
    response.setAdminServiceUrl(userAttributesHandler.getIsAdminSympa().orElse(null));
    response.setUserSympaListDetailList(sympaList.stream().map(UserSympaListDetail::new).collect(Collectors.toList()));
    return ResponseEntity.ok().body(response);
}

  private List<SympaListCriterion> formToCriterion(SympaListRequestForm form) {
    if ( form == null ) return null;
    List<SympaListCriterion> crits = new ArrayList<SympaListCriterion>();
    if ( form.isEditor() )
      crits.add(new SympaListCriterion(SympaListFields.editor, true));
    if ( form.isOwner() )
      crits.add(new SympaListCriterion(SympaListFields.owner,true));
    if ( form.isSubscriber() )
      crits.add(new SympaListCriterion(SympaListFields.subscriber, true));
    return crits;
  }
}
