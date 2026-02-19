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
import fr.recia.sympaApi.groupfinder.IEtabGroupsFinder;
import fr.recia.sympaApi.groupfinder.impl.RegexGroupFinder;
import fr.recia.sympaApi.pojo.RobotSympaConf;
import fr.recia.sympaApi.pojo.RobotSympaInfo;
import fr.recia.sympaApi.servlet.JsCreateListRow;
import fr.recia.sympaApi.servlet.JsList;
import fr.recia.sympaApi.sympa.admin.LdapFilterSourceRequest;
import fr.recia.sympaApi.sympa.admin.RobotDomaineNameResolver;
import fr.recia.sympaApi.sympa.listfinder.IDaoService;
import fr.recia.sympaApi.sympa.listfinder.model.Model;
import fr.recia.sympaApi.sympa.listfinder.model.ModelRequest;
import fr.recia.sympaApi.sympa.listfinder.model.ModelSubscribers;
import fr.recia.sympaApi.sympa.listfinder.model.PreparedRequest;
import fr.recia.sympaApi.sympa.listfinder.services.HibernateDaoServiceImpl;
import fr.recia.sympaApi.utils.SessionAttributesHandler;
import fr.recia.sympaApi.utils.UserAttributesHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.yaml.snakeyaml.util.ArrayUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@Controller
//@Scope("session")
//@Configuration
//@PropertySource({"classpath:esco-main.properties",
//                 "file:${portal.home}/global.properties",
//                 "file:${portal.home}/admin-portlet-sympa.properties"})

@Slf4j
@RestController
@RequestMapping("/api/admin-cmd")
public class ServletAjaxController implements Serializable {

	/** Svuid. */
	private static final long serialVersionUID = -6975100227000700771L;

	/** Base of error messages for list creation. */
	private static final String CREATE_ERROR_MSG_BASE = "esupsympaCreateList";

	/** Base of error messages for list modification. */
	private static final String UPDATE_ERROR_MSG_BASE = "esupsympaUpdateList";

	/** Base of error messages for list closing. */
	private static final String CLOSE_ERROR_MSG_BASE = "esupsympaCloseList";

	@Autowired
	protected ApplicationContext context;

  @Autowired
  protected RobotDomaineNameResolver robotDomaineNameResolver;


	@Autowired
  HibernateDaoServiceImpl daoService;

	@Autowired
	protected LdapFilterSourceRequest ldapFilterSourceRequest;

	@Autowired
	private RobotSympaConf robotSympaConf;

  @Autowired
  private SessionAttributesHandler sessionAttributesHandler;

	@Autowired
	protected RegexGroupFinder jsTreeGroupFinder;

	protected Locale locale;

	private Pattern operationPattern = Pattern.compile(".*operation=([^&]*).*");

  @Autowired
  UserAttributesHandler userAttributesHandler;



	/**
//	 * @param establishementId the UAI, id of the establishment
//	 * @param modelId The database id of the model
//	 * @param listDescription Contains the model's subject/description with any option model parameter filled in
//	 * @param modelParam The parameter of the model
//	 * @param request http request
	 * @return Spring MVC ModelAndView
	 */





