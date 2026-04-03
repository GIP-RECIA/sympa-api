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
import fr.recia.sympaApi.dto.request.admin.CloseListRequestPayload;
import fr.recia.sympaApi.dto.request.admin.CreateOrUpdateListRequestPayload;
import fr.recia.sympaApi.dto.request.admin.CreateOrUpdateListFormDataRequestPayload;
import fr.recia.sympaApi.dto.response.admin.AdminSympaListResponseForDisplay;
import fr.recia.sympaApi.dto.response.admin.CreateOrUpdateListFormDataResponsePayload;
import fr.recia.sympaApi.groupfinder.impl.RegexGroupFinder;
import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.service.AdminService;
import fr.recia.sympaApi.service.DomainService;
import fr.recia.sympaApi.servlet.JsList;
import fr.recia.sympaApi.sympa.admin.EscoUserAttributeMapping;
import fr.recia.sympaApi.sympa.admin.LdapFilterSourceRequest;
import fr.recia.sympaApi.sympa.admin.LdapPerson;
import fr.recia.sympaApi.sympa.admin.RobotDomaineNameResolver;
import fr.recia.sympaApi.sympa.listfinder.services.HibernateDaoServiceImpl;
import fr.recia.sympaApi.utils.FormToCriterion;
import fr.recia.sympaApi.utils.SessionAttributesHandler;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

  @Autowired
  protected RobotDomaineNameResolver robotDomaineNameResolver;

  @Autowired
  HibernateDaoServiceImpl daoService;

  @Autowired
  protected LdapFilterSourceRequest ldapFilterSourceRequest;

  @Autowired
  private RobotSympaConf robotSympaConf;

  @Autowired
  protected RegexGroupFinder jsTreeGroupFinder;

  @Autowired
  private RestTemplate restTemplate;

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


  @GetMapping("/lists")
  public ResponseEntity<AdminSympaListResponseForDisplay> fetchLists(@RequestBody(required = false) SympaListRequestForm sympaListRequestForm) throws Exception {

    if(Objects.isNull(sympaListRequestForm)){
      sympaListRequestForm = new SympaListRequestForm(true, true, true);
    }

    Optional<AdminSympaListResponseForDisplay> adminSympaListResponseForDisplayOptional = Optional.ofNullable(adminService.fetchLists(sympaListRequestForm));

    return adminSympaListResponseForDisplayOptional.map(adminSympaListResponseForDisplay -> ResponseEntity.ok().body(adminSympaListResponseForDisplay)).orElseGet(() -> ResponseEntity.internalServerError().body(null));

  }

  @PostMapping("/createOrUpdateListFormData")
  public ResponseEntity<CreateOrUpdateListFormDataResponsePayload> createOrUpdateListFormData(@RequestBody CreateOrUpdateListFormDataRequestPayload requestPayload) {
    return ResponseEntity.ok(adminService.createOrUpdateListFormData(requestPayload));
  }

  @PostMapping("/createList")
  public ResponseEntity<Map<String, String>> createList(@RequestBody @Validated CreateOrUpdateListRequestPayload requestPayload) {
    String operation = "operation=CREATE"; //always in this RequestMapping
    String messageKey = adminService.createOrUpdate(requestPayload, operation);

    if(Objects.nonNull(messageKey)) {
      Map<String, String> responseMap = new HashMap<>();
      responseMap.put("messageKey", messageKey);
      if (responseMap.get("messageKey").contains("0")) {
        return ResponseEntity.internalServerError().body(responseMap);
      } else {
        return ResponseEntity.ok(responseMap);
      }
    }
    return ResponseEntity.ok(null);
  }

  @PostMapping("/updateList")
  public ResponseEntity<Map<String, String>> updateList(@RequestBody @Validated CreateOrUpdateListRequestPayload requestPayload) {
    //todo check if update cause exception if dont already exist
    String operation = "operation=UPDATE"; //always in this RequestMapping
    String messageKey = adminService.createOrUpdate(requestPayload, operation);

    if(Objects.nonNull(messageKey)) {
      Map<String, String> responseMap = new HashMap<>();
      responseMap.put("messageKey", messageKey);
      if (responseMap.get("messageKey").contains("0")) {
        return ResponseEntity.internalServerError().body(responseMap);
      } else {
        return ResponseEntity.ok(responseMap);
      }
    }

    return ResponseEntity.ok(null);
  }





  @PostMapping("/closeList")
  public @ResponseBody ResponseEntity<Map<String, String>> closeList(@RequestBody CloseListRequestPayload requestPayload) throws Exception {

    Map<String, String> responseMap = new HashMap<>();


    String listName = String.format("&listname=%s", requestPayload.getListName());

    // statics
    String operation = "operation=CLOSE"; //always in this RequestMapping
    String queryCreatedFromInputs = operation + listName;
    responseMap.put("queryCreatedFromInputs", queryCreatedFromInputs);


    // --- DEBUT CHECK ---
    // 1 check si le domaine de la liste à supprimer correspond au domaine courant
    // todo use trim !!!
    String[] listSplit = listName.split("@");
    Assert.isTrue(listSplit.length == 2, "List should have been split in 2 part around '@'");
    String domainName = robotDomaineNameResolver.resolveRobotDomainName();
    log.info("doCloseList check domain name {} against from the list {}", domainName, listSplit[1]);
    if (!listSplit[1].equals(domainName)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "List domain does not match current user establishment");
    }

    // 2 check si le user est admin sur cet etab
    boolean isAdmin = robotSympaConf.isAdminRobotSympaByUai(userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow(), userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF).orElseThrow());

    if(!isAdmin){
      return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new HashMap<>());
    }

    if (!listSplit[1].equals(domainName)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current user is not an admin for this establishment");
    }
    // --- FIN CHECK ---


    final String sympaRemoteEndpointUrl = this.adminService.retrieveSympaRemoteEndpointUrl();
    this.log.debug("Connecting to SympaRemote with the url [" + sympaRemoteEndpointUrl + "]");

    String errorCode = adminService.postToSympaRemote(sympaRemoteEndpointUrl, queryCreatedFromInputs);

    if(Objects.nonNull(errorCode)){
     responseMap.put("messageKey", adminService.errorCodeToMessageKey(errorCode, queryCreatedFromInputs));
     if(responseMap.get("messageKey").contains("0")){
       return ResponseEntity.internalServerError().body(responseMap);
     }
    }
    return ResponseEntity.ok(responseMap);
  }



  final static String createListAdditionalGroupsCacheKey = "createListAdditionalGroupsCache";

  @SuppressWarnings("unchecked")
  @GetMapping("/jstreeData")
  public @ResponseBody ResponseEntity<List<JsList>> jstreeData() {

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow();


    List<String> additionalGroups = null;
    Map<String, List<String>> createListAdditionalGroupsCache = null;

    try {
      //First check if we have results cached

      //todo put in REDIS cache, not session cache, in case two user from the same etabs use the service before cache expiry
      createListAdditionalGroupsCache = (Map<String, List<String>>)
        sessionAttributesHandler.getSessionAttribute(createListAdditionalGroupsCacheKey, Map.class).orElse(null);

    } catch (Exception ex) {
      log.error("", ex);
    }

    if ((createListAdditionalGroupsCache != null)
      && createListAdditionalGroupsCache.containsKey(uai)) {
      additionalGroups = createListAdditionalGroupsCache.get(uai);
      this.log.debug("Fetched additional groups from cache, size: " + additionalGroups.size());
    }

    //if the list was not in the cache, then fetch them
    if (additionalGroups == null) {
      //Fetch the list of available lists

      // Construct user info map to call the groups finder.
      Map<String, String> userInfo = new HashMap<String, String>();
      String uaiUserPropertyKey = UserAttributesHandler.UAI_CURRENT;
      userInfo.put(uaiUserPropertyKey, uai);

      Collection<String> additionalGroupsColl = this.jsTreeGroupFinder.findGroupsOfEtab(userInfo);

      additionalGroups = new ArrayList<>(additionalGroupsColl);
      Collections.sort(additionalGroups);

      //Stock in cache for later retrieval
      if (createListAdditionalGroupsCache != null) {
        createListAdditionalGroupsCache.put(uai, additionalGroups);
      }
    }

    List<JsList> rootNodes = new ArrayList<>();

    List<JsList> allNodes = new ArrayList<>();

    for (String groupStr : additionalGroups) {
      String[] levels = groupStr.split(":");
      int lastLevel = levels.length - 1;
      String nodeKey = "";
      JsList previousNode = null;

      for (int i = 0; i <= lastLevel; ++i) {
        String currentLevel = levels[i];
        nodeKey += i == 0 ? currentLevel : ":" + currentLevel;

        JsList node = JsList.getMatchingNodeOnKey(allNodes, nodeKey);

        if (Objects.isNull(node)) {
          node = new JsList();
          node.setData(currentLevel);
          node.setNodeKey(nodeKey);
          allNodes.add(node);

          if (i == 0) {
            rootNodes.add(node);
          }
          if (i != 0) {
            previousNode.getChildren().add(node);
          }
        }


        if (i == lastLevel) {
          node.getMetadata().put("groupName", groupStr);
          node.getAttr().put("rel", "group");
          //si groupe remettre clé entière en nom ou recombiner dans le JS ?
          node.setFolder(false);
        } else {
          node.getAttr().put("rel", "folder");
          node.setFolder(true);
        }

        //Set the html id of the nodes.  This is needed in order for the JSTree to work properly.  Must not contain special characters as jsTree does not handle them well.
        //As such, a hashcode is used which is unique enough and doesn't use special characters
        node.getAttr().put("id", "nodeId" + (groupStr + i).hashCode());
        node.setId("nodeId" + (groupStr + i).hashCode());

        previousNode = node;
      }

    }

    return ResponseEntity.ok(rootNodes);
  }








}
