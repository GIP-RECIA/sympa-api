# esup-sympa-api

API pour les services : 
- Listes de diffusion
- Administration des listes de diffusion

### To run server with properties outside of project scope

`./mvnw clean compile spring-boot:run -Dspring-boot.run.fork=false -Dspring.config.additional-location=$PATH_PROPERTIES/EsupSympaApi/application-local.yml`

Without '-Dspring-boot.run.fork=false' the -Dspring.config.additional-location is not received by SpringBoot.


### Commandes pour notice et license
- `mvn notice:check`
- `mvn notice:generate`
- `mvn license:check`
- `mvn license:format`
- `mvn license:remove`
