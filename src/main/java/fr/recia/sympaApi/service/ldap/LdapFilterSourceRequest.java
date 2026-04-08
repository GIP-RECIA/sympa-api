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
package fr.recia.sympaApi.service.ldap;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.recia.sympaApi.config.bean.CacheProperties;
import fr.recia.sympaApi.entity.PreparedRequest;
import fr.recia.sympaApi.utils.CacheHandler;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

@Getter
@Setter
@Slf4j
@Service
public class LdapFilterSourceRequest {

	private static String UAIregex = "\\{UAI\\}";
	private static String SIRENregex = "\\{SIREN\\}";

  @Autowired
	private LdapTemplate ldapTemplate;

  @Autowired
  private CacheProperties cacheProperties;
  
  @Autowired
  private CacheHandler cacheHandler;

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
				
					// on test si on a déjà fait la requete
				String request = String.format("%s:%s", filter, base);


        String nameFromCache = cacheHandler.getFromCache(cacheProperties.getLdapFilterRequestCacheName(), request, new TypeReference<>() {});

        // si trouvé dans le cache
        if(Objects.nonNull(nameFromCache)){

          name = nameFromCache;
        }else {
          // sinon on interroge le ldap
          log.debug("LdapFilterSourceRequest PreparedRequest source="  + source+";");
          log.debug(" LdapFilterSourceRequestfilter =" + filter + "; base =" + base+";");

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
          if(Objects.nonNull(name) && !name.isEmpty()){
            log.debug("LdapFilterSourceRequest put name in cache {};{}",request, name);
            cacheHandler.putObjectInCache(cacheProperties.getLdapFilterRequestCacheName(), request, name );
          }
        }
			} else {
				name = preparedRequest.getDisplayName();
			}
			
		}
		return name;
	}
}
