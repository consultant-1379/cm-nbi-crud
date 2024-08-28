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

import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.extractNameFromFdn;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.extractTypeFromFdn;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.purgedFdn;

import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;
import com.ericsson.oss.services.cmnbicrud.spi.output.ResourceRepresentationType;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by enmadmin on 9/2/21.
 */
public class Node {

    private static final String RDN_SEPARATOR = ",";
    public static final int INFINITE_LEVELS = 1000000;
    private int scopeLevel = INFINITE_LEVELS;

    String fdn;
    Map<String, Node> subnodes;
    CmObject cmobject;

    private Node() {
    }


    /*
    *         Node Hierarchy Creation
    *
    * */

    public static Node createNodeHierarchy(Collection<CmObject> cmObjectPlusAllDescendants, String fdnToBeRemoved) {
        Node node = Node.createRootNode(Node.INFINITE_LEVELS); //we never remove elements now
        Iterator<CmObject> it = cmObjectPlusAllDescendants.iterator();
        while (it.hasNext()) {
            final CmObject cmObject = it.next();
            cmObject.setFdn(purgedFdn(cmObject.getFdn(), fdnToBeRemoved));
            node.insertCmObject(node,0, cmObject);
        }
        return node;
    }


    private static Node createRootNode(int scopeLevel) {
        Node node = new Node();
        node.fdn ="";
        node.scopeLevel = scopeLevel;
        return node;
    }

    private String getFdn(String[] rdns, int depth) {
        StringBuilder fdnSb = new StringBuilder();
        for (int i=0 ; i<=depth ; i++) {
            if (fdnSb.length() == 0) {
                fdnSb.append(rdns[i]);
            } else {
                fdnSb.append(RDN_SEPARATOR + rdns[i]);
            }
        }
        return fdnSb.toString();
    }

    private void insertCmObject(Node node, int depth, CmObject cmObject) {
        final String[] rdns = cmObject.getFdn().split(RDN_SEPARATOR);

        //END RECURSION
        if (depth == rdns.length) {
            node.cmobject = cmObject;
            return;
        }
        if (depth == (scopeLevel + 1)) {
            return;
        }


        String rdn = rdns[depth];
        if (node.subnodes == null) {
            node.subnodes = new HashMap<>();
        }

        //manage subnode
        Node subNode = null;
        if (!node.subnodes.containsKey(rdn)) {
            subNode = new Node();
            subNode.fdn = getFdn(rdns, depth);
        } else {
            subNode = node.subnodes.get(rdn);
        }
        node.subnodes.put(rdn, subNode);

        depth++;
        insertCmObject(subNode, depth, cmObject);
    }


    /*
    *         MoObjects Conversion
    *
    * */

    // create empty MoObjects
    public static MoObjects convertInEmptyMoObjects() {
        return new MoObjects();
    }

    // create MoObjects with one element inside
    public static MoObjects convertInMoObjects(CmObject cmObject) {
        MoObjects root = new MoObjects();
        ResourceRepresentationType resource = new ResourceRepresentationType();
        resource.setId(extractNameFromFdn(cmObject.getFdn()));
        resource.setAttributes(cmObject.getAttributes());
        List<ResourceRepresentationType> beans = new ArrayList<>();
        beans.add(resource);
        root.addMoObjects(extractTypeFromFdn(cmObject.getFdn()), beans);
        return root;
    }

    // create MoObjects with multiple elements inside
    public static MoObjects convertInMoObjects(Node node) {
        MoObjects root = new MoObjects();
        if (node.fdn == "" && node.subnodes!=null) {
            for (String key:node.subnodes.keySet()) {
                Node subnode = node.subnodes.get(key);

                //fill RootMoObject
                ResourceRepresentationType resource = new ResourceRepresentationType();
                resource.setId(extractNameFromFdn(subnode.fdn));
                if (subnode.cmobject!= null) {
                    Map<String, Object> attributes = subnode.cmobject.getAttributes();
                    if (attributes != null && !attributes.isEmpty()) {
                        resource.setAttributes(attributes);
                    }
                }

                String type = extractTypeFromFdn(subnode.fdn);
                root.initMoObjects();
                if (!root.getMoObjects().containsKey(type)) {
                    root.addMoObjects(type, new ArrayList<>());
                }
                root.getMoObjects().get(type).add(resource);

                manageSubnode(subnode, resource);
            }
        }
        return root;
    }

    private static void manageSubnode(Node node, ResourceRepresentationType resource) {
        if (node.subnodes!=null) {
            for (String key:node.subnodes.keySet()) {
                Node subnode = node.subnodes.get(key);

                ResourceRepresentationType subResource = new ResourceRepresentationType();
                subResource.setId(extractNameFromFdn(subnode.fdn));
                if (subnode.cmobject!= null) {
                    Map<String, Object> attributes = subnode.cmobject.getAttributes();
                    if (attributes != null && !attributes.isEmpty()) {
                        subResource.setAttributes(attributes);
                    }
                }

                String type = extractTypeFromFdn(subnode.fdn);
                resource.initAdditionalProperties();
                if (!resource.getAdditionalProperties().containsKey(type)) {
                    resource.addAdditionalProperty(type, new ArrayList<>());
                }
                resource.getAdditionalProperties().get(type).add(subResource);
                manageSubnode(subnode, subResource);
            }
        }
    }

}
