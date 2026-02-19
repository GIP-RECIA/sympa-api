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
package fr.recia.sympaApi.sympa.admin;

import fr.recia.sympaApi.sympa.listfinder.model.PreparedRequest;
import fr.recia.sympaApi.utils.LdapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Getter
@Setter
@Slf4j
@Service
public class LdapFilterSourceRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6561990436374565291L;





	
	
	
	private static String UAIregex = "\\{UAI\\}";
	private static String SIRENregex = "\\{SIREN\\}";
	
	// les valeurs pour former la requette ldap de recherche le siren d'un etab connaissant son uai.
	// Ici on a les valeurs par défauts elles peuvent êtres modifiées via les setters.
	// declaration du bean dans escoLdapContext.
	private String objectClassEtab= "ENTStructure";
	private String ldapAttrSiren = "ENTStructureSiren";
	private String ldapAttrUai="ENTStructureUAI";
	private String ldapBaseRdnEtab = "ou=structures";
	private String filtreLdapSearchSirenByUaiFormat="(&(objectClass=%s)(%s=%s))";
	
	/*
	 * Le bean est de session on memorise ici les requettes ldap de cette session: 
	 */
	private transient HashMap<String, String> request2name = new HashMap<String, String>();
	
	private String functionSources;
	
	

	private transient Set<String> sourceSet = new HashSet<String>();
	
	private transient int pourtest = 0;

  @Autowired
	private LdapTemplate ldapTemplate;

	static private String replaceAll(String in, String uai, String siren) {
		String res = in;
		if (uai != null) {
			res = res.replaceAll(UAIregex, uai);
		}
		if (siren != null) {
			res = res.replaceAll(SIRENregex, siren);
		}
		return res;
	}

	/**
	 * Test si on doit afficher les adresses mail correspondant à la  preparedRequest.
	 * Si non alors donne le displayName de la preparedRequest
	 * Si oui alors interoge ldap pour recuperer l'adresse correspondante:
	 * 	La premiere adresse trouvé sera ajoutée au displayName.
	 * 	si aucunne adresse trouvé renvoie null;
	 * @param preparedRequest
	 * @param uai
	 * @param siren
	 * @return
	 */
	public String makeDisplayName(PreparedRequest preparedRequest, String uai, String siren) {
      log.debug("MakeDisplayName POUR TEST=" + pourtest++);
		
		
		String name = null;
		if (preparedRequest != null) {
			String source = preparedRequest.getDataSource();
			String suffix = preparedRequest.getLdapSuffix();
			String filter = preparedRequest.getLdapFilter();
			if (filter != null && Strings.isNotEmpty(suffix)) {
					// si c'est une source pour laquelle il faut afficher le mail:
					// le suffix contient l'arborescence de recherche.
					// on calcul le filtre ldap
				filter = replaceAll(filter, uai, siren);
				String base = replaceAll(suffix,uai, siren);
				base = base.substring(0, base.indexOf(",dc="));
				
					// on test si on a déjà fait la requette 
				String request = String.format("%s:%s", filter, base);
				if (request2name.containsKey(request)) {
						// si oui on redonne la meme reponse (elle peut etre vide)
          //todo retablir après debugage
				 //	name = request2name.get(request);
          //return name
				}

        {
						// sinon on interroge le ldap
            log.debug("PreparedRequest source="  + source+";");
            log.debug("filter =" + filter + "; base =" + base+";");

					Collection<String> mails =LdapUtils.ldapSearch(ldapTemplate, filter, base, 
							new AttributesMapper() {
								
								@Override
								public Object mapFromAttributes(Attributes attrs) throws NamingException {
									Attribute attr = attrs.get("mail");
									return attr.get();
								}
							}
							
							);
						// si le resultat n'est pas vide
					if (mails != null && ! mails.isEmpty()) {
						Iterator<String> it = mails.iterator();			
						String mail =  it.next();
						String autre = "";
						// on affiche  le premier mail a la suite du nom 
						// et on indique s'il y en a d'autre
						if (it.hasNext()) {
							autre = "...";
						}
						name = String.format("%s: %s%s", preparedRequest.getDisplayName(), mail, autre );
					} 
						// le nom calcule peut etre vide
				//	request2name.put(request, name);
				}
			} else {
				name = preparedRequest.getDisplayName();
			}
			
		}
		return name;
	}
	
	private Set<String> getSourceSet(){
		if (sourceSet.isEmpty() && functionSources != null) {
			for (String src : functionSources.split("\\s+")) {
				sourceSet.add(src);
			}
		}
		return sourceSet;
	}

	public void setFunctionSources(String sources) {
		this.functionSources = sources;
		sourceSet.clear();
	}
}
