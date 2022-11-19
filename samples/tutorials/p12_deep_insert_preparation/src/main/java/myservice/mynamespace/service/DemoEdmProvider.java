/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package myservice.mynamespace.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.edm.provider.CsdlTerm;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlCollection;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlExpression;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlPropertyPath;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlPropertyValue;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlRecord;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;

/*
 * this class is supposed to declare the metadata of the OData service
 * it is invoked by the Olingo framework e.g. when the metadata document of the service is invoked
 * e.g. http://localhost:8080/ExampleService1/ExampleService1.svc/$metadata
 */
public class DemoEdmProvider extends CsdlAbstractEdmProvider {

  // Service Namespace
  public static final String NAMESPACE = "OData.Demo";

  // EDM Container
  public static final String CONTAINER_NAME = "Container";
  public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

  // Entity Types Names
  public static final String ET_PRODUCT_NAME = "Product";
  public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

  public static final String ET_CATEGORY_NAME = "Category";
  public static final FullQualifiedName ET_CATEGORY_FQN = new FullQualifiedName(NAMESPACE, ET_CATEGORY_NAME);

  // Entity Set Names
  public static final String ES_PRODUCTS_NAME = "Products";
  public static final String ES_CATEGORIES_NAME = "Categories";

  public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {

    // this method is called for each EntityType that are configured in the Schema
    CsdlEntityType entityType = null;

    if (entityTypeName.equals(ET_PRODUCT_FQN)) {
      // create EntityType properties
      CsdlProperty id = new CsdlProperty().setName("ID")
          .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName()).setNullable(false);
      CsdlProperty name = new CsdlProperty().setName("Name")
          .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
      CsdlProperty description = new CsdlProperty().setName("Description")
          .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      // create PropertyRef for Key element
      CsdlPropertyRef propertyRef = new CsdlPropertyRef();
      propertyRef.setName("ID");

      // navigation property: many-to-one, null not allowed (product must have a
      // category)
      CsdlNavigationProperty navProp = new CsdlNavigationProperty().setName("Category")
          .setType(ET_CATEGORY_FQN).setNullable(true)
          .setPartner("Products");
      List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
      navPropList.add(navProp);

      // configure EntityType
      entityType = new CsdlEntityType();
      entityType.setName(ET_PRODUCT_NAME);
      entityType.setProperties(Arrays.asList(id, name, description));
      entityType.setKey(Arrays.asList(propertyRef));
      entityType.setNavigationProperties(navPropList);

    } else if (entityTypeName.equals(ET_CATEGORY_FQN)) {
      // create EntityType properties
      CsdlProperty id = new CsdlProperty().setName("ID")
          .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
          .setNullable(false);
      CsdlProperty name = new CsdlProperty().setName("Name")
          .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

      // create PropertyRef for Key element
      CsdlPropertyRef propertyRef = new CsdlPropertyRef();
      propertyRef.setName("ID");

      // navigation property: one-to-many
      CsdlNavigationProperty navProp = new CsdlNavigationProperty().setName("Products")
          .setType(ET_PRODUCT_FQN).setCollection(true)
          .setPartner("Category");
      List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
      navPropList.add(navProp);

      // configure EntityType
      entityType = new CsdlEntityType();
      entityType.setName(ET_CATEGORY_NAME);
      entityType.setProperties(Arrays.asList(id, name));
      entityType.setKey(Arrays.asList(propertyRef));
      entityType.setNavigationProperties(navPropList);
    }

