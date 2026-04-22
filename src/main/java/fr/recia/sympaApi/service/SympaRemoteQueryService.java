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
import fr.recia.sympaApi.entity.Model;
import fr.recia.sympaApi.entity.ModelRequest;
import fr.recia.sympaApi.entity.PreparedRequest;
import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.pojo.RobotSympaInfo;
import fr.recia.sympaApi.service.listfinder.impl.HibernateDaoServiceImpl;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class SympaRemoteQueryService {

  @Autowired
  UserAttributesHandler userAttributesHandler;

  @Autowired
  HibernateDaoServiceImpl daoService;

  public String createCloseQuery(CloseListRequestPayload requestPayload){
    String listName = String.format("&listname=%s", requestPayload.getListName());
    final String operation = "operation=CLOSE"; //always in this RequestMapping
    return operation + listName;
  }

  @Autowired
  private RobotSympaConf robotSympaConf;

 public String createCreateOrUpdateQuery(String operation, CreateOrUpdateListRequestPayload requestPayload){

   String type = String.format("&type=%s", requestPayload.getType());   //var type = $("#createListURL_type").html() || " ";  MODEL NAME

   //add check of mandatory ?
   List<String> requiredAliases = allMandatoryPreparedRequestToStringList(requestPayload.getModelId()); // todo fetch required list

   log.debug("Required aliases list {}", requiredAliases);
   if (!requiredAliases.isEmpty()) {

     // si il manque au moins un groupe "MANDATORY" on tombe en erreur
     if (!new HashSet<>(List.of(requestPayload.getEditorsAliases().split("\\$"))).containsAll(requiredAliases)) {
       log.debug("Required aliases list {}", requestPayload.getEditorsAliases());
       throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[editorsAliases] parameter is missing required value(s)");
       //todo reason is not transmitted to browser
     }
   }

   String editorsAliases = Objects.isNull(requestPayload.getEditorsAliases()) ? "" : String.format("&editors_aliases=%s", requestPayload.getEditorsAliases()); //  args.get("editorAliases"));
   String editorsGroups = Objects.isNull(requestPayload.getEditorsGroups()) ? "" : String.format("&editors_groups=%s", requestPayload.getEditorsGroups());
   String typeParam = Objects.isNull(requestPayload.getTypeParam()) ? "" : String.format("&type_param=%s", requestPayload.getTypeParam());
   //todo test with missing type param when required

   // statics
   String policy = "&policy=newsletter"; // always

   // ces valeurs doivent venir du user info
   String siren = String.format("&siren=%s", userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT));
   String rne = String.format("&rne=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT));
   String uai = String.format("&uai=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT));

   // Get SympaRemote database Id
   final String sympaRemoteDatabaseId = retrieveSympaRemoteDatabaseId();
   final String dbId = "&databaseId=" + sympaRemoteDatabaseId;

   return operation + policy +
     type + siren + rne + uai + editorsAliases + editorsGroups + typeParam + dbId;
 }


  private List<String> allMandatoryPreparedRequestToStringList(String modelId) {
    List<PreparedRequest> listPreparedRequest = this.daoService.getAllPreparedRequests();
    List<String> idToStringList = new ArrayList<>();
    Model model = this.daoService.getModel(new BigInteger(modelId));
    for (PreparedRequest preparedRequest : listPreparedRequest) {
      ModelRequest modelRequest = this.daoService.getModelRequest(model, preparedRequest);
      if (modelRequest != null) {
        if (modelRequest.getCategoryAsEnum() == ModelRequest.ModelRequestRequired.MANDATORY) {
          idToStringList.add(preparedRequest.getId().toString());
        }
      }
    }
    return idToStringList;
  }

  /**
   * Retrieve the Sympa Remote endpoint URL from the HTTP session. or userInfo
   */
  public String retrieveSympaRemoteDatabaseId() {
    RobotSympaInfo rsi = getRobotInfo();
    String sympaRemoteDatabaseId = null;
    if (rsi != null) {
      log.info("retrieved sympa remote database id is {}",rsi.getSympaRemoteDatabaseId());
      sympaRemoteDatabaseId = rsi.getSympaRemoteDatabaseId();
    } else {
      log.debug("RobotSympaInfo est null");
    }
    return sympaRemoteDatabaseId;
  }

  private RobotSympaInfo getRobotInfo() {
    return robotSympaConf.getRobotSympaInfoByUai(userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT), userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF), true);
  }

}
