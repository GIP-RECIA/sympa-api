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
package fr.recia.sympaApi.sympa.listfinder;


import fr.recia.sympaApi.sympa.listfinder.model.Model;
import fr.recia.sympaApi.sympa.listfinder.model.ModelRequest;
import fr.recia.sympaApi.sympa.listfinder.model.ModelSubscribers;
import fr.recia.sympaApi.sympa.listfinder.model.PreparedRequest;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface IDaoService {


	/**
	 * @return all model subscribers
	 */
	public List<ModelSubscribers> getAllModelSubscribers();



	/**
	 * @return all models
	 */
	public List<Model> getAllModels();

	/**
	 * @param id
	 * @return
	 */
	public Model getModel(BigInteger id);

	/**
	 * @param preparedRequest
	 * @param model
	 * @return The corresponding ModelRequest
	 */
	public ModelRequest getModelRequest(Model model, PreparedRequest preparedRequest);

	/**
	 * @param model
	 * @return
	 */
	public ModelSubscribers getModelSubscriber(Model model);

	/**
	 * @return
	 */
	public List<PreparedRequest> getAllPreparedRequests();

	/**
	 * Convert hibernate objects into IMailingListModel objects in order to work with the available list finder component
	 * @param listModels
	 * @param userInfo
	 * @return
	 */
	public List<IMailingListModel> getMailingListModels(List<Model> listModels, Map<String, String> userInfo);
}
