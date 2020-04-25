package org.hisp.dhis.tracker.validation.hooks;

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

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventCategoryOptValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Autowired
    protected I18nManager i18nManager;

    @Override
    public int getOrder()
    {
        return 303;
    }

    public EventCategoryOptValidationHook()
    {
        super( Event.class, TrackerImportStrategy.CREATE_AND_UPDATE );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        Program program = context.getProgram( event.getProgram() );
        Objects.requireNonNull( program, Constants.PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( context.getBundle().getUser(), Constants.USER_CANT_BE_NULL );
        Objects.requireNonNull( program, Constants.PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( event, Constants.EVENT_CANT_BE_NULL );

        CategoryOptionCombo categoryOptionCombo = context
            .getCategoryOptionCombo(
                reporter.getValidationContext().getCachedEventCategoryOptionCombo( event.getEvent() ) );

        Objects.requireNonNull( categoryOptionCombo, Constants.CATEGORY_OPTION_COMBO_CANT_BE_NULL );

        if ( categoryOptionCombo.isDefault()
            && program.getCategoryCombo() != null
            && !program.getCategoryCombo().isDefault() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1055 ) );
            return;
        }

        // NOTE: How to best get current date into iso format?
        Date eventDate = DateUtils.parseDate( ObjectUtils
            .firstNonNull( event.getOccurredAt(), event.getScheduledAt(), DateUtils.getIso8601( new Date() ) ) );

        I18nFormat i18nFormat = i18nManager.getI18nFormat();

        for ( CategoryOption option : categoryOptionCombo.getCategoryOptions() )
        {
            if ( option.getStartDate() != null && eventDate.compareTo( option.getStartDate() ) < 0 )
            {
                reporter.addError( newReport( TrackerErrorCode.E1056 )
                    .addArg( i18nFormat.formatDate( eventDate ) )
                    .addArg( i18nFormat.formatDate( option.getStartDate() ) )
                    .addArg( option.getName() ) );
            }

            if ( option.getEndDate() != null && eventDate.compareTo( option.getEndDate() ) > 0 )
            {
                reporter.addError( newReport( TrackerErrorCode.E1057 ).
                    addArg( eventDate )
                    .addArg( option.getEndDate() )
                    .addArg( categoryOptionCombo ) );
            }
        }
    }
}
