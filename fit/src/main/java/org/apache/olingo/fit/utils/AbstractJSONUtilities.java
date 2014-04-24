/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.fit.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.NotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.fit.metadata.Metadata;
import org.apache.olingo.fit.metadata.NavigationProperty;

public abstract class AbstractJSONUtilities extends AbstractUtilities {

  public AbstractJSONUtilities(final ODataServiceVersion version) throws Exception {
    super(version);
  }

  @Override
  protected Accept getDefaultFormat() {
    return Accept.JSON_FULLMETA;
  }

  @Override
  protected InputStream addLinks(
          final String entitySetName, final String entitykey, final InputStream is, final Set<String> links)
          throws Exception {
    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(is);
    IOUtils.closeQuietly(is);

    for (String link : links) {
      srcNode.set(link + Constants.get(version, ConstantKey.JSON_NAVIGATION_SUFFIX),
              new TextNode(Commons.getLinksURI(version, entitySetName, entitykey, link)));
    }

    return IOUtils.toInputStream(srcNode.toString(), "UTF-8");
  }

  @Override
  protected Set<String> retrieveAllLinkNames(InputStream is) throws Exception {
    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(is);
    IOUtils.closeQuietly(is);

    final Set<String> links = new HashSet<String>();

    final Iterator<String> fieldIter = srcNode.fieldNames();

    while (fieldIter.hasNext()) {
      final String field = fieldIter.next();

      if (field.endsWith(Constants.get(version, ConstantKey.JSON_NAVIGATION_BIND_SUFFIX))
              || field.endsWith(Constants.get(version, ConstantKey.JSON_NAVIGATION_SUFFIX))
              || field.endsWith(Constants.get(version, ConstantKey.JSON_MEDIA_SUFFIX))
              || field.endsWith(Constants.get(version, ConstantKey.JSON_EDITLINK_NAME))) {
        if (field.indexOf('@') > 0) {
          links.add(field.substring(0, field.indexOf('@')));
        } else {
          links.add(field);
        }
      }
    }

    return links;
  }

  @Override
  protected NavigationLinks retrieveNavigationInfo(final String entitySetName, final InputStream is)
          throws Exception {

    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(is);
    IOUtils.closeQuietly(is);

    final NavigationLinks links = new NavigationLinks();

    final Iterator<Map.Entry<String, JsonNode>> fieldIter = srcNode.fields();

    final Metadata metadata = Commons.getMetadata(version);
    final Map<String, NavigationProperty> navigationProperties = metadata.getNavigationProperties(entitySetName);

    while (fieldIter.hasNext()) {
      final Map.Entry<String, JsonNode> field = fieldIter.next();
      if (field.getKey().endsWith(Constants.get(version, ConstantKey.JSON_NAVIGATION_BIND_SUFFIX))) {
        final String title = field.getKey().substring(0, field.getKey().indexOf('@'));
        final List<String> hrefs = new ArrayList<String>();
        if (field.getValue().isArray()) {
          for (JsonNode href : ((ArrayNode) field.getValue())) {
            final String uri = href.asText();
            hrefs.add(uri.substring(uri.lastIndexOf('/') + 1));
          }
        } else {
          final String uri = field.getValue().asText();
          hrefs.add(uri.substring(uri.lastIndexOf('/') + 1));
        }

        links.addLinks(title, hrefs);
      } else if (navigationProperties.containsKey(field.getKey())) {
        links.addInlines(field.getKey(), IOUtils.toInputStream(field.getValue().toString(), "UTF-8"));
      }
    }

    return links;
  }

