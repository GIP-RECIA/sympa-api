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

import fr.recia.sympaApi.dto.request.SympaListRequestForm;
import fr.recia.sympaApi.pojo.SympaListCriterion;
import fr.recia.sympaApi.service.DomainService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FormToCriterion {
  public List<SympaListCriterion> formToCriterion(SympaListRequestForm form) {
    if ( form == null ) return null;
    List<SympaListCriterion> crits = new ArrayList<>();
    if ( form.isEditor() )
      crits.add(new SympaListCriterion(DomainService.SympaListFields.editor, true));
    if ( form.isOwner() )
      crits.add(new SympaListCriterion(DomainService.SympaListFields.owner,true));
    if ( form.isSubscriber() )
      crits.add(new SympaListCriterion(DomainService.SympaListFields.subscriber, true));
    return crits;
  }
}