    return entityType;
  }

  @Override
  public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {

    CsdlEntitySet entitySet = null;

    if (entityContainer.equals(CONTAINER)) {

      if (entitySetName.equals(ES_PRODUCTS_NAME)) {

        entitySet = new CsdlEntitySet();
        entitySet.setName(ES_PRODUCTS_NAME);
        entitySet.setType(ET_PRODUCT_FQN);

        // navigation
        CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
        navPropBinding.setTarget("Categories"); // the target entity set, where the navigation property points to
        navPropBinding.setPath("Category"); // the path from entity type to navigation property
        List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
        navPropBindingList.add(navPropBinding);
        entitySet.setNavigationPropertyBindings(navPropBindingList);
        // annotation
        List<CsdlPropertyValue> propertyList = new ArrayList<CsdlPropertyValue>();
        propertyList.add(
            new CsdlPropertyValue()
                .setProperty("Updatable")
                .setValue(new CsdlConstantExpression(ConstantExpressionType.Bool, "true")));
        propertyList.add(
            new CsdlPropertyValue()
                .setProperty("DeltaUpdateSupported")
                .setValue(new CsdlConstantExpression(ConstantExpressionType.Bool, "true")));

        propertyList.add(
            new CsdlPropertyValue()
                .setProperty("UpdateMethod")
                .setValue(new CsdlConstantExpression(ConstantExpressionType.EnumMember,
                    "Org.OData.Capabilities.V1.HttpMethod/PATCH")));

        propertyList.add(
            new CsdlPropertyValue()
                .setProperty("FilterSegmentSupported")
                .setValue(new CsdlConstantExpression(ConstantExpressionType.Bool, "false")));

        propertyList.add(
            new CsdlPropertyValue()
                .setProperty("TypecastSegmentSupported")
                .setValue(new CsdlConstantExpression(ConstantExpressionType.Bool, "false")));

        propertyList.add(
            new CsdlPropertyValue()
                .setProperty("NonUpdatableProperties")
                .setValue(
                    new CsdlCollection().setItems(
                        Collections.singletonList((CsdlExpression) new CsdlPropertyPath().setValue("Name")))));

        propertyList.add(
            new CsdlPropertyValue()
                .setProperty("MaxLevels")
                .setValue(new CsdlConstantExpression(ConstantExpressionType.Int, "0")));

        entitySet.setAnnotations(
            Collections.singletonList(new CsdlAnnotation().setTerm("Org.OData.Capabilities.V1.UpdateRestrictions")
                .setExpression(new CsdlRecord().setPropertyValues(propertyList))));

      } else if (entitySetName.equals(ES_CATEGORIES_NAME)) {

        entitySet = new CsdlEntitySet();
        entitySet.setName(ES_CATEGORIES_NAME);
        entitySet.setType(ET_CATEGORY_FQN);

        // navigation
        CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
        navPropBinding.setTarget("Products"); // the target entity set, where the navigation property points to
        navPropBinding.setPath("Products"); // the path from entity type to navigation property
        List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
        navPropBindingList.add(navPropBinding);
        entitySet.setNavigationPropertyBindings(navPropBindingList);
      }
    }

    return entitySet;

  }

  @Override
  public List<CsdlSchema> getSchemas() {

    // create Schema
    CsdlSchema schema = new CsdlSchema();
    schema.setNamespace(NAMESPACE);

    // add EntityTypes
    List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
    entityTypes.add(getEntityType(ET_PRODUCT_FQN));
    entityTypes.add(getEntityType(ET_CATEGORY_FQN));
    schema.setEntityTypes(entityTypes);

    // add EntityContainer
    schema.setEntityContainer(getEntityContainer());

    // finally
    List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
    schemas.add(schema);

    return schemas;
  }

  public CsdlEntityContainer getEntityContainer() {
    // create EntitySets
    List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
    entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));
    entitySets.add(getEntitySet(CONTAINER, ES_CATEGORIES_NAME));

    // create EntityContainer
    CsdlEntityContainer entityContainer = new CsdlEntityContainer();
    entityContainer.setName(CONTAINER_NAME);
    entityContainer.setEntitySets(entitySets);

    return entityContainer;

  }

  @Override
  public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {

    // This method is invoked when displaying the service document at
    // e.g. http://localhost:8080/DemoService/DemoService.svc
    if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
      CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
      entityContainerInfo.setContainerName(CONTAINER);
      return entityContainerInfo;
    }
    return null;
  }

  @Override
  public CsdlTerm getTerm(FullQualifiedName termName) throws ODataException {
    return new CsdlTerm().setName(termName.getName()).setBaseTerm(termName.getNamespace());
  }
}
