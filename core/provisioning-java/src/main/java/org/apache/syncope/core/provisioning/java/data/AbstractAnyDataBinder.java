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
package org.apache.syncope.core.provisioning.java.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.misc.utils.ConnObjectUtils;
import org.apache.syncope.core.misc.utils.MappingUtils;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractAnyDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractAnyDataBinder.class);

    private static final IntMappingType[] FOR_MANDATORY = new IntMappingType[] {
        IntMappingType.AnyObjectPlainSchema, IntMappingType.AnyObjectDerivedSchema,
        IntMappingType.UserPlainSchema, IntMappingType.UserDerivedSchema,
        IntMappingType.GroupPlainSchema, IntMappingType.GroupDerivedSchema };

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected DerSchemaDAO derSchemaDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected PlainAttrDAO plainAttrDAO;

    @Autowired
    protected DerAttrDAO derAttrDAO;

    @Autowired
    protected PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    protected ExternalResourceDAO resourceDAO;

    @Autowired
    protected PolicyDAO policyDAO;

    @Autowired
    protected RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected VirAttrHandler virAttrHander;

    @Autowired
    protected ConnObjectUtils connObjectUtils;

    @Autowired
    protected MappingUtils mappingUtils;

    protected void setRealm(final Any<?, ?> any, final AnyPatch anyPatch) {
        if (anyPatch.getRealm() != null && StringUtils.isNotBlank(anyPatch.getRealm().getValue())) {
            Realm newRealm = realmDAO.find(anyPatch.getRealm().getValue());
            if (newRealm == null) {
                LOG.debug("Invalid realm specified: {}, ignoring", anyPatch.getRealm().getValue());
            } else {
                any.setRealm(newRealm);
            }
        }
    }

    protected PlainSchema getPlainSchema(final String schemaName) {
        PlainSchema schema = null;
        if (StringUtils.isNotBlank(schemaName)) {
            schema = plainSchemaDAO.find(schemaName);

            // safely ignore invalid schemas from AttrTO
            if (schema == null) {
                LOG.debug("Ignoring invalid schema {}", schemaName);
            } else if (schema.isReadonly()) {
                schema = null;
                LOG.debug("Ignoring readonly schema {}", schemaName);
            }
        }

        return schema;
    }

    private DerSchema getDerSchema(final String derSchemaName) {
        DerSchema schema = null;
        if (StringUtils.isNotBlank(derSchemaName)) {
            schema = derSchemaDAO.find(derSchemaName);
            if (schema == null) {
                LOG.debug("Ignoring invalid derived schema {}", derSchemaName);
            }
        }

        return schema;
    }

    private void fillAttr(final List<String> values, final AnyUtils anyUtils,
            final PlainSchema schema, final PlainAttr<?> attr, final SyncopeClientException invalidValues) {

        // if schema is multivalue, all values are considered for addition;
        // otherwise only the fist one - if provided - is considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                        ? Collections.<String>emptyList()
                        : Collections.singletonList(values.iterator().next()));

        for (String value : valuesProvided) {
            if (value == null || value.isEmpty()) {
                LOG.debug("Null value for {}, ignoring", schema.getKey());
            } else {
                try {
                    attr.add(value, anyUtils);
                } catch (InvalidPlainAttrValueException e) {
                    LOG.warn("Invalid value for attribute " + schema.getKey() + ": " + value, e);

                    invalidValues.getElements().add(schema.getKey() + ": " + value + " - " + e.getMessage());
                }
            }
        }
    }

    private List<String> evaluateMandatoryCondition(final Provision provision, final Any<?, ?> any) {
        List<String> missingAttrNames = new ArrayList<>();

        if (provision != null) {
            for (MappingItem item : provision.getMapping().getItems()) {
                if (ArrayUtils.contains(FOR_MANDATORY, item.getIntMappingType())
                        && (item.getPurpose() == MappingPurpose.PROPAGATION
                        || item.getPurpose() == MappingPurpose.BOTH)) {

                    List<PlainAttrValue> values = mappingUtils.getIntValues(
                            provision, item, Collections.<Any<?, ?>>singletonList(any));
                    if (values.isEmpty() && JexlUtils.evaluateMandatoryCondition(item.getMandatoryCondition(), any)) {
                        missingAttrNames.add(item.getIntAttrName());
                    }
                }
            }
        }

        return missingAttrNames;
    }

    private SyncopeClientException checkMandatoryOnResources(
            final Any<?, ?> any, final Set<ExternalResource> resources) {

        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        for (ExternalResource resource : resources) {
            Provision provision = resource.getProvision(any.getType());
            if (resource.isEnforceMandatoryCondition() && provision != null) {
                List<String> missingAttrNames = evaluateMandatoryCondition(provision, any);
                if (!missingAttrNames.isEmpty()) {
                    LOG.error("Mandatory schemas {} not provided with values", missingAttrNames);

                    reqValMissing.getElements().addAll(missingAttrNames);
                }
            }
        }

        return reqValMissing;
    }

    private SyncopeClientException checkMandatory(final Any<?, ?> any) {
        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        // Check if there is some mandatory schema defined for which no value has been provided
        for (PlainSchema schema : any.getAllowedPlainSchemas()) {
            if (any.getPlainAttr(schema.getKey()) == null
                    && !schema.isReadonly()
                    && (JexlUtils.evaluateMandatoryCondition(schema.getMandatoryCondition(), any))) {

                LOG.error("Mandatory schema " + schema.getKey() + " not provided with values");

                reqValMissing.getElements().add(schema.getKey());
            }
        }

        return reqValMissing;
    }

    private Set<ExternalResource> getAllResources(final Any<?, ?> any) {
        Set<ExternalResource> resources = new HashSet<>();

        if (any instanceof User) {
            resources.addAll(userDAO.findAllResources((User) any));
        } else if (any instanceof Group) {
            resources.addAll(((Group) any).getResources());
        } else if (any instanceof AnyObject) {
            resources.addAll(anyObjectDAO.findAllResources((AnyObject) any));
        }

        return resources;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void processAttrPatch(final Any any, final AttrPatch patch, final PlainSchema schema,
            final AnyUtils anyUtils, final Set<ExternalResource> resources, final PropagationByResource propByRes,
            final SyncopeClientException invalidValues) {

        PlainAttr<?> attr = any.getPlainAttr(schema.getKey());
        if (attr == null) {
            LOG.debug("No plain attribute found for schema {}", schema);

            switch (patch.getOperation()) {
                case ADD_REPLACE:
                    attr = anyUtils.newPlainAttr();
                    ((PlainAttr) attr).setOwner(any);
                    attr.setSchema(schema);
                    any.add(attr);
                    break;

                case DELETE:
                default:
                    return;
            }
        }

        switch (patch.getOperation()) {
            case ADD_REPLACE:
                // 1.1 remove values
                if (attr.getSchema().isUniqueConstraint()) {
                    if (attr.getUniqueValue() != null
                            && !patch.getAttrTO().getValues().isEmpty()
                            && !patch.getAttrTO().getValues().get(0).equals(attr.getUniqueValue().getValueAsString())) {

                        plainAttrValueDAO.delete(attr.getUniqueValue().getKey(), anyUtils.plainAttrUniqueValueClass());
                    }
                } else {
                    Collection<Long> valuesToBeRemoved = CollectionUtils.collect(attr.getValues(),
                            new Transformer<PlainAttrValue, Long>() {

                                @Override
                                public Long transform(final PlainAttrValue input) {
                                    return input.getKey();
                                }
                            });
                    for (Long attrValueKey : valuesToBeRemoved) {
                        plainAttrValueDAO.delete(attrValueKey, anyUtils.plainAttrValueClass());
                    }
                }

                // 1.2 add values
                List<String> valuesToBeAdded = patch.getAttrTO().getValues();
                if (!valuesToBeAdded.isEmpty()
                        && (!schema.isUniqueConstraint() || attr.getUniqueValue() == null
                        || !valuesToBeAdded.iterator().next().equals(attr.getUniqueValue().getValueAsString()))) {

                    fillAttr(valuesToBeAdded, anyUtils, schema, attr, invalidValues);
                }

                // if no values are in, the attribute can be safely removed
                if (attr.getValuesAsStrings().isEmpty()) {
                    plainAttrDAO.delete(attr);
                }
                break;

            case DELETE:
            default:
                any.remove(attr);
                plainAttrDAO.delete(attr.getKey(), anyUtils.plainAttrClass());
        }

        for (ExternalResource resource : resources) {
            for (MappingItem mapItem : MappingUtils.getPropagationMappingItems(resource.getProvision(any.getType()))) {
                if (schema.getKey().equals(mapItem.getIntAttrName())
                        && mapItem.getIntMappingType() == anyUtils.plainIntMappingType()) {

                    propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                    if (mapItem.isConnObjectKey() && !attr.getValuesAsStrings().isEmpty()) {
                        propByRes.addOldConnObjectKey(resource.getKey(), attr.getValuesAsStrings().get(0));
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void processAttrPatch(final Any any, final AttrPatch patch, final DerSchema schema,
            final AnyUtils anyUtils, final Set<ExternalResource> resources, final PropagationByResource propByRes) {

        DerAttr<?> attr = any.getDerAttr(schema.getKey());
        if (attr == null) {
            LOG.debug("No plain attribute found for schema {}", schema);

            switch (patch.getOperation()) {
                case ADD_REPLACE:
                    attr = anyUtils.newDerAttr();
                    ((DerAttr) attr).setOwner(any);
                    attr.setSchema(schema);
                    any.add(attr);
                    break;

                case DELETE:
                default:
                    return;
            }
        }

        if (patch.getOperation() == PatchOperation.DELETE) {
            derAttrDAO.delete(attr);
        }

        for (ExternalResource resource : resources) {
            for (MappingItem mapItem : MappingUtils.getPropagationMappingItems(resource.getProvision(any.getType()))) {
                if (schema.getKey().equals(mapItem.getIntAttrName())
                        && mapItem.getIntMappingType() == anyUtils.derIntMappingType()) {

                    propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                    if (mapItem.isConnObjectKey() && !attr.getValue(any.getPlainAttrs()).isEmpty()) {
                        propByRes.addOldConnObjectKey(resource.getKey(), attr.getValue(any.getPlainAttrs()));
                    }
                }
            }
        }
    }

    protected PropagationByResource fill(final Any<?, ?> any, final AnyPatch anyPatch, final AnyUtils anyUtils,
            final SyncopeClientCompositeException scce) {

        PropagationByResource propByRes = new PropagationByResource();

        // 1. anyTypeClasses
        for (StringPatchItem patch : anyPatch.getAuxClasses()) {
            AnyTypeClass auxClass = anyTypeClassDAO.find(patch.getValue());
            if (auxClass == null) {
                LOG.debug("Invalid " + AnyTypeClass.class.getSimpleName() + "{}, ignoring...", patch.getValue());
            } else {
                switch (patch.getOperation()) {
                    case ADD_REPLACE:
                        any.add(auxClass);
                        break;

                    case DELETE:
                    default:
                        any.remove(auxClass);
                }
            }
        }

        // 2. resources
        for (StringPatchItem patch : anyPatch.getResources()) {
            ExternalResource resource = resourceDAO.find(patch.getValue());
            if (resource == null) {
                LOG.debug("Invalid " + ExternalResource.class.getSimpleName() + "{}, ignoring...", patch.getValue());
            } else {
                switch (patch.getOperation()) {
                    case ADD_REPLACE:
                        propByRes.add(ResourceOperation.CREATE, resource.getKey());
                        any.add(resource);
                        break;

                    case DELETE:
                    default:
                        propByRes.add(ResourceOperation.DELETE, resource.getKey());
                        any.remove(resource);
                }
            }
        }

        Set<ExternalResource> resources = getAllResources(any);
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // 3. plain attributes
        for (AttrPatch patch : anyPatch.getPlainAttrs()) {
            if (patch.getAttrTO() != null) {
                PlainSchema schema = getPlainSchema(patch.getAttrTO().getSchema());
                if (schema == null) {
                    LOG.debug("Invalid " + PlainSchema.class.getSimpleName()
                            + "{}, ignoring...", patch.getAttrTO().getSchema());
                } else {
                    processAttrPatch(any, patch, schema, anyUtils, resources, propByRes, invalidValues);
                }
            }
        }
        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        // 4. derived attributes
        for (AttrPatch patch : anyPatch.getDerAttrs()) {
            if (patch.getAttrTO() != null) {
                DerSchema schema = getDerSchema(patch.getAttrTO().getSchema());
                if (schema == null) {
                    LOG.debug("Invalid " + DerSchema.class.getSimpleName()
                            + "{}, ignoring...", patch.getAttrTO().getSchema());
                } else {
                    processAttrPatch(any, patch, schema, anyUtils, resources, propByRes);
                }
            }
        }

        SyncopeClientException requiredValuesMissing = checkMandatory(any);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }
        requiredValuesMissing = checkMandatoryOnResources(any, resources);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        return propByRes;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void fill(
            final Any any, final AnyTO anyTO, final AnyUtils anyUtils, final SyncopeClientCompositeException scce) {

        // 0. aux classes
        any.getAuxClasses().clear();
        for (String className : anyTO.getAuxClasses()) {
            AnyTypeClass auxClass = anyTypeClassDAO.find(className);
            if (auxClass == null) {
                LOG.debug("Invalid " + AnyTypeClass.class.getSimpleName() + "{}, ignoring...", auxClass);
            } else {
                any.add(auxClass);
            }
        }

        // 1. attributes
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // Only consider attributeTO with values
        for (AttrTO attrTO : anyTO.getPlainAttrs()) {
            if (attrTO.getValues() != null && !attrTO.getValues().isEmpty()) {
                PlainSchema schema = getPlainSchema(attrTO.getSchema());
                if (schema != null) {
                    PlainAttr attr = any.getPlainAttr(schema.getKey());
                    if (attr == null) {
                        attr = anyUtils.newPlainAttr();
                        attr.setOwner(any);
                        attr.setSchema(schema);
                    }
                    fillAttr(attrTO.getValues(), anyUtils, schema, attr, invalidValues);

                    if (attr.getValuesAsStrings().isEmpty()) {
                        attr.setOwner(null);
                    } else {
                        any.add(attr);
                    }
                }
            }
        }

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        // 2. derived attributes
        for (AttrTO attributeTO : anyTO.getDerAttrs()) {
            DerSchema derSchema = getDerSchema(attributeTO.getSchema());
            if (derSchema != null) {
                DerAttr derAttr = anyUtils.newDerAttr();
                derAttr.setOwner(any);
                derAttr.setSchema(derSchema);
                any.add(derAttr);
            }
        }

        SyncopeClientException requiredValuesMissing = checkMandatory(any);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // 3. realm & resources
        Realm realm = realmDAO.find(anyTO.getRealm());
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + anyTO.getRealm());
            scce.addException(noRealm);
        }
        any.setRealm(realm);

        for (String resourceName : anyTO.getResources()) {
            ExternalResource resource = resourceDAO.find(resourceName);
            if (resource == null) {
                LOG.debug("Invalid " + ExternalResource.class.getSimpleName() + "{}, ignoring...", resourceName);
            } else {
                any.add(resource);
            }
        }

        requiredValuesMissing = checkMandatoryOnResources(any, getAllResources(any));
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    protected void fillTO(final AnyTO anyTO,
            final String realmFullPath,
            final Collection<? extends AnyTypeClass> auxClasses,
            final Collection<? extends PlainAttr<?>> attrs,
            final Collection<? extends DerAttr<?>> derAttrs,
            final Map<VirSchema, List<String>> virAttrs,
            final Collection<? extends ExternalResource> resources) {

        anyTO.setRealm(realmFullPath);

        CollectionUtils.collect(auxClasses, new Transformer<AnyTypeClass, String>() {

            @Override
            public String transform(final AnyTypeClass role) {
                return role.getKey();
            }
        }, anyTO.getAuxClasses());

        for (PlainAttr<?> attr : attrs) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(attr.getSchema().getKey());
            attrTO.getValues().addAll(attr.getValuesAsStrings());
            attrTO.setReadonly(attr.getSchema().isReadonly());

            anyTO.getPlainAttrs().add(attrTO);
        }

        for (DerAttr<?> derAttr : derAttrs) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(derAttr.getSchema().getKey());
            attrTO.getValues().add(derAttr.getValue(attrs));
            attrTO.setReadonly(true);

            anyTO.getDerAttrs().add(attrTO);
        }

        for (Map.Entry<VirSchema, List<String>> entry : virAttrs.entrySet()) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(entry.getKey().getKey());
            attrTO.getValues().addAll(entry.getValue());
            attrTO.setReadonly(entry.getKey().isReadonly());

            anyTO.getVirAttrs().add(attrTO);
        }

        for (ExternalResource resource : resources) {
            anyTO.getResources().add(resource.getKey());
        }
    }

    protected RelationshipTO getRelationshipTO(final Relationship<? extends Any<?, ?>, AnyObject> relationship) {
        return new RelationshipTO.Builder().
                left(relationship.getLeftEnd().getType().getKey(), relationship.getLeftEnd().getKey()).
                right(relationship.getRightEnd().getType().getKey(), relationship.getRightEnd().getKey()).
                build();
    }

    protected MembershipTO getMembershipTO(final Membership<? extends Any<?, ?>> membership) {
        return new MembershipTO.Builder().
                left(membership.getLeftEnd().getType().getKey(), membership.getLeftEnd().getKey()).
                group(membership.getRightEnd().getKey(), membership.getRightEnd().getName()).
                build();
    }

    protected Map<String, String> getConnObjectKeys(final Any<?, ?> any) {
        Map<String, String> connObjectKeys = new HashMap<>();

        Iterable<? extends ExternalResource> iterable = any instanceof User
                ? userDAO.findAllResources((User) any)
                : any instanceof AnyObject
                        ? anyObjectDAO.findAllResources((AnyObject) any)
                        : ((Group) any).getResources();
        for (ExternalResource resource : iterable) {
            Provision provision = resource.getProvision(any.getType());
            if (provision != null && provision.getMapping() != null) {
                MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
                if (connObjectKeyItem == null) {
                    throw new NotFoundException(
                            "ConnObjectKey mapping for " + any.getType().getKey() + " " + any.getKey()
                            + " on resource '" + resource.getKey() + "'");
                }

                String connObjectKey = mappingUtils.getConnObjectKeyValue(any, provision);
                if (connObjectKey != null) {
                    connObjectKeys.put(resource.getKey(), connObjectKey);
                }
            }
        }

        return connObjectKeys;
    }
}
