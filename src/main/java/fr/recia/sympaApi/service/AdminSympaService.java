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

import com.fasterxml.jackson.core.type.TypeReference;
import fr.recia.sympaApi.config.bean.CacheProperties;
import fr.recia.sympaApi.config.bean.DebugProperties;
import fr.recia.sympaApi.dto.request.admin.CloseListRequestPayload;
import fr.recia.sympaApi.dto.request.admin.CreateOrUpdateListFormDataRequestPayload;
import fr.recia.sympaApi.dto.request.admin.CreateOrUpdateListRequestPayload;
import fr.recia.sympaApi.dto.response.admin.AdminSympaCreatableList;
import fr.recia.sympaApi.dto.response.admin.AdminSympaListResponseForDisplay;
import fr.recia.sympaApi.dto.response.admin.AdminSympaUpdatableList;
import fr.recia.sympaApi.dto.response.admin.CreateOrUpdateListFormDataResponsePayload;
import fr.recia.sympaApi.entity.Model;
import fr.recia.sympaApi.entity.ModelRequest;
import fr.recia.sympaApi.entity.ModelSubscribers;
import fr.recia.sympaApi.entity.PreparedRequest;
import fr.recia.sympaApi.exception.IsNotAdminException;
import fr.recia.sympaApi.groupfinder.impl.RegexGroupFinder;
import fr.recia.sympaApi.model.AvailableMailingListsFound;
import fr.recia.sympaApi.model.IMailingList;
import fr.recia.sympaApi.model.IMailingListModel;
import fr.recia.sympaApi.pojo.EditorAlias;
import fr.recia.sympaApi.pojo.JsCreateListTableRow;
import fr.recia.sympaApi.pojo.JsTreeNode;
import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.pojo.RobotSympaInfo;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.service.ldap.LdapFilterSourceRequest;
import fr.recia.sympaApi.service.ldap.LdapPerson;
import fr.recia.sympaApi.service.listfinder.impl.AvailableListsFinderBasicImpl;
import fr.recia.sympaApi.service.listfinder.impl.HibernateDaoServiceImpl;
import fr.recia.sympaApi.utils.CacheHandler;
import fr.recia.sympaApi.utils.SessionAttributesHandler;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.util.annotation.Nullable;

import java.math.BigInteger;
import java.net.URI;
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
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
@Service
public class AdminSympaService {

  protected List<JsCreateListTableRow> convertMailingListsToJsListTableTow(
    final String domain, final Collection<IMailingList> creatableLists) {
    List<JsCreateListTableRow> tableData = new ArrayList<>();

    if (creatableLists != null) {
      for(IMailingList mailList : creatableLists) {
        JsCreateListTableRow row = new JsCreateListTableRow();
        row.setName(mailList.getName().toLowerCase() + "@" + domain);
        row.setSubject(mailList.getDescription());
        row.setModelId(mailList.getModel().getId());
        row.setModelParam(mailList.getModelParameter());
        log.debug("Loading creatable list " + row);
        tableData.add(row);
      }
    }

    return tableData;
  }

  @Autowired
  CacheProperties cacheProperties;

  @Autowired
  RobotDomaineNameResolver robotDomainNameResolver;

  @Autowired
  HibernateDaoServiceImpl daoService;

  @Autowired
  AvailableListsFinderBasicImpl availableListFinder;

  @Autowired
  UserAttributesHandler userAttributesHandler;
;

  @Autowired
  DomainService domainService;

  @Autowired
  private LdapPerson ldapPerson;

  @Autowired
  private LdapFilterSourceRequest ldapFilterSourceRequest;

  @Autowired
  private SessionAttributesHandler sessionAttributesHandler;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private RobotSympaConf robotSympaConf;

  @Autowired
  DebugProperties debugProperties;

  @Autowired
  private CacheHandler cacheHandler;

  @Autowired
  protected RobotDomaineNameResolver robotDomaineNameResolver;

  @Autowired
  protected RegexGroupFinder jsTreeGroupFinder;

  @Autowired
  protected SympaRemoteQueryService sympaRemoteQueryService;

  private final Pattern operationPattern = Pattern.compile(".*operation=([^&]*).*");


  /**
   * Base of error messages for list creation.
   */
  private static final String CREATE_ERROR_MSG_BASE = "modal.response.create";

