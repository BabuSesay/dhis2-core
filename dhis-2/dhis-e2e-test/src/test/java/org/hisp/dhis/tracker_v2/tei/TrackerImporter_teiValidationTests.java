/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.tracker_v2.tei;

import com.google.gson.JsonObject;
import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.tracker_v2.TrackerActions;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsStringIgnoringCase;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImporter_teiValidationTests
    extends ApiTest
{
    private TrackerActions trackerActions;
    private String program = Constants.TRACKER_PROGRAM_ID;
    private String mandatoryAttribute;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();

        new LoginActions().loginAsSuperUser();
    }

    @Test
    public void shouldReturnErrorReportsWhenTeiIncorrect()
    {
        // arrange
        JsonObject trackedEntities = new JsonObjectBuilder()
            .addProperty( "trackedEntityType", "" )
            .addProperty( "orgUnit", Constants.ORG_UNIT_IDS[0] )
            .wrapIntoArray( "trackedEntities" );

        // act
        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        // assert
        response.validateErrorReport()
            .validate()
            .body( "validationReport.errorReports[0].message",
                containsStringIgnoringCase( "Could not find TrackedEntityType" ) );
    }

    @Test
    public void shouldReturnErrorWhenMandatoryAttributesMissing() {
        setupData();
        JsonObject trackedEntities = new JsonObjectBuilder()
            .addProperty( "trackedEntityType", "Q9GufDoplCL" )
            .addProperty( "orgUnit", Constants.ORG_UNIT_IDS[0] )
            //.addArray( "attributes", new JsonObjectBuilder().addProperty( "attribute", mandatoryAttribute ).addProperty( "value", "IAKsAS" ).build() )
            .wrapIntoArray( "trackedEntities" );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( trackedEntities );

        // assert
        response.validateErrorReport();
        //todo add more validation
    }

    private void setupData() {
        JsonObject attribute = JsonObjectBuilder.jsonObject()
            .addProperty( "name", "TA attribute " + DataGenerator.randomEntityName() )
            .addProperty( "valueType", "TEXT" )
            .addProperty( "aggregationType", "NONE" )
            .addProperty( "shortName", "TA attribute" )
            .addUserGroupAccess()
            .build();

        mandatoryAttribute = new RestApiActions( "trackedEntityAttributes" ).create( attribute );
        JsonObject programPayload = new ProgramActions().get(program).getBody();

        programPayload.getAsJsonArray("programTrackedEntityAttributes")
            .add( new JsonObjectBuilder()
                .addProperty( "mandatory", "true" )
                .addObject( "trackedEntityAttribute", new JsonObjectBuilder().addProperty( "id", mandatoryAttribute ) )
            .build())
        ;

        new ProgramActions().update( program, programPayload ).validate().statusCode( 200 );
    }
}