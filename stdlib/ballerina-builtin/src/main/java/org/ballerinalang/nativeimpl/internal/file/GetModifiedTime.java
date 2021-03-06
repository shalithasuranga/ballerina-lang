/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.ballerinalang.nativeimpl.internal.file;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMErrors;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.nativeimpl.Utils;
import org.ballerinalang.nativeimpl.internal.Constants;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;

import static org.ballerinalang.nativeimpl.Utils.PACKAGE_TIME;
import static org.ballerinalang.nativeimpl.Utils.STRUCT_TYPE_TIME;
import static org.ballerinalang.nativeimpl.Utils.getTimeStructInfo;
import static org.ballerinalang.nativeimpl.Utils.getTimeZoneStructInfo;

/**
 * Retrieves the last modified time of the specified file.
 *
 * @since 0.970.0-alpha4
 */
@BallerinaFunction(
        orgName = Constants.ORG_NAME,
        packageName = Constants.PACKAGE_NAME,
        functionName = "getModifiedTime",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = Constants.PATH_STRUCT,
                             structPackage = Constants.PACKAGE_PATH)
        ,
        returnType = {
                @ReturnType(type = TypeKind.OBJECT, structType = STRUCT_TYPE_TIME, structPackage = PACKAGE_TIME)
        },
        isPublic = true
)
public class GetModifiedTime extends BlockingNativeCallableUnit {

    private static final Logger log = LoggerFactory.getLogger(GetModifiedTime.class);

    @Override
    public void execute(Context context) {
        BMap<String, BValue> pathStruct = (BMap<String, BValue>) context.getRefArgument(0);
        Path path = (Path) pathStruct.getNativeData(Constants.PATH_DEFINITION_NAME);
        BMap<String, BValue> lastModifiedStruct;
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(lastModified.toString());
            lastModifiedStruct = Utils.createTimeStruct(getTimeZoneStructInfo(context), getTimeStructInfo(context),
                    lastModified.toMillis(), zonedDateTime.getZone().toString());
            context.setReturnValues(lastModifiedStruct);
        } catch (IOException | SecurityException e) {
            String msg;
            if (e instanceof IOException) {
                msg = "Error in reading file: " + path;
            } else {
                msg = "Read permission denied for file: " + path;
            }
            log.error(msg, e);
            context.setReturnValues(BLangVMErrors.createError(context, msg));
        }
    }
}