  /**
   * Base of error messages for list modification.
   */
  private static final String UPDATE_ERROR_MSG_BASE = "modal.response.update";

  /**
   * Base of error messages for list closing.
   */
  private static final String CLOSE_ERROR_MSG_BASE = "modal.response.close";


  final static String createListAdditionalGroupsCacheKey = "createListAdditionalGroupsCache";


  @SuppressWarnings("unchecked")
  public List<JsTreeNode> fetchAdditionalGroupsTree(){
    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT);


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

    List<JsTreeNode> rootNodes = new ArrayList<>();

    List<JsTreeNode> allNodes = new ArrayList<>();

    for (String groupStr : additionalGroups) {
      String[] levels = groupStr.split(":");
      int lastLevel = levels.length - 1;
      String nodeKey = "";
      JsTreeNode previousNode = null;

      for (int i = 0; i <= lastLevel; ++i) {
        String currentLevel = levels[i];
        nodeKey += i == 0 ? currentLevel : ":" + currentLevel;

        JsTreeNode node = JsTreeNode.getMatchingNodeOnKey(allNodes, nodeKey);

        if (Objects.isNull(node)) {
          node = new JsTreeNode();
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
    return rootNodes;
  }

  @Nullable
  public AdminSympaListResponseForDisplay fetchLists() throws Exception {

    Map<String, String> userInfo = new HashMap<>();

    userInfo.put(UserAttributesHandler.UAI_CURRENT, userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT));
    userInfo.put(UserAttributeMapping.USER_ATTRIBUTE_SIREN_KEY, userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT));

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT);

