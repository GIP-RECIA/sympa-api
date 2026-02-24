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

import fr.recia.sympaApi.config.bean.DebugProperties;
import fr.recia.sympaApi.config.custom.impl.UserCustomImplementation;
import fr.recia.sympaApi.dto.request.SympaListRequestForm;
import fr.recia.sympaApi.dto.request.admin.CloseListRequestPayload;
import fr.recia.sympaApi.dto.request.admin.CreateOrUpdateListRequestPayload;
import fr.recia.sympaApi.dto.request.admin.CreateOrUpdateListFormDataRequestPayload;
import fr.recia.sympaApi.dto.response.admin.AdminSympaListResponseForDisplay;
import fr.recia.sympaApi.dto.response.admin.CreateOrUpdateListFormDataResponsePayload;
import fr.recia.sympaApi.groupfinder.impl.RegexGroupFinder;
import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.pojo.RobotSympaInfo;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.service.AdminService;
import fr.recia.sympaApi.service.DomainService;
import fr.recia.sympaApi.servlet.JsCreateListRow;
import fr.recia.sympaApi.servlet.JsList;
import fr.recia.sympaApi.sympa.admin.EscoUserAttributeMapping;
import fr.recia.sympaApi.sympa.admin.LdapFilterSourceRequest;
import fr.recia.sympaApi.sympa.admin.LdapPerson;
import fr.recia.sympaApi.sympa.admin.RobotDomaineNameResolver;
import fr.recia.sympaApi.sympa.listfinder.model.Model;
import fr.recia.sympaApi.sympa.listfinder.model.ModelRequest;
import fr.recia.sympaApi.sympa.listfinder.model.ModelSubscribers;
import fr.recia.sympaApi.sympa.listfinder.model.PreparedRequest;
import fr.recia.sympaApi.sympa.listfinder.services.HibernateDaoServiceImpl;
import fr.recia.sympaApi.utils.FormToCriterion;
import fr.recia.sympaApi.utils.SessionAttributesHandler;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private final Pattern operationPattern = Pattern.compile(".*operation=([^&]*).*");

  /**
   * Base of error messages for list creation.
   */
  private static final String CREATE_ERROR_MSG_BASE = "sympaCreateList";

  /**
   * Base of error messages for list modification.
   */
  private static final String UPDATE_ERROR_MSG_BASE = "sympaUpdateList";

  /**
   * Base of error messages for list closing.
   */
  private static final String CLOSE_ERROR_MSG_BASE = "sympaCloseList";

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

  @PostMapping("/createOrUpdateListFormData")
  public ResponseEntity<CreateOrUpdateListFormDataResponsePayload> createOrUpdateListFormData(@RequestBody CreateOrUpdateListFormDataRequestPayload requestPayload) {
    log.info("------- BEGIN loadCreateList ---------");

    String modelParam = requestPayload.getModelParam();
    String modelId = requestPayload.getModelId();

    log.info("dao service  {}", daoService);
    log.info("dao service  {}", modelId);

    Model model = this.daoService.getModel(new BigInteger(modelId));
    ModelSubscribers modelSubscribers = this.daoService.getModelSubscriber(model);
    log.debug("Additional groups filter is " + modelSubscribers.getId().getGroupFilter());

    List<JsCreateListRow> editorsAliases = new ArrayList<JsCreateListRow>();

    List<PreparedRequest> listPreparedRequest = this.daoService.getAllPreparedRequests();

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow();
    String siren =  userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT).orElseThrow();

    for (PreparedRequest preparedRequest : listPreparedRequest) {
      JsCreateListRow row = new JsCreateListRow();
      ModelRequest modelRequest = this.daoService.getModelRequest(model, preparedRequest);
      if (modelRequest != null) {
        switch (modelRequest.getCategoryAsEnum()) {
          case CHECKED:
            row.setChecked(true);
            row.setEditable(true);
            break;
          case UNCHECKED:
            row.setChecked(false);
            row.setEditable(true);
            break;
          case MANDATORY:
            row.setChecked(true);
            row.setEditable(false);
            break;
        }

        //MADE pierre
        String name = ldapFilterSourceRequest.makeDisplayName(preparedRequest, uai, siren);
        if (name != null) {
          row.setName(name);
          row.setIdRequest(modelRequest.getId().getIdRequest().toString());
          editorsAliases.add(row);
        }
      }
    }

    CreateOrUpdateListFormDataResponsePayload responsePayload = new CreateOrUpdateListFormDataResponsePayload();

    responsePayload.setEditorsAliases(editorsAliases);
    responsePayload.setType(model.getModelName());

    Pattern p = Pattern.compile("\\{((?!UAI).*)\\}");
    Matcher m = p.matcher(model.getListname());

    if (m.find()) {
      responsePayload.setTypeParam(modelParam);
      responsePayload.setTypeParamName(m.group(1));
    }

    // TODO à mettre en cache redis et non session
    sessionAttributesHandler.setSessionAttribute(createListAdditionalGroupsCacheKey, new HashMap<String, List<String>>());
    responsePayload.setSubscribersGroup(modelSubscribers.getId().getGroupFilter());
    return ResponseEntity.ok(responsePayload);
  }

  @PostMapping("/createList")
  public ResponseEntity<Map<String, String>> createList(@RequestBody CreateOrUpdateListRequestPayload requestPayload) {
    String operation = "operation=CREATE"; //always in this RequestMapping
    return createOrUpdate(requestPayload, operation);
  }

  @PostMapping("/updateList")
  public ResponseEntity<Map<String, String>> updateList(@RequestBody CreateOrUpdateListRequestPayload requestPayload) {
    //todo check if update cause exception if dont already exist
    String operation = "operation=UPDATE"; //always in this RequestMapping
    return createOrUpdate(requestPayload, operation);
  }


  public ResponseEntity<Map<String, String>> createOrUpdate(CreateOrUpdateListRequestPayload requestPayload, String operation) {
    Map<String, String> responseMap = new HashMap<>();

    // use request args
    if (Objects.isNull(requestPayload.getModelId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[modelId] parameter is required");
    }

    if (Objects.isNull(requestPayload.getType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[modelName] parameter is required");
    }

    String type = String.format("&type=%s", requestPayload.getType());   //var type = $("#createListURL_type").html() || " ";  MODEL NAME

    //add check of mandatory ?
    List<String> requiredAliases = allMandatoryPreparedRequestToStringList(requestPayload.getModelId()); // todo fetch required list

    log.debug("Required aliases list {}", requiredAliases);
    if (!requiredAliases.isEmpty()) {

      //si le champ n'est pas présent
      if (Objects.isNull(requestPayload.getEditorsAliases())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[editorsAliases] parameter is required");
      }

      // si il manque au moins un groupe "MANDATORY" on tombe en erreur
      if (!new HashSet<>(List.of(requestPayload.getEditorsAliases().split("\\$"))).containsAll(requiredAliases)) {
        log.debug("Required aliases list {}", requestPayload.getEditorsAliases());
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "[editorsAliases] parameter is missing required value(s)");
      }
    }

    String editorsAliases = Objects.isNull(requestPayload.getModelName()) ? "" : String.format("&editors_aliases=%s", requestPayload.getEditorsAliases()); //  args.get("editorAliases"));
    String editorsGroups = Objects.isNull(requestPayload.getModelName()) ? "" : String.format("&editors_groups=%s", requestPayload.getEditorsGroups());
    String typeParam = Objects.isNull(requestPayload.getModelName()) ? "" : String.format("&type_param=%s", requestPayload.getTypeParam());
    //todo test with missing type param when required

    // statics
    String policy = "&policy=newsletter"; // always

    // ces valeurs doivent venir du user info
    String siren = String.format("&siren=%s", userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT).orElseThrow());
    String rne = String.format("&rne=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow());
    String uai = String.format("&uai=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow());

    String queryCreatedFromInputs = operation + policy +
      type + siren + rne + uai + editorsAliases + editorsGroups + typeParam;

    responseMap.put("queryCreatedFromInputs", queryCreatedFromInputs);

    try {
      // Get SympaRemote database Id
      final String sympaRemoteDatabaseId = this.retrieveSympaRemoteDatabaseId();
      final String queryStringWithDbId = queryCreatedFromInputs + "&databaseId=" + sympaRemoteDatabaseId;


      responseMap.put("sympaRemoteDatabaseId", sympaRemoteDatabaseId);
      responseMap.put("queryStringWithDbId", queryStringWithDbId);

      // Get SympaRemote endpoint URL
      final String sympaRemoteEndpointUrl = this.retrieveSympaRemoteEndpointUrl();
      responseMap.put("sympaRemoteEndpointUrl", sympaRemoteEndpointUrl);


      log.debug("Connecting to SympaRemote with the url [" + sympaRemoteEndpointUrl + "]");
      URL uri = new URL(sympaRemoteEndpointUrl);

      URLConnection urlConnection = uri.openConnection();

      //Use POST to hit the SympaRemote web application
      urlConnection.setDoOutput(true);
      OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());

      log.debug("Posting querystring [" + queryStringWithDbId + "]");
      //Send the queryString
      wr.write(queryStringWithDbId);
      wr.flush();

      BufferedReader in = new BufferedReader(
        new InputStreamReader(
          urlConnection.getInputStream()));
      StringBuffer input = new StringBuffer();
      String inputLine;

      log.debug("create List response: ");
      while ((inputLine = in.readLine()) != null) {
        log.debug(inputLine);
        input.append(inputLine);
      }

      in.close();
      String errorCode = input.toString();

      //Match a regular expression to determine if this is an error code in the
      //form Digit,CODE
      Pattern p = Pattern.compile("(\\d),(.*)");
      Matcher m = p.matcher(errorCode);
      if (m.matches()) {
        String errorCodeNumber = m.group(1);
        String errorCodeText = m.group(2).toLowerCase();

        //***Remove any (s) from the error code as ( ) are not valid characters in a resource key***
        errorCodeText = errorCodeText.replaceAll(Pattern.quote("(s)"), "");
        String message = errorCodeText;


        final String baseErrorMsg = this.findErrorMessageBase(queryStringWithDbId);
        if (Strings.isNotEmpty(baseErrorMsg)) {
          //Build a resource key in order to display a translated message
          String errorMessageKey = baseErrorMsg + ".failure."
            + errorCodeNumber + "." + errorCodeText;

          responseMap.put("messageKey", errorMessageKey);
          //  message = this.context.getMessage(errorMessageKey, null, this.locale);
        }

        responseMap.put("message", message);


        //0 means success, anything else, return an error code to let the ajax handler know something is amiss
        if (!errorCodeNumber.equals("0")) {
          return ResponseEntity.internalServerError().body(responseMap);
        }

        return ResponseEntity.ok(responseMap);
      }

    } catch (IOException ex) {
      log.error("URL exception", ex);
    }

    return ResponseEntity.ok().body(responseMap);
  }


  @PostMapping("/closeList")
  public @ResponseBody ResponseEntity<Map<String, String>> doCloseList(@RequestBody CloseListRequestPayload requestPayload) throws Exception {

    Map<String, String> responseMap = new HashMap<>();

    if (Objects.isNull(requestPayload.getListName())) {

    }

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
    if (!listSplit[1].equals(domainName)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current user is not an admin for this establishment");
    }
    // --- FIN CHECK ---


    try {
      final String sympaRemoteEndpointUrl = this.retrieveSympaRemoteEndpointUrl();
      this.log.debug("Connecting to SympaRemote with the url [" + sympaRemoteEndpointUrl + "]");
      URL uri = new URL(sympaRemoteEndpointUrl);

      URLConnection urlConnection = uri.openConnection();

      //Use POST to hit the SympaRemote web application
      urlConnection.setDoOutput(true);
      OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
      this.log.debug("Posting querystring [" + queryCreatedFromInputs + "]");
      //Send the queryString
      wr.write(queryCreatedFromInputs);
      wr.flush();

      BufferedReader in = new BufferedReader(
        new InputStreamReader(
          urlConnection.getInputStream()));
      StringBuffer input = new StringBuffer();
      String inputLine;

      this.log.debug("create List response: ");
      while ((inputLine = in.readLine()) != null) {
        this.log.debug(inputLine);
        input.append(inputLine);
      }

      in.close();
      String errorCode = input.toString();

      //Match a regular expression to determine if this is an error code in the
      //form Digit,CODE
      Pattern p = Pattern.compile("(\\d),(.*)");
      Matcher m = p.matcher(errorCode);
      if (m.matches()) {
        String errorCodeNumber = m.group(1);
        String errorCodeText = m.group(2).toLowerCase();

        //***Remove any (s) from the error code as ( ) are not valid characters in a resource key***
        errorCodeText = errorCodeText.replaceAll(Pattern.quote("(s)"), "");

        //Build a resource key in order to display a translated message
        String errorMessageKey = CLOSE_ERROR_MSG_BASE + ".failure."
          + errorCodeNumber + "." + errorCodeText;

        // String message = this.context.getMessage(errorMessageKey, null, this.locale);
        responseMap.put("errorMessageKey", errorMessageKey);

        log.info("");

        //0 means success, anything else, return an error code to let the ajax handler know something is amiss
        if (!errorCodeNumber.equals("0")) {
//          responseMap.put("message", message);
          return ResponseEntity.internalServerError().body(responseMap);
          //   response.setStatus(500);
        }

//        responseMap.put("message", message);
        return ResponseEntity.ok(responseMap);
      }

    } catch (MalformedURLException ex) {
      log.error("URL exception", ex);
    } catch (IOException ex) {
      log.error("URL exception", ex);
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


  @Autowired
  DebugProperties debugProperties;

  protected String retrieveSympaRemoteEndpointUrl() {

    //todo redirect temporary to test sympa remote

    if(debugProperties.isUseTestSympaRemote()){
      return debugProperties.getTestSympaRemoteUri();
    }

    RobotSympaInfo rsi = getRobotInfo();
    String sympaRemoteEndpointUrl = null;
    if (rsi != null) {
      sympaRemoteEndpointUrl = rsi.getSympaRemoteUrl();
    } else {
      log.debug("RobotSympaInfo est null");
    }
    return sympaRemoteEndpointUrl;
  }

  /**
   * Retrieve the Sympa Remote endpoint URL from the HTTP session. or userInfo
   */
  protected String retrieveSympaRemoteDatabaseId() {
    RobotSympaInfo rsi = getRobotInfo();
    String sympaRemoteDatabaseId = null;
    if (rsi != null) {
      sympaRemoteDatabaseId = rsi.getSympaRemoteDatabaseId();
    } else {
      log.debug("RobotSympaInfo est null");
    }
    return sympaRemoteDatabaseId;
  }

  protected String findErrorMessageBase(final String queryString) {
    String baseErrorMsg = null;
    Matcher opMatcher = this.operationPattern.matcher(queryString);
    if (opMatcher.find()) {
      final String operation = opMatcher.group(1);
      if ("CREATE".equals(operation)) {
        baseErrorMsg = CREATE_ERROR_MSG_BASE;
      } else if ("UPDATE".equals(operation)) {
        baseErrorMsg = UPDATE_ERROR_MSG_BASE;
      } else if ("CLOSE".equals(operation)) {
        baseErrorMsg = CLOSE_ERROR_MSG_BASE;
      }
    }
    return baseErrorMsg;
  }

  protected RobotSympaInfo getRobotInfo() {
    return robotSympaConf.getRobotSympaInfoByUai(userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow(), userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF).orElseThrow(), true);
  }

  protected List<String> allMandatoryPreparedRequestToStringList(String modelId) {
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



}
