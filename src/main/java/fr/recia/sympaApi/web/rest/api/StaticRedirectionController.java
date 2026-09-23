package fr.recia.sympaApi.web.rest.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
Controlleur utilisé pour rediriger vers /ui/index.html afin de servir le front sur / et /ui
 */
@Controller
public class StaticRedirectionController {

    @GetMapping("/")
    public String root() {
        return "redirect:/ui";
    }

    @GetMapping({"/ui", "/ui/"})
    public String forward() {
        return "forward:/ui/index.html";
    }

    @GetMapping({"/ui/admin",})
    public String admin() {
        return "forward:/ui/admin.html";
    }

}
