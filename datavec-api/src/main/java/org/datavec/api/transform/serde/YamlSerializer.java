/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

package org.datavec.api.transform.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Serializer used for converting objects (Transforms, Conditions, etc) to YAML format
 *
 * @author Alex Black
 */
public class YamlSerializer extends BaseSerializer {

    private ObjectMapper om;

    public YamlSerializer(){
        this.om = getMapper();
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return om;
    }

    private ObjectMapper getMapper(){
        return getObjectMapper(new YAMLFactory());
    }
}
