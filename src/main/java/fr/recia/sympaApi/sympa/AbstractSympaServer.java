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
package fr.recia.sympaApi.sympa;

import fr.recia.sympaApi.service.CASCredentialRetrieverService;
import fr.recia.sympaApi.pojo.CreateListInfo;
import fr.recia.sympaApi.pojo.UserSympaListWithUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class AbstractSympaServer {
	/**
	 * name of the sympa server
	 */
	private String name;
	/**
	 * root url of the sympa server
	 */
	private String homeUrl;
	/**
	 * wrapper url (%s) will be replaced by various service url (userfull for cas)
	 */
	private String connectUrl;
	/**
	 * administrative url (%l) will be replaced by list name
	 */
	private String adminUrl;
	/**
	 * new list url
	 */
	private String newListUrl;
	/**
	 * createListInfos available on if user in this roles
	 */
	private Set<String> newListForRoles;
	/**
	 * this server is used only if user in this roles (or if usedForRoles is null)
	 */
	private Set<String> usedForRoles;
	
	@Autowired
	private CASCredentialRetrieverService credentialRetriever;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the homeUrl
	 */
	public String getHomeUrl() {
		return homeUrl;
	}
	/**
	 * @param homeUrl the homeUrl to set
	 */
	public void setHomeUrl(String homeUrl) {
		this.homeUrl = homeUrl;
	}
	/**
	 * @return the connectUrl
	 */
	public String getConnectUrl() {
		return connectUrl;
	}
	/**
	 * @param connectUrl the connectUrl to set
	 */
	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
	}
	/**
	 * @return the adminUrl
	 */
	public String getAdminUrl() {
		return adminUrl;
	}
	/**
	 * @param adminUrl the adminUrl to set
	 */
	public void setAdminUrl(String adminUrl) {
		this.adminUrl = adminUrl;
	}
	/**
	 * @return the newListUrl
	 */
	public String getNewListUrl() {
		return newListUrl;
	}
	/**
	 * @param newListUrl the newListUrl to set
	 */
	public void setNewListUrl(String newListUrl) {
		this.newListUrl = newListUrl;
	}
	
	public abstract List<UserSympaListWithUrl> getWhich();


	public CreateListInfo getCreateListInfo() {

		CreateListInfo infos  = new CreateListInfo();
			infos.setServerName(getName());
			infos.setAccessUrl(generateConnectUrl(getNewListUrl()));

		return infos;
	}
	
	protected String generateListUrl(String listHomepage) {
		return generateConnectUrl(listHomepage);
	}
	protected String generateConnectUrl(String url) {
		String tmpConnectUrl = getConnectUrl();
		if ( tmpConnectUrl == null || tmpConnectUrl.trim().length() <= 0 ) return url;
		String tmpUrl = url;
		try {
			tmpUrl = URLEncoder.encode(tmpUrl,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("unable to urlencode",e);
		}
		String strTmp = tmpConnectUrl.replaceFirst("%s", tmpUrl);
		return strTmp;
	}

	protected String generateListAdminUrl(String listAddress) {
		String strListName = listAddress;
		if ( listAddress != null && listAddress.length() > 0 ) {
			int atIdx = listAddress.indexOf("@");
			if ( atIdx > 0) {
				strListName = listAddress.substring(0, atIdx);
			}
		}
		String tmpUrl = getAdminUrl();
		return generateConnectUrl(tmpUrl.replaceFirst("%l", strListName));
	}
	/**
	 * @return the credentialRetriever
	 */
	public CASCredentialRetrieverService getCredentialRetriever() {
		return credentialRetriever;
	}
	/**
	 * @param credentialRetriever the credentialRetriever to set
	 */
	public void setCredentialRetriever(CASCredentialRetrieverService credentialRetriever) {
		this.credentialRetriever = credentialRetriever;
	}
	/**
	 * @return the newListForRoles
	 */
	public Set<String> getNewListForRoles() {
		return newListForRoles;
	}
	/**
	 * @param newListForRoles the newListForRoles to set
	 */
	public void setNewListForRoles(Set<String> newListForRoles) {
		this.newListForRoles = newListForRoles;
	}
	/**
	 * @return the usedForRoles
	 */
	public Set<String> getUsedForRoles() {
		return usedForRoles;
	}
	/**
	 * @param usedForRoles the usedForRoles to set
	 */
	public void setUsedForRoles(Set<String> usedForRoles) {
		this.usedForRoles = usedForRoles;
	}
	
}
