/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.getFdnDepth;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.isRootMo;
import com.ericsson.oss.services.cm.cmshared.dto.AutoAttributeList;
import com.ericsson.oss.services.cm.cmshared.dto.CmObjectSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.CmOutputSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.StringifiedAttributeSpecifications;
import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecificationContainer;
import com.ericsson.oss.services.cm.cmshared.dto.builders.CmSearchCriteriaBuilder;
import com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition;
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria;
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchScope;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiError;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ExceptionFactory;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType;
import com.ericsson.oss.services.cmnbicrud.ejb.impl.executor.xpath.AttributeXpathParser;
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.extractTypeFromFdn;

/**
 * Created by enmadmin on 9/2/21.
 */
public class CmGetRequestParser {
    private static final String OR_CLAUSE = "|";
    private static final String SLASH_SEPARATOR = "/";
    private static final String RDN_SEPARATOR = ",";
    private static final String DOT = ".";

    @Inject
    ExceptionFactory exceptionFactory;

    @Inject
    AttributeXpathParser attributeXpathParser;

    private static final Logger logger = LoggerFactory.getLogger(CmGetRequestParser.class);

    /**
     *  Create CmSearchCriteria
     */

    public CmSearchCriteria createCmSearchCriteria(CmGetRequest cmGetRequest) {
            String filter = cmGetRequest.getFilter();
            checkIfValidFilter(filter);

            ArrayList<CmObjectSpecification> cmObjectSpecifications = new ArrayList<>();
            cmObjectSpecifications.add(constructCmObjectSpecification(filter, cmGetRequest.getFdn()));

            CmSearchScope cmSearchScope = isRootMo(cmGetRequest.getFdn()) ? getUnspecifiedCmSearchScope() : getCmSearchScope(cmGetRequest.getFdn());
            CmSearchCriteria cmSearchCriteria = new CmSearchCriteriaBuilder().withCmSearchScopes(cmSearchScope).build();
            cmSearchCriteria.setCmObjectSpecifications(cmObjectSpecifications);

            return cmSearchCriteria;
        }

        private CmSearchScope getCmSearchScope(final String fdn) {
        CmSearchScope cmSearchScope = new CmSearchScope();
          if (fdn != null) {
              cmSearchScope.setScopeType(CmSearchScope.ScopeType.FDN);
              cmSearchScope.setValue(fdn);
              cmSearchScope.setCmMatchCondition(null);
          }
            return  cmSearchScope;
        }

     private void checkIfValidFilter(final String filter) {
         checkIfFilterContainsOrClause(filter);
         checkIfFilterHasTooManySlash(filter);
     }

     private CmSearchScope getUnspecifiedCmSearchScope() {
         CmSearchScope cmSearchScope = new CmSearchScope();
         cmSearchScope.setScopeType(CmSearchScope.ScopeType.UNSPECIFIED);
         cmSearchScope.setValue(null);
         cmSearchScope.setCmMatchCondition(null);
         return  cmSearchScope;
     }

        private String getMoAttributes(String mo, Map<String, String> attributes) {
            return attributes.get(mo);
        }
        
        private CmObjectSpecification getChild(ArrayList<String> mos, int depth, Map<String, String> attributesMap) {
            if (depth >= mos.size()) {
                return null;
            }

            final String mo = mos.get(depth);
            CmObjectSpecification cmObjectSpecification = new CmObjectSpecification();
            cmObjectSpecification.setType(mo);

            // NO Attributes
            cmObjectSpecification.setAttributeSpecificationContainer(attributeXpathParser.parseAttribute(getMoAttributes(mo, attributesMap)));

            depth++;
            CmObjectSpecification child = getChild(mos, depth, attributesMap);
            if (child!=null) {
                cmObjectSpecification.getChildCmObjectSpecifications().add(child);
            }

            return cmObjectSpecification;
        }