  /**
   * {@inheritDoc }
   */
  @Override
  protected InputStream normalizeLinks(
          final String entitySetName, final String entityKey, final InputStream is, final NavigationLinks links)
          throws Exception {

    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(is);

    if (links != null) {
      for (String linkTitle : links.getLinkNames()) {
        // normalize link
        srcNode.remove(linkTitle + Constants.get(version, ConstantKey.JSON_NAVIGATION_BIND_SUFFIX));
        srcNode.set(
                linkTitle + Constants.get(version, ConstantKey.JSON_NAVIGATION_SUFFIX),
                new TextNode(String.format("%s(%s)/%s", entitySetName, entityKey, linkTitle)));
      }

      for (String linkTitle : links.getInlineNames()) {
        // normalize link if exist; declare a new one if missing
        srcNode.remove(linkTitle + Constants.get(version, ConstantKey.JSON_NAVIGATION_BIND_SUFFIX));
        srcNode.set(
                linkTitle + Constants.get(version, ConstantKey.JSON_NAVIGATION_SUFFIX),
                new TextNode(String.format("%s(%s)/%s", entitySetName, entityKey, linkTitle)));

        // remove inline
        srcNode.remove(linkTitle);

        // remove from links
        links.removeLink(linkTitle);
      }
    }

    srcNode.set(
            Constants.get(version, ConstantKey.JSON_EDITLINK_NAME), new TextNode(
                    Constants.get(version, ConstantKey.DEFAULT_SERVICE_URL) + entitySetName + "(" + entityKey + ")"));

    return IOUtils.toInputStream(srcNode.toString(), Constants.ENCODING);
  }

  @Override
  public InputStream getPropertyValue(final InputStream src, final List<String> path)
          throws Exception {
    
    final JsonNode srcNode = mapper.readTree(src);
    final JsonNode node = getProperty(srcNode, path);
    return IOUtils.toInputStream(node.asText(), Constants.ENCODING);
  }

  @Override
  public InputStream getProperty(
          final String entitySetName, final String entityId, final List<String> path, final String edmType)
          throws Exception {

    final InputStream src = fsManager.readFile(
            Commons.getEntityBasePath(entitySetName, entityId) + Constants.get(version, ConstantKey.ENTITY),
            Accept.JSON_FULLMETA);

    
    final JsonNode srcNode = mapper.readTree(src);

    final ObjectNode propertyNode = new ObjectNode(JsonNodeFactory.instance);

    if (StringUtils.isNotBlank(edmType) && version.compareTo(ODataServiceVersion.V40) < 0) {
      propertyNode.put(Constants.get(
              version, ConstantKey.JSON_ODATAMETADATA_NAME),
              Constants.get(version, ConstantKey.ODATA_METADATA_PREFIX) + edmType);
    }

    JsonNode jsonNode = getProperty(srcNode, path);

    if (jsonNode.isArray()) {
      propertyNode.put("value", (ArrayNode) jsonNode);
    } else if (jsonNode.isObject()) {
      propertyNode.putAll((ObjectNode) jsonNode);
    } else {
      propertyNode.put("value", jsonNode.asText());
    }

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, propertyNode);

    final InputStream res = new ByteArrayInputStream(bos.toByteArray());
    IOUtils.closeQuietly(bos);

