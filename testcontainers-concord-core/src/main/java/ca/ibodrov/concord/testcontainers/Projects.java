package ca.ibodrov.concord.testcontainers;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2020 Ivan Bodrov
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.walmartlabs.concord.ApiClient;
import com.walmartlabs.concord.ApiException;
import com.walmartlabs.concord.client.ProjectEntry;
import com.walmartlabs.concord.client.ProjectOperationResponse;
import com.walmartlabs.concord.client.ProjectsApi;

public class Projects {

    private final ProjectsApi projectApi;

    Projects(ApiClient apiClient) {
        this.projectApi = new ProjectsApi(apiClient);
    }

    public ProjectOperationResponse create(String orgName, String projectName) throws ApiException {
        ProjectEntry projectEntry = new ProjectEntry()
                .setName(projectName)
                .setOrgName(orgName)
                .setAcceptsRawPayload(Boolean.TRUE)
                .setVisibility(ProjectEntry.VisibilityEnum.PUBLIC);

        return projectApi.createOrUpdate(orgName, projectEntry);
    }
}