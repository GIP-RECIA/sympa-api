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

import fr.recia.sympaApi.config.bean.CacheProperties;
import fr.recia.sympaApi.dto.request.SympaListRequestForm;
import fr.recia.sympaApi.dto.request.admin.CreateOrUpdateListFormDataRequestPayload;
import fr.recia.sympaApi.dto.response.admin.AdminSympaCreatableList;
import fr.recia.sympaApi.dto.response.admin.AdminSympaListResponseForDisplay;
import fr.recia.sympaApi.dto.response.admin.AdminSympaUpdatableList;
import fr.recia.sympaApi.dto.response.admin.CreateOrUpdateListFormDataResponsePayload;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.servlet.JsCreateListRow;
import fr.recia.sympaApi.servlet.JsCreateListTableRow;
import fr.recia.sympaApi.sympa.admin.EscoUserAttributeMapping;
import fr.recia.sympaApi.sympa.admin.LdapFilterSourceRequest;
import fr.recia.sympaApi.sympa.admin.LdapPerson;
import fr.recia.sympaApi.sympa.admin.RobotDomaineNameResolver;
import fr.recia.sympaApi.sympa.listfinder.IMailingList;
import fr.recia.sympaApi.sympa.listfinder.IMailingListModel;
import fr.recia.sympaApi.sympa.listfinder.model.AvailableMailingListsFound;
import fr.recia.sympaApi.sympa.listfinder.model.Model;
import fr.recia.sympaApi.sympa.listfinder.model.ModelRequest;
import fr.recia.sympaApi.sympa.listfinder.model.ModelSubscribers;
import fr.recia.sympaApi.sympa.listfinder.model.PreparedRequest;
import fr.recia.sympaApi.sympa.listfinder.services.AvailableListsFinderBasicImpl;
import fr.recia.sympaApi.sympa.listfinder.services.HibernateDaoServiceImpl;
import fr.recia.sympaApi.utils.FormToCriterion;
import fr.recia.sympaApi.utils.SessionAttributesHandler;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import reactor.util.annotation.Nullable;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
public class AdminService {

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

  @Autowired
  CacheManager cacheManager;

  @Autowired
  DomainService domainService;

  @Autowired
  EscoUserAttributeMapping escoUserAttributeMapping;

  @Autowired
  private FormToCriterion formToCriterion;

  @Autowired
  private LdapPerson ldapPerson;

  @Autowired
  private LdapFilterSourceRequest ldapFilterSourceRequest;

  @Autowired
  private SessionAttributesHandler sessionAttributesHandler;

  @Autowired
  private RestTemplate restTemplate;

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

  Cache cache;

  @PostConstruct
  public void init() {
    cache = cacheManager.getCache(cacheProperties.getAdminServiceCacheName());
  }

  final static String createListAdditionalGroupsCacheKey = "createListAdditionalGroupsCache";


  @Nullable
  public AdminSympaListResponseForDisplay fetchLists(SympaListRequestForm sympaListRequestForm) throws Exception {

    Map<String,Object> map = new HashMap<>();

    Map<String, String> userInfo = new HashMap<>();

    //enhanceUserInfo => add siren
    userInfo.put(UserAttributesHandler.UAI_CURRENT, userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElse(null));

    userInfo = this.escoUserAttributeMapping.enhanceUserInfo(userInfo);

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElse(null);
    assert uai != null;

    List<String> isMemberOf = userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF).orElse(null);
    assert isMemberOf != null;


    List<UserSympaListWithUrl> sympaList;


    final String uid = SecurityContextHolder.getContext().getAuthentication().getName();// userInfo.get(UserInfoService.getPortalUidAttribute());

    Assert.hasText(uid, "UID shouldn't be empty !");
    Assert.hasText(uai, "UAI shouldn't be empty !");

    map.put("uai", uai);

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
    sympaList = this.getDomainService().getWhich(this.formToCriterion.formToCriterion(sympaListRequestForm), false);

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

    List<JsCreateListRow> editorsAliases = new ArrayList<>();

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

    return responsePayload;
  }

    public List<String> fetchEmailProfileList(final Map<String, List<Object>> mvUserInfo, final LdapPerson ldapPerson, final String uid) {
    List<String> emailProfileList = null;
    //Check for at least the isMemberOfList which won't be empty should the multi-value map exist
    if ((mvUserInfo != null) && mvUserInfo.containsKey(ldapPerson.getMemberAttribute())) {


      emailProfileList = new ArrayList<>();
      log.debug("Reading email profiles for ldap person using attribute [" + ldapPerson.getWebmailProfileAttribute() + "]");
      List<Object> listObjects = mvUserInfo.get(ldapPerson.getWebmailProfileAttribute());
      if (listObjects != null) {
        for(Object o : listObjects) {
          emailProfileList.add(o == null ? "" : o.toString());
        }
      }
    } else {
      log.debug("MV map not found or does not contain isMemberOf.");

      //Backup plan, use direct ldap queries
      LdapPerson.Person person = ldapPerson.getPerson(uid);

      if (person != null) {
        log.debug("Ldap person found");
        emailProfileList = person.getProfile();
      } else {
        log.error("Ldap person NOT found");
      }
    }
    return emailProfileList;
  }


  public boolean fetchIsAdmin(final List<String> isMemberOf, String adminRegex, final String uai) {
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

    String uai = userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow();

    Cache.ValueWrapper vw = cache.get(uai);
    if(Objects.nonNull(vw) && Objects.nonNull(vw.get()) && (vw.get() instanceof AvailableMailingListsFound)){
      availableLists = (AvailableMailingListsFound) vw.get();
      if(Objects.isNull(availableLists)){
        log.warn("availableLists from cache is null despite having key");
      }
    }

    if(Objects.isNull(availableLists)){
      availableLists = this.availableListFinder.getAvailableAndNonExistingLists(userInfo, listMailingListModels);
      try {
        cache.put(uai, availableLists);
      } catch (Exception e) {
        log.error("Could not put availableLists in cache for uai: {}", uai);
      }
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
            log.debug("new jcltr " + jcltr);
          }
          jcltr.setUrls(uslwu);
          updateTableData.add(jcltr);
          log.debug("jcltr " + jcltr);
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
  public String errorCodeToMessageKey(String errorCode, String query) {
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

  public String postToSympaRemote(String sympaRemoteEndpointUrl, String query)  {
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


  public String findErrorMessageBase(final String queryString) {
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



}
