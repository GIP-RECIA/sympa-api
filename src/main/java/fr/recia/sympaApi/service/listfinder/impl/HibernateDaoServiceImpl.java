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
package fr.recia.sympaApi.service.listfinder.impl;


import fr.recia.sympaApi.repositories.ModelRepository;
import fr.recia.sympaApi.repositories.ModelRequestRepository;
import fr.recia.sympaApi.repositories.ModelSubscribersRepository;
import fr.recia.sympaApi.repositories.PreparedRequestRepository;
import fr.recia.sympaApi.service.listfinder.IDaoService;
import fr.recia.sympaApi.service.UserAttributeMapping;
import fr.recia.sympaApi.model.IMailingListModel;
import fr.recia.sympaApi.model.impl.BasicMailingListModel;
import fr.recia.sympaApi.entity.Model;
import fr.recia.sympaApi.entity.ModelRequest;
import fr.recia.sympaApi.entity.ModelRequestId;
import fr.recia.sympaApi.entity.ModelSubscribers;
import fr.recia.sympaApi.entity.PreparedRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Hiberate implementation of the DAO service.
 * 
 * See /properties/dao/dao-example.xml
 */
@Getter
@Setter
@Service
public class HibernateDaoServiceImpl
  implements IDaoService {

  @Autowired
  private UserAttributeMapping userAttributeMapping;

  @Autowired
  ModelRepository modelRepository;

  @Autowired
  private ModelRequestRepository modelRequestRepository;

  @Autowired
  private ModelSubscribersRepository modelSubscribersRepository;

  @Autowired
  private PreparedRequestRepository preparedRequestRepository;


  public List<ModelSubscribers> getAllModelSubscribers() {
    return modelSubscribersRepository.findAll();
  }

  /**
   * @return a list of groups that could have access to the list
   */
  public List<ModelRequest> getAllModelRequests() {
    return modelRequestRepository.findAll();
  }

  /**
   * @return a list of groups that could have access to the list
   */
  public List<Model> getAllModels() {
    return modelRepository.findAll();
  }

  public Model getModel(final BigInteger id) {

    Optional<Model> modelOptional = modelRepository.findById(id);
    return modelOptional.orElse(null);
  }

  public ModelSubscribers getModelSubscriber(final Model model) {

    return modelSubscribersRepository.findByIdId(model.getId()).orElse(null);

  }

  /**
   * @return a list of groups that could have access to the list
   */
  @SuppressWarnings("unchecked")
  public List<PreparedRequest> getAllPreparedRequests() {
    return this.getPreparedRequestRepository().findAll();
  }

  /**
   * @param preparedRequest
   * @param model
   * @return
   */
  public ModelRequest getModelRequest(final Model model, final PreparedRequest preparedRequest) {

    ModelRequestId modelRequestId = new ModelRequestId();
    modelRequestId.setIdModel(model.getId());
    modelRequestId.setIdRequest(preparedRequest.getId());

    return modelRequestRepository.findById(modelRequestId).orElse(null);

  }

  // ////////////////////////////////////////////////////////////
  // misc
  // ////////////////////////////////////////////////////////////
  public List<IMailingListModel> getMailingListModels(final List<Model> listModels, final Map<String, String> userInfo) {
    List<IMailingListModel> listMailingListModels = new ArrayList<IMailingListModel>();

    // Convert hibernate objects into IMailingListModel objects
    for (Model model : listModels) {
      BasicMailingListModel mailingListModel = new BasicMailingListModel(model
        .getId().toString(),
        model.getListname(),
        this.userAttributeMapping.substitutePlaceholder(model.getPattern(), userInfo),
        model.getDescription());
      listMailingListModels.add(mailingListModel);
    }

    return listMailingListModels;
  }

}
