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
package org.apache.syncope.core.persistence.javers.dao;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ChangesByCommitTO;
import org.apache.syncope.common.lib.to.ChangesTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropertyChangeTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.ShadowTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.javers.client.util.JaversDomainLocator;
import org.javers.core.Javers;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.diff.changetype.PropertyChangeType;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.CollectionChange;
import org.javers.repository.jql.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class JaversAuditEventDAOImpl implements JaversAuditEventDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(JaversAuditEventDAOImpl.class);

    private final JaversDomainLocator javersDomainLocator;

    public JaversAuditEventDAOImpl(final JaversDomainLocator javersDomainLocator) {
        this.javersDomainLocator = javersDomainLocator;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends AnyTO> Page<ShadowTO<T>> searchForShadows(
            final String entityKey,
            final OffsetDateTime from,
            final OffsetDateTime to,
            final Pageable pageable,
            final Class<T> clazz) {
        Javers javers = javersDomainLocator.getBean(AuthContextUtils.getDomain(), Javers.class);

        List<ShadowTO<T>> result = javers.findShadowsAndStream(QueryBuilder.byInstanceId(entityKey, clazz)
                        .limit(pageable.getPageSize())
                        .skip(pageable.getPageNumber() * pageable.getPageSize())
                        .build())
                .map(shadow -> new ShadowTO.Builder<T>(shadow.getCdoSnapshot().getGlobalId().value()).version(
                                shadow.getCdoSnapshot().getVersion())
                        .type(shadow.getCdoSnapshot().getType().name())
                        .anyTO((T) shadow.get())
                        .when(Optional.ofNullable(shadow.getCommitMetadata().getCommitDate())
                                .map(cd -> cd.atZone(ZoneId.systemDefault()).toOffsetDateTime())
                                .orElse(null))
                        .who(shadow.getCommitMetadata().getAuthor())
                        .additionalInfo(shadow.getCommitMetadata().getProperties())
                        .build())
                .toList();
        return new SyncopePage<>(result, pageable, result.size());
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends AnyTO> List<ChangesByCommitTO> searchForChanges(
            final String entityKey,
            final String author,
            final OffsetDateTime from,
            final OffsetDateTime to,
            final Pageable pageable,
            final Class<T> clazz) {
        Javers javers = javersDomainLocator.getBean(AuthContextUtils.getDomain(), Javers.class);

        QueryBuilder queryBuilder = StringUtils.isNotBlank(entityKey)
                ? QueryBuilder.byInstanceId(entityKey, clazz)
                : QueryBuilder.byClass(clazz);
        if (from != null) {
            queryBuilder.from(from.toLocalDate());
        }
        if (to != null) {
            queryBuilder.to(to.toLocalDate());
        }
        if (StringUtils.isNotBlank(author)) {
            queryBuilder.byAuthor(author);
        }
        return javers.findChanges(queryBuilder.limit(pageable.getPageSize())
                        .skip(pageable.getPageNumber() * pageable.getPageSize())
                        .build())
                .groupByCommit()
                .stream()
                .map(changes -> new ChangesByCommitTO.Builder(changes.getCommit().getId().value()).changes(
                        new ChangesTO.Builder(changes.getCommit().getId().value()).when(
                                        Optional.ofNullable(changes.getCommit().getCommitDate())
                                                .map(cd -> cd.atZone(ZoneId.systemDefault()).toOffsetDateTime())
                                                .orElse(null))
                                .who(changes.getCommit().getAuthor())
                                .additionalInfo(changes.getCommit().getProperties())
                                .valueChanges(changes.get()
                                        .stream()
                                        .filter(change -> change instanceof PropertyChange<?>)
                                        .flatMap(change -> processChange(javers, change, clazz).stream())
                                        .toList())
                                .build()).build())
                .toList();
    }

    protected <T extends AnyTO> List<PropertyChangeTO> processChange(
            final Javers javers,
            final Change change,
            final Class<T> clazz) {
        List<PropertyChangeTO> propertyChangeTOs = new ArrayList<>();

        if (change instanceof PropertyChange<?>) {
            PropertyChange propertyChange = (PropertyChange) change;

            Optional.ofNullable(propertyChange.getLeft()).ifPresent(left -> {
                PropertyChangeTO propertyChangeTO = new PropertyChangeTO();
                propertyChangeTO.setEntityKey(propertyChange.getAffectedGlobalId().value());
                propertyChangeTO.setChangeType(propertyChange.getChangeType().name());
                propertyChangeTO.setField(propertyChange.getPropertyName());
                if (propertyChange instanceof ValueChange) {
                    propertyChangeTO.getOldValues().add(Optional.of(left).map(Object::toString).orElse(null));
                    if (PropertyChangeType.PROPERTY_ADDED == propertyChange.getChangeType()
                            || PropertyChangeType.PROPERTY_VALUE_CHANGED == propertyChange.getChangeType()) {
                        propertyChangeTO.getNewValues()
                                .add(Optional.ofNullable(propertyChange.getRight()).map(Object::toString).orElse(null));
                    }
                    propertyChangeTOs.add(propertyChangeTO);
                } else if (propertyChange instanceof CollectionChange<?>) {
                    switch (propertyChange.getPropertyName()) {
                        case "memberships":
                            List<MembershipTO> oldMembs = ((CollectionChange<?>) propertyChange).getLeft()
                                    .stream()
                                    .map(MembershipTO.class::cast)
                                    .toList();
                            List<MembershipTO> newMembs = ((CollectionChange<?>) propertyChange).getRight()
                                    .stream()
                                    .map(MembershipTO.class::cast)
                                    .toList();

                            propertyChangeTO.getOldValues().addAll(oldMembs.stream().map(Object::toString).toList());
                            if (PropertyChangeType.PROPERTY_ADDED == propertyChange.getChangeType()
                                    || PropertyChangeType.PROPERTY_VALUE_CHANGED == propertyChange.getChangeType()) {
                                propertyChangeTO.getNewValues()
                                        .addAll(((CollectionChange<?>) propertyChange).getRight()
                                                .stream()
                                                .map(Object::toString)
                                                .toList());
                            }
                            if (!propertyChangeTO.isEmpty()) {
                                propertyChangeTOs.add(propertyChangeTO);
                            }

                            // also manage membership attributes
                            // first manage memberships added or updated
                            for (MembershipTO newMemb : newMembs) {
                                // if present both in old and new memberships it's an update
                                oldMembs.stream()
                                        .filter(om -> om.getGroupKey().equals(newMemb.getGroupKey()))
                                        .findFirst()
                                        .ifPresentOrElse(oldMemb -> javers.compareCollections(oldMemb.getPlainAttrs(),
                                                                newMemb.getPlainAttrs(), Attr.class)
                                                        .getChangesByType(PropertyChange.class)
                                                        .forEach(pc -> plainAttrsDiff((CollectionChange<?>) pc,
                                                                propertyChangeTOs,
                                                                propertyChange.getAffectedGlobalId().value(),
                                                                "memberships[" + newMemb.getGroupName() + "].")),
                                                () -> newMemb.getPlainAttrs().forEach(mpa -> {
                                                    PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
                                                    plainAttrChangeTO.setEntityKey(
                                                            propertyChange.getAffectedGlobalId().value());
                                                    plainAttrChangeTO.setChangeType(
                                                            PropertyChangeType.PROPERTY_ADDED.name());
                                                    plainAttrChangeTO.setField(
                                                            "memberships[" + newMemb.getGroupName() + "].plainAttrs["
                                                                    + mpa.getSchema() + "]");
                                                    plainAttrChangeTO.getNewValues().addAll(mpa.getValues());

                                                    propertyChangeTOs.add(plainAttrChangeTO);
                                                }));
                            }
                            // then manage memberships removed
                            oldMembs.stream()
                                    .filter(oldMemb -> newMembs.stream()
                                            .noneMatch(newMemb -> newMemb.getGroupKey().equals(oldMemb.getGroupKey())))
                                    .forEach(oldMemb -> oldMemb.getPlainAttrs().forEach(mpa -> {
                                        PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
                                        plainAttrChangeTO.setEntityKey(propertyChange.getAffectedGlobalId().value());
                                        plainAttrChangeTO.setChangeType(PropertyChangeType.PROPERTY_REMOVED.name());
                                        plainAttrChangeTO.setField(
                                                "memberships[" + oldMemb.getGroupName() + "].plainAttrs["
                                                        + mpa.getSchema() + "]");
                                        plainAttrChangeTO.getNewValues().addAll(mpa.getValues());

                                        propertyChangeTOs.add(plainAttrChangeTO);
                                    }));
                            break;
                        case "relationships":
                            List<RelationshipTO> oldRels = ((CollectionChange<?>) propertyChange).getLeft()
                                    .stream()
                                    .map(RelationshipTO.class::cast)
                                    .toList();
                            List<RelationshipTO> newRels = ((CollectionChange<?>) propertyChange).getRight()
                                    .stream()
                                    .map(RelationshipTO.class::cast)
                                    .toList();
                            propertyChangeTO.getOldValues().addAll(oldRels.stream().map(Object::toString).toList());
                            if (PropertyChangeType.PROPERTY_ADDED == propertyChange.getChangeType()
                                    || PropertyChangeType.PROPERTY_VALUE_CHANGED == propertyChange.getChangeType()) {
                                propertyChangeTO.getNewValues()
                                        .addAll(((CollectionChange<?>) propertyChange).getRight()
                                                .stream()
                                                .map(Object::toString)
                                                .toList());
                            }
                            if (!propertyChangeTO.isEmpty()) {
                                propertyChangeTOs.add(propertyChangeTO);
                            }

                            // also manage relationship attributes
                            // first manage relationships added or updated
                            for (RelationshipTO newRel : newRels) {
                                // if present both in old and new relationships, it's an update
                                oldRels.stream()
                                        .filter(om -> om.getOtherEndKey().equals(newRel.getOtherEndKey()))
                                        .findFirst()
                                        .ifPresentOrElse(oldRel -> javers.compareCollections(oldRel.getPlainAttrs(),
                                                                newRel.getPlainAttrs(), Attr.class)
                                                        .getChangesByType(PropertyChange.class)
                                                        .forEach(pc -> plainAttrsDiff((CollectionChange<?>) pc,
                                                                propertyChangeTOs,
                                                                propertyChange.getAffectedGlobalId().value(),
                                                                "relationships[" + newRel.getOtherEndName() + "].")),
                                                () -> newRel.getPlainAttrs().forEach(mpa -> {
                                                    PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
                                                    plainAttrChangeTO.setEntityKey(
                                                            propertyChange.getAffectedGlobalId().value());
                                                    plainAttrChangeTO.setChangeType(
                                                            PropertyChangeType.PROPERTY_ADDED.name());
                                                    plainAttrChangeTO.setField(
                                                            "relationships[" + newRel.getOtherEndName()
                                                                    + "].plainAttrs[" + mpa.getSchema() + "]");
                                                    plainAttrChangeTO.getNewValues().addAll(mpa.getValues());

                                                    if (!plainAttrChangeTO.isEmpty()) {
                                                        propertyChangeTOs.add(plainAttrChangeTO);
                                                    }
                                                }));
                            }
                            // then manage relationships removed
                            oldRels.stream()
                                    .filter(oldRel -> newRels.stream()
                                            .noneMatch(newMemb -> newMemb.getOtherEndKey()
                                                    .equals(oldRel.getOtherEndKey())))
                                    .forEach(oldRel -> oldRel.getPlainAttrs().forEach(mpa -> {
                                        PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
                                        plainAttrChangeTO.setEntityKey(propertyChange.getAffectedGlobalId().value());
                                        plainAttrChangeTO.setChangeType(PropertyChangeType.PROPERTY_REMOVED.name());
                                        plainAttrChangeTO.setField(
                                                "relationships[" + oldRel.getOtherEndName() + "].plainAttrs["
                                                        + mpa.getSchema() + "]");
                                        plainAttrChangeTO.getNewValues().addAll(mpa.getValues());

                                        if (!plainAttrChangeTO.isEmpty()) {
                                            propertyChangeTOs.add(plainAttrChangeTO);
                                        }
                                    }));
                            break;
                        case "linkedAccounts":
                            List<LinkedAccountTO> oldLinkedAccounts = ((CollectionChange<?>) propertyChange).getLeft()
                                    .stream()
                                    .map(LinkedAccountTO.class::cast)
                                    .toList();
                            List<LinkedAccountTO> newLinkedAccounts = ((CollectionChange<?>) propertyChange).getRight()
                                    .stream()
                                    .map(LinkedAccountTO.class::cast)
                                    .toList();
                            propertyChangeTO.getOldValues()
                                    .addAll(oldLinkedAccounts.stream()
                                            .map(la -> "linkedAccounts[" + la.getConnObjectKeyValue() + ","
                                                    + la.getResource() + "]")
                                            .toList());
                            if (PropertyChangeType.PROPERTY_ADDED == propertyChange.getChangeType()
                                    || PropertyChangeType.PROPERTY_VALUE_CHANGED == propertyChange.getChangeType()) {
                                propertyChangeTO.getNewValues()
                                        .addAll(newLinkedAccounts.stream()
                                                .map(la -> "linkedAccounts[" + la.getConnObjectKeyValue() + ","
                                                        + la.getResource() + "]")
                                                .toList());
                            }
                            if (!propertyChangeTO.isEmpty()) {
                                propertyChangeTOs.add(propertyChangeTO);
                            }

                            // also manage linked accounts attributes
                            // first manage linked accounts added or updated
                            for (LinkedAccountTO newLinkedAccount : newLinkedAccounts) {
                                // if present both in old and new relationships, it's an update
                                oldLinkedAccounts.stream()
                                        .filter(ola -> ola.getConnObjectKeyValue()
                                                .equals(newLinkedAccount.getConnObjectKeyValue()))
                                        .findFirst()
                                        .ifPresentOrElse(oldRel -> javers.compareCollections(oldRel.getPlainAttrs(),
                                                                newLinkedAccount.getPlainAttrs(), Attr.class)
                                                        .getChangesByType(PropertyChange.class)
                                                        .forEach(pc -> plainAttrsDiff((CollectionChange<?>) pc,
                                                                propertyChangeTOs,
                                                                propertyChange.getAffectedGlobalId().value(),
                                                                "linkedAccounts[" 
                                                                        + newLinkedAccount.getConnObjectKeyValue()
                                                                        + "," + newLinkedAccount.getResource() + "].")),
                                                () -> newLinkedAccount.getPlainAttrs().forEach(mpa -> {
                                                    PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
                                                    plainAttrChangeTO.setEntityKey(
                                                            propertyChange.getAffectedGlobalId().value());
                                                    plainAttrChangeTO.setChangeType(
                                                            PropertyChangeType.PROPERTY_ADDED.name());
                                                    plainAttrChangeTO.setField(
                                                            "linkedAccounts[" + newLinkedAccount.getConnObjectKeyValue()
                                                                    + "," + newLinkedAccount.getResource()
                                                                    + "].plainAttrs[" + mpa.getSchema() + "]");
                                                    plainAttrChangeTO.getNewValues().addAll(mpa.getValues());

                                                    if (!plainAttrChangeTO.isEmpty()) {
                                                        propertyChangeTOs.add(plainAttrChangeTO);
                                                    }
                                                }));
                            }
                            // then manage linked accounts removed
                            oldLinkedAccounts.stream()
                                    .filter(oldLinkedAccount -> newLinkedAccounts.stream()
                                            .noneMatch(newLinkedAccount -> newLinkedAccount.getConnObjectKeyValue()
                                                    .equals(oldLinkedAccount.getConnObjectKeyValue())))
                                    .forEach(oldLinkedAccount -> oldLinkedAccount.getPlainAttrs().forEach(mpa -> {
                                        PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
                                        plainAttrChangeTO.setEntityKey(propertyChange.getAffectedGlobalId().value());
                                        plainAttrChangeTO.setChangeType(PropertyChangeType.PROPERTY_REMOVED.name());
                                        plainAttrChangeTO.setField(
                                                "linkedAccounts[" + oldLinkedAccount.getConnObjectKeyValue() + ","
                                                        + oldLinkedAccount.getResource() + "].plainAttrs["
                                                        + mpa.getSchema() + "]");
                                        plainAttrChangeTO.getNewValues().addAll(mpa.getValues());

                                        if (!plainAttrChangeTO.isEmpty()) {
                                            propertyChangeTOs.add(plainAttrChangeTO);
                                        }
                                    }));
                            break;
                        case "plainAttrs":
                            plainAttrsDiff((CollectionChange<?>) propertyChange, propertyChangeTOs,
                                    propertyChange.getAffectedGlobalId().value(), StringUtils.EMPTY);
                            break;
                        case "resources":
                        case "roles":
                        case "auxClasses":
                        case "delegatingDelegations":
                        case "delegatedDelegations":
                            propertyChangeTO.getNewValues()
                                    .addAll(((CollectionChange<?>) propertyChange).getRight()
                                            .stream()
                                            .map(Object::toString)
                                            .toList());
                            propertyChangeTO.getOldValues()
                                    .addAll(((CollectionChange<?>) propertyChange).getLeft()
                                            .stream()
                                            .map(Object::toString)
                                            .toList());
                            propertyChangeTOs.add(propertyChangeTO);
                            break;
                        // GroupTO properties
                        case "typeExtensions":
                            List<TypeExtensionTO> oldTypeExtensions = ((CollectionChange<?>) propertyChange).getLeft()
                                    .stream()
                                    .map(TypeExtensionTO.class::cast)
                                    .toList();
                            List<TypeExtensionTO> newTypeExtensions = ((CollectionChange<?>) propertyChange).getRight()
                                    .stream()
                                    .map(TypeExtensionTO.class::cast)
                                    .toList();
                            propertyChangeTO.getOldValues()
                                    .addAll(oldTypeExtensions.stream()
                                            .map(te -> "typeExtensions[" + te.getAnyType() + "].auxClasses["
                                                    + String.join(",", te.getAuxClasses()) + "]")
                                            .toList());
                            if (PropertyChangeType.PROPERTY_ADDED == propertyChange.getChangeType()
                                    || PropertyChangeType.PROPERTY_VALUE_CHANGED == propertyChange.getChangeType()) {
                                propertyChangeTO.getNewValues()
                                        .addAll(newTypeExtensions.stream()
                                                .map(te -> "typeExtensions[" + te.getAnyType() + "].auxClasses["
                                                        + String.join(",", te.getAuxClasses()) + "]")
                                                .toList());
                            }
                            if (!propertyChangeTO.isEmpty()) {
                                propertyChangeTOs.add(propertyChangeTO);
                            }
                            break;
                        default:
                            LOG.warn("Unexpected property for class [{}] [{}] unable to compute changes on it",
                                    clazz.getSimpleName(), propertyChange.getPropertyName());
                            break;
                    }
                }
            });

        } else {
            LOG.error("Unexpected change type: {}", change.getClass().getName());
            return null;
        }

        return propertyChangeTOs;
    }

    protected void plainAttrsDiff(
            final CollectionChange<?> propertyChange,
            final List<PropertyChangeTO> propertyChangeTOs,
            final String entityKey,
            final String prefix) {
        List<Attr> addedAttrs =
                propertyChange.getValueAddedChanges().stream().map(va -> (Attr) va.getAddedValue()).toList();
        List<Attr> removedAttrs =
                propertyChange.getValueRemovedChanges().stream().map(vr -> (Attr) vr.getRemovedValue()).toList();

        Map<String, Attr> addedBySchema =
                addedAttrs.stream().collect(Collectors.toMap(Attr::getSchema, Function.identity(), (a, b) -> a));

        Map<String, Attr> removedBySchema =
                removedAttrs.stream().collect(Collectors.toMap(Attr::getSchema, Function.identity(), (a, b) -> a));

        for (String schema : addedBySchema.keySet()) {
            if (removedBySchema.containsKey(schema)) {
                Attr oldAttr = removedBySchema.get(schema);
                Attr newAttr = addedBySchema.get(schema);

                PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
                plainAttrChangeTO.setEntityKey(entityKey);
                plainAttrChangeTO.setChangeType(PropertyChangeType.PROPERTY_VALUE_CHANGED.name());
                plainAttrChangeTO.setField(prefix + "plainAttrs[" + schema + "]");
                plainAttrChangeTO.getOldValues().addAll(oldAttr.getValues());
                plainAttrChangeTO.getNewValues().addAll(newAttr.getValues());

                propertyChangeTOs.add(plainAttrChangeTO);
            }
        }

        addedBySchema.keySet().stream().filter(s -> !removedBySchema.containsKey(s)).forEach(schema -> {
            PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
            plainAttrChangeTO.setEntityKey(entityKey);
            plainAttrChangeTO.setChangeType(PropertyChangeType.PROPERTY_ADDED.name());
            plainAttrChangeTO.setField(prefix + "plainAttrs[" + schema + "]");
            plainAttrChangeTO.getNewValues().addAll(addedBySchema.get(schema).getValues());

            propertyChangeTOs.add(plainAttrChangeTO);
        });

        removedBySchema.keySet().stream().filter(s -> !addedBySchema.containsKey(s)).forEach(schema -> {
            PropertyChangeTO plainAttrChangeTO = new PropertyChangeTO();
            plainAttrChangeTO.setEntityKey(entityKey);
            plainAttrChangeTO.setChangeType(PropertyChangeType.PROPERTY_REMOVED.name());
            plainAttrChangeTO.setField(prefix + "plainAttrs[" + schema + "]");
            plainAttrChangeTO.getOldValues().addAll(removedBySchema.get(schema).getValues());

            propertyChangeTOs.add(plainAttrChangeTO);
        });
    }

}