    return res;
  }

  private JsonNode getProperty(final JsonNode node, final List<String> path)
          throws NotFoundException {

    JsonNode propertyNode = node;
    for (int i = 0; i < path.size(); i++) {
      propertyNode = propertyNode.get(path.get(i));
      if (propertyNode == null) {
        throw new NotFoundException();
      }
    }

    return propertyNode;
  }

  public InputStream addJsonInlinecount(
          final InputStream src, final int count, final Accept accept)
          throws Exception {
    
    final JsonNode srcNode = mapper.readTree(src);

    ((ObjectNode) srcNode).put(Constants.get(version, ConstantKey.ODATA_COUNT_NAME), count);

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, srcNode);

    final InputStream res = new ByteArrayInputStream(bos.toByteArray());
    IOUtils.closeQuietly(bos);

    return res;
  }

  public InputStream wrapJsonEntities(final InputStream entities) throws Exception {
    
    final JsonNode node = mapper.readTree(entities);

    final ObjectNode res;

    final JsonNode value = node.get(Constants.get(version, ConstantKey.JSON_VALUE_NAME));

    if (value.isArray()) {
      res = mapper.createObjectNode();
      res.set("value", value);
      final JsonNode next = node.get(Constants.get(version, ConstantKey.JSON_NEXTLINK_NAME));
      if (next != null) {
        res.set(Constants.get(version, ConstantKey.JSON_NEXTLINK_NAME), next);
      }
    } else {
      res = (ObjectNode) value;
    }

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    mapper.writeValue(bos, res);

    final InputStream is = new ByteArrayInputStream(bos.toByteArray());
    IOUtils.closeQuietly(bos);

    return is;
  }

  @Override
  public InputStream selectEntity(final InputStream src, final String[] propertyNames) throws Exception {
    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(src);

    final Set<String> retain = new HashSet<String>();
    retain.add(Constants.get(version, ConstantKey.JSON_ID_NAME));
    retain.add(Constants.get(version, ConstantKey.JSON_TYPE_NAME));
    retain.add(Constants.get(version, ConstantKey.JSON_EDITLINK_NAME));
    retain.add(Constants.get(version, ConstantKey.JSON_NEXTLINK_NAME));
    retain.add(Constants.get(version, ConstantKey.JSON_ODATAMETADATA_NAME));
    retain.add(Constants.get(version, ConstantKey.JSON_VALUE_NAME));

    for (String name : propertyNames) {
      retain.add(name);
      retain.add(name + Constants.get(version, ConstantKey.JSON_NAVIGATION_SUFFIX));
      retain.add(name + Constants.get(version, ConstantKey.JSON_MEDIA_SUFFIX));
      retain.add(name + Constants.get(version, ConstantKey.JSON_TYPE_SUFFIX));
    }

    srcNode.retain(retain);

    return IOUtils.toInputStream(srcNode.toString(), "UTF-8");
  }

  @Override
  public InputStream readEntities(
          final List<String> links, final String linkName, final String next, final boolean forceFeed)
          throws Exception {

    if (links.isEmpty()) {
      throw new NotFoundException();
    }

    
    final ObjectNode node = mapper.createObjectNode();

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    if (forceFeed || links.size() > 1) {
      bos.write("[".getBytes());
    }

    for (String link : links) {
      try {
        final Map.Entry<String, String> uriMap = Commons.parseEntityURI(link);
        final Map.Entry<String, InputStream> entity =
                readEntity(uriMap.getKey(), uriMap.getValue(), Accept.JSON_FULLMETA);

        if (bos.size() > 1) {
          bos.write(",".getBytes());
        }

        IOUtils.copy(entity.getValue(), bos);
      } catch (Exception e) {
        // log and ignore link
        LOG.warn("Error parsing uri {}", link, e);
      }
    }

    if (forceFeed || links.size() > 1) {
      bos.write("]".getBytes());
    }

    node.set(Constants.get(version, ConstantKey.JSON_VALUE_NAME),
            mapper.readTree(new ByteArrayInputStream(bos.toByteArray())));

    if (StringUtils.isNotBlank(next)) {
      node.set(Constants.get(version, ConstantKey.JSON_NEXTLINK_NAME), new TextNode(next));
    }

    return IOUtils.toInputStream(node.toString(), "UTF-8");
  }

  @Override
  protected InputStream replaceLink(
          final InputStream toBeChanged, final String linkName, final InputStream replacement)
          throws Exception {
    

    final ObjectNode toBeChangedNode = (ObjectNode) mapper.readTree(toBeChanged);
    final ObjectNode replacementNode = (ObjectNode) mapper.readTree(replacement);

    if (toBeChangedNode.get(linkName + Constants.get(version, ConstantKey.JSON_NAVIGATION_SUFFIX)) == null) {
      throw new NotFoundException();
    }

    toBeChangedNode.set(linkName, replacementNode.get(Constants.get(version, ConstantKey.JSON_VALUE_NAME)));

    final JsonNode next = replacementNode.get(linkName + Constants.get(version, ConstantKey.JSON_NEXTLINK_NAME));
    if (next != null) {
      toBeChangedNode.set(linkName + Constants.get(version, ConstantKey.JSON_NEXTLINK_SUFFIX), next);
    }

    return IOUtils.toInputStream(toBeChangedNode.toString(), "UTF-8");
  }

  @Override
  protected Map<String, InputStream> getChanges(final InputStream src) throws Exception {
    final Map<String, InputStream> res = new HashMap<String, InputStream>();

    
    final JsonNode srcObject = mapper.readTree(src);

    final Iterator<Map.Entry<String, JsonNode>> fields = srcObject.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      res.put(field.getKey(), IOUtils.toInputStream(field.getValue().toString(), "UTF-8"));
    }

    return res;
  }

  @Override
  public Map.Entry<String, List<String>> extractLinkURIs(
          final String entitySetName, final String entityId, final String linkName)
          throws Exception {
    final LinkInfo links = readLinks(entitySetName, entityId, linkName, Accept.JSON_FULLMETA);
    return extractLinkURIs(links.getLinks());
  }

  @Override
  public Map.Entry<String, List<String>> extractLinkURIs(final InputStream is)
          throws Exception {
    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(is);
    IOUtils.closeQuietly(is);

    final List<String> links = new ArrayList<String>();

    JsonNode uris = srcNode.get("value");
    if (uris == null) {
      final JsonNode url = srcNode.get("url");
      if (url != null) {
        links.add(url.textValue());
      }
    } else {
      final Iterator<JsonNode> iter = ((ArrayNode) uris).iterator();
      while (iter.hasNext()) {
        links.add(iter.next().get("url").textValue());
      }
    }

    final JsonNode next = srcNode.get(Constants.get(version, ConstantKey.JSON_NEXTLINK_NAME));

    return new SimpleEntry<String, List<String>>(next == null ? null : next.asText(), links);
  }

  @Override
  public InputStream addEditLink(
          final InputStream content, final String title, final String href) throws Exception {
    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(content);
    IOUtils.closeQuietly(content);

    srcNode.set(Constants.get(version, ConstantKey.JSON_EDITLINK_NAME), new TextNode(href));
    return IOUtils.toInputStream(srcNode.toString(), "UTF-8");
  }

  @Override
  public InputStream addOperation(final InputStream content, final String name, final String metaAnchor,
          final String href) throws Exception {

    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(content);
    IOUtils.closeQuietly(content);

    final ObjectNode action = mapper.createObjectNode();
    action.set("title", new TextNode(name));
    action.set("target", new TextNode(href));

    srcNode.set(metaAnchor, action);
    return IOUtils.toInputStream(srcNode.toString(), "UTF-8");
  }

  @Override
  public InputStream replaceProperty(
          final InputStream src, final InputStream replacement, final List<String> path, final boolean justValue)
          throws Exception {
    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(src);
    IOUtils.closeQuietly(src);

    final JsonNode replacementNode;
    if (justValue) {
      replacementNode = new TextNode(IOUtils.toString(replacement));
    } else {
      replacementNode = (ObjectNode) mapper.readTree(replacement);
    }
    IOUtils.closeQuietly(replacement);

    JsonNode node = srcNode;
    for (int i = 0; i < path.size() - 1; i++) {
      node = node.get(path.get(i));
      if (node == null) {
        throw new NotFoundException();
      }
    }

    ((ObjectNode) node).set(path.get(path.size() - 1), replacementNode);

    return IOUtils.toInputStream(srcNode.toString(), "UTF-8");
  }

  @Override
  public InputStream deleteProperty(final InputStream src, final List<String> path) throws Exception {
    
    final ObjectNode srcNode = (ObjectNode) mapper.readTree(src);
    IOUtils.closeQuietly(src);

    JsonNode node = srcNode;
    for (int i = 0; i < path.size() - 1; i++) {
      node = node.get(path.get(i));
      if (node == null) {
        throw new NotFoundException();
      }
    }

    ((ObjectNode) node).set(path.get(path.size() - 1), null);

    return IOUtils.toInputStream(srcNode.toString(), "UTF-8");
  }
}
