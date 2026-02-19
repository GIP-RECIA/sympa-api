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
package fr.recia.sympaApi.pojo;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author GIP RECIA 2013 - Maxime BOSSARD.
 *
 */

@Service
public class SympaRobot {

	private static final Pattern ENPOINT_URL_PATTERN =
			Pattern.compile("([^:]*://)([^/\\?]*)(/?.*)");

	private static final SympaRobot DEFAULT_ROBOT = new SympaRobot();
	static {
		SympaRobot.DEFAULT_ROBOT.defaultRobot = true;
	}

	private String domainName;

	private boolean defaultRobot = false;

	private SympaRobot() {
		super();
	}

	@Override
	public String toString() {
		return "SympaRobot [domainName=" + this.domainName + "]";
	}

	public SympaRobot(final String pDomainName) {
		super();

		Assert.hasText(pDomainName, "No domain name passed to build this robot !");
		this.domainName = pDomainName;
	}

	public static SympaRobot getDefaultRobot() {
		return SympaRobot.DEFAULT_ROBOT;

	}

	public String transformRobotUrl(final String serverUrl) {
		String result = serverUrl;

		if (!this.defaultRobot) {
			// If not the default robot
			Matcher m = SympaRobot.ENPOINT_URL_PATTERN.matcher(serverUrl);
			if (m.find()) {
				StringBuilder sb = new StringBuilder(serverUrl.length() * 2);
				sb.append(m.group(1));
				sb.append(this.domainName);
				sb.append(m.group(3));
				result = sb.toString();
			}
		}

		return result;
	}

	public String getDomainName() {
		return this.domainName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.domainName == null) ? 0 : this.domainName.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		SympaRobot other = (SympaRobot) obj;
		if (this.domainName == null) {
			if (other.domainName != null) {
				return false;
			}
		} else if (!this.domainName.equals(other.domainName)) {
			return false;
		}
		return true;
	}

}
