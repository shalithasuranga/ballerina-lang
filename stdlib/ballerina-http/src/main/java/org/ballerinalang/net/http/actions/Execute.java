/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.net.http.actions;

import static org.wso2.transport.http.netty.common.Constants.ACCEPT_ENCODING;
import static org.wso2.transport.http.netty.common.Constants.ENCODING_DEFLATE;
import static org.wso2.transport.http.netty.common.Constants.ENCODING_GZIP;

import java.util.Locale;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BConnector;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaAction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.transport.http.netty.contract.ClientConnectorException;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;

/**
 * {@code Execute} action can be used to invoke execute a http call with any httpVerb.
 */
@BallerinaAction(
        packageName = "ballerina.net.http",
        actionName = "execute",
        connectorName = HttpConstants.CONNECTOR_NAME,
        args = {
                @Argument(name = "c", type = TypeKind.CONNECTOR),
                @Argument(name = "httpVerb", type = TypeKind.STRING),
                @Argument(name = "path", type = TypeKind.STRING),
                @Argument(name = "req", type = TypeKind.STRUCT, structType = "OutRequest",
                        structPackage = "ballerina.net.http")
        },
        returnType = {
                @ReturnType(type = TypeKind.STRUCT, structType = "InResponse", structPackage = "ballerina.net.http"),
                @ReturnType(type = TypeKind.STRUCT, structType = "HttpConnectorError",
                        structPackage = "ballerina.net.http"),
        },
        connectorArgs = {
                @Argument(name = "serviceUri", type = TypeKind.STRING),
                @Argument(name = "options", type = TypeKind.STRUCT, structType = "Options",
                          structPackage = "ballerina.net.http")
        }
)
public class Execute extends AbstractHTTPAction {

    @Override
    public void execute(Context context, CallableUnitCallback callback) {
        try {
            // Execute the operation
            executeNonBlockingAction(context, createOutboundRequestMsg(context), callback);
        } catch (ClientConnectorException clientConnectorException) {
            throw new BallerinaException("Failed to invoke 'execute' action in " + HttpConstants.CONNECTOR_NAME
                    + ". " + clientConnectorException.getMessage(), context);
        }
    }

    protected HTTPCarbonMessage createOutboundRequestMsg(Context context) {
        // Extract Argument values
        BConnector bConnector = (BConnector) context.getRefArgument(0);
        String httpVerb = context.getStringArgument(0);
        String path = context.getStringArgument(1);
        BStruct requestStruct = ((BStruct) context.getRefArgument(1));
        //TODO check below line
        HTTPCarbonMessage defaultCarbonMsg = HttpUtil.createHttpCarbonMessage(true);
        HTTPCarbonMessage outboundRequestMsg = HttpUtil.getCarbonMsg(requestStruct, defaultCarbonMsg);
        prepareOutboundRequest(bConnector, path, outboundRequestMsg);

        // If the verb is not specified, use the verb in incoming message
        if (httpVerb == null || "".equals(httpVerb)) {
            httpVerb = (String) outboundRequestMsg.getProperty(HttpConstants.HTTP_METHOD);
        }
        outboundRequestMsg.setProperty(HttpConstants.HTTP_METHOD, httpVerb.trim().toUpperCase(Locale.getDefault()));
        if (outboundRequestMsg.getHeader(ACCEPT_ENCODING) == null) {
            outboundRequestMsg.setHeader(ACCEPT_ENCODING, ENCODING_DEFLATE + ", " + ENCODING_GZIP);
        }
        return outboundRequestMsg;
    }
}
