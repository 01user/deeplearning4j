/*
 *
 *  * Copyright 2016 Skymind,Inc.
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
 *
 */
package org.deeplearning4j.arbiter.optimize.ui.resources;

import org.deeplearning4j.arbiter.optimize.ui.components.RenderElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

/**Summary stats: number of completed tasks etc
 */
@Path("/summary")
@Produces(MediaType.APPLICATION_JSON)
public class SummaryStatusResource {
    public static Logger log = LoggerFactory.getLogger(SummaryStatusResource.class);

    private RenderElements renderElements = new RenderElements();

    @GET
    public Response getStatus(){
        log.info("Get with elements: {}",renderElements);
        return Response.ok(renderElements).build();
    }

    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(RenderElements renderElements){
        log.info("Post with new elements: {}",renderElements);
        this.renderElements = renderElements;
        return Response.ok(Collections.singletonMap("status", "ok")).build();
    }

}
