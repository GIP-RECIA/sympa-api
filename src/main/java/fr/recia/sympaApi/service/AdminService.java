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

import fr.recia.sympaApi.dto.response.admin.AdminSympaCreatableList;
import fr.recia.sympaApi.dto.response.admin.AdminSympaListResponseForDisplay;
import fr.recia.sympaApi.dto.response.admin.AdminSympaUpdatableList;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import fr.recia.sympaApi.pojo.email.EmailConfiguration;
import fr.recia.sympaApi.pojo.email.IEmailUtility;
import fr.recia.sympaApi.servlet.JsCreateListTableRow;
import fr.recia.sympaApi.sympa.admin.LdapPerson;
import fr.recia.sympaApi.sympa.admin.RobotDomaineNameResolver;
import fr.recia.sympaApi.sympa.listfinder.IDaoService;
import fr.recia.sympaApi.sympa.listfinder.IMailingList;
import fr.recia.sympaApi.sympa.listfinder.IMailingListModel;
import fr.recia.sympaApi.sympa.listfinder.model.AvailableMailingListsFound;
import fr.recia.sympaApi.sympa.listfinder.model.Model;
import fr.recia.sympaApi.sympa.listfinder.services.AvailableListsFinderBasicImpl;
import fr.recia.sympaApi.sympa.listfinder.services.HibernateDaoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminService {

  protected List<JsCreateListTableRow> convertMailingListsToJsListTableTow(
    final String domain, final Collection<IMailingList> creatableLists) {
    List<JsCreateListTableRow> tableData = new ArrayList<JsCreateListTableRow>();

    if (creatableLists != null) {
      for(IMailingList mailList : creatableLists) {
        JsCreateListTableRow row = new JsCreateListTableRow();
        row.setName(mailList.getName().toLowerCase() + "@" + domain);
        row.setSubject(mailList.getDescription());
        row.setModelId(mailList.getModel().getId());
        row.setModelParam(mailList.getModelParameter());
        log.debug("Loading creatable list " + row.toString());
        tableData.add(row);
      }
    }

    return tableData;
  }

  @Autowired
  EmailConfiguration emailConfig;

  @Autowired
  RobotDomaineNameResolver robotDomainNameResolver;

  @Autowired
  HibernateDaoServiceImpl daoService;

  @Autowired
  AvailableListsFinderBasicImpl availableListFinder;


  public List<String> fetchEmailProfileList(final Map<String, List<Object>> mvUserInfo, final LdapPerson ldapPerson, final String uid) {
    List<String> emailProfileList = null;
    //Check for at least the isMemberOfList which won't be empty should the multi-value map exist
    if ((mvUserInfo != null) && mvUserInfo.containsKey(ldapPerson.getMemberAttribute())) {


      emailProfileList = new ArrayList<String>();
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


  public void fetchIsAdmin(final Map<String, Object> map, final List<String> isMemberOfList, String adminRegex, final String uai) {
    // /////////////////////////////////////////////////////////
    // Determine if user is an admin or not

    // Initialize to false
    map.put("isListAdmin", false);

    if (StringUtils.hasText(uai)) {
      adminRegex = adminRegex.replaceAll(Pattern.quote("%UAI"), uai);
    }

    if (isMemberOfList != null) {
      for (String memberOf : isMemberOfList) {
        if (memberOf.matches(adminRegex)) {
          map.put("isListAdmin", true);
          return;
        }
      }
    } else {
      log.warn("isMemberOfList is NULL!");
    }

  }


  //todo remove, used for mail form
  public void fetchEmailUtility(final Map<String, Object> map, final List<String> emailProfileList) {
    //Find which email program is configured for the user

    IEmailUtility selectedEmailUtility = null;

    if (emailProfileList != null) {
      for (String profile : emailProfileList) {
        for (IEmailUtility emailUtil : emailConfig.getUtils()) {
          if (emailUtil.isCorrectEmailUtility(profile)) {
            selectedEmailUtility = emailUtil;
            break;
          }
        }
      }
    }
    map.put("emailUtil", selectedEmailUtility);
  }


// TODO : return void after debugging
  public AdminSympaListResponseForDisplay fetchCreateListTableData(final Map<String,Object> map, final Map<String,String> userInfo, List<UserSympaListWithUrl> sympaList) throws Exception {


    AdminSympaListResponseForDisplay response = new AdminSympaListResponseForDisplay();

    Map<String,Object> responseMap = new HashMap<>();

    //	String establishementId = userInfo.get(UserInfoService.getPortalUaiAttribute());

    //	EscoHomeController.LOG.debug("Entering loadCreateListTable.  UAI: [" + establishementId + "]");

    //Find the establishements email address domain
    final String domain = this.robotDomainNameResolver.resolveRobotDomainName();
    log.debug("Mailing list domain for establishment is [" + domain + "]");

    responseMap.put("domain", domain);

    //Fetch the models from the ESCO-SympaRemote database
    List<Model> listModels = this.daoService.getAllModels();

    log.debug("Fetched models from SympaRemote db.  Count: " + listModels.size());

    responseMap.put("listModels", listModels);

    responseMap.put("userInfo for getMailingListModels", userInfo);

    List<IMailingListModel> listMailingListModels = this.daoService.getMailingListModels(listModels, userInfo);
    responseMap.put("IMailingListModel", listMailingListModels);




    //Get the mailing lists that we can create
    final AvailableMailingListsFound availableLists =
      this.availableListFinder.getAvailableAndNonExistingLists(userInfo, listMailingListModels);
    final Collection<IMailingList> creatableLists = availableLists.getCreatableLists();
    final Collection<IMailingList> updatableLists = availableLists.getUpdatableLists();

    responseMap.put("availableLists", availableLists);
    responseMap.put("creatableLists", creatableLists);
    responseMap.put("updatableLists", updatableLists); // ceux avec icones fermer la liste et modifier la liste, pas l'intégralité qui ont accéder aux archives/accéder à l'administration




    //Convert domain objects to UI
    List<JsCreateListTableRow> createTableData = this.convertMailingListsToJsListTableTow(domain, creatableLists);
    List<JsCreateListTableRow> updateTableData = this.convertMailingListsToJsListTableTow(domain, updatableLists);




    if (updateTableData != null && sympaList != null && updateTableData.size() > 0) {
      // on merge les données de sympaList dans updateTableData pour recuperer les urls
      // on cree une map provisoire avce updateTable
      HashMap<String, JsCreateListTableRow> mapTmp = new HashMap<String, JsCreateListTableRow>(updateTableData.size());
      for (JsCreateListTableRow liste : updateTableData) {
        if (liste != null) {
          String addr = liste.getName();
          if (addr != null) {
            mapTmp.put(addr, liste);
            log.debug("liste =" + liste.toString());
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


    responseMap.put("createTableData", createTableData);
    responseMap.put("updateTableData", updateTableData);
    responseMap.put("sympaList", sympaList);


    return response;

   // return responseMap;
  }

}