	@SuppressWarnings("unchecked")
	@PostMapping("/loadCreateList")
	public ResponseEntity<Map<String, Object>> loadCreateList(@RequestBody Map<String, String> args
			) {
    log.info("------- BEGIN loadCreateList ---------");

    String establishementId = args.get("establishementId");
    String listDescription = args.get("listDescription");
    String modelParam = args.get("modelParam");
    String modelId = args.get("modelId");

    Map<String, Object> responseMap = new HashMap<>();

		//TODO pb placeholderValuesMap peut revenir null en cas de perte de session

    Map<String, String> placeholderValuesMap = sessionAttributesHandler.getSessionAttribute(SessionAttributesHandler.PLACEHOLDER_VALUES_MAP_SESSION_KEY, Map.class).orElse(null);

    responseMap.put("placeholderValuesMap from session", placeholderValuesMap);

//		ModelMap modelMap = new ModelMap();

		if ((modelId == null) || (listDescription == null)) {
			//TODO the error page has an error...
			log.error("Model id or list description is null");
			//return new ModelAndView("error", modelMap);
		}

    log.info("dao service  {}", daoService);
    log.info("dao service  {}", modelId);

		Model model = this.daoService.getModel(new BigInteger(modelId));

//		modelMap.put("listDescription", listDescription);
    responseMap.put("listDescription", listDescription);


		ModelSubscribers modelSubscribers = this.daoService.getModelSubscriber(model);
		log.debug("Additional groups filter is " + modelSubscribers.getId().getGroupFilter());
    responseMap.put("subscribersGroup", modelSubscribers.getId().getGroupFilter());
//		modelMap.put("subscribersGroup", modelSubscribers.getId().getGroupFilter());

		List<JsCreateListRow> editorsAliases = new ArrayList<JsCreateListRow>();

		List<PreparedRequest> listPreparedRequest = this.daoService.getAllPreparedRequests();



		String uai = null;
		String siren = null;
		if (placeholderValuesMap != null ) {
			uai = placeholderValuesMap.get("uai");
			siren =  placeholderValuesMap.get("siren");
		}

		if (uai == null && establishementId != null){
			uai = establishementId;
		}
		if (uai != null && siren == null) {
			siren = ldapFilterSourceRequest.findSirenByUai(uai);
		}


		for (PreparedRequest preparedRequest : listPreparedRequest) {
			JsCreateListRow row = new JsCreateListRow();
			ModelRequest modelRequest = this.daoService.getModelRequest(model, preparedRequest);
			if (modelRequest != null) {
				switch(modelRequest.getCategoryAsEnum()) {
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
        log.info("makeDisplayName  result is {}", name);
				if (name != null) {
					row.setName(name);
					row.setIdRequest(modelRequest.getId().getIdRequest().toString());
					editorsAliases.add(row);
				}
			}
		}

    responseMap.put("editorsAliases", editorsAliases);
    responseMap.put("type", model.getModelName());

		Pattern p = Pattern.compile("\\{((?!UAI).*)\\}");
		Matcher m = p.matcher(model.getListname());

		if (m.find()) {
      responseMap.put("typeParamName", m.group(1));
      responseMap.put("typeParam", modelParam);
		}

    responseMap.put("uai", establishementId);

		StringBuilder userAttributes = new StringBuilder(128);
		for (Entry<String, String> userAttribute : placeholderValuesMap.entrySet()) {
      log.debug("USER ATTRIBUT : " +  userAttribute.getKey() + " ; " +userAttribute.getValue() );
			userAttributes.append("&");
			userAttributes.append(userAttribute.getKey());
			userAttributes.append("=");
			userAttributes.append(userAttribute.getValue());
		}
    responseMap.put("userAttributes", userAttributes.toString());

    log.debug("Model map content: " + responseMap.toString());
    // TODO à mettre en cache redis et non session
		 sessionAttributesHandler.setSessionAttribute(createListAdditionalGroupsCacheKey, new HashMap<String, List<String>>());
		return ResponseEntity.ok(responseMap);
	}


  @PostMapping("/doUpdateList")
  public ResponseEntity<Map<String, String>> doUpdateList(@RequestBody Map<String, String> args) {
    //todo check if update cause exception if dont already exist
    String operation = "operation=UPDATE"; //always in this RequestMapping
    return createOrUpdate(args, operation);
  }


  @PostMapping("/doCreateList")
  public ResponseEntity<Map<String, String>> doCreateList(@RequestBody Map<String, String> args) {
    String operation = "operation=CREATE"; //always in this RequestMapping
    return createOrUpdate(args, operation);
  }

  public  ResponseEntity<Map<String, String>> createOrUpdate(Map<String, String> args, String operation){
    Map<String, String> responseMap = new HashMap<>();

    // use request args
    String type = String.format("&type=%s", args.get("modelName"));   //var type = $("#createListURL_type").html() || " ";  MODEL NAME
    String editorsAliases  = String.format("&editors_aliases=%s",  args.get("editorAliases"));
    String editorsGroups  = String.format("&editors_groups=%s",  args.get("editorsGroups"));
    String typeParam  = String.format("&type_param=%s",  args.get("typeParam"));

    // statics
    String policy = "&policy=newsletter"; // always

    // use user infos
    String siren = String.format("&siren=%s", userAttributesHandler.getAttribute(UserAttributesHandler.SIREN_CURRENT).orElseThrow());
    String rne  = String.format("&rne=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow());
    String uai  = String.format("&uai=%s", userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow());

    String queryCreatedFromInputs = operation + policy +
      type + siren + rne + uai + editorsAliases +editorsGroups +typeParam;

    responseMap.put("queryCreatedFromInputs", queryCreatedFromInputs);

    try {
      // Get SympaRemote database Id
      final String sympaRemoteDatabaseId = this.retrieveSympaRemoteDatabaseId();
      final String queryStringWithDbId =  queryCreatedFromInputs + "&databaseId=" + sympaRemoteDatabaseId;


      responseMap.put("sympaRemoteDatabaseId", sympaRemoteDatabaseId);
      responseMap.put("queryStringWithDbId", queryStringWithDbId);

      // Get SympaRemote endpoint URL
      final String sympaRemoteEndpointUrl = this.retrieveSympaRemoteEndpointUrl();
      responseMap.put("sympaRemoteEndpointUrl", sympaRemoteEndpointUrl);


      this.log.debug("Connecting to SympaRemote with the url [" + sympaRemoteEndpointUrl + "]");
      URL uri = new URL(sympaRemoteEndpointUrl);

      URLConnection urlConnection = uri.openConnection();

      //Use POST to hit the SympaRemote web application
      urlConnection.setDoOutput(true);
      OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
      this.log.debug("Posting querystring [" + queryStringWithDbId + "]");
      //Send the queryString
      wr.write(queryStringWithDbId);
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
        String message = errorCodeText;
        responseMap.put("message", message);


        //0 means success, anything else, return an error code to let the ajax handler know something is amiss
        if (!errorCodeNumber.equals("0")) {
          return ResponseEntity.internalServerError().body(responseMap);
        }

        return ResponseEntity.ok(responseMap);
      }


    } catch (MalformedURLException ex) {
      this.log.error("URL exception", ex);
    }  catch (IOException ex) {
      this.log.error("URL exception", ex);
    }

    return ResponseEntity.ok().body(responseMap);
  }











	@PostMapping("/doCloseList")
	public @ResponseBody ResponseEntity<Map<String, String>> doCloseList(@RequestBody Map<String, String> args) throws Exception {

    Map<String, String> responseMap = new HashMap<>();

    String listName = String.format("&listname=%s", args.get("listName"));

    // statics
    String operation = "operation=CLOSE"; //always in this RequestMapping
    String queryCreatedFromInputs = operation + listName;
    responseMap.put("queryCreatedFromInputs", queryCreatedFromInputs);



    // --- DEBUT CHECK ---
    //
    // 1 check si le domaine de la liste à supprimer correspond au domaine courant
    // todo use trim !!!
    String[] listSplit = listName.split("@");
    Assert.isTrue(listSplit.length == 2,"List should have been split in 2 part around '@'");
    String domainName = robotDomaineNameResolver.resolveRobotDomainName();
    log.info("doCloseList check domain name {} against from the list {}", domainName, listSplit[1]);
    if(!listSplit[1].equals(domainName)){
      throw new Exception("domains does not match");
    }

    // 2 check si le user est admin sur cet etab
   boolean isAdmin = robotSympaConf.isAdminRobotSympaByUai(userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow(), userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF).orElseThrow());
    if(!listSplit[1].equals(domainName)){
      throw new Exception("domains does not match");
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
				String errorMessageKey = ServletAjaxController.CLOSE_ERROR_MSG_BASE + ".failure."
						+ errorCodeNumber + "." + errorCodeText;

				String message = this.context.getMessage(errorMessageKey, null, this.locale);

        log.info("");

				//0 means success, anything else, return an error code to let the ajax handler know something is amiss
				if (!errorCodeNumber.equals("0")) {
          responseMap.put("message",message);
          return ResponseEntity.internalServerError().body(responseMap);
       //   response.setStatus(500);
				}

        responseMap.put("message",message);
				return ResponseEntity.ok(responseMap);
			}

		} catch (MalformedURLException ex) {
			this.log.error("URL exception", ex);
		}  catch (IOException ex) {
			this.log.error("URL exception", ex);
		}

