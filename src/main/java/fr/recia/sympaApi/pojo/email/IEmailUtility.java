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
package fr.recia.sympaApi.pojo.email;

import java.util.List;

/**
 * Configuration to an email utility (what will be used when
 * user sends an email).
 * 
 * @author Maxime BOSSARD - GIP RECIA 2012.
 *
 */
public interface IEmailUtility {

	/** Type (horde / simple email form). */	
	String getType();
	
	/** Type (horde / simple email form). */	
	void setType(String type);
	
	/** The patterns which will match the use of this email configuration. */
	void setPatterns(List<String> patterns);
	
	/** Url of the email utility. */
	String getUrl();
	
	/** Url of the email utility. */
	void setUrl(String pattern);
	
	/**
	 * Test if the EmailUtility is the one to choose.
	 * 
	 * @param profileWebMail chose field to match against.
	 * @return true if this is the correct EmailUtility.
	 */
	boolean isCorrectEmailUtility(String profileWebMail);
	
}
