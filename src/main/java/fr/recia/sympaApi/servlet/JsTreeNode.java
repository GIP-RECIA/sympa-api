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
package fr.recia.sympaApi.servlet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that represents an email list in the create list screen in the jsTree.
 * 
 *
 */
@Getter
@Setter
public class JsTreeNode {

  @JsonProperty("text")
	private String data;
  @JsonIgnore
	private Map<String, String> attr;
  @JsonIgnore
  private Map<String, String> metadata;

  @JsonProperty("getChildren")
	private List<JsTreeNode> children;

  private String id;

  @JsonIgnore
  private boolean isFolder;

  @JsonProperty("children")
  public boolean hasChildren() {
    return !children.isEmpty();
  }

  @JsonProperty("iconIndex")
  public int getIconIndex() {
    return isFolder ? 0 : 1;
  }

  @JsonIgnore
  private String nodeKey;

	
	public JsTreeNode() {
		children = new ArrayList<>();
		attr = new HashMap<>();
		metadata = new HashMap<>();
    isFolder = false;
	}

  public static JsTreeNode getMatchingNodeOnKey(List<JsTreeNode> listNodes, String nodeKey) {
    for(JsTreeNode l : listNodes) {
      if (l.getNodeKey().equals(nodeKey)) {
        return l;
      }
    }

    return null;
  }
}
