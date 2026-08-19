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
package fr.recia.sympaApi.config.custom.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Map;

@Getter
@Setter
public class UserCustomImplementation extends User {
    public UserCustomImplementation(String username, String password, Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes) {
        super(username, password, authorities);
        this.setAttributes(attributes);
    }

    public UserCustomImplementation(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.setAttributes(attributes);
    }

    Map<String, Object> attributes;

    @Override
    public String toString() {
        return "UserCustomImplementation{" +
                "attributes=" + attributes +
                '}';
    }
}