		return ResponseEntity.ok(responseMap);
	}

//	/**
//	 * Send an email.
//	 * This email is BCC sended to the sender.
//	 *
//	 * @param fromAddress sender
//	 * @param toAddress recipient
//	 * @param subject subject
//	 * @param message message content
//	 * @param request HTTP request
//	 * @param response HTTP rersponse
//	 */
//	@RequestMapping("/sendEmail")
//	public void sendEmail(final String fromAddress, final String toAddress,
//			final String subject, final String message, final HttpServletRequest request,
//			final HttpServletResponse response) {
//		// MBD: Ajout du choix du sujet du mail
//		try {
//			this.sendEmail(fromAddress, toAddress, subject, message);
//
//			// MBD: Envoi d'un second mail au from pour qu'il est une trace dans sa boite mail.
//			this.sendEmail(toAddress, fromAddress, subject, message);
//		} catch (UnsupportedEncodingException ex) {
//			this.log.warn(ex);
//			response.setStatus(403);
//			return;
//		}
//
//		//set status ok
//		response.setStatus(200);
//	}
//
//	protected void sendEmail(final String fromAddress, final String toAddress, final String subject,
//			final String message) throws UnsupportedEncodingException {
//		InternetAddress[] tos = new InternetAddress[1];
//		tos[0] = new InternetAddress(toAddress,  toAddress);
//
//		InternetAddress from = new InternetAddress(fromAddress, fromAddress);
//		InternetAddress[] bccs = new InternetAddress[0];
//
//		if (this.smtp instanceof SimpleSmtpServiceImpl) {
//			SimpleSmtpServiceImpl impl = (SimpleSmtpServiceImpl) this.smtp;
//			impl.setFromAddress(from);
//		}
//
//		//smtp.send(tos, "-", "", message);
//
//		if (StringUtils.isEmpty(subject)) {
//			this.smtp.sendtocc(tos, null, bccs, "-", null, message, null);
//		} else {
//			this.smtp.sendtocc(tos, null, bccs, subject, null, message, null);
//		}
//	}
//

  final static String createListAdditionalGroupsCacheKey = "createListAdditionalGroupsCache";


  @SuppressWarnings("unchecked")
  @RequestMapping("/jstreeData")
  public @ResponseBody ResponseEntity<List<JsList>> jstreeData(@RequestBody Map<String, String> args) {

    Map<String, Object> responseMap = new HashMap<>();

    String establishementId = args.get("establishementId");

    List<String> additionalGroups = null;
    Map<String, List<String>> createListAdditionalGroupsCache = null;

    try {
      //First check if we have results cached

      //todo put in REDIS cache, not session cache, in case two user from the same etabs use the service before cache expiry
      createListAdditionalGroupsCache = (Map<String, List<String>>)
         sessionAttributesHandler.getSessionAttribute(createListAdditionalGroupsCacheKey, Map.class).orElse(null);

    } catch (Exception ex) {
      log.error("",ex);
    }

    if ((createListAdditionalGroupsCache != null)
      && createListAdditionalGroupsCache.containsKey(establishementId)) {
      additionalGroups = createListAdditionalGroupsCache.get(establishementId);
      this.log.debug("Fetched additional groups from cache, size: " + additionalGroups.size());
    }

    //if the list was not in the cache, then fetch them
    if (additionalGroups == null) {
      //Fetch the list of available lists

      // Construct user info map to call the groups finder.
      Map<String, String> userInfo = new HashMap<String, String>();
      String uaiUserPropertyKey = UserAttributesHandler.UAI_CURRENT;
      userInfo.put(uaiUserPropertyKey, establishementId);

      Collection<String> additionalGroupsColl = this.jsTreeGroupFinder.findGroupsOfEtab(userInfo);

      additionalGroups = new ArrayList<String>(additionalGroupsColl);
      Collections.sort(additionalGroups);

      //Stock in cache for later retrieval
      if (createListAdditionalGroupsCache != null) {
        createListAdditionalGroupsCache.put(establishementId, additionalGroups);
      }
    }

    //Convert strings in additionalGroups into a tree structure for the jsTree in the UI
    List<JsList> listsToCreate = new ArrayList<JsList>();


    List<JsList> rootNodes = new ArrayList<>();

    List<JsList> allNodes = new ArrayList<>();

    for (String groupStr : additionalGroups){
      String[] levels = groupStr.split(":");
      int lastLevel = levels.length - 1;
      String nodeKey = "";
      JsList previousNode = null;

      for (int i = 0; i <= lastLevel; ++i){
        String currentLevel = levels[i];
        nodeKey += i == 0 ? currentLevel : ":"+currentLevel;

        JsList node = JsList.getMatchingNodeOnKey(allNodes, nodeKey);

        if(Objects.isNull(node)){
          node = new JsList();
          node.setData(currentLevel);
          node.setNodeKey(nodeKey);
          allNodes.add(node);

          if(i==0){
            rootNodes.add(node);
          }
          if(i!=0){
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
        node.getAttr().put("id", "nodeId" + Integer.toString((groupStr + i).hashCode()));
        node.setId("nodeId" + Integer.toString((groupStr + i).hashCode()));

        previousNode = node;
      }

    }




    for (String groupStr : additionalGroups) {
      String[] levels = groupStr.split(":");
      int lastLevel = levels.length - 1;

      List<JsList> nodesForLevel = listsToCreate;

      for (int i = 0; i <= lastLevel; ++i) {
        String level = levels[i];
        JsList node = JsList.getMatchingNode(nodesForLevel, level);

        if (node == null) {
          node = new JsList();
          node.setData(level);
          nodesForLevel.add(node);
        }

        if (i == lastLevel) {
          node.getMetadata().put("groupName", groupStr);
          node.getAttr().put("rel", "group");
        } else {
          node.getAttr().put("rel", "folder");
        }

        //Set the html id of the nodes.  This is needed in order for the JSTree to work properly.  Must not contain special characters as jsTree does not handle them well.
        //As such, a hashcode is used which is unique enough and doesn't use special characters
        node.getAttr().put("id", "nodeId" + Integer.toString((groupStr + i).hashCode()));

        nodesForLevel = node.getChildren();
      }
    }

    responseMap.put("new",rootNodes);
//    responseMap.put("old",listsToCreate);
//    responseMap.put("newCount",countNodes(rootNodes));
//    responseMap.put("oldCount",countNodes(listsToCreate));
    return ResponseEntity.ok(rootNodes);
  }

  //todo remove after debug

  public static int countNodes(List<JsList> roots) {
    int totalCount = 0;
    for (JsList root : roots) {
      totalCount += root.countNodes();
    }
    return totalCount;
  }


	protected RobotSympaInfo getRobotInfo(){
		RobotSympaInfo rsi = null;

    rsi = robotSympaConf.getRobotSympaInfoByUai(userAttributesHandler.getAttribute(UserAttributesHandler.UAI_CURRENT).orElseThrow(), userAttributesHandler.getAttributeList(UserAttributesHandler.IS_MEMBER_OF).orElseThrow(), true);
//				rsi = robotSympaConf.getRobotSympaInfo(uib);

		return rsi;
	}



  @Autowired
  DebugProperties debugProperties;

	/**
	 * Retrieve the Sympa Remote endpoint URL from the HTTP session.
	 */
	protected String retrieveSympaRemoteEndpointUrl() {

    //todo redirect temporary to test sympa remote

    return debugProperties.getTestSympaRemoteUri();

//		RobotSympaInfo rsi = getRobotInfo();
//		String sympaRemoteEndpointUrl = null;
//		if (rsi != null) {
//			sympaRemoteEndpointUrl = rsi.getSympaRemoteUrl();
//		} else {
//			log.debug("RobotSympaInfo est null");
//		}
//		return sympaRemoteEndpointUrl;
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
//
//	protected String findErrorMessageBase(final String queryString) {
//		String baseErrorMsg = null;
//		Matcher opMatcher = this.operationPattern.matcher(queryString);
//		if (opMatcher.find()) {
//			final String operation = opMatcher.group(1);
//			if ("CREATE".equals(operation)) {
//				baseErrorMsg = ServletAjaxController.CREATE_ERROR_MSG_BASE;
//			} else if ("UPDATE".equals(operation)) {
//				baseErrorMsg = ServletAjaxController.UPDATE_ERROR_MSG_BASE;
//			} else if ("CLOSE".equals(operation)) {
//				baseErrorMsg = ServletAjaxController.CLOSE_ERROR_MSG_BASE;
//			}
//		}
//
//		return baseErrorMsg;
//	}
//
//	public String getDefaultSympaRemoteEndpointUrl() {
//		return this.defaultSympaRemoteEndpointUrl;
//	}
//
//	public void setDefaultSympaRemoteEndpointUrl(final String url) {
//		this.defaultSympaRemoteEndpointUrl = url;
//	}
//
//	public String getDefaultSympaRemoteDatabaseId() {
//		return defaultSympaRemoteDatabaseId;
//	}
//
//	public void setDefaultSympaRemoteDatabaseId(String defaultSympaRemoteDatabaseId) {
//		this.defaultSympaRemoteDatabaseId = defaultSympaRemoteDatabaseId;
//	}
//
//	public UserInfoBean getUserInfoBean() {
//		return userInfoBean;
//	}
//
//	public void setUserInfoBean(UserInfoBean userInfoBean) {
//		this.userInfoBean = userInfoBean;
//	}

}