    @SuppressWarnings({"squid:S135"})
    private String fillAttributesMap(String filter, Map<String, String> attributes) {
        StringBuilder inside = new StringBuilder();
        boolean isInside = false;
        char[] chars = filter.toCharArray();

        StringBuilder newFilter = new StringBuilder();

        for (char c : chars) {
            if (c == '[') {
                isInside = true;
                inside.append(c);
                continue;
            }
            if (c == ']') {
                isInside = false;
                inside.append(c);

                String[] tokens = newFilter.toString().split(SLASH_SEPARATOR);
                String mo = tokens[tokens.length-1];
                attributes.put(mo, inside.toString());
                continue;
            }

            if (isInside) {
                inside.append(c);
            } else {
                newFilter.append(c);
            }
        }

        return newFilter.toString();
    }

     private CmObjectSpecification constructCmObjectSpecification(final String filter, final String fdn) {
            int depth = 0 ;
            HashMap<String, String> attributesMap = new HashMap<>();
            String filterWithoutAttributes = fillAttributesMap(filter, attributesMap);
            return getChild(getSimpleMos(filterWithoutAttributes, fdn), depth, attributesMap);
        }

    @SuppressWarnings({"squid:S1449"})
    private Set<String> getLowerCaseMosInBaseFdn(final String fdn) {
        Set<String> mosInBaseFdn = new LinkedHashSet<>();

        if (isRootMo(fdn)) {
            return  mosInBaseFdn;
        }

        String[] tokens = fdn.split(RDN_SEPARATOR);

        for (String token:tokens) {
            String type = extractTypeFromFdn(token);
            mosInBaseFdn.add(type.toLowerCase());
        }

        return mosInBaseFdn;
    }