    List<String> isMemberOf = userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF);

    List<UserSympaListWithUrl> sympaList;

    boolean isAdmin;
    try {
      isAdmin =   this.fetchIsAdmin(isMemberOf, this.ldapPerson.getAdminRegex(), uai);
    } catch (Exception e) {
      isAdmin = false;
      log.error("exception during fetchIsAdmin, defaulting value to false", e);
    }

    if(!isAdmin){
      return null;
    }

    //Filter the user lists to make sure we only display lists that are in the current establishment.  This
    //is done by comparing the domain of the list address (after the @).
    //As domains are 1 to 1 with establishments
    //this can be used to tell what lists belong to which establishment.
    sympaList = this.getDomainService().getWhich();

    AdminSympaListResponseForDisplay response = fetchCreateListTableData(userInfo, sympaList);
    return response;
  }

  @Nullable
  public CreateOrUpdateListFormDataResponsePayload createOrUpdateListFormData(@RequestBody CreateOrUpdateListFormDataRequestPayload requestPayload) {
    log.info("------- BEGIN createOrUpdateListFormData ---------");

    String modelParam = requestPayload.getModelParam();
    String modelId = requestPayload.getModelId();

    log.debug("[createOrUpdateListFormData] model id from request {}", modelId);

    Model model = this.daoService.getModel(new BigInteger(modelId));
    ModelSubscribers modelSubscribers = this.daoService.getModelSubscriber(model);
    log.debug("[createOrUpdateListFormData] Additional groups filter is " + modelSubscribers.getId().getGroupFilter());

    List<EditorAlias> editorsAliases = new ArrayList<>();

    List<PreparedRequest> listPreparedRequest = this.daoService.getAllPreparedRequests();

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT);
    String siren =  userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT);

    for (PreparedRequest preparedRequest : listPreparedRequest) {
      EditorAlias row = new EditorAlias();
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

    return responsePayload;
  }


  protected boolean isCurrentDomain(CloseListRequestPayload requestPayload){
    String listName = requestPayload.getListName().trim();
    String[] listSplit = listName.split("@");
    Assert.isTrue(listSplit.length == 2, "List should have been split in 2 part around '@'");
    String domainName = robotDomaineNameResolver.resolveRobotDomainName();
    log.info("doCloseList check domain name {} against from the list {}", domainName, listSplit[1]);
    return listSplit[1].equals(domainName);
  }

  protected boolean isAdminOnCurrentDomain(){
    return robotSympaConf.isAdminRobotSympaByUai(userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT), userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF));
  }

  @Nullable
  public String closeList(CloseListRequestPayload requestPayload){

    String queryCreatedFromInputs = sympaRemoteQueryService.createCloseQuery(requestPayload);

    // --- DEBUT CHECK ---
    // 1 check si le domaine de la liste à supprimer correspond au domaine courant
    if (!isCurrentDomain(requestPayload)){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "List domain does not match current user establishment");
    }

    // 2 check si le user est admin sur cet etab
    if(!isAdminOnCurrentDomain()){
      throw new IsNotAdminException("Current user is not an admin for this establishment");
    }
    // --- FIN CHECK ---


    final String sympaRemoteEndpointUrl = retrieveSympaRemoteEndpointUrl();
    log.debug("Connecting to SympaRemote with the url [" + sympaRemoteEndpointUrl + "]");

    String errorCode = postToSympaRemote(sympaRemoteEndpointUrl, queryCreatedFromInputs);


    Map<String, String> responseMap = new HashMap<>();

    if(Objects.nonNull(errorCode)){
      return errorCodeToMessageKey(errorCode, queryCreatedFromInputs);
    }
    return null;
  }

  @Nullable
  public String createOrUpdate(CreateOrUpdateListRequestPayload requestPayload, String operation) {
    Map<String, String> responseMap = new HashMap<>();

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

    String queryCreatedFromInputs = operation + policy +
      type + siren + rne + uai + editorsAliases + editorsGroups + typeParam;

    responseMap.put("queryCreatedFromInputs", queryCreatedFromInputs);

    // Get SympaRemote database Id
    final String sympaRemoteDatabaseId = retrieveSympaRemoteDatabaseId();
    final String queryStringWithDbId = queryCreatedFromInputs + "&databaseId=" + sympaRemoteDatabaseId;


    responseMap.put("sympaRemoteDatabaseId", sympaRemoteDatabaseId);
    responseMap.put("queryStringWithDbId", queryStringWithDbId);

    // Get SympaRemote endpoint URL
    final String sympaRemoteEndpointUrl = retrieveSympaRemoteEndpointUrl();
    responseMap.put("sympaRemoteEndpointUrl", sympaRemoteEndpointUrl);


    log.debug("Connecting to SympaRemote with the url [" + sympaRemoteEndpointUrl + "]");

    String errorCode = postToSympaRemote(sympaRemoteEndpointUrl, queryCreatedFromInputs);

    if(Objects.nonNull(errorCode)){
      return errorCodeToMessageKey(errorCode, queryCreatedFromInputs);
    }
    return null;
  }

  private boolean fetchIsAdmin(final List<String> isMemberOf, String adminRegex, final String uai) {
    // /////////////////////////////////////////////////////////
    // Determine if user is an admin or not

    if (StringUtils.hasText(uai)) {
      adminRegex = adminRegex.replaceAll(Pattern.quote("%UAI"), uai);
    }

    if (isMemberOf != null) {
      for (String memberOf : isMemberOf) {
        if (memberOf.matches(adminRegex)) {
          return true;
        }
      }
    } else {
      log.warn("isMemberOfList is NULL!"); //todo, exception if isMemberOf Null or empty ?
      return false;
    }

    return false;

  }

  public AdminSympaListResponseForDisplay fetchCreateListTableData(final Map<String,String> userInfo, List<UserSympaListWithUrl> sympaList) throws Exception {

    AdminSympaListResponseForDisplay response = new AdminSympaListResponseForDisplay();

    //Find the establishements email address domain
    final String domain = this.robotDomainNameResolver.resolveRobotDomainName();
    log.debug("Mailing list domain for establishment is [" + domain + "]");


    //Fetch the models from the ESCO-SympaRemote database
    List<Model> listModels = this.daoService.getAllModels();

    log.debug("Fetched models from SympaRemote db.  Count: " + listModels.size());

    List<IMailingListModel> listMailingListModels = this.daoService.getMailingListModels(listModels, userInfo);

    //Get the mailing lists that we can create
    AvailableMailingListsFound availableLists = null;

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT);


    AvailableMailingListsFound availableMailingListsFound = cacheHandler.getFromCache(cacheProperties.getAdminServiceCacheName(), uai, new TypeReference<>() {
    });

    if(Objects.nonNull(availableMailingListsFound)){
      availableLists = availableMailingListsFound;
    } else {
      availableLists = this.availableListFinder.getAvailableAndNonExistingLists(userInfo, listMailingListModels);
        cacheHandler.putObjectInCache(cacheProperties.getAdminServiceCacheName(), uai, availableLists);
    }

    final Collection<IMailingList> creatableLists = availableLists.getCreatableLists();
    final Collection<IMailingList> updatableLists = availableLists.getUpdatableLists();





    //Convert domain objects to UI
    List<JsCreateListTableRow> createTableData = this.convertMailingListsToJsListTableTow(domain, creatableLists);
    List<JsCreateListTableRow> updateTableData = this.convertMailingListsToJsListTableTow(domain, updatableLists);




    if (updateTableData != null && sympaList != null && !updateTableData.isEmpty()) {
      // on merge les données de sympaList dans updateTableData pour recuperer les urls
      // on cree une map provisoire avce updateTable
      HashMap<String, JsCreateListTableRow> mapTmp = new HashMap<>(updateTableData.size());
      for (JsCreateListTableRow liste : updateTableData) {
        if (liste != null) {
          String addr = liste.getName();
          if (addr != null) {
            mapTmp.put(addr, liste);
            log.debug("liste =" + liste);
          }
        }
      }
      updateTableData.clear();
      // on copie toutes la sympaList au format update

      for (UserSympaListWithUrl uslwu : sympaList){
        if (uslwu != null){
          String addr = uslwu.getAddress();
          JsCreateListTableRow jcltr = mapTmp.get(addr);
          if (jcltr == null ) {
            jcltr = new JsCreateListTableRow();
            jcltr.setName(addr);
            jcltr.setSubject(uslwu.getSubject());
          }
          jcltr.setUrls(uslwu);
          updateTableData.add(jcltr);
        }
      }

    }

    response.setAdminSympaCreatableListList(createTableData.stream().map(AdminSympaCreatableList::new).collect(Collectors.toList()));

    if(Objects.isNull(updateTableData)){
      response.setAdminSympaUpdatableListList(new ArrayList<>());
    }else {
      response.setAdminSympaUpdatableListList(updateTableData.stream().map(AdminSympaUpdatableList::new).collect(Collectors.toList()));
    }

    return response;
  }
  @Nullable
  private String errorCodeToMessageKey(String errorCode, String query) {
    //Match a regular expression to determine if this is an error code in the
    //form Digit,CODE
    Pattern p = Pattern.compile("(\\d),(.*)");
    Matcher m = p.matcher(errorCode);
    if (m.matches()) {
      String errorCodeNumber = m.group(1);
      String errorCodeText = m.group(2).toLowerCase();

      //***Remove any (s) from the error code as ( ) are not valid characters in a resource key***
      errorCodeText = errorCodeText.replaceAll(Pattern.quote("(s)"), "");
      final String baseErrorMsg = this.findErrorMessageBase(query);
      if (Strings.isNotEmpty(baseErrorMsg)) {

        //Build a resource key in order to display a translated message
        String errorMessageKey = baseErrorMsg + ".failure."
          + errorCodeNumber + "." + errorCodeText;
        log.debug("errorMessageKey: {}", errorMessageKey);
        if (Strings.isNotEmpty(baseErrorMsg)) {
          return errorMessageKey;
        }
        //0 means success, anything else, return an error code to let the ajax handler know something is amiss

      }
    }
    return null;
  }

  private String postToSympaRemote(String sympaRemoteEndpointUrl, String query)  {
    URI uri = URI.create(sympaRemoteEndpointUrl);
    String url = uri.toString();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<String> request = new HttpEntity<>(query, headers);

    log.debug("Posting querystring [" + query + "]");
    ResponseEntity<String> response = restTemplate.postForEntity(
      url,
      request,
      String.class
    );

    log.debug("postToSympaRemote response statys code: {}", response.getStatusCode());
    String errorCode = response.getBody();
    log.debug("postToSympaRemote response body: {}", errorCode);
    return errorCode;
  }


  private String findErrorMessageBase(final String queryString) {
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

  private String retrieveSympaRemoteEndpointUrl() {

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
  public String retrieveSympaRemoteDatabaseId() {
    RobotSympaInfo rsi = getRobotInfo();
    String sympaRemoteDatabaseId = null;
    if (rsi != null) {
      sympaRemoteDatabaseId = rsi.getSympaRemoteDatabaseId();
    } else {
      log.debug("RobotSympaInfo est null");
    }
    return sympaRemoteDatabaseId;
  }



  private RobotSympaInfo getRobotInfo() {
    return robotSympaConf.getRobotSympaInfoByUai(userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT), userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF), true);
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



}
