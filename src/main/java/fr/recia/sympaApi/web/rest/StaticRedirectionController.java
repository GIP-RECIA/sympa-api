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

package fr.recia.sympaApi.web.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticRedirectionController {

    @GetMapping("/")
    public String root() {
        return "redirect:/ui";
    }

    @GetMapping({"/ui", "/ui/"})
    public String index() {
        return "forward:/ui/index.html";
    }

    @GetMapping({"/ui/admin", "/ui/admin/"})
    public String admin() {
        return "forward:/ui/admin.html";
    }
}