    @SuppressWarnings({"squid:S1449"})
    private ArrayList<String> getSimpleMos(final String filter, final String fdn) {
        //here we remove Mos that are contained inside fdn
        //with:
        // fdn=SubNetwork=1,MeContext=ERBS002
        // filter=SubNetwork,MeContext,ManagedElement,ENodeBFunction
        // only ManagedElement,ENodeBFunction are maintained

        //split fdn in Mos
        Set<String> lowerCaseMosInBaseFdn = getLowerCaseMosInBaseFdn(fdn);

        //clean tokens with multiple /
        String[] mos = filter.split(SLASH_SEPARATOR);
        ArrayList<String> nonNullMos = new ArrayList<>();
        for (String mo:mos) {
                if (mo!=null && !mo.isEmpty() && !lowerCaseMosInBaseFdn.contains(mo.toLowerCase())) {
                        nonNullMos.add(mo);
                }
            }

        if (nonNullMos.isEmpty()) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_FILTER_FOR_FDN, filter, fdn);
        }

        return nonNullMos;
    }

    /**
     *  Create CmOutputSpecifications
     */


    public final List<CmOutputSpecification> createCmOutputSpecifications(CmSearchCriteria cmSearchCriteria, CmGetRequest cmGetRequest) {
        String filter = cmGetRequest.getFilter();
        List<CmOutputSpecification> cmOutputSpecifications = new ArrayList<>();
         cmOutputSpecifications.addAll(constructCmOutputSpecifications(cmSearchCriteria, filter, cmGetRequest));
        return cmOutputSpecifications;
    }

    private List<CmOutputSpecification> constructCmOutputSpecifications(CmSearchCriteria cmSearchCriteria, final String filter, CmGetRequest cmGetRequest) {
        List<CmOutputSpecification> cmOutputSpecifications = new ArrayList<>();

        HashMap<String, String> attributesMap = new HashMap<>();
        String filterWithoutAttributes = fillAttributesMap(filter, attributesMap);

       ArrayList<String> mos = getSimpleMos(filterWithoutAttributes, cmGetRequest.getFdn());

       if (!mos.isEmpty()) {
           final String leafMo = mos.get(mos.size()-1);
           CmOutputSpecification cmOutputSpecification = new CmOutputSpecification();
           if (cmGetRequest.hasNullAttributes()) {
                   cmOutputSpecification.setType(leafMo);
                   cmOutputSpecification.setAttributeNames(AutoAttributeList.PERSISTED_ATTRIBUTES);
                   cmOutputSpecifications.add(cmOutputSpecification);
           } else if (cmGetRequest.hasEmptyAttributes()) {
                   cmOutputSpecification.setType(leafMo);
                   cmOutputSpecification.setAttributeNames(new ArrayList<String>());
                   cmOutputSpecifications.add(cmOutputSpecification);
           } else {
                   //here only when  hasAttributes() and !hasEmptyAttributes()
                   cmOutputSpecification.setType(leafMo);
                   cmOutputSpecification.setAttributeNames(getCmOutputSpecificationAttributeNames(cmGetRequest));
                   cmOutputSpecifications.add(cmOutputSpecification);

                   //this method add output specification attributes to leaf cmObjectSpecification so that validation could be performed
                    addAttributesToCmObjectSpecificationIfPossible(leafMo, cmSearchCriteria, cmGetRequest);
           }
       }
        return  cmOutputSpecifications;
    }

    private CmObjectSpecification getFirstLastChild(CmObjectSpecification cmObjectSpecification) {
        if (cmObjectSpecification.getChildCmObjectSpecifications().isEmpty()) {
            return cmObjectSpecification;
        } else {
            return getFirstLastChild(cmObjectSpecification.getChildCmObjectSpecifications().iterator().next());
        }
    }

    private void addAttributesToCmObjectSpecificationIfPossible(final String leafMo, final CmSearchCriteria cmSearchCriteria,
                                                               CmGetRequest cmGetRequest) {
        //recover last child
        CmObjectSpecification leafCmObjectSpecification = getFirstLastChild(cmSearchCriteria.getCmObjectSpecifications().iterator().next());

        if (leafCmObjectSpecification.getType().equals(leafMo)) {
            final AttributeSpecificationContainer attributeSpecificationContainer =  leafCmObjectSpecification.getAttributeSpecificationContainer();
            for (Map.Entry<String,Set<String>> entry : cmGetRequest.getAttributes().entrySet()) {
                String attributeName = entry.getKey();
                Set<String> members = entry.getValue();

                if (members.isEmpty()) {
                    final AttributeSpecification attributeSpecification = new AttributeSpecification();
                    attributeSpecification.setName(attributeName);
                    attributeSpecification.setCmMatchCondition(CmMatchCondition.NO_MATCH_REQUIRED);
                    attributeSpecificationContainer.addExtendedAttributeSpecification(attributeSpecification);

                } else {
                    final AttributeSpecification attributeSpecification = new AttributeSpecification();
                    attributeSpecification.setName(attributeName);
                    attributeSpecification.setCmMatchCondition(CmMatchCondition.COMPLEX_MATCH_REQUIRED);

                    final StringifiedAttributeSpecifications attrSpecContainer = new StringifiedAttributeSpecifications();
                    for (final String member:members) {
                        final AttributeSpecification memberSpec = new AttributeSpecification();
                        memberSpec.setName(member);
                        memberSpec.setCmMatchCondition(CmMatchCondition.NO_MATCH_REQUIRED);
                        attrSpecContainer.addExtendedAttributeSpecification(memberSpec);

                    }
                    attributeSpecification.setValue(attrSpecContainer);
                    attributeSpecificationContainer.addExtendedAttributeSpecification(attributeSpecification);
                }
            }

        }
    }

    //return request Mo Type
    public String getRequestMoType(final CmSearchCriteria cmSearchCriteria) {
        //recover last child
        CmObjectSpecification leafCmObjectSpecification = getFirstLastChild(cmSearchCriteria.getCmObjectSpecifications().iterator().next());
        return leafCmObjectSpecification.getType();
    }

    private List<String> getCmOutputSpecificationAttributeNames(CmGetRequest cmGetRequest) {
        List<String> attributeNames =  new ArrayList<>();
        for (Map.Entry<String,Set<String>> entry : cmGetRequest.getAttributes().entrySet()) {
            String attributeName = entry.getKey();
            Set<String> members = entry.getValue();
            if (members.isEmpty()) {
                attributeNames.add(attributeName);
            } else {
                // syntax complex attribute is "attributeName.member"
                for (final String member:members) {
                    attributeNames.add(attributeName + DOT + member);
                }
            }
         }
        return attributeNames;
    }

    private void checkIfFilterContainsOrClause(final String filter) {
        if (filter.contains(OR_CLAUSE)) {
            throw exceptionFactory.createValidationException(CmRestNbiError.OR_CLAUSE_IN_FILTER, filter);
        }
    }

    private void checkIfFilterHasTooManySlash(final String filter) {
        String filterWithoutAttributes = getFilterWithoutAttributes(filter);
       if (filterWithoutAttributesHasTooManySlash(filterWithoutAttributes)) {
           throw exceptionFactory.createValidationException(CmRestNbiError.TOO_MANY_SLASH_IN_FILTER, filterWithoutAttributes);
       }
    }

    private boolean filterWithoutAttributesHasTooManySlash(final String filterWithoutAttributes) {
        if (filterWithoutAttributes == null ) return false;

        char[] chars = filterWithoutAttributes.toCharArray();

        int num = 0;
        for (char c : chars) {
            if (c == '/') {
                num++;
                if (num > 2) {
                    return true;
                }
             } else {
                num = 0;
            }
        }

        return false;
    }

    @SuppressWarnings({"squid:S135"})
    public void fiilterCmResponseIfNecessary(CmGetRequest cmGetRequest, CmResponse cmResponse) {
        if (isSuccessful(cmResponse)) {
            long startTime = System.currentTimeMillis();
            String regexPattern = createRegexForVerifyingFdnContainsAllSpecifiedParentsInCorrectOrder(cmGetRequest.getFilter());

            List<CmObject> filteredCmObjectList = new ArrayList<>();
            Iterator<CmObject> it = cmResponse.getCmObjects().iterator();
            while (it.hasNext()) {
                final CmObject cmObject = it.next();

                if (!isFdnValidAgainstRegexPattern(cmObject.getFdn(), regexPattern)) {
                    logger.debug("filterCmResponseIfNecessary:: fdn={} DISCARDED for regexPattern={}" , cmObject.getFdn(), regexPattern);
                    continue;
                } else if (!isFdnValidAgainstScopeAndScopeLevel(cmObject.getFdn(), cmGetRequest)) {
                    logger.debug("filterCmResponseIfNecessary:: fdn={} DISCARDED for scopeLevel" , cmObject.getFdn());
                    continue;
                }

                logger.debug("filterCmResponseIfNecessary:: fdn={} ADDED", cmObject.getFdn());
                filteredCmObjectList.add(cmObject);
            }

            long delta = System.currentTimeMillis() - startTime;
            logger.debug("filterCmResponseIfNecessary:: [TIME={}]", delta);
            cmResponse.setTargetedCmObjects(filteredCmObjectList);
        }
    }

    private boolean isSuccessful(CmResponse cmResponse) {
        return cmResponse.getErrorCode() == 0;
    }

    private String getFilterWithoutAttributes(final String filter) {
        return fillAttributesMap(filter, new HashMap<>());
    }

    public String createRegexForVerifyingFdnContainsAllSpecifiedParentsInCorrectOrder(final String filter) {
        String filterWithoutAttributes = getFilterWithoutAttributes(filter);
        return ParentChildRegexProducer.createRegexForVerifyingFdnContainsAllSpecifiedParentsInCorrectOrder(filterWithoutAttributes);
    }

    //used to implement check on / or // relationship
    public boolean isFdnValidAgainstRegexPattern(final String fdn, final String regexPattern) {
        return ParentChildRegexProducer.isFdnValidAgainstRegexPattern(fdn, regexPattern);
    }

    //used to implement BASE_SUBTREE and BASE_NTH_LEVEL
    public boolean isFdnValidAgainstScopeAndScopeLevel(final String fdn, CmGetRequest cmGetRequest) {
      if (cmGetRequest.getScopeType() == ScopeType.BASE_SUBTREE) {
          int requestFdnDepth = getFdnDepth(cmGetRequest.getFdn());
          int fdnDepth = getFdnDepth(fdn);
          return (fdnDepth <= (requestFdnDepth + cmGetRequest.getScopeLevel()));
        }
        else if (cmGetRequest.getScopeType() == ScopeType.BASE_NTH_LEVEL) {
          int requestFdnDepth = getFdnDepth(cmGetRequest.getFdn());
          int fdnDepth = getFdnDepth(fdn);
          return (fdnDepth == (requestFdnDepth + cmGetRequest.getScopeLevel()));
        }

        return true;
    }

}
