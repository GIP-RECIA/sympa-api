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


import fr.recia.sympaApi.dto.request.SympaListRequestForm;
import fr.recia.sympaApi.dto.response.SympaListResponseForDisplay;
import fr.recia.sympaApi.dto.response.UserSympaListDetail;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.utils.FormToCriterion;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Service
public class SympaService {

  @Autowired
  DomainService domainService;

  @Autowired
  FormToCriterion formToCriterion;

  @Autowired
  UserAttributesHandler userAttributesHandler;

  public SympaListResponseForDisplay fetchSympaList(SympaListRequestForm sympaListRequestForm) throws Exception {

    List<UserSympaListWithUrl> sympaList = domainService.getWhich(formToCriterion.formToCriterion(sympaListRequestForm),false);
    SympaListResponseForDisplay response = new SympaListResponseForDisplay();
    response.setAdminServiceUrl(userAttributesHandler.getIsAdminSympa().orElse(null));
    response.setUserSympaListDetailList(sympaList.stream().map(UserSympaListDetail::new).collect(Collectors.toList()));
    return response;

  }

}
