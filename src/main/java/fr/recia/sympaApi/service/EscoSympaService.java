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
import fr.recia.sympaApi.pojo.SympaListCriterion;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EscoSympaService {

  @Autowired
  DomainService domainService;


  public Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

    // HomeForm form = (HomeForm)command; //le filter coté front, maintenant géré par le front
    SympaListRequestForm form = new SympaListRequestForm(true, true, true);
    List<UserSympaListWithUrl> sympaList = domainService.getWhich(formToCriterion(form),false);
//    List<CreateListInfo> createList = domainService.getCreateListInfo(); // n'est pas utilisé pour liste diffusable (non admin)
//    String homeUrl = domainService.getHomeUrl();
    Map<String,Object> map = new HashMap<String, Object>();
//    map.put("homeUrl",homeUrl);
    map.put("sympaList", sympaList);
//    map.put("createList", createList);
    return map;

  }


  private List<SympaListCriterion> formToCriterion(SympaListRequestForm form) {
    if ( form == null ) return null;
    List<SympaListCriterion> crits = new ArrayList<SympaListCriterion>();
    if ( form.isEditor() )
      crits.add(new SympaListCriterion(DomainService.SympaListFields.editor,(form.isEditor())));
    if ( form.isOwner() )
      crits.add(new SympaListCriterion(DomainService.SympaListFields.owner,(form.isOwner())));
    if ( form.isSubscriber() )
      crits.add(new SympaListCriterion(DomainService.SympaListFields.subscriber,(form.isSubscriber())));
    return crits;
  }
}
